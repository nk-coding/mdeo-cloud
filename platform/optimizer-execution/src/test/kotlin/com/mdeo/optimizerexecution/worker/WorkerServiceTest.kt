package com.mdeo.optimizerexecution.worker

import com.mdeo.expression.ast.TypedCallableBody
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.statements.TypedWhileStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.metamodel.SerializedModel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.optimizer.config.*
import com.mdeo.optimizer.worker.BatchTask
import com.mdeo.optimizer.worker.NodeWorkBatchRequest
import com.mdeo.optimizer.worker.NodeWorkBatchResponse
import com.mdeo.optimizer.worker.SolutionTransferItem
import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.TypedFunction
import com.mdeo.script.ast.TypedImport
import com.mdeo.script.ast.expressions.TypedExpressionSerializer as ScriptExpressionSerializer
import com.mdeo.script.ast.statements.TypedStatementSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for [WorkerService] that verify subprocess-based mutation execution,
 * including normal operation and per-mutation timeout enforcement.
 *
 * Each test starts a real [WorkerSubprocessMain] child JVM, sends work via the service,
 * and verifies the returned results.
 */
class WorkerServiceTest {

    // ─── Script JSON serialiser (must match WorkerSubprocessMain) ─────────────────

    private val scriptJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        // encodeDefaults = true is required so that 'kind' discriminator fields
        // (which have default values) are always included in serialised JSON.
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, ScriptExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }

    // ─── AST helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds a script AST with a single function that always returns 1.0.
     */
    private fun constantDoubleAst(functionName: String): TypedAst {
        val doubleTypeIdx = 0
        return TypedAst(
            types = listOf(ClassTypeRef("builtin", "double", false)),
            metamodelPath = null,
            imports = emptyList<TypedImport>(),
            functions = listOf(
                TypedFunction(
                    name = functionName,
                    parameters = emptyList(),
                    returnType = doubleTypeIdx,
                    body = TypedCallableBody(
                        listOf(
                            TypedReturnStatement(
                                value = TypedDoubleLiteralExpression(evalType = doubleTypeIdx, value = "1.0")
                            )
                        )
                    )
                )
            )
        )
    }

    /**
     * Builds a script AST with a single function that loops forever (`while (true) {}`).
     * Used to trigger a per-mutation timeout.
     */
    private fun infiniteLoopAst(functionName: String): TypedAst {
        val boolTypeIdx = 0
        val doubleTypeIdx = 1
        return TypedAst(
            types = listOf(
                ClassTypeRef("builtin", "boolean", false),
                ClassTypeRef("builtin", "double", false)
            ),
            metamodelPath = null,
            imports = emptyList<TypedImport>(),
            functions = listOf(
                TypedFunction(
                    name = functionName,
                    parameters = emptyList(),
                    returnType = doubleTypeIdx,
                    body = TypedCallableBody(
                        listOf(
                            TypedWhileStatement(
                                condition = TypedBooleanLiteralExpression(evalType = boolTypeIdx, value = true),
                                body = emptyList()
                            ),
                            // Unreachable — satisfies the compiler's return-type requirement
                            TypedReturnStatement(
                                value = TypedDoubleLiteralExpression(evalType = doubleTypeIdx, value = "0.0")
                            )
                        )
                    )
                )
            )
        )
    }

    // ─── Request builder ──────────────────────────────────────────────────────────

    private fun buildRequest(
        executionId: String = "test-exec",
        scriptAstJsons: Map<String, String> = emptyMap(),
        goalConfig: GoalConfig = GoalConfig(emptyList(), emptyList()),
        initialSolutionCount: Int = 1
    ) = com.mdeo.optimizer.worker.WorkerAllocationRequest(
        executionId = executionId,
        metamodelData = MetamodelData(),
        initialModelData = ModelData(metamodelPath = "", instances = emptyList(), links = emptyList()),
        transformationAstJsons = emptyMap(),
        scriptAstJsons = scriptAstJsons,
        goalConfig = goalConfig,
        solverConfig = SolverConfig(),
        initialSolutionCount = initialSolutionCount,
        useLocalChannel = true
    )

    // ─── Tests ────────────────────────────────────────────────────────────────────

    /**
     * Verifies that allocation starts a subprocess, generates initial solutions,
     * and that subsequent mutations succeed and return valid fitness values.
     *
     * Uses local-channel mode so that orchestrator messages travel over the subprocess
     * stdin/stdout pipe rather than a WebSocket connection.
     */
    @Test
    @Timeout(60)
    fun `allocation and mutation succeed`() = runBlocking {
        val objPath = "test://obj.fn"
        val objFn = "objective"
        val scriptAstJsons = mapOf(objPath to scriptJson.encodeToString(constantDoubleAst(objFn)))
        val goalConfig = GoalConfig(
            objectives = listOf(ObjectiveConfig(ObjectiveTendency.MINIMIZE, objPath, objFn)),
            constraints = emptyList()
        )

        val service = WorkerService(workerThreads = 1, scriptTimeoutMs = 10_000L, transformationTimeoutMs = 10_000L)
        try {
            val allocResponse = service.allocate(buildRequest(scriptAstJsons = scriptAstJsons, goalConfig = goalConfig))

            assertEquals(1, allocResponse.initialSolutions.size)

            val parentId = allocResponse.initialSolutions[0].solutionId
            val batchRequest = NodeWorkBatchRequest(
                requestId = "req-1",
                imports = emptyList(),
                tasks = listOf(BatchTask(parentId)),
                discards = emptyList()
            )

            val responses = service.dispatchToSubprocess("test-exec", batchRequest)
            val batchResponse = responses.filterIsInstance<NodeWorkBatchResponse>().firstOrNull()
            requireNotNull(batchResponse) { "Expected NodeWorkBatchResponse, got: $responses" }

            assertEquals(1, batchResponse.results.size)
            val result = batchResponse.results[0]
            assertTrue(result.succeeded, "Mutation should succeed")
            assertEquals(parentId, result.parentSolutionId)
            assertEquals(listOf(1.0), result.objectives)
            assertTrue(result.newSolutionId.isNotEmpty())
        } finally {
            try {
                service.cleanup("test-exec")
            } catch (_: Exception) {
                service.close()
            }
        }
    }

    /**
     * Verifies that a per-mutation timeout kills an infinite-loop constraint script.
     *
     * The constraint is defined as `while (true) {}` (loops forever). Allocation is done
     * with [initialSolutionCount] = 0 so the constraint is NOT evaluated during setup.
     * A seed is imported, then mutation triggers the infinite loop. The child-process
     * watchdog fires, the subprocess halts, and [dispatchToSubprocess] returns an empty
     * list (no valid response) once the process-exit callback cancels the pending deferred.
     */
    @Test
    @Timeout(60)
    @OptIn(ExperimentalSerializationApi::class)
    fun `mutation times out on infinite-loop constraint`() = runBlocking {
        val conPath = "test://con.fn"
        val conFn = "constraint"
        val scriptAstJsons = mapOf(conPath to scriptJson.encodeToString(infiniteLoopAst(conFn)))
        val goalConfig = GoalConfig(
            objectives = emptyList(),
            constraints = listOf(ConstraintConfig(conPath, conFn))
        )

        // Short timeout so the test does not take long
        val service = WorkerService(workerThreads = 1, scriptTimeoutMs = 500L, transformationTimeoutMs = 500L)
        try {
            val allocResponse = service.allocate(
                buildRequest(
                    scriptAstJsons = scriptAstJsons,
                    goalConfig = goalConfig,
                    initialSolutionCount = 0
                )
            )
            assertEquals(0, allocResponse.initialSolutions.size)

            // Import a seed solution so the subprocess has something to mutate.
            val seedModel = SerializedModel.AsModelData(
                ModelData(metamodelPath = "", instances = emptyList(), links = emptyList())
            )
            val seedId = "seed-solution"
            val importBatch = NodeWorkBatchRequest(
                requestId = "import-req",
                imports = listOf(SolutionTransferItem(seedId, seedModel)),
                tasks = emptyList(),
                discards = emptyList()
            )
            service.dispatchToSubprocess("test-exec", importBatch)

            // Mutate — the constraint spins forever, so the watchdog kills the subprocess.
            // dispatchToSubprocess returns empty when the process-exit callback fires.
            val mutateBatch = NodeWorkBatchRequest(
                requestId = "mutate-req",
                imports = emptyList(),
                tasks = listOf(BatchTask(seedId)),
                discards = emptyList()
            )
            val responses = service.dispatchToSubprocess("test-exec", mutateBatch)

            // Subprocess died: no valid NodeWorkBatchResponse is returned.
            assertTrue(responses.isEmpty(), "Expected empty response when subprocess times out, got: $responses")
        } finally {
            // Subprocess is already dead after the timeout; cleanup is best-effort.
            try {
                service.cleanup("test-exec")
            } catch (_: Exception) {
                service.close()
            }
        }
    }
}
