package com.mdeo.execution.common.subprocess

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubprocessRunnerTest {

    @Test
    @Timeout(30)
    fun `subprocess executes command and returns result`() {
        val runner = SubprocessRunner(
            mainClass = EchoSubprocess::class.java.name
        )

        assertTrue(runner.start(), "Subprocess should start")
        val result = runner.sendCommand("hello".toByteArray())
        assertIs<SubprocessResult.Success>(result)
        assertEquals("ECHO:hello", result.data.decodeToString())
        runner.stop()
    }

    @Test
    @Timeout(30)
    fun `timeout kills subprocess`() {
        val runner = SubprocessRunner(
            mainClass = SelfTimingOutSubprocess::class.java.name
        )

        assertTrue(runner.start(), "Subprocess should start")
        val result = runner.sendCommand("500".toByteArray())
        assertIs<SubprocessResult.Timeout>(result)
        assertEquals(1, result.timeoutId)
    }

    @Test
    @Timeout(30)
    fun `cancellation kills subprocess`() {
        var checkCount = 0
        val runner = SubprocessRunner(
            mainClass = SlowSubprocess::class.java.name,
            cancellationCheck = { _ ->
                checkCount++
                checkCount >= 2
            },
            cancellationCheckIntervalMs = 200
        )
        runner.executionId = "test-cancellation"

        assertTrue(runner.start(), "Subprocess should start")
        val result = runner.sendCommand("work".toByteArray())
        assertIs<SubprocessResult.Cancelled>(result)
    }

    @Test
    @Timeout(30)
    fun `channel messages are forwarded bidirectionally`() {
        val receivedChannel = AtomicReference<String>(null)
        val latch = CountDownLatch(1)

        val runner = SubprocessRunner(
            mainClass = ChannelSubprocess::class.java.name,
            onChannelMessage = { payload, respond ->
                receivedChannel.set(payload.decodeToString())
                respond("pong".toByteArray())
                latch.countDown()
            }
        )

        assertTrue(runner.start(), "Subprocess should start")
        val result = runner.sendCommand("trigger-channel".toByteArray())
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Channel message should be received")
        assertEquals("ping", receivedChannel.get())
        assertIs<SubprocessResult.Success>(result)
        runner.stop()
    }

    @Test
    @Timeout(30)
    fun `subprocess error is returned as Failed result`() {
        val runner = SubprocessRunner(
            mainClass = ErrorSubprocess::class.java.name
        )

        assertTrue(runner.start(), "Subprocess should start")
        val result = runner.sendCommand("fail".toByteArray())
        assertIs<SubprocessResult.Failed>(result)
        assertTrue(result.message.contains("Intentional error"), result.message)
        runner.stop()
    }
}

/**
 * Test subprocess that echoes commands with a prefix.
 */
class EchoSubprocess : SubprocessMain() {
    override fun handleCommand(payload: ByteArray): ByteArray {
        return "ECHO:${payload.decodeToString()}".toByteArray()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            EchoSubprocess().run(args)
        }
    }
}

/**
 * Test subprocess that registers an internal timeout then sleeps longer than the
 * timeout duration. The child-process watchdog fires, sends [SubprocessMessage.Timeout],
 * and halts the JVM.
 */
class SelfTimingOutSubprocess : SubprocessMain() {
    override fun handleCommand(payload: ByteArray): ByteArray {
        val timeoutMs = payload.decodeToString().toLongOrNull() ?: 500L
        registerTimeout(1, timeoutMs)
        Thread.sleep(60_000) // Sleep much longer than the timeout
        return ByteArray(0) // Never reached
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SelfTimingOutSubprocess().run(args)
        }
    }
}

/**
 * Test subprocess that sleeps for a long time (to test timeout).
 */
class SlowSubprocess : SubprocessMain() {
    override fun handleCommand(payload: ByteArray): ByteArray {
        Thread.sleep(60_000)
        return ByteArray(0)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SlowSubprocess().run(args)
        }
    }
}

/**
 * Test subprocess that sends a channel message and waits for a response before completing.
 */
class ChannelSubprocess : SubprocessMain() {
    @Volatile
    private var channelResponse: ByteArray? = null

    override fun handleCommand(payload: ByteArray): ByteArray {
        sendChannelMessage("ping".toByteArray())
        while (channelResponse == null) {
            Thread.sleep(10)
        }
        return "done:${channelResponse!!.decodeToString()}".toByteArray()
    }

    override fun handleChannelMessage(payload: ByteArray) {
        channelResponse = payload
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ChannelSubprocess().run(args)
        }
    }
}

/**
 * Test subprocess that always throws an error.
 */
class ErrorSubprocess : SubprocessMain() {
    override fun handleCommand(payload: ByteArray): ByteArray {
        throw RuntimeException("Intentional error for testing")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ErrorSubprocess().run(args)
        }
    }
}
