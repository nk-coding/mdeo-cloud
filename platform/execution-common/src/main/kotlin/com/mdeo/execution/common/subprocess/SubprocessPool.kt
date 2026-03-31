package com.mdeo.execution.common.subprocess

import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingDeque

/**
 * Pool of reusable [SubprocessRunner] instances backed by live child JVM processes.
 *
 * After an execution finishes, its subprocess is reset (clearing all state) and returned
 * to the pool. The next execution can acquire a subprocess without paying the JVM startup
 * and class-loading cost.
 *
 * **Reset-before-store** (eager reset): [resetAndRelease] performs a protocol-level reset
 * via [performReset] before storing the runner. Subprocesses are reset on return rather
 * than lazily on acquisition, so idle subprocesses hold minimal resources.
 *
 * **Synchronization**: The pool uses a [LinkedBlockingDeque] which is thread-safe. No
 * additional locking is needed on the pool itself.
 *
 * **Reset protocol**: The caller supplies [performReset], which must send an appropriate
 * reset command to the subprocess and return `true` when the subprocess confirms it is
 * ready for reuse. Returning `false` causes the subprocess to be destroyed instead of
 * pooled. This keeps the pool agnostic of the specific inter-process protocol used by
 * different execution services.
 *
 * @param maxSize Maximum number of idle subprocesses to keep (default 3).
 * @param performReset Sends a reset command to the subprocess and returns `true` if the
 *        subprocess responded with a successful acknowledgement and is ready for reuse.
 */
class SubprocessPool(
    private val maxSize: Int = DEFAULT_POOL_SIZE,
    private val performReset: (SubprocessRunner) -> Boolean
) {

    private val logger = LoggerFactory.getLogger(SubprocessPool::class.java)
    private val available = LinkedBlockingDeque<SubprocessRunner>()

    companion object {
        /** Default pool capacity. Kept low to limit idle memory footprint. */
        const val DEFAULT_POOL_SIZE = 3
    }

    /**
     * Attempts to acquire an idle subprocess from the pool.
     *
     * The returned runner is already started and has been successfully reset; it is ready
     * to receive new setup commands. Its [SubprocessRunner.onChannelMessage] callback is
     * cleared and should be reassigned by the caller before use.
     *
     * @return An idle [SubprocessRunner], or `null` if the pool is empty.
     */
    fun acquire(): SubprocessRunner? {
        val runner = available.pollFirst() ?: return null
        if (!runner.isRunning) {
            logger.warn("Discarding pooled subprocess that is no longer running")
            return acquire()
        }
        runner.onChannelMessage = null
        logger.debug("Acquired subprocess from pool ({} remaining)", available.size)
        return runner
    }

    /**
     * Resets the subprocess via [performReset] and, on success, returns it to the pool.
     * If the reset fails or the pool is already full, the subprocess is destroyed instead.
     *
     * This method performs the reset eagerly so idle subprocesses consume minimal resources.
     *
     * @param runner The subprocess runner to reset and return.
     */
    fun resetAndRelease(runner: SubprocessRunner) {
        if (!runner.isRunning) {
            logger.debug("Subprocess no longer running — not returning to pool")
            runner.destroy()
            return
        }

        runner.onChannelMessage = null

        val succeeded = try {
            performReset(runner)
        } catch (e: Exception) {
            logger.warn("Subprocess reset threw an exception, destroying: {}", e.message)
            runner.destroy()
            return
        }

        if (!succeeded) {
            logger.warn("Subprocess reset failed, destroying instead of pooling")
            runner.destroy()
            return
        }

        if (available.size < maxSize) {
            available.addLast(runner)
            logger.debug("Subprocess returned to pool ({} pooled)", available.size)
        } else {
            logger.debug("Pool full ({}/{}), destroying subprocess", available.size, maxSize)
            runner.stop()
        }
    }

    /**
     * Returns the number of idle subprocesses currently in the pool.
     */
    val size: Int get() = available.size

    /**
     * Destroys all pooled subprocesses and clears the pool.
     *
     * Should be called during application shutdown.
     */
    fun close() {
        var count = 0
        while (true) {
            val runner = available.pollFirst() ?: break
            runner.stop()
            count++
        }
        if (count > 0) {
            logger.info("Closed {} pooled subprocess(es)", count)
        }
    }
}
