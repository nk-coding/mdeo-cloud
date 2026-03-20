package com.mdeo.optimizer.solution

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.graph.TinkerModelGraph
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [Solution] — the candidate solution wrapper.
 */
class SolutionTest {

    private val metamodel = Metamodel.compile(MetamodelData())

    @Test
    fun `deepCopy creates independent solution`() {
        val modelGraph = TinkerModelGraph.create(
            ModelData(metamodelPath = "", instances = emptyList(), links = emptyList()),
            metamodel
        )
        modelGraph.traversal().addV("node").next()
        val original = Solution(modelGraph)
        original.recordTransformationStep(listOf("transform1"))

        val copy = original.deepCopy()

        // Verify independent graph
        copy.modelGraph.traversal().addV("extra").next()
        assertEquals(1L, original.modelGraph.traversal().V().count().next())
        assertEquals(2L, copy.modelGraph.traversal().V().count().next())

        // Verify independent transformation chain
        copy.recordTransformationStep(listOf("transform2"))
        assertEquals(1, original.transformationsChain.size)
        assertEquals(2, copy.transformationsChain.size)

        original.close()
        copy.close()
    }

    @Test
    fun `recordTransformationStep appends to chain`() {
        val modelGraph = TinkerModelGraph.create(
            ModelData(metamodelPath = "", instances = emptyList(), links = emptyList()),
            metamodel
        )
        val solution = Solution(modelGraph)

        solution.recordTransformationStep(listOf("t1", "t2"))
        solution.recordTransformationStep(listOf("t3"))

        assertEquals(2, solution.transformationsChain.size)
        assertEquals(listOf("t1", "t2"), solution.transformationsChain[0])
        assertEquals(listOf("t3"), solution.transformationsChain[1])

        solution.close()
    }
}
