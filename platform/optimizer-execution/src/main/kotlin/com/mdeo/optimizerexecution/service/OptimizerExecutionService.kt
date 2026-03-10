package com.mdeo.optimizerexecution.service

import com.mdeo.common.model.ExecutionState
import com.mdeo.execution.common.routes.FileEntry
import com.mdeo.execution.common.service.ExecutionServiceWithFileTree
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.TypedAst as TransformationTypedAst
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import com.mdeo.modeltransformation.service.GraphToModelDataConverter
import com.mdeo.modeltransformation.service.ModelDataGraphLoader
import com.mdeo.optimizer.OptimizationOrchestrator
import com.mdeo.optimizer.config.*
import com.mdeo.optimizer.rulegen.MutationRuleGenerator
import com.mdeo.optimizer.graph.TinkerGraphBackend
import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.guidance.ScriptGuidanceFunction
import com.mdeo.optimizer.guidance.ScriptModelFactory
import com.mdeo.optimizer.moea.SearchResult
import com.mdeo.optimizer.solution.Solution
import com.mdeo.optimizerexecution.database.OptimizerExecutionsTable
import com.mdeo.optimizerexecution.database.OptimizerResultFilesTable
import com.mdeo.script.ast.TypedAst as ScriptTypedAst
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.CompiledProgram
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.script.runtime.model.ModelDataScriptModel
import com.mdeo.script.runtime.model.ScriptModel
import com.mdeo.optimizer.graph.GraphBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Service for executing optimization runs.
 *
 * Implements the full optimization lifecycle:
 * 1. Fetch all transformation typed ASTs at the start
 * 2. Fetch all script typed ASTs for objectives/constraints at the start
 * 3. Compile all scripts together with metamodel at the start
 * 4. Load the initial model into a graph backend
 * 5. Wire guidance functions, mutation strategies, and the search algorithm
 * 6. Run the evolutionary optimization
 * 7. Store results and update execution state
 *
 * Execution is asynchronous: [createAndStartExecution] returns as soon as the config is
 * parsed and the execution record is created.  The optimization itself runs in a background
 * coroutine bound to [executionScope].
 *
 * @param apiClient Client for backend API communication
 * @param timeoutMs Overall execution timeout in milliseconds
 * @param executionScope Scope in which background execution coroutines are launched
 */
class OptimizerExecutionService(
    private val apiClient: OptimizerApiClient,
    private val timeoutMs: Long,
    private val executionScope: CoroutineScope
) : ExecutionServiceWithFileTree {
    private val logger = LoggerFactory.getLogger(OptimizerExecutionService::class.java)
    private val scriptCompiler = ScriptCompiler()
    private val graphLoader = ModelDataGraphLoader()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    companion object {
        private const val MAX_PATH_LENGTH = 1000
        private const val RESULTS_DIR = "results"
        private const val SUMMARY_FILE = "summary.md"
        private const val MIME_TYPE_JSON = "application/json"
        private const val MIME_TYPE_MARKDOWN = "text/markdown"
    }

    override suspend fun createAndStartExecution(
        executionId: UUID,
        projectId: UUID,
        filePath: String,
        data: JsonElement,
        jwtToken: String
    ): String = withContext(Dispatchers.IO) {
        val config = parseOptimizationConfig(data)
        validateConfig(config)

        createExecutionRecord(executionId, projectId, filePath, data)
        logger.info("Created optimizer execution $executionId for project $projectId")

        executionScope.launch(Dispatchers.Default) {
            try {
                withTimeout(timeoutMs) {
                    runOptimization(executionId, projectId, config, jwtToken)
                }
            } catch (e: TimeoutCancellationException) {
                handleTimeout(executionId, jwtToken)
            } catch (e: Exception) {
                handleUnexpectedError(executionId, e, jwtToken)
            }
        }

        "Optimization: ${config.solver.algorithm} on ${config.problem.modelPath}"
    }

    override suspend fun cancelExecution(executionId: UUID) = withContext(Dispatchers.IO) {
        transaction {
            OptimizerExecutionsTable.update({
                OptimizerExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[state] = ExecutionState.CANCELLED
                it[progress] = "Cancelled by user"
                it[completedAt] = Instant.now()
            }
        }
        logger.info("Cancelled optimizer execution $executionId")
    }

    override suspend fun deleteExecution(executionId: UUID) = withContext(Dispatchers.IO) {
        transaction {
            OptimizerResultFilesTable.deleteWhere {
                OptimizerResultFilesTable.executionId eq executionId.toKotlinUuid()
            }
            OptimizerExecutionsTable.deleteWhere { id eq executionId.toKotlinUuid() }
        }
        logger.info("Deleted optimizer execution $executionId")
    }

    override suspend fun getSummary(executionId: UUID): String? = withContext(Dispatchers.IO) {
        val execution = findExecution(executionId) ?: return@withContext null
        buildSummary(execution)
    }

    override suspend fun getFileTree(executionId: UUID, path: String?): List<FileEntry>? {
        return withContext(Dispatchers.IO) {
            findExecution(executionId) ?: return@withContext null

            val files = transaction {
                OptimizerResultFilesTable.selectAll()
                    .where { OptimizerResultFilesTable.executionId eq executionId.toKotlinUuid() }
                    .map { row ->
                        FileEntry(row[OptimizerResultFilesTable.filePath], FileEntry.TYPE_FILE)
                    }
                    .filter { it.name != SUMMARY_FILE }
            }

            if (path.isNullOrBlank()) files
            else files.filter { it.name.startsWith(path) }
        }
    }

    override suspend fun getFileContents(executionId: UUID, filePath: String): String? {
        return withContext(Dispatchers.IO) {
            transaction {
                OptimizerResultFilesTable.selectAll()
                    .where {
                        (OptimizerResultFilesTable.executionId eq executionId.toKotlinUuid()) and
                            (OptimizerResultFilesTable.filePath eq filePath)
                    }
                    .firstOrNull()
                    ?.get(OptimizerResultFilesTable.content)
            }
        }
    }

    // ========================= Execution pipeline =========================

    /**
     * Full optimization pipeline: fetches metamodel, model, transformation ASTs, script ASTs,
     * compiles scripts, wires guidance functions, and runs the optimizer.
     * Updates execution state at each phase; sets state to FAILED on any error.
     *
     * @param executionId The execution record to drive.
     * @param projectId Project context for all API calls.
     * @param config Parsed optimization configuration.
     * @param jwtToken Bearer token for backend API authentication.
     */
    private suspend fun runOptimization(
        executionId: UUID,
        projectId: UUID,
        config: OptimizationConfig,
        jwtToken: String
    ) {
        // --- Phase 1: Fetch metamodel ---
        updateState(executionId, ExecutionState.INITIALIZING, "Fetching metamodel...", jwtToken)
        val metamodelData = fetchMetamodelData(executionId, projectId, config.problem.metamodelPath, jwtToken)
            ?: return

        // --- Phase 2: Fetch model ---
        updateState(executionId, ExecutionState.INITIALIZING, "Fetching model...", jwtToken)
        val modelData = fetchModelData(executionId, projectId, config.problem.modelPath, jwtToken)
            ?: return

        // --- Phase 3: Fetch all transformation typed ASTs ---
        updateState(
            executionId, ExecutionState.INITIALIZING,
            "Fetching ${config.search.mutations.usingPaths.size} transformation(s)...", jwtToken
        )
        val fetchedTransformations = fetchAllTransformations(
            executionId, projectId, config.search.mutations.usingPaths, jwtToken
        ) ?: return

        // --- Phase 3b: Auto-generate mutation operators from metamodel specs ---
        val transformations: Map<String, TransformationTypedAst> =
            if (config.search.mutations.generate.isNotEmpty()) {
                val generated = MutationRuleGenerator.generate(
                    metamodelData = metamodelData,
                    specs = config.search.mutations.generate,
                    metamodelPath = config.problem.metamodelPath
                )
                logger.info(
                    "Auto-generated {} mutation rule(s) from {} spec(s)",
                    generated.size, config.search.mutations.generate.size
                )
                updateState(
                    executionId, ExecutionState.INITIALIZING,
                    "Auto-generated ${generated.size} mutation rule(s)...", jwtToken
                )
                // Merge: hand-written transformations take precedence over generated ones
                val merged = LinkedHashMap<String, TransformationTypedAst>()
                generated.forEach { m -> merged[m.name] = m.typedAst }
                merged.putAll(fetchedTransformations)
                merged
            } else {
                fetchedTransformations
            }

        // --- Phase 4: Fetch all script typed ASTs for objectives/constraints ---
        val scriptPaths = collectScriptPaths(config.goal)
        updateState(
            executionId, ExecutionState.INITIALIZING,
            "Fetching ${scriptPaths.size} script file(s)...", jwtToken
        )
        val scriptAsts = fetchAllScripts(executionId, projectId, scriptPaths, jwtToken)
            ?: return

        // --- Phase 5: Compile all scripts together ---
        updateState(
            executionId, ExecutionState.INITIALIZING,
            "Compiling ${scriptAsts.size} script file(s)...", jwtToken
        )
        val compiledProgram = compileScripts(executionId, scriptAsts, metamodelData, jwtToken)
            ?: return

        // --- Phase 6: Build guidance functions ---
        val environment = ExecutionEnvironment(compiledProgram)
        val scriptModelFactory = createScriptModelFactory(metamodelData, modelData, environment)

        val objectives = config.goal.objectives.map { obj ->
            ScriptGuidanceFunction(environment, obj.path, obj.functionName, scriptModelFactory)
        }
        val constraints = config.goal.constraints.map { con ->
            ScriptGuidanceFunction(environment, con.path, con.functionName, scriptModelFactory)
        }

        // --- Phase 7: Create initial solution provider ---
        val initialSolutionProvider = createInitialSolutionProvider(
            modelData, metamodelData
        )

        // --- Phase 8: Run optimizer ---
        updateState(
            executionId, ExecutionState.RUNNING,
            "Running ${config.solver.algorithm} optimizer...", jwtToken
        )

        val orchestrator = OptimizationOrchestrator(
            config = config,
            objectives = objectives,
            constraints = constraints,
            transformations = transformations,
            metamodelData = metamodelData,
            initialSolutionProvider = initialSolutionProvider
        )

        val result = try {
            orchestrator.run { generation ->
                val approxEvaluations = generation * config.solver.parameters.population
                updateState(
                    executionId,
                    ExecutionState.RUNNING,
                    "Generation $generation (~$approxEvaluations evaluations)",
                    jwtToken
                )
                checkCancelled(executionId)
            }
        } catch (e: CancellationException) {
            logger.info("Optimizer execution $executionId stopped: ${e.message}")
            return
        } catch (e: Exception) {
            logger.error("Optimization failed", e)
            updateState(executionId, ExecutionState.FAILED, "Optimization failed: ${e.message}", jwtToken)
            return
        }

        // --- Phase 9: Store results ---
        storeResults(executionId, result, metamodelData, modelData.metamodelUri)
        updateState(executionId, ExecutionState.COMPLETED, "Completed successfully", jwtToken)
        logger.info("Optimizer execution $executionId completed")
    }

    // ========================= Data fetching =========================

    /**
     * Fetches metamodel data from the backend API.
     * Sets execution state to FAILED and returns null when the fetch fails.
     *
     * @param executionId Execution to update on failure.
     * @param projectId Project context.
     * @param metamodelPath Path to the metamodel file.
     * @param jwtToken Authentication token.
     * @return The metamodel data, or `null` on failure.
     */
    private suspend fun fetchMetamodelData(
        executionId: UUID, projectId: UUID, metamodelPath: String, jwtToken: String
    ): MetamodelData? {
        val data = apiClient.getMetamodelData(projectId.toString(), metamodelPath, jwtToken)
        if (data == null) {
            updateState(executionId, ExecutionState.FAILED, "Failed to fetch metamodel: $metamodelPath", jwtToken)
        }
        return data
    }

    /**
     * Fetches model data from the backend API.
     * Sets execution state to FAILED and returns null when the fetch fails.
     *
     * @param executionId Execution to update on failure.
     * @param projectId Project context.
     * @param modelPath Path to the model file.
     * @param jwtToken Authentication token.
     * @return The model data, or `null` on failure.
     */
    private suspend fun fetchModelData(
        executionId: UUID, projectId: UUID, modelPath: String, jwtToken: String
    ): ModelData? {
        val data = apiClient.getModelData(projectId.toString(), modelPath, jwtToken)
        if (data == null) {
            updateState(executionId, ExecutionState.FAILED, "Failed to fetch model: $modelPath", jwtToken)
        }
        return data
    }

    /**
     * Fetches all transformation typed ASTs for the given paths from the backend API.
     * Fails fast: returns null and sets execution state to FAILED if any path cannot be resolved.
     *
     * @param executionId Execution to update on failure.
     * @param projectId Project context.
     * @param usingPaths Ordered list of transformation file paths to fetch.
     * @param jwtToken Authentication token.
     * @return Map from path to typed AST, or `null` if any fetch failed.
     */
    private suspend fun fetchAllTransformations(
        executionId: UUID,
        projectId: UUID,
        usingPaths: List<String>,
        jwtToken: String
    ): Map<String, TransformationTypedAst>? {
        val result = mutableMapOf<String, TransformationTypedAst>()
        for (path in usingPaths) {
            val ast = apiClient.getTransformationTypedAst(projectId.toString(), path, jwtToken)
            if (ast == null) {
                updateState(
                    executionId, ExecutionState.FAILED,
                    "Failed to fetch transformation: $path", jwtToken
                )
                return null
            }
            result[path] = ast
        }
        return result
    }

    /**
     * Fetches all script typed ASTs for the given paths from the backend API.
     * Fails fast: returns null and sets execution state to FAILED if any path cannot be resolved.
     *
     * @param executionId Execution to update on failure.
     * @param projectId Project context.
     * @param scriptPaths Set of script file paths to fetch.
     * @param jwtToken Authentication token.
     * @return Map from path to typed AST, or `null` if any fetch failed.
     */
    private suspend fun fetchAllScripts(
        executionId: UUID,
        projectId: UUID,
        scriptPaths: Set<String>,
        jwtToken: String
    ): Map<String, ScriptTypedAst>? {
        val result = mutableMapOf<String, ScriptTypedAst>()
        for (path in scriptPaths) {
            val ast = apiClient.getScriptTypedAst(projectId.toString(), path, jwtToken)
            if (ast == null) {
                updateState(
                    executionId, ExecutionState.FAILED,
                    "Failed to fetch script: $path", jwtToken
                )
                return null
            }
            result[path] = ast
        }
        return result
    }

    /**
     * Collects the unique set of script file paths referenced by objective and constraint configs.
     *
     * @param goal The goal configuration containing objectives and constraints.
     * @return Deduplicated set of script file paths.
     */
    private fun collectScriptPaths(goal: GoalConfig): Set<String> {
        val paths = mutableSetOf<String>()
        goal.objectives.forEach { paths.add(it.path) }
        goal.constraints.forEach { paths.add(it.path) }
        return paths
    }

    // ========================= Script compilation =========================

    /**
     * Compiles all fetched script ASTs together with the metamodel.
     * Sets execution state to FAILED and returns null if compilation throws.
     *
     * @param executionId Execution to update on failure.
     * @param scriptAsts Map of path → typed AST for all scripts.
     * @param metamodelData Metamodel used during compilation.
     * @param jwtToken Authentication token for error reporting.
     * @return The compiled program, or `null` on compile error.
     */
    private suspend fun compileScripts(
        executionId: UUID,
        scriptAsts: Map<String, ScriptTypedAst>,
        metamodelData: MetamodelData,
        jwtToken: String
    ): CompiledProgram? {
        return try {
            val input = CompilationInput(scriptAsts)
            scriptCompiler.compile(input, metamodelData)
        } catch (e: Exception) {
            logger.error("Script compilation failed", e)
            updateState(
                executionId, ExecutionState.FAILED,
                "Script compilation error: ${e.message}", jwtToken
            )
            null
        }
    }

    // ========================= Solution bootstrapping =========================

    /**
     * Creates a factory that produces fresh initial [Solution] instances by loading
     * the [modelData] into new [TinkerGraphBackend]s.
     */
    private fun createInitialSolutionProvider(
        modelData: ModelData,
        metamodelData: MetamodelData
    ): () -> Solution {
        return {
            val backend = TinkerGraphBackend()
            val g = backend.traversal()
            val nameRegistry = InstanceNameRegistry()
            graphLoader.load(g, modelData, nameRegistry, metamodelData)
            Solution(backend)
        }
    }

    /**
     * Creates a [ScriptModelFactory] that converts graph state to [ScriptModel]
     * for use by guidance functions (objectives/constraints).
     *
     * Uses the eager conversion strategy: graph → ModelData → ModelDataScriptModel.
     */
    private fun createScriptModelFactory(
        metamodelData: MetamodelData,
        modelData: ModelData,
        environment: ExecutionEnvironment
    ): ScriptModelFactory {
        return object : ScriptModelFactory {
            override fun create(graphBackend: GraphBackend): ScriptModel {
                val g = graphBackend.traversal()
                val nameRegistry = buildNameRegistryFromGraph(g)
                val converter = GraphToModelDataConverter(
                    metamodelData = metamodelData,
                    types = emptyList(),
                    typeRegistry = TypeRegistry.GLOBAL
                )
                val convertedModelData = converter.convert(g, modelData.metamodelUri, nameRegistry)
                return ModelDataScriptModel(
                    modelData = convertedModelData,
                    metamodelData = metamodelData,
                    classLoader = environment.classLoader,
                    program = environment.program
                )
            }
        }
    }

    /**
     * Builds an [InstanceNameRegistry] from the current graph state.
     * Assigns names based on vertex labels and IDs.
     */
    private fun buildNameRegistryFromGraph(g: GraphTraversalSource): InstanceNameRegistry {
        val registry = InstanceNameRegistry()
        g.V().toList().forEach { vertex ->
            val baseName = "${vertex.label()}${vertex.id()}"
            registry.registerWithUniqueName(vertex.id(), baseName)
        }
        return registry
    }

    // ========================= Result storage =========================

    /**
     * Serialises all Pareto-front solutions and a markdown summary, then persists them
     * in the result-files table.
     *
     * @param executionId The execution whose results are being stored.
     * @param result The completed search result containing the final population.
     * @param metamodelData Metamodel used to convert graph state back to model data.
     * @param metamodelUri URI of the metamodel, embedded in each serialised model.
     */
    private fun storeResults(
        executionId: UUID,
        result: SearchResult,
        metamodelData: MetamodelData,
        metamodelUri: String
    ) {
        val solutions = result.getFinalSolutions()
        val solutionGraphs = result.getFinalSolutionGraphs()
        val summaryContent = buildResultSummary(solutions)

        // Convert each solution's graph to ModelData JSON outside the transaction
        val converter = GraphToModelDataConverter(
            metamodelData = metamodelData,
            types = emptyList(),
            typeRegistry = TypeRegistry.GLOBAL
        )
        val solutionModelFiles = solutionGraphs.map { solution ->
            val g = solution.graphBackend.traversal()
            val nameRegistry = buildNameRegistryFromGraph(g)
            val modelData = converter.convert(g, metamodelUri, nameRegistry)
            json.encodeToString(modelData)
        }

        transaction {
            // Store summary
            OptimizerResultFilesTable.insert {
                it[id] = Uuid.random()
                it[OptimizerResultFilesTable.executionId] = executionId.toKotlinUuid()
                it[filePath] = SUMMARY_FILE
                it[content] = summaryContent
                it[mimeType] = MIME_TYPE_MARKDOWN
            }

            // Store each Pareto-front solution as a model file
            solutionModelFiles.forEachIndexed { index, jsonContent ->
                OptimizerResultFilesTable.insert {
                    it[id] = Uuid.random()
                    it[OptimizerResultFilesTable.executionId] = executionId.toKotlinUuid()
                    it[filePath] = "$RESULTS_DIR/solution_$index.m_gen"
                    it[content] = jsonContent
                    it[mimeType] = MIME_TYPE_JSON
                }
            }
        }
    }

    /**
     * Builds a markdown result summary table for the given Pareto-front solutions.
     *
     * @param solutions List of solution results from the final population.
     * @return Markdown-formatted summary string.
     */
    private fun buildResultSummary(
        solutions: List<com.mdeo.optimizer.moea.SolutionResult>
    ): String {
        return buildString {
            append("## Optimization Results\n\n")
            append("**Solutions found:** ${solutions.size}\n\n")

            if (solutions.isNotEmpty()) {
                append("### Pareto Front\n\n")
                append("| Solution | Objectives | Constraints |\n")
                append("|----------|-----------|-------------|\n")
                solutions.forEachIndexed { index, sol ->
                    val objStr = sol.objectives.joinToString(", ") { "%.4f".format(it) }
                    val conStr = if (sol.constraints.isEmpty()) "—"
                    else sol.constraints.joinToString(", ") { "%.4f".format(it) }
                    append("| $index | $objStr | $conStr |\n")
                }

                append("\n### Solution Models\n\n")
                solutions.indices.forEach { index ->
                    append("![Solution $index]($RESULTS_DIR/solution_$index.m_gen)\n")
                }
            }
        }
    }

    // ========================= Config parsing =========================

    /**
     * Deserialises the raw JSON request payload into an [OptimizationConfig].
     *
     * @param data The JSON element from the execution request.
     * @return The parsed [OptimizationConfig].
     */
    private fun parseOptimizationConfig(data: JsonElement): OptimizationConfig {
        return json.decodeFromJsonElement(OptimizationConfig.serializer(), data)
    }

    /**
     * Validates the parsed configuration, throwing [IllegalArgumentException] on any violation.
     *
     * @param config The configuration to validate.
     */
    private fun validateConfig(config: OptimizationConfig) {
        require(config.problem.metamodelPath.isNotBlank()) { "metamodelPath cannot be empty" }
        require(config.problem.modelPath.isNotBlank()) { "modelPath cannot be empty" }
        require(config.goal.objectives.isNotEmpty()) { "At least one objective is required" }
        require(config.search.mutations.usingPaths.isNotEmpty() || config.search.mutations.generate.isNotEmpty()) {
            "At least one mutation source is required: specify usingPaths or generate in search.mutations"
        }
        require(!config.problem.metamodelPath.contains("..")) { "Path traversal not allowed" }
        require(!config.problem.modelPath.contains("..")) { "Path traversal not allowed" }
    }

    // ========================= State management =========================

    /**
     * Inserts a new execution record in the database with state [ExecutionState.SUBMITTED].
     *
     * @param executionId Unique identifier for this execution.
     * @param projectId Project that owns the execution.
     * @param filePath Path to the config file that triggered the execution.
     * @param data The raw JSON config data, stored for debugging.
     */
    private fun createExecutionRecord(
        executionId: UUID, projectId: UUID, filePath: String, data: JsonElement
    ) {
        val now = Instant.now()
        transaction {
            OptimizerExecutionsTable.insert {
                it[id] = executionId.toKotlinUuid()
                it[OptimizerExecutionsTable.projectId] = projectId.toKotlinUuid()
                it[configPath] = filePath
                it[state] = ExecutionState.SUBMITTED
                it[progress] = null
                it[createdAt] = now
                it[startedAt] = null
                it[completedAt] = null
                it[configData] = data.toString()
            }
        }
    }

    /**
     * Updates the execution state in the database and notifies the backend progress API.
     * Also sets [startedAt] or [completedAt] timestamps as appropriate.
     *
     * @param executionId The execution to update.
     * @param state New [ExecutionState] string.
     * @param progressText Human-readable progress message, or `null` to clear.
     * @param jwtToken Authentication token for the backend API call.
     */
    private suspend fun updateState(
        executionId: UUID, state: String, progressText: String?, jwtToken: String
    ) {
        val now = Instant.now()
        transaction {
            OptimizerExecutionsTable.update({
                OptimizerExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[OptimizerExecutionsTable.state] = state
                it[progress] = progressText
                when (state) {
                    ExecutionState.INITIALIZING, ExecutionState.RUNNING ->
                        it[startedAt] = now
                    ExecutionState.COMPLETED, ExecutionState.FAILED, ExecutionState.CANCELLED ->
                        it[completedAt] = now
                }
            }
        }
        apiClient.updateExecutionState(executionId.toString(), state, progressText, jwtToken)
    }

    /**
     * Sets the execution state to FAILED with a timeout message.
     *
     * @param executionId The timed-out execution.
     * @param jwtToken Authentication token for the state update API call.
     */
    private suspend fun handleTimeout(executionId: UUID, jwtToken: String) {
        logger.error("Optimizer execution timeout after ${timeoutMs}ms")
        updateState(
            executionId, ExecutionState.FAILED,
            "Execution timeout: Optimization exceeded maximum time of ${timeoutMs}ms", jwtToken
        )
    }

    /**
     * Checks whether the execution was cancelled or deleted since the last generation.
     * Throws [kotlinx.coroutines.CancellationException] to abort the optimization loop when
     * the execution record is missing (deleted) or its state is [ExecutionState.CANCELLED].
     *
     * @param executionId The execution to inspect.
     */
    private suspend fun checkCancelled(executionId: UUID) {
        val state = withContext(Dispatchers.IO) {
            transaction {
                OptimizerExecutionsTable.selectAll()
                    .where { OptimizerExecutionsTable.id eq executionId.toKotlinUuid() }
                    .firstOrNull()
                    ?.get(OptimizerExecutionsTable.state)
            }
        }
        if (state == null || state == ExecutionState.CANCELLED) {
            logger.info("Optimizer execution $executionId was cancelled or deleted — stopping")
            throw kotlinx.coroutines.CancellationException("Optimization was cancelled or deleted")
        }
    }

    /**
     * Persists the exception message and sets the execution state to FAILED.
     *
     * @param executionId The failed execution.
     * @param e The unexpected exception.
     * @param jwtToken Authentication token for the state update API call.
     */
    private suspend fun handleUnexpectedError(executionId: UUID, e: Exception, jwtToken: String) {
        logger.error("Unexpected error during optimization", e)
        transaction {
            OptimizerExecutionsTable.update({
                OptimizerExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[error] = e.message
            }
        }
        updateState(
            executionId, ExecutionState.FAILED,
            "Unexpected error: ${e.message}", jwtToken
        )
    }

    /**
     * Looks up an execution record by its ID.
     *
     * @param executionId The execution to look up.
     * @return The database row, or `null` if not found.
     */
    private fun findExecution(executionId: UUID) = transaction {
        OptimizerExecutionsTable.selectAll()
            .where { OptimizerExecutionsTable.id eq executionId.toKotlinUuid() }
            .firstOrNull()
    }

    /**
     * Builds a markdown summary for the given execution record.
     * Returns stored summary markdown for completed executions, or a short failure/state string.
     *
     * @param execution The database row for the execution.
     * @return Markdown-formatted summary string.
     */
    private fun buildSummary(execution: org.jetbrains.exposed.v1.core.ResultRow): String {
        val state = execution[OptimizerExecutionsTable.state]
        val error = execution[OptimizerExecutionsTable.error]

        return when (state) {
            ExecutionState.COMPLETED -> {
                // Return the stored summary markdown
                val summaryContent = transaction {
                    OptimizerResultFilesTable.selectAll()
                        .where {
                            (OptimizerResultFilesTable.executionId eq
                                execution[OptimizerExecutionsTable.id]) and
                                (OptimizerResultFilesTable.filePath eq SUMMARY_FILE)
                        }
                        .firstOrNull()
                        ?.get(OptimizerResultFilesTable.content)
                }
                summaryContent ?: "## Optimization Completed\n\nNo summary available."
            }
            ExecutionState.FAILED -> {
                buildString {
                    append("## Optimization Failed\n\n")
                    append(error ?: "Unknown error")
                }
            }
            else -> "Execution is in state: $state"
        }
    }
}
