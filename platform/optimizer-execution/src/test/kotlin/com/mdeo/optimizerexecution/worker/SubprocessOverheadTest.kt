package com.mdeo.optimizerexecution.worker

import com.mdeo.execution.common.subprocess.SubprocessResult
import com.mdeo.execution.common.subprocess.SubprocessRunner
import com.mdeo.expression.ast.TypedCallableBody
import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.optimizer.config.*
import com.mdeo.optimizer.worker.*
import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.TypedFunction
import com.mdeo.script.ast.TypedImport
import com.mdeo.script.ast.expressions.TypedExpressionSerializer as ScriptExpressionSerializer
import com.mdeo.script.ast.statements.TypedStatementSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Measures subprocess overhead: JVM spawn + setup latency and per-batch IPC round-trip cost.
 *
 * Uses a trivial model (no real metamodel/transformations) to isolate IPC overhead from
 * computation. Tagged as "performance" so it doesn't run in normal test suites.
 */
@OptIn(ExperimentalSerializationApi::class)
@Tag("performance")
class SubprocessOverheadTest {

    private val cbor = Cbor { ignoreUnknownKeys = true }

    private val scriptJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, ScriptExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }

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

    private fun buildSetup(
        scriptAstJsons: Map<String, String> = emptyMap(),
        goalConfig: GoalConfig = GoalConfig(emptyList(), emptyList()),
        initialSolutionCount: Int = 1
    ) = WorkerSubprocessRequest.Setup(
        metamodelData = MetamodelData(),
        initialModelData = ModelData(metamodelPath = "", instances = emptyList(), links = emptyList()),
        transformationAstJsons = emptyMap(),
        scriptAstJsons = scriptAstJsons,
        goalConfig = goalConfig,
        solverConfig = SolverConfig(),
        initialSolutionCount = initialSolutionCount,
        useLocalChannel = true
    )

    /**
     * Measures two things:
     * 1. **Setup overhead**: SubprocessRunner.start() + Setup request → SetupOk response
     *    (JVM spawn, class loading, AST deserialization, script compilation, initial solution creation)
     * 2. **Per-batch IPC overhead**: average round-trip time for sending a NodeWorkBatchRequest
     *    with 1 mutation task and receiving the NodeWorkBatchResponse (CBOR encode/decode + pipe I/O)
     */
    @Test
    @Timeout(120)
    fun `measure subprocess setup and per-batch IPC overhead`() {
        val iterations = 100

        val objPath = "test://obj.fn"
        val objFn = "objective"
        val scriptAstJsons = mapOf(objPath to scriptJson.encodeToString(constantDoubleAst(objFn)))
        val goalConfig = GoalConfig(
            objectives = listOf(ObjectiveConfig(ObjectiveTendency.MINIMIZE, objPath, objFn)),
            constraints = emptyList()
        )

        val responses = CopyOnWriteArrayList<WorkerWsMessage>()

        var runner: SubprocessRunner? = null
        try {
            // --- Phase 1: Measure setup overhead ---
            val setupStart = System.nanoTime()

            var currentLatch = CountDownLatch(1)

            runner = SubprocessRunner(
                mainClass = WorkerSubprocessMain::class.java.name,
                onChannelMessage = { payload, _ ->
                    try {
                        val msg = cbor.decodeFromByteArray<SubprocessChannelMessage>(payload)
                        if (msg is SubprocessChannelMessage.OrchestratorResponses) {
                            for (p in msg.payloads) {
                                responses.add(cbor.decodeFromByteArray<WorkerWsMessage>(p))
                            }
                            currentLatch.countDown()
                        }
                    } catch (_: Exception) {}
                }
            )

            val started = runner.start(startupTimeoutMs = 30_000L)
            check(started) { "Subprocess failed to start" }
            val spawnElapsedMs = (System.nanoTime() - setupStart) / 1_000_000

            val setupCmdStart = System.nanoTime()
            val setup = buildSetup(scriptAstJsons = scriptAstJsons, goalConfig = goalConfig)
            val setupResult = runner.sendCommand(cbor.encodeToByteArray<WorkerSubprocessRequest>(setup))
            val setupOk = cbor.decodeFromByteArray<WorkerSubprocessResponse>(
                (setupResult as SubprocessResult.Success).data
            ) as WorkerSubprocessResponse.SetupOk
            val setupCmdElapsedMs = (System.nanoTime() - setupCmdStart) / 1_000_000

            val totalSetupMs = (System.nanoTime() - setupStart) / 1_000_000

            val initialSolutionId = setupOk.solutions[0].solutionId

            println("=== Subprocess Overhead Benchmark ===")
            println("Setup phase:")
            println("  JVM spawn + start():    ${spawnElapsedMs} ms")
            println("  Setup command (AST deser + compile + initial solution): ${setupCmdElapsedMs} ms")
            println("  Total setup overhead:   ${totalSetupMs} ms")
            println()

            // --- Phase 2: Measure per-batch IPC round-trip ---
            // Use the initial solution as parent for each mutation.
            // The subprocess will create new solutions, but we always mutate from the initial one
            // to keep the test simple (solution store grows but that's fine for 100 iterations).

            val batchTimesNs = LongArray(iterations)

            for (i in 0 until iterations) {
                responses.clear()
                currentLatch = CountDownLatch(1)

                val batchRequest = NodeWorkBatchRequest(
                    requestId = "perf-$i",
                    tasks = listOf(BatchTask(initialSolutionId)),
                    discards = emptyList()
                )

                val batchStart = System.nanoTime()
                val requestPayload = cbor.encodeToByteArray<WorkerWsMessage>(batchRequest)
                runner.sendChannelMessage(
                    cbor.encodeToByteArray<SubprocessChannelMessage>(
                        SubprocessChannelMessage.OrchestratorRequest(requestPayload)
                    )
                )

                check(currentLatch.await(30, TimeUnit.SECONDS)) {
                    "Timed out waiting for batch response on iteration $i"
                }
                batchTimesNs[i] = System.nanoTime() - batchStart
            }

            val batchTimesMs = batchTimesNs.map { it / 1_000_000.0 }
            val avgMs = batchTimesMs.average()
            val minMs = batchTimesMs.min()
            val maxMs = batchTimesMs.max()
            val p50Ms = batchTimesMs.sorted()[iterations / 2]
            val p99Ms = batchTimesMs.sorted()[(iterations * 0.99).toInt()]

            println("Per-batch IPC round-trip ($iterations iterations, 1 mutation each):")
            println("  avg:  ${"%.2f".format(avgMs)} ms")
            println("  min:  ${"%.2f".format(minMs)} ms")
            println("  max:  ${"%.2f".format(maxMs)} ms")
            println("  p50:  ${"%.2f".format(p50Ms)} ms")
            println("  p99:  ${"%.2f".format(p99Ms)} ms")
            println()

            // Extrapolate to full optimization run
            val gen500overhead = avgMs * 500
            println("Extrapolated overhead for 500 generations × 1 batch/gen:")
            println("  ~${"%.1f".format(gen500overhead)} ms  (${"%.1f".format(gen500overhead / 1000)} s)")
            println()
            println("Extrapolated overhead for 500 generations × 80 batches/gen:")
            val gen500x80overhead = avgMs * 500 * 80
            println("  ~${"%.1f".format(gen500x80overhead)} ms  (${"%.1f".format(gen500x80overhead / 1000)} s)")
            println("=== End Benchmark ===")

        } finally {
            runner?.destroy()
        }
    }
}
