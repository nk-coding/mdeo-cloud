package com.mdeo.optimizer.solution

import com.mdeo.optimizer.graph.GraphBackend

/**
 * Represents a candidate solution in the optimization process.
 *
 * Wraps a graph backend containing the model state and tracks
 * the chain of transformations applied to reach this state.
 *
 * Ported from the original Solution.java, replacing EMF EObject with graph backend.
 *
 * @param graphBackend The graph backend holding the current model state.
 * @param transformationsChain History of applied transformation names.
 */
class Solution(
    var graphBackend: GraphBackend,
    val transformationsChain: MutableList<List<String>> = mutableListOf()
) : AutoCloseable {

    /**
     * Creates a deep copy of this solution.
     * The graph is fully cloned so mutations on the copy do not affect the original.
     */
    fun deepCopy(): Solution {
        val copiedBackend = graphBackend.deepCopy()
        val copiedChain = transformationsChain.map { it.toList() }.toMutableList()
        return Solution(copiedBackend, copiedChain)
    }

    /**
     * Records a step of transformations applied to this solution.
     */
    fun recordTransformationStep(transformationNames: List<String>) {
        transformationsChain.add(transformationNames)
    }

    override fun close() {
        graphBackend.close()
    }
}
