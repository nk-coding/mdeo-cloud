package com.mdeo.execution.common.subprocess

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A watchdog thread for use inside a subprocess that monitors registered timeouts.
 *
 * When a timeout expires, the [onTimeout] callback is invoked with the timeout identifier.
 * After firing, the watchdog stops its loop — the callback is expected to terminate the
 * subprocess (e.g. by sending a timeout notification and halting the JVM).
 *
 * Supports concurrent timeout registrations from multiple threads, making it safe to use
 * when the subprocess evaluates multiple mutations in parallel.
 *
 * @param onTimeout Callback invoked on the watchdog thread when a registered timeout
 *        expires. Receives the numeric timeout id.
 */
class ChildProcessWatchdog(
    private val onTimeout: (Int) -> Unit
) {
    private sealed class Event {
        data class Register(val id: Int, val deadlineMs: Long) : Event()
        data class Cancel(val id: Int) : Event()
        data object Shutdown : Event()
    }

    private val eventQueue = LinkedBlockingQueue<Event>()
    private val activeDeadlines = ConcurrentHashMap<Int, Long>()

    @Volatile
    private var running = true

    private val thread = Thread {
        runLoop()
    }.apply {
        isDaemon = true
        name = "child-process-watchdog"
        start()
    }

    /**
     * Registers a timeout with the given identifier and duration.
     *
     * If not cancelled before [durationMs] elapses, [onTimeout] is invoked and the
     * subprocess is expected to terminate.
     *
     * Thread-safe: can be called from any thread, including concurrent mutation threads.
     *
     * @param id Numeric identifier for this timeout.
     * @param durationMs Duration in milliseconds after which the timeout fires.
     */
    fun registerTimeout(id: Int, durationMs: Long) {
        val deadline = System.currentTimeMillis() + durationMs
        eventQueue.offer(Event.Register(id, deadline))
    }

    /**
     * Cancels a previously registered timeout.
     *
     * If the timeout has already fired or does not exist, this is a no-op.
     * Thread-safe: can be called from any thread.
     *
     * @param id The numeric identifier of the timeout to cancel.
     */
    fun cancelTimeout(id: Int) {
        eventQueue.offer(Event.Cancel(id))
    }

    /**
     * Shuts down the watchdog thread. After this call, no further timeout events
     * will be processed.
     */
    fun shutdown() {
        running = false
        eventQueue.offer(Event.Shutdown)
    }

    private fun runLoop() {
        while (running) {
            val now = System.currentTimeMillis()
            val nextDeadline = activeDeadlines.values.minOrNull()

            val event = if (nextDeadline == null) {
                // No active deadlines — block until an event arrives.
                try {
                    eventQueue.take()
                } catch (_: InterruptedException) {
                    continue
                }
            } else {
                val waitMs = (nextDeadline - now).coerceAtLeast(1)
                eventQueue.poll(waitMs, TimeUnit.MILLISECONDS)
            }

            when (event) {
                is Event.Register -> activeDeadlines[event.id] = event.deadlineMs
                is Event.Cancel -> activeDeadlines.remove(event.id)
                is Event.Shutdown -> {
                    running = false
                    return
                }
                null -> { /* Poll timeout — check deadlines below */ }
            }

            val currentTime = System.currentTimeMillis()
            val expired = activeDeadlines.entries.firstOrNull { it.value <= currentTime }
            if (expired != null) {
                activeDeadlines.remove(expired.key)
                running = false
                onTimeout(expired.key)
                return
            }
        }
    }
}
