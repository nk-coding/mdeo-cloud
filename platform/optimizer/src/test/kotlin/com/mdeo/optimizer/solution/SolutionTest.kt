package com.mdeo.optimizer.solution

import com.mdeo.optimizer.graph.TinkerGraphBackend
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [Solution] — the candidate solution wrapper.
 */
class SolutionTest {

    @Test
    fun `deepCopy creates independent solution`() {
        val backend = TinkerGraphBackend()
        backend.traversal().addV("node").next()
        val original = Solution(backend)
        original.recordTransformationStep(listOf("transform1"))

        val copy = original.deepCopy()

        // Verify independent graph
        copy.graphBackend.traversal().addV("extra").next()
        assertEquals(1L, original.graphBackend.traversal().V().count().next())
        assertEquals(2L, copy.graphBackend.traversal().V().count().next())

        // Verify independent transformation chain
        copy.recordTransformationStep(listOf("transform2"))
        assertEquals(1, original.transformationsChain.size)
        assertEquals(2, copy.transformationsChain.size)

        original.close()
        copy.close()
    }

    @Test
    fun `recordTransformationStep appends to chain`() {
        val backend = TinkerGraphBackend()
        val solution = Solution(backend)

        solution.recordTransformationStep(listOf("t1", "t2"))
        solution.recordTransformationStep(listOf("t3"))

        assertEquals(2, solution.transformationsChain.size)
        assertEquals(listOf("t1", "t2"), solution.transformationsChain[0])
        assertEquals(listOf("t3"), solution.transformationsChain[1])

        solution.close()
    }
}
