package com.mdeo.optimizerexecution.service

import io.ktor.websocket.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [OrchestratorRegistry]: register/complete/remove lifecycle and edge cases.
 */
class OrchestratorRegistryTest {

    private val stubSession: DefaultWebSocketSession = mockk(relaxed = true)

    @Test
    @Timeout(5)
    fun `register creates a pending deferred`() {
        val registry = OrchestratorRegistry()
        val deferred = registry.register("exec1:node0")
        assertFalse(deferred.isCompleted)
    }

    @Test
    @Timeout(5)
    fun `complete resolves the deferred`() = runBlocking {
        val registry = OrchestratorRegistry()
        val deferred = registry.register("exec1:node0")

        assertTrue(registry.complete("exec1:node0", stubSession))
        assertTrue(deferred.isCompleted)

        val session = withTimeout(1000) { deferred.await() }
        assertNotNull(session)
    }

    @Test
    @Timeout(5)
    fun `complete returns false for unknown key`() {
        val registry = OrchestratorRegistry()
        assertFalse(registry.complete("unknown:key", stubSession))
    }

    @Test
    @Timeout(5)
    fun `remove cancels pending deferred with exception`() {
        val registry = OrchestratorRegistry()
        val deferred = registry.register("exec1:node0")

        registry.remove("exec1:node0")
        assertTrue(deferred.isCompleted)

        assertThrows<IllegalStateException> {
            runBlocking { deferred.await() }
        }
    }

    @Test
    @Timeout(5)
    fun `key helper builds correct format`() {
        val key = OrchestratorRegistry.key("exec-123", "node-0")
        assertTrue(key == "exec-123:node-0")
    }
}
