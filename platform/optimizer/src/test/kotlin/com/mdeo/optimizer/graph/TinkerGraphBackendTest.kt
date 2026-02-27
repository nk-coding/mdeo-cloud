package com.mdeo.optimizer.graph

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [TinkerGraphBackend] — the in-process graph backend used during optimization.
 */
class TinkerGraphBackendTest {

    @Test
    fun `empty graph has zero vertices`() {
        val backend = TinkerGraphBackend()
        val count = backend.traversal().V().count().next()
        assertEquals(0L, count)
        backend.close()
    }

    @Test
    fun `add vertex increases count`() {
        val backend = TinkerGraphBackend()
        backend.traversal().addV("node").property("name", "a").next()
        assertEquals(1L, backend.traversal().V().count().next())
        backend.close()
    }

    @Test
    fun `deepCopy creates independent copy`() {
        val original = TinkerGraphBackend()
        original.traversal().addV("node").property("name", "a").next()
        original.traversal().addV("node").property("name", "b").next()

        val copy = original.deepCopy() as TinkerGraphBackend

        // Verify counts match
        assertEquals(
            original.traversal().V().count().next(),
            copy.traversal().V().count().next()
        )

        // Mutate copy — original should be unaffected
        copy.traversal().addV("node").property("name", "c").next()
        assertEquals(2L, original.traversal().V().count().next())
        assertEquals(3L, copy.traversal().V().count().next())

        original.close()
        copy.close()
    }

    @Test
    fun `deepCopy preserves edges`() {
        val original = TinkerGraphBackend()
        val g = original.traversal()
        val v1 = g.addV("person").property("name", "Alice").next()
        val v2 = g.addV("person").property("name", "Bob").next()
        g.addE("knows").from(v1).to(v2).property("since", 2020).next()

        val copy = original.deepCopy() as TinkerGraphBackend

        assertEquals(2L, copy.traversal().V().count().next())
        assertEquals(1L, copy.traversal().E().count().next())
        assertEquals("knows", copy.traversal().E().label().next())

        original.close()
        copy.close()
    }

    @Test
    fun `deepCopy preserves vertex properties`() {
        val original = TinkerGraphBackend()
        original.traversal().addV("item").property("key", "value").next()

        val copy = original.deepCopy() as TinkerGraphBackend

        val props = copy.traversal().V().has("key", "value").toList()
        assertEquals(1, props.size)

        original.close()
        copy.close()
    }

    @Test
    fun `close is idempotent`() {
        val backend = TinkerGraphBackend()
        assertDoesNotThrow {
            backend.close()
            backend.close()
        }
    }
}
