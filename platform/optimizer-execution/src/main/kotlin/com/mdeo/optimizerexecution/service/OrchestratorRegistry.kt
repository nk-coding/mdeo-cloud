package com.mdeo.optimizerexecution.service

import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry that routes incoming subprocess WebSocket connections to the
 * correct orchestrator-side handler.
 *
 * When the orchestrator expects a worker subprocess to connect back, it registers
 * a [CompletableDeferred] keyed by a worker-scoped identifier (e.g.
 * `"{executionId}:{nodeId}"`). The subprocess WebSocket route [complete]s the
 * deferred when the connection arrives, unblocking the orchestrator coroutine
 * that is waiting for it.
 *
 * Thread-safe: all operations delegate to a [ConcurrentHashMap].
 */
class OrchestratorRegistry {

    private val logger = LoggerFactory.getLogger(OrchestratorRegistry::class.java)

    private val pending = ConcurrentHashMap<String, CompletableDeferred<DefaultWebSocketSession>>()

    /**
     * Registers a pending deferred for a subprocess connection identified by [key].
     *
     * @param key Unique identifier for the expected connection (typically `"{executionId}:{nodeId}"`).
     * @return A [CompletableDeferred] that will be completed when the subprocess connects.
     */
    fun register(key: String): CompletableDeferred<DefaultWebSocketSession> {
        val deferred = CompletableDeferred<DefaultWebSocketSession>()
        pending[key] = deferred
        logger.debug("Registered pending subprocess connection: {}", key)
        return deferred
    }

    /**
     * Completes a previously registered deferred with the arrived WebSocket session.
     *
     * @param key The identifier that was used in [register].
     * @param session The WebSocket session opened by the subprocess.
     * @return `true` if a pending registration existed and was completed, `false` otherwise.
     */
    fun complete(key: String, session: DefaultWebSocketSession): Boolean {
        val deferred = pending.remove(key)
        if (deferred != null) {
            deferred.complete(session)
            logger.debug("Subprocess connection completed: {}", key)
            return true
        }
        logger.warn("No pending registration for subprocess connection: {}", key)
        return false
    }

    /**
     * Removes a pending registration without completing it.
     *
     * Used for cleanup when an orchestrator is cancelled before the subprocess connects.
     *
     * @param key The identifier to remove.
     */
    fun remove(key: String) {
        val deferred = pending.remove(key)
        if (deferred != null && !deferred.isCompleted) {
            deferred.completeExceptionally(
                IllegalStateException("Registration removed before subprocess connected: $key")
            )
        }
    }

    /**
     * Builds the standard registry key for a worker subprocess connection.
     *
     * @param executionId The optimization execution identifier.
     * @param nodeId The worker node identifier.
     * @return The registry key string.
     */
    companion object {
        fun key(executionId: String, nodeId: String): String = "$executionId:$nodeId"
    }
}
