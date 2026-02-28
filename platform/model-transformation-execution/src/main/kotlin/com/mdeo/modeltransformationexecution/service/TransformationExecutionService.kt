package com.mdeo.modeltransformationexecution.service

import com.mdeo.common.model.ExecutionState
import com.mdeo.execution.common.routes.FileEntry
import com.mdeo.execution.common.service.ExecutionServiceWithFileTree
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.service.GraphToModelDataConverter
import com.mdeo.modeltransformationexecution.database.TransformationExecutionsTable
import com.mdeo.modeltransformationexecution.database.TransformationResultFilesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
 * Service for executing model transformations.
 * Implements ExecutionServiceWithFileTree to support result file trees.
 *
 * @param apiClient Client for backend API communication
 * @param timeoutMs Execution timeout in milliseconds
 */
class TransformationExecutionService(
    private val apiClient: TransformationApiClient,
    private val timeoutMs: Long
) : ExecutionServiceWithFileTree {
    private val logger = LoggerFactory.getLogger(TransformationExecutionService::class.java)
    private val graphLoader = ModelDataGraphLoader()
    private val json = Json { prettyPrint = true }
    
    companion object {
        private const val MAX_PATH_LENGTH = 1000
        private const val RESULT_FILE_NAME = "result.m_gen"
        private const val MIME_TYPE_JSON = "application/json"
    }

    override suspend fun createAndStartExecution(
        executionId: UUID,
        projectId: UUID,
        filePath: String,
        data: JsonElement,
        jwtToken: String
    ): String = withContext(Dispatchers.IO) {
        val modelPath = extractModelPath(data)
        validateInputs(filePath, modelPath)
        
        createExecutionRecord(executionId, projectId, filePath, modelPath)

        logger.info("Created transformation execution $executionId for project $projectId")

        withContext(Dispatchers.Default) {
            try {
                withTimeout(timeoutMs) {
                    executeTransformation(executionId, projectId, filePath, modelPath, jwtToken)
                }
            } catch (e: TimeoutCancellationException) {
                handleTimeout(executionId, jwtToken)
            } catch (e: Exception) {
                handleUnexpectedError(executionId, e, jwtToken)
            }
        }

        "$filePath -> $modelPath"
    }
    
    override suspend fun cancelExecution(executionId: UUID) = withContext(Dispatchers.IO) {
        transaction {
            TransformationExecutionsTable.update({
                TransformationExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[state] = ExecutionState.CANCELLED
                it[progress] = "Cancelled by user"
                it[completedAt] = Instant.now()
            }
        }
        logger.info("Cancelled execution $executionId")
    }
    
    override suspend fun deleteExecution(executionId: UUID) = withContext(Dispatchers.IO) {
        transaction {
            TransformationResultFilesTable.deleteWhere { 
                TransformationResultFilesTable.executionId eq executionId.toKotlinUuid() 
            }
            TransformationExecutionsTable.deleteWhere { id eq executionId.toKotlinUuid() }
        }
        logger.info("Deleted execution $executionId")
    }
    
    override suspend fun getSummary(executionId: UUID): String? = withContext(Dispatchers.IO) {
        val execution = findExecution(executionId) ?: return@withContext null
        buildExecutionSummary(execution)
    }
    
    override suspend fun getFileTree(executionId: UUID, path: String?): List<FileEntry>? {
        return withContext(Dispatchers.IO) {
            val execution = findExecution(executionId) ?: return@withContext null
            
            val files = transaction {
                TransformationResultFilesTable.selectAll()
                    .where { TransformationResultFilesTable.executionId eq executionId.toKotlinUuid() }
                    .map { row ->
                        val filePath = row[TransformationResultFilesTable.filePath]
                        FileEntry(filePath, FileEntry.TYPE_FILE)
                    }
            }
            
            filterFilesByPath(files, path)
        }
    }
    
    override suspend fun getFileContents(executionId: UUID, filePath: String): String? {
        return withContext(Dispatchers.IO) {
            transaction {
                TransformationResultFilesTable.selectAll()
                    .where {
                        (TransformationResultFilesTable.executionId eq executionId.toKotlinUuid()) and
                            (TransformationResultFilesTable.filePath eq filePath)
                    }
                    .firstOrNull()
                    ?.get(TransformationResultFilesTable.content)
            }
        }
    }
    
    private fun extractModelPath(data: JsonElement): String {
        return data.jsonObject["modelPath"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("modelPath is required in data")
    }
    
    private fun validateInputs(transformationPath: String, modelPath: String) {
        require(transformationPath.isNotBlank()) { "transformationPath cannot be empty" }
        require(modelPath.isNotBlank()) { "modelPath cannot be empty" }
        require(!transformationPath.contains("..")) { "Path traversal not allowed" }
        require(!modelPath.contains("..")) { "Path traversal not allowed" }
        require(transformationPath.length <= MAX_PATH_LENGTH) { "Path too long" }
        require(modelPath.length <= MAX_PATH_LENGTH) { "Path too long" }
    }
    
    private fun createExecutionRecord(
        executionId: UUID,
        projectId: UUID,
        transformationPath: String,
        modelPath: String
    ) {
        val now = Instant.now()
        transaction {
            TransformationExecutionsTable.insert {
                it[id] = executionId.toKotlinUuid()
                it[TransformationExecutionsTable.projectId] = projectId.toKotlinUuid()
                it[TransformationExecutionsTable.transformationPath] = transformationPath
                it[TransformationExecutionsTable.modelPath] = modelPath
                it[state] = ExecutionState.SUBMITTED
                it[progress] = null
                it[createdAt] = now
                it[startedAt] = null
                it[completedAt] = null
            }
        }
    }
    
    private suspend fun executeTransformation(
        executionId: UUID,
        projectId: UUID,
        transformationPath: String,
        modelPath: String,
        jwtToken: String
    ) {
        updateState(executionId, ExecutionState.INITIALIZING, "Fetching data...", jwtToken)

        val typedAst = fetchTypedAst(executionId, projectId, transformationPath, jwtToken)
            ?: return

        val modelData = fetchModelData(executionId, projectId, modelPath, jwtToken)
            ?: return

        val metamodelData = fetchMetamodelData(executionId, projectId, typedAst.metamodelPath, jwtToken)
            ?: return

        updateState(executionId, ExecutionState.RUNNING, "Executing transformation...", jwtToken)

        val resultModel = runTransformation(executionId, typedAst, metamodelData, modelData, jwtToken)
            ?: return

        storeResult(executionId, resultModel)
        
        updateState(executionId, ExecutionState.COMPLETED, "Completed successfully", jwtToken)
        logger.info("Execution $executionId completed successfully")
    }
    
    private suspend fun fetchTypedAst(
        executionId: UUID,
        projectId: UUID,
        transformationPath: String,
        jwtToken: String
    ): com.mdeo.modeltransformation.ast.TypedAst? {
        val typedAst = apiClient.getTransformationTypedAst(
            projectId.toString(),
            transformationPath,
            jwtToken
        )
        
        if (typedAst == null) {
            updateState(
                executionId,
                ExecutionState.FAILED,
                "Failed to fetch transformation typed AST",
                jwtToken
            )
        }
        return typedAst
    }

    private suspend fun fetchMetamodelData(
        executionId: UUID,
        projectId: UUID,
        metamodelPath: String,
        jwtToken: String
    ): com.mdeo.expression.ast.types.MetamodelData? {
        val metamodelData = apiClient.getMetamodelData(
            projectId.toString(),
            metamodelPath,
            jwtToken
        )
        
        if (metamodelData == null) {
            updateState(
                executionId,
                ExecutionState.FAILED,
                "Failed to fetch metamodel data for $metamodelPath",
                jwtToken
            )
        }
        return metamodelData
    }
    
    private suspend fun fetchModelData(
        executionId: UUID,
        projectId: UUID,
        modelPath: String,
        jwtToken: String
    ): ModelData? {
        val modelData = apiClient.getModelData(projectId.toString(), modelPath, jwtToken)
        
        if (modelData == null) {
            updateState(
                executionId,
                ExecutionState.FAILED,
                "Failed to fetch model data",
                jwtToken
            )
        }
        return modelData
    }
    
    private fun runTransformation(
        executionId: UUID,
        typedAst: com.mdeo.modeltransformation.ast.TypedAst,
        metamodelData: com.mdeo.expression.ast.types.MetamodelData,
        modelData: ModelData,
        jwtToken: String
    ): ModelData? {
        val graph = TinkerGraph.open()
        
        return try {
            val g = graph.traversal()
            val engine = TransformationEngine.create(g, typedAst, metamodelData, deterministic = false)
            
            graphLoader.load(g, modelData, engine.instanceNameRegistry, metamodelData)
            
            val result = engine.execute()
            
            val graphConverter = GraphToModelDataConverter(
                metamodelData = engine.metamodelData,
                types = engine.types,
                typeRegistry = engine.typeRegistry
            )
            
            when (result) {
                is TransformationExecutionResult.Success -> {
                    graphConverter.convert(g, modelData.metamodelUri, engine.instanceNameRegistry)
                }
                is TransformationExecutionResult.Failure -> {
                    handleTransformationFailure(executionId, result, jwtToken)
                    null
                }
                is TransformationExecutionResult.Stopped -> {
                    if (result.isNormalStop) {
                        graphConverter.convert(g, modelData.metamodelUri, engine.instanceNameRegistry)
                    } else {
                        handleTransformationKilled(executionId, jwtToken)
                        null
                    }
                }
            }
        } finally {
            graph.close()
        }
    }
    
    private fun handleTransformationFailure(
        executionId: UUID,
        failure: TransformationExecutionResult.Failure,
        jwtToken: String
    ) {
        val errorMessage = "Transformation failed: ${failure.reason}"
        
        transaction {
            TransformationExecutionsTable.update({
                TransformationExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[error] = errorMessage
            }
        }
        
        kotlinx.coroutines.runBlocking {
            updateState(executionId, ExecutionState.FAILED, errorMessage, jwtToken)
        }
    }
    
    private fun handleTransformationKilled(executionId: UUID, jwtToken: String) {
        val errorMessage = "Transformation killed explicitly."
        
        transaction {
            TransformationExecutionsTable.update({
                TransformationExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[error] = errorMessage
            }
        }
        
        kotlinx.coroutines.runBlocking {
            updateState(executionId, ExecutionState.FAILED, errorMessage, jwtToken)
        }
    }
    
    private fun storeResult(executionId: UUID, resultModel: ModelData) {
        val jsonContent = json.encodeToString(resultModel)
        
        transaction {
            TransformationResultFilesTable.insert {
                it[id] = Uuid.random()
                it[TransformationResultFilesTable.executionId] = executionId.toKotlinUuid()
                it[filePath] = RESULT_FILE_NAME
                it[content] = jsonContent
                it[mimeType] = MIME_TYPE_JSON
            }
        }
    }
    
    private suspend fun updateState(
        executionId: UUID,
        state: String,
        progressText: String?,
        jwtToken: String
    ) {
        val now = Instant.now()
        
        transaction {
            TransformationExecutionsTable.update({
                TransformationExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[TransformationExecutionsTable.state] = state
                it[progress] = progressText
                
                when (state) {
                    ExecutionState.INITIALIZING, ExecutionState.RUNNING -> {
                        it[startedAt] = now
                    }
                    ExecutionState.COMPLETED, ExecutionState.FAILED, ExecutionState.CANCELLED -> {
                        it[completedAt] = now
                    }
                }
            }
        }
        
        apiClient.updateExecutionState(executionId.toString(), state, progressText, jwtToken)
    }
    
    private suspend fun handleTimeout(executionId: UUID, jwtToken: String) {
        logger.error("Execution timeout after ${timeoutMs}ms")
        updateState(
            executionId,
            ExecutionState.FAILED,
            "Execution timeout: Transformation exceeded maximum time of ${timeoutMs}ms",
            jwtToken
        )
    }
    
    private suspend fun handleUnexpectedError(executionId: UUID, e: Exception, jwtToken: String) {
        logger.error("Unexpected error during execution", e)
        
        transaction {
            TransformationExecutionsTable.update({
                TransformationExecutionsTable.id eq executionId.toKotlinUuid()
            }) {
                it[error] = e.message
            }
        }
        
        updateState(
            executionId,
            ExecutionState.FAILED,
            "Unexpected error: ${e.message}",
            jwtToken
        )
    }
    
    private fun findExecution(executionId: UUID) = transaction {
        TransformationExecutionsTable.selectAll()
            .where { TransformationExecutionsTable.id eq executionId.toKotlinUuid() }
            .firstOrNull()
    }
    
    private fun buildExecutionSummary(execution: org.jetbrains.exposed.v1.core.ResultRow): String {
        val state = execution[TransformationExecutionsTable.state]
        val error = execution[TransformationExecutionsTable.error]
        val logs = execution[TransformationExecutionsTable.logMessages]
        val modelPath = execution[TransformationExecutionsTable.modelPath]
        
        return when (state) {
            ExecutionState.COMPLETED -> buildCompletedSummary(modelPath, logs)
            ExecutionState.FAILED -> buildFailedSummary(error, logs)
            else -> "Execution is in state: $state"
        }
    }
    
    /**
     * Builds a summary for a completed transformation execution.
     * Includes embedded graphical editor views for the source model (absolute path)
     * and the result model (relative path to execution result file).
     *
     * @param modelPath The path to the original model file
     * @param logs Optional log messages from the execution
     * @return Markdown summary string
     */
    private fun buildCompletedSummary(modelPath: String, logs: String?): String {
        return buildString {
            append("## Transformation Completed\n\n")
            append("The model transformation executed successfully.\n\n")
            append("### Result Model\n\n")
            append("![Result Model](${RESULT_FILE_NAME})\n")
            append("### Source Model\n\n")
            append("Caution: The source model may have been modifed since the transformation was run.\n\n")
            append("![Source Model](${modelPath})\n\n")
            
            if (!logs.isNullOrBlank()) {
                append("\n### Log Messages\n\n```\n")
                append(logs)
                append("\n```")
            }
        }
    }
    
    private fun buildFailedSummary(error: String?, logs: String?): String {
        return buildString {
            append("## Transformation Failed\n\n")
            append(error ?: "Unknown error")
            
            if (!logs.isNullOrBlank()) {
                append("\n\n### Log Messages\n\n```\n")
                append(logs)
                append("\n```")
            }
        }
    }
    
    private fun filterFilesByPath(files: List<FileEntry>, path: String?): List<FileEntry> {
        if (path.isNullOrBlank()) return files
        return files.filter { it.name.startsWith(path) }
    }
}
