package com.mdeo.scriptexecution.service

import com.mdeo.script.ast.TypedAst
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.common.model.ExecutionState
import com.mdeo.scriptexecution.database.ExecutionsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant
import java.util.*

/**
 * Merged service for managing and executing scripts.
 * Handles database operations and script execution logic.
 *
 * @param backendApiService Service for backend API communication
 * @param timeoutMs Execution timeout in milliseconds
 */
class ExecutionService(
    private val backendApiService: BackendApiService,
    private val timeoutMs: Long
) {
    private val logger = LoggerFactory.getLogger(ExecutionService::class.java)
    private val compiler = ScriptCompiler()
    private val dependencyResolver = TypedAstDependencyResolver(backendApiService)

    companion object {
        private const val MAX_PATH_LENGTH = 1000
        private const val MAX_METHOD_NAME_LENGTH = 200
    }

    /**
     * Creates a new execution and starts it asynchronously.
     *
     * @param executionId UUID of the execution
     * @param projectId UUID of the project
     * @param filePath Path to the file
     * @param data JSON data containing execution parameters
     * @param jwtToken JWT token to pass through to backend
     * @return Display name for the execution
     */
    suspend fun createAndStartExecution(
        executionId: UUID,
        projectId: UUID,
        filePath: String,
        data: JsonElement,
        jwtToken: String
    ): String = withContext(Dispatchers.IO) {
        val methodName = data.toString().removeSurrounding("\"")
        validateInputs(filePath, methodName)

        val now = Instant.now()
        transaction {
            ExecutionsTable.insert {
                it[id] = executionId
                it[ExecutionsTable.projectId] = projectId
                it[ExecutionsTable.filePath] = filePath
                it[state] = ExecutionState.SUBMITTED
                it[progress] = null
                it[createdAt] = now
                it[startedAt] = null
                it[completedAt] = null
                it[ExecutionsTable.data] = data.toString()
            }
        }

        logger.info("Created execution $executionId for project $projectId")

        withContext(Dispatchers.Default) {
            try {
                withTimeout(timeoutMs) {
                    executeScript(executionId, projectId, filePath, methodName, jwtToken)
                }
            } catch (e: TimeoutCancellationException) {
                logger.error("Execution timeout after ${timeoutMs}ms", e)
                updateExecutionState(
                    executionId,
                    ExecutionState.FAILED,
                    "Execution timeout: Script exceeded maximum execution time of ${timeoutMs}ms",
                    jwtToken
                )
            } catch (e: IllegalArgumentException) {
                logger.error("Input validation failed", e)
                updateExecutionState(
                    executionId,
                    ExecutionState.FAILED,
                    "Invalid input: ${e.message}",
                    jwtToken
                )
            } catch (e: Exception) {
                logger.error("Unexpected error during execution", e)
                updateExecutionState(
                    executionId,
                    ExecutionState.FAILED,
                    "Unexpected error: ${e.message}",
                    jwtToken
                )
            }
        }

        "$filePath:$methodName"
    }

    /**
     * Validates input parameters for security and sanity.
     *
     * @param filePath Path to the file
     * @param methodName Name of the method
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateInputs(filePath: String, methodName: String) {
        require(filePath.isNotBlank()) { "filePath cannot be empty" }
        require(methodName.isNotBlank()) { "methodName cannot be empty" }
        require(!filePath.contains("..")) { "filePath cannot contain '..' (path traversal attempt)" }
        require(filePath.length <= MAX_PATH_LENGTH) {
            "filePath too long (max $MAX_PATH_LENGTH characters)"
        }
        require(methodName.length <= MAX_METHOD_NAME_LENGTH) {
            "methodName too long (max $MAX_METHOD_NAME_LENGTH characters)"
        }
        require(methodName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
            "methodName must be a valid identifier (alphanumeric and underscore only)"
        }
    }

    /**
     * Executes a script method.
     *
     * @param executionId UUID of the execution
     * @param projectId UUID of the project
     * @param filePath Path to the file
     * @param methodName Name of the method to execute
     * @param jwtToken JWT token to pass through to backend
     */
    private suspend fun executeScript(
        executionId: UUID,
        projectId: UUID,
        filePath: String,
        methodName: String,
        jwtToken: String
    ) {
        updateExecutionState(executionId, ExecutionState.INITIALIZING, "Fetching AST and dependencies...", jwtToken)

        val typedAsts = dependencyResolver.resolveWithDependencies(
            projectId.toString(),
            filePath,
            jwtToken
        )

        if (typedAsts == null) {
            updateExecutionState(
                executionId,
                ExecutionState.FAILED,
                "Failed to fetch typed AST or its dependencies from backend",
                jwtToken
            )
            return
        }

        val mainTypedAst = typedAsts[filePath]
        if (mainTypedAst == null) {
            updateExecutionState(
                executionId,
                ExecutionState.FAILED,
                "Main file typed AST not found in resolved dependencies",
                jwtToken
            )
            return
        }

        val targetFunction = mainTypedAst.functions.find { it.name == methodName }
        if (targetFunction == null) {
            updateExecutionState(
                executionId,
                ExecutionState.FAILED,
                "Method '$methodName' not found in file",
                jwtToken
            )
            return
        }

        if (targetFunction.parameters.isNotEmpty()) {
            updateExecutionState(
                executionId,
                ExecutionState.FAILED,
                "Method '$methodName' has parameters. Only methods with no parameters are supported.",
                jwtToken
            )
            return
        }

        updateExecutionState(
            executionId,
            ExecutionState.INITIALIZING,
            "Compiling ${typedAsts.size} file(s)...",
            jwtToken
        )

        val compiledProgram = try {
            val input = CompilationInput(typedAsts)
            compiler.compile(input)
        } catch (e: Exception) {
            logger.error("Compilation failed", e)
            
            val errorMessage = "Compilation error: ${e.message}"
            
            transaction {
                ExecutionsTable.update({ ExecutionsTable.id eq executionId }) {
                    it[ExecutionsTable.error] = errorMessage
                }
            }
            
            updateExecutionState(
                executionId,
                ExecutionState.FAILED,
                errorMessage,
                jwtToken
            )
            return
        }

        updateExecutionState(executionId, ExecutionState.RUNNING, "Executing...", jwtToken)

        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream, true, Charsets.UTF_8)

        try {
            val env = ExecutionEnvironment(compiledProgram, printStream)
            val result = env.invoke(filePath, methodName)

            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            val resultString = result?.toString() ?: "null"

            transaction {
                ExecutionsTable.update({ ExecutionsTable.id eq executionId }) {
                    it[ExecutionsTable.result] = resultString
                    it[ExecutionsTable.output] = capturedOutput
                }
            }

            updateExecutionState(
                executionId,
                ExecutionState.COMPLETED,
                "Completed successfully",
                jwtToken
            )

            logger.info("Execution $executionId completed successfully")
        } catch (e: Exception) {
            logger.error("Runtime error during execution", e)

            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            val errorMessage = "${e.javaClass.simpleName}: ${e.message}"

            transaction {
                ExecutionsTable.update({ ExecutionsTable.id eq executionId }) {
                    it[ExecutionsTable.output] = capturedOutput
                    it[ExecutionsTable.error] = errorMessage
                }
            }

            updateExecutionState(
                executionId,
                ExecutionState.FAILED,
                "Runtime error: ${e.message}",
                jwtToken
            )
        } finally {
            printStream.close()
        }
    }

    /**
     * Updates the state of an execution.
     *
     * @param executionId UUID of the execution
     * @param state New state
     * @param progressText Optional progress text
     */
    private suspend fun updateExecutionState(
        executionId: UUID,
        state: String,
        progressText: String?,
        jwtToken: String?
    ) = withContext(Dispatchers.IO) {
        val now = Instant.now()

        val currentStartedAt = transaction {
            ExecutionsTable.selectAll()
                .where { ExecutionsTable.id eq executionId }
                .firstOrNull()
                ?.get(ExecutionsTable.startedAt)
        }

        transaction {
            ExecutionsTable.update({ ExecutionsTable.id eq executionId }) {
                it[ExecutionsTable.state] = state
                it[progress] = progressText

                when (state) {
                    ExecutionState.INITIALIZING, ExecutionState.RUNNING -> {
                        if (currentStartedAt == null) {
                            it[startedAt] = now
                        }
                    }

                    ExecutionState.COMPLETED, ExecutionState.FAILED, ExecutionState.CANCELLED -> {
                        it[completedAt] = now
                    }
                }
            }
        }

        logger.debug("Updated execution $executionId to state $state")
        if (jwtToken != null) {
            try {
                val ok = backendApiService.updateExecutionState(executionId.toString(), state, progressText, jwtToken)
                if (!ok) {
                    logger.warn("Backend update for execution state failed for $executionId")
                }
            } catch (e: Exception) {
                logger.error("Error while updating execution state on backend for $executionId", e)
            }
        }
    }

    /**
     * Cancels an execution.
     *
     * @param executionId UUID of the execution
     */
    suspend fun cancelExecution(executionId: UUID) = withContext(Dispatchers.IO) {
        transaction {
            ExecutionsTable.update({ ExecutionsTable.id eq executionId }) {
                it[state] = ExecutionState.CANCELLED
                it[progress] = "Cancelled by user"
                it[completedAt] = Instant.now()
            }
        }
        logger.info("Cancelled execution $executionId")
    }

    /**
     * Deletes an execution.
     *
     * @param executionId UUID of the execution
     */
    suspend fun deleteExecution(executionId: UUID) = withContext(Dispatchers.IO) {
        transaction {
            ExecutionsTable.deleteWhere { id eq executionId }
        }

        logger.info("Deleted execution $executionId")
    }

    /**
     * Generates a markdown summary for an execution.
     *
     * @param executionId UUID of the execution
     * @return Markdown-formatted summary, or null if execution not found
     */
    suspend fun getSummary(executionId: UUID): String? = withContext(Dispatchers.IO) {
        val execution = transaction {
            ExecutionsTable.selectAll()
                .where { ExecutionsTable.id eq executionId }
                .firstOrNull()
        } ?: return@withContext null

        val state = execution[ExecutionsTable.state]
        val result = execution[ExecutionsTable.result]
        val output = execution[ExecutionsTable.output]
        val error = execution[ExecutionsTable.error]

        when (state) {
            ExecutionState.COMPLETED -> {
                buildString {
                    append("## Result\n\n")
                    append(result ?: "null")
                    append("\n\n## Output\n\n")
                    if (output.isNullOrBlank()) {
                        append("*no output produced*")
                    } else {
                        append("```\n")
                        append(output)
                        append("\n```")
                    }
                }
            }

            ExecutionState.FAILED -> {
                val errorText = error ?: "Unknown error"

                if (errorText.contains("Compilation", ignoreCase = true)) {
                    buildString {
                        append("## Compilation Error\n\n```\n")
                        append(errorText)
                        append("\n```")
                    }
                } else {
                    buildString {
                        append("## Runtime Error\n\n")
                        append(errorText)
                        append("\n\n## Output\n\n")
                        if (output.isNullOrBlank()) {
                            append("*no output produced*")
                        } else {
                            append("```\n")
                            append(output)
                            append("\n```")
                        }
                    }
                }
            }

            else -> {
                "Execution is in state: $state"
            }
        }
    }
}
