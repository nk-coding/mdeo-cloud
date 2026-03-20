package com.mdeo.optimizer.solution

import com.mdeo.modeltransformation.graph.ModelGraph

/**
 * Represents a candidate solution in the optimization process.
 *
 * Wraps a [ModelGraph] containing the model state and tracks
 * the chain of transformations applied to reach this state.
 *
 * @param modelGraph The model graph holding the current model state.
 * @param transformationsChain History of applied transformation step names.
 */
class Solution(
    var modelGraph: ModelGraph,
    val transformationsChain: MutableList<List<String>> = mutableListOf()
) : AutoCloseable {

    /**
     * Creates a deep copy of this solution.
     *
     * The graph is fully cloned (with shuffled vertex order for nondeterminism)
     * so mutations on the copy do not affect the original.
     */
    fun deepCopy(): Solution {
        val copiedGraph = modelGraph.deepCopy()
        val copiedChain = transformationsChain.map { it.toList() }.toMutableList()
        return Solution(copiedGraph, copiedChain)
    }

    /**
     * Creates a shallow copy of this solution (shares the same [ModelGraph]).
     *
     * Use this for bookkeeping (e.g. tracking Pareto-front entries)
     * where the graph state does not need to be isolated.
     */
    fun copy(): Solution {
        val copiedChain = transformationsChain.map { it.toList() }.toMutableList()
        return Solution(modelGraph, copiedChain)
    }

    /**
     * Records a step of transformations applied to this solution.
     */
    fun recordTransformationStep(transformationNames: List<String>) {
        transformationsChain.add(transformationNames)
    }

    override fun close() {
        modelGraph.close()
    }
}
