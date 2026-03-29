package com.mdeo.optimizerexecution.worker

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.SerializedModel
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.ast.TypedAst as TransformationTypedAst
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer as TransformationExpressionSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import com.mdeo.modeltransformation.graph.MdeoModelGraph
import com.mdeo.optimizer.config.GoalConfig
import com.mdeo.optimizer.evaluation.LocalMutationEvaluator
import com.mdeo.optimizer.evaluation.MutationTask
import com.mdeo.optimizer.evaluation.NodeBatch
import com.mdeo.optimizer.evaluation.SolutionImportData
import com.mdeo.optimizer.evaluation.WorkerSolutionRef
import com.mdeo.optimizer.guidance.ScriptGuidanceFunction
import com.mdeo.optimizer.operators.MutationStrategyFactory
import com.mdeo.optimizer.solution.Solution
import com.mdeo.optimizer.worker.*
import com.mdeo.script.ast.TypedAst as ScriptTypedAst
import com.mdeo.script.ast.expressions.TypedExpressionSerializer as ScriptExpressionSerializer
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.runtime.ExecutionEnvironment
import io.ktor.websocket.*
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Service that manages worker-side execution state for multi-node optimization.
 *
 * Each optimization execution is tracked as a [WorkerExecutionState] keyed by execution
 * identifier. The service handles the full worker lifecycle: allocation (resource setup
 * and initial solution generation), per-generation work batches (imports + mutations +
 * discards in one message), solution data retrieval, and cleanup.
 *
 * @param workerThreads Number of threads to allocate per execution for parallel evaluation.
 */
class WorkerService(private val workerThreads: Int) {

    private val logger = LoggerFactory.getLogger(WorkerService::class.java)
    private val executions = ConcurrentHashMap<String, WorkerExecutionState>()
    private val scriptCompiler = ScriptCompiler()

    @OptIn(ExperimentalSerializationApi::class)
    private val cbor = Cbor { ignoreUnknownKeys = true }

    private val transformationJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TransformationExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }

    private val scriptJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, ScriptExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }

    /**
     * Allocates resources for a new optimization execution on this worker.
     *
     * Deserializes the metamodel, transformation ASTs, and script ASTs from the request,
     * compiles scripts into guidance functions, creates the mutation strategy and local
     * evaluator, generates initial solutions, and stores the execution state.
     *
     * @param request The allocation request containing all resources and configuration.
     * @return The allocation response containing initial solution fitness data.
     */
    suspend fun allocate(request: WorkerAllocationRequest): WorkerAllocationResponse {
        logger.info("Allocating execution {} with {} initial solutions", request.executionId, request.initialSolutionCount)

        val metamodel = Metamodel.compile(request.metamodelData)

        val transformations = try {
            deserializeTransformations(request.transformationAstJsons)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to deserialize transformation ASTs: ${e.message}", e)
        }

        val scriptAsts = try {
            deserializeScripts(request.scriptAstJsons)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to deserialize script ASTs: ${e.message}", e)
        }

        val compiledProgram = try {
            compileScripts(scriptAsts, request.metamodelData)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to compile scripts: ${e.message}", e)
        }

        val environment = ExecutionEnvironment(compiledProgram)

        val objectives = buildObjectiveFunctions(environment, compiledProgram, request.goalConfig)
        val constraints = buildConstraintFunctions(environment, compiledProgram, request.goalConfig)
        val mutationStrategy = MutationStrategyFactory.create(request.solverConfig.parameters.mutation, transformations)
        val initialSolutionProvider = createInitialSolutionProvider(request.initialModelData, metamodel)

        val evaluator = LocalMutationEvaluator(
            initialSolutionProvider = initialSolutionProvider,
            mutationStrategy = mutationStrategy,
            objectives = objectives,
            constraints = constraints,
            metamodel = metamodel
        )

        val initialResults = evaluator.initialize(request.initialSolutionCount)
        val dispatcher = Executors.newFixedThreadPool(workerThreads).asCoroutineDispatcher()
        executions[request.executionId] = WorkerExecutionState(evaluator, dispatcher)

        logger.info("Execution {} allocated with {} initial solutions", request.executionId, initialResults.size)

        return WorkerAllocationResponse(
            initialSolutions = initialResults.map { result ->
                InitialSolutionData(
                    solutionId = result.solutionId,
                    objectives = result.objectives,
                    constraints = result.constraints
                )
            }
        )
    }

    /**
     * Retrieves the serialized model for a specific solution.
     *
     * @param executionId The execution identifier.
     * @param solutionId The solution identifier to retrieve.
     * @return The [SerializedModel] for the solution.
     * @throws IllegalArgumentException if the execution or solution does not exist.
     */
    suspend fun getSolutionData(executionId: String, solutionId: String): SerializedModel {
        val state = requireExecution(executionId)
        val ref = WorkerSolutionRef(nodeId = "local", solutionId = solutionId)
        return state.evaluator.getSolutionData(ref)
    }

    /**
     * Cleans up all resources for the specified execution and removes it from the map.
     *
     * @param executionId The execution identifier to clean up.
     * @throws IllegalArgumentException if the execution does not exist.
     */
    suspend fun cleanup(executionId: String) {
        val state = executions.remove(executionId)
            ?: throw IllegalArgumentException("Execution not found: $executionId")
        state.suspendClose()
        logger.info("Execution {} cleaned up", executionId)
    }

    /**
     * Deserializes transformation AST JSON strings into typed AST objects.
     *
     * @param astJsons Map of transformation path to JSON-serialized typed AST.
     * @return Map of transformation path to deserialized [TransformationTypedAst].
     */
    private fun deserializeTransformations(astJsons: Map<String, String>): Map<String, TransformationTypedAst> {
        return astJsons.mapValues { (_, json) ->
            transformationJson.decodeFromString<TransformationTypedAst>(json)
        }
    }

    /**
     * Deserializes script AST JSON strings into typed AST objects.
     *
     * @param astJsons Map of script path to JSON-serialized typed AST.
     * @return Map of script path to deserialized [ScriptTypedAst].
     */
    private fun deserializeScripts(astJsons: Map<String, String>): Map<String, ScriptTypedAst> {
        return astJsons.mapValues { (_, json) ->
            scriptJson.decodeFromString<ScriptTypedAst>(json)
        }
    }

    /**
     * Compiles all script ASTs into a single program using the script compiler.
     *
     * @param scriptAsts Map of file path to typed AST.
     * @param metamodelData The metamodel data for type-safe model access during compilation.
     * @return The compiled program containing generated bytecode.
     */
    private fun compileScripts(
        scriptAsts: Map<String, ScriptTypedAst>,
        metamodelData: com.mdeo.metamodel.data.MetamodelData
    ): com.mdeo.script.compiler.CompiledProgram {
        val input = CompilationInput(scriptAsts)
        return scriptCompiler.compile(input, metamodelData)
    }

    /**
     * Builds objective guidance functions from the compiled program and goal config.
     *
     * @param environment The script execution environment with the compiled class.
     * @param compiledProgram The compiled program containing function lookup tables.
     * @param goalConfig The goal configuration specifying objectives.
     * @return A list of [ScriptGuidanceFunction] instances for each objective.
     */
    private fun buildObjectiveFunctions(
        environment: ExecutionEnvironment,
        compiledProgram: com.mdeo.script.compiler.CompiledProgram,
        goalConfig: GoalConfig
    ): List<ScriptGuidanceFunction> {
        val clazz = environment.scriptProgramClass
        return goalConfig.objectives.map { obj ->
            val jvmMethodName = compiledProgram.functionLookup[obj.path]?.get(obj.functionName)
                ?: error("Function '${obj.functionName}' not found in '${obj.path}'")
            ScriptGuidanceFunction(clazz, jvmMethodName, System.out, "${obj.path}::${obj.functionName}")
        }
    }

    /**
     * Builds constraint guidance functions from the compiled program and goal config.
     *
     * @param environment The script execution environment with the compiled class.
     * @param compiledProgram The compiled program containing function lookup tables.
     * @param goalConfig The goal configuration specifying constraints.
     * @return A list of [ScriptGuidanceFunction] instances for each constraint.
     */
    private fun buildConstraintFunctions(
        environment: ExecutionEnvironment,
        compiledProgram: com.mdeo.script.compiler.CompiledProgram,
        goalConfig: GoalConfig
    ): List<ScriptGuidanceFunction> {
        val clazz = environment.scriptProgramClass
        return goalConfig.constraints.map { con ->
            val jvmMethodName = compiledProgram.functionLookup[con.path]?.get(con.functionName)
                ?: error("Function '${con.functionName}' not found in '${con.path}'")
            ScriptGuidanceFunction(clazz, jvmMethodName, System.out, "${con.path}::${con.functionName}")
        }
    }

    /**
     * Creates a factory that produces fresh initial [Solution] instances from model data.
     *
     * @param modelData The seed model data from which to create solutions.
     * @param metamodel The compiled metamodel governing model structure.
     * @return A factory function producing new [Solution] instances.
     */
    private fun createInitialSolutionProvider(
        modelData: ModelData,
        metamodel: Metamodel
    ): () -> Solution {
        return {
            val modelGraph = MdeoModelGraph.create(modelData, metamodel)
            Solution(modelGraph)
        }
    }

    /**
     * Retrieves the execution state for the given identifier or throws.
     *
     * @param executionId The execution identifier to look up.
     * @return The [WorkerExecutionState] for the execution.
     * @throws IllegalArgumentException if no execution exists for the given identifier.
     */
    private fun requireExecution(executionId: String): WorkerExecutionState {
        return executions[executionId]
            ?: throw IllegalArgumentException("Execution not found: $executionId")
    }

    /**
     * Internal state for a single worker-side optimization execution.
     *
     * Holds the local evaluator and the dedicated thread pool dispatcher for
     * parallel mutation+evaluation work.
     *
     * @param evaluator The local mutation evaluator managing solutions.
     * @param dispatcher The coroutine dispatcher backed by a dedicated thread pool.
     */
    private class WorkerExecutionState(
        val evaluator: LocalMutationEvaluator,
        val dispatcher: ExecutorCoroutineDispatcher
    ) : AutoCloseable {

        /**
         * Releases the evaluator and shuts down the thread pool dispatcher.
         */
        suspend fun suspendClose() {
            evaluator.cleanup()
            dispatcher.close()
        }

        override fun close() {
            dispatcher.close()
        }
    }

    // ─── WebSocket session handling ───────────────────────────────────────────────

    /**
     * Services a long-lived WebSocket connection from the orchestrator.
     *
     * The session remains open for the full duration of the execution. Incoming binary
     * frames are decoded as CBOR [WorkerWsMessage] values and dispatched concurrently;
     * each handler sends its response back on the same session before returning.
     * A [supervisorScope] ensures that one failing message handler does not tear down
     * handling for subsequent messages.
     *
     * @param executionId The execution this session is associated with.
     * @param session The Ktor WebSocket session opened by the orchestrator.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun handleOrchestratorSession(executionId: String, session: DefaultWebSocketSession) {
        logger.info("Orchestrator WebSocket connected for execution {}", executionId)
        try {
            supervisorScope {
                for (frame in session.incoming) {
                    if (frame !is Frame.Binary) continue
                    val bytes = frame.readBytes()
                    val msg = try {
                        cbor.decodeFromByteArray<WorkerWsMessage>(bytes)
                    } catch (e: Exception) {
                        logger.warn("Ignoring malformed WS frame for execution {}: {}", executionId, e.message)
                        continue
                    }
                    launch {
                        val responses = handleOrchestratorMessage(executionId, msg)
                        for (response in responses) {
                            session.send(Frame.Binary(true, cbor.encodeToByteArray<WorkerWsMessage>(response)))
                        }
                    }
                }
            }
        } finally {
            logger.info("Orchestrator WebSocket disconnected for execution {}", executionId)
        }
    }

    /**
     * Handles a single message received from the orchestrator WebSocket session.
     *
     * @param executionId The execution context.
     * @param msg The decoded message to process.
     * @return The response messages to send back (may be empty for unrecognised messages).
     */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun handleOrchestratorMessage(executionId: String, msg: WorkerWsMessage): List<WorkerWsMessage> =
        when (msg) {
            is NodeWorkBatchRequest -> listOf(handleNodeWorkBatch(executionId, msg))
            is SolutionFetchRequest -> {
                val serializedModel = getSolutionData(executionId, msg.solutionId)
                listOf(SolutionFetchResponse(msg.requestId, msg.solutionId, serializedModel))
            }
            is SolutionBatchFetchRequest -> handleBatchFetch(executionId, msg)
            else -> {
                logger.warn("Unexpected WS message type '{}' for execution {}", msg::class.simpleName, executionId)
                emptyList()
            }
        }

    /**
     * Processes a unified work batch from the orchestrator: imports solutions (rebalancing),
     * runs mutation+evaluation tasks, then discards no-longer-needed solutions — all in one call.
     *
     * @param executionId The execution context.
     * @param msg The unified batch request from the orchestrator.
     * @return The batch response with evaluation results for each task.
     */
    private suspend fun handleNodeWorkBatch(executionId: String, msg: NodeWorkBatchRequest): NodeWorkBatchResponse {
        val state = requireExecution(executionId)
        val nodeId = state.evaluator.getNodeIds().first()
        val batch = NodeBatch(
            nodeId = nodeId,
            imports = msg.imports.map { SolutionImportData(it.solutionId, it.serializedModel) },
            tasks = msg.tasks.map { MutationTask(it.solutionId, nodeId) },
            discards = msg.discards
        )
        val results = withContext(state.dispatcher) {
            state.evaluator.executeNodeBatches(listOf(batch))
        }
        return NodeWorkBatchResponse(
            requestId = msg.requestId,
            results = results.map { result ->
                BatchResult(
                    parentSolutionId = result.parentSolutionId,
                    newSolutionId = result.newSolutionId,
                    objectives = result.objectives,
                    constraints = result.constraints,
                    succeeded = result.succeeded
                )
            }
        )
    }

    /**
     * Handles a batch fetch request by serializing each requested solution individually.
     *
     * Each solution is serialized independently so that responses can be sent back
     * as individual frames, allowing the orchestrator to process them as they arrive.
     *
     * @param executionId The execution context.
     * @param msg The batch fetch request.
     * @return One [SolutionFetchResponse] per requested solution.
     */
    private suspend fun handleBatchFetch(
        executionId: String,
        msg: SolutionBatchFetchRequest
    ): List<SolutionFetchResponse> {
        return msg.solutionIds.map { solutionId ->
            val serializedModel = getSolutionData(executionId, solutionId)
            SolutionFetchResponse(msg.requestId, solutionId, serializedModel)
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Releases all active executions and their associated resources.
     *
     * Should be called when the application is shutting down to ensure
     * thread pools and solutions are properly cleaned up.
     */
    suspend fun close() {
        for ((id, state) in executions) {
            try {
                state.suspendClose()
            } catch (e: Exception) {
                logger.warn("Failed to close execution {}: {}", id, e.message)
            }
        }
        executions.clear()
    }
}
