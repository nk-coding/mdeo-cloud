package com.mdeo.optimizerexecution.worker

import com.mdeo.execution.common.subprocess.SubprocessPool
import com.mdeo.execution.common.subprocess.SubprocessResult
import com.mdeo.execution.common.subprocess.SubprocessRunner
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [SubprocessPool]: verifies subprocess reuse via Reset, pool capacity limits,
 * and handling of dead subprocesses.
 *
 * Uses the real [WorkerSubprocessMain] subprocess to ensure the Reset command properly
 * clears state in the child JVM process.
 */
@OptIn(ExperimentalSerializationApi::class)
class SubprocessPoolTest {

    private val cbor = Cbor { ignoreUnknownKeys = true }

    @Test
    @Timeout(30)
    fun `acquire from empty pool returns null`() {
        val pool = WorkerService.buildDefaultPool(maxSize = 3)
        assertNull(pool.acquire())
        pool.close()
    }

    @Test
    @Timeout(60)
    fun `resetAndRelease returns subprocess to pool and acquire retrieves it`() {
        val pool = WorkerService.buildDefaultPool(maxSize = 3)
        val runner = SubprocessRunner(mainClass = WorkerSubprocessMain::class.java.name)
        assert(runner.start()) { "Subprocess should start" }

        sendSetup(runner)

        pool.resetAndRelease(runner)
        assertEquals(1, pool.size)

        val reused = pool.acquire()
        assertNotNull(reused)
        assertEquals(0, pool.size)

        val setupResult = sendSetup(reused)
        assertIs<SubprocessResult.Success>(setupResult)

        reused.stop()
        pool.close()
    }

    @Test
    @Timeout(60)
    fun `pool respects maxSize and destroys excess subprocesses`() {
        val pool = WorkerService.buildDefaultPool(maxSize = 1)

        val runner1 = SubprocessRunner(mainClass = WorkerSubprocessMain::class.java.name)
        assert(runner1.start())
        sendSetup(runner1)

        val runner2 = SubprocessRunner(mainClass = WorkerSubprocessMain::class.java.name)
        assert(runner2.start())
        sendSetup(runner2)

        pool.resetAndRelease(runner1)
        assertEquals(1, pool.size)

        pool.resetAndRelease(runner2)
        assertEquals(1, pool.size)

        val acquired = pool.acquire()
        assertNotNull(acquired)
        acquired.stop()
        pool.close()
    }

    @Test
    @Timeout(60)
    fun `reused subprocess has clean state after reset`() {
        val pool = WorkerService.buildDefaultPool(maxSize = 3)
        val runner = SubprocessRunner(mainClass = WorkerSubprocessMain::class.java.name)
        assert(runner.start())

        val setupPayload = cbor.encodeToByteArray<WorkerSubprocessRequest>(
            WorkerSubprocessRequest.Setup(
                metamodelData = com.mdeo.metamodel.data.MetamodelData(),
                initialModelData = com.mdeo.metamodel.data.ModelData(
                    metamodelPath = "", instances = emptyList(), links = emptyList()
                ),
                transformationAstJsons = emptyMap(),
                scriptAstJsons = emptyMap(),
                goalConfig = com.mdeo.optimizer.config.GoalConfig(emptyList(), emptyList()),
                solverConfig = com.mdeo.optimizer.config.SolverConfig(),
                initialSolutionCount = 1,
                skipInitialization = false
            )
        )
        val setupResult = runner.sendCommand(setupPayload)
        assertIs<SubprocessResult.Success>(setupResult)

        pool.resetAndRelease(runner)

        val reused = pool.acquire()!!
        val setupResult2 = reused.sendCommand(setupPayload)
        assertIs<SubprocessResult.Success>(setupResult2)

        val response2 = cbor.decodeFromByteArray<WorkerSubprocessResponse>(setupResult2.data)
        assertIs<WorkerSubprocessResponse.SetupOk>(response2)
        assertEquals(1, response2.solutions.size)

        reused.stop()
        pool.close()
    }

    private fun sendSetup(runner: SubprocessRunner): SubprocessResult {
        val payload = cbor.encodeToByteArray<WorkerSubprocessRequest>(
            WorkerSubprocessRequest.Setup(
                metamodelData = com.mdeo.metamodel.data.MetamodelData(),
                initialModelData = com.mdeo.metamodel.data.ModelData(
                    metamodelPath = "", instances = emptyList(), links = emptyList()
                ),
                transformationAstJsons = emptyMap(),
                scriptAstJsons = emptyMap(),
                goalConfig = com.mdeo.optimizer.config.GoalConfig(emptyList(), emptyList()),
                solverConfig = com.mdeo.optimizer.config.SolverConfig(),
                initialSolutionCount = 0,
                skipInitialization = false
            )
        )
        return runner.sendCommand(payload)
    }
}
