package com.mdeo.optimizer.evaluation

import java.io.Serializable

/**
 * A reference to a solution stored on a specific worker node.
 *
 * Used by the orchestrator to track which node owns which solution,
 * enabling targeted operations such as fetching model data, requesting
 * mutations, or signaling solution disposal.
 *
 * Implements [Serializable] so that it can be stored as a MOEA Framework
 * [org.moeaframework.core.Solution] attribute, which requires serializability
 * for checkpointing and state management.
 *
 * @param nodeId The identifier of the worker node that stores the solution.
 * @param solutionId The identifier of the solution on that worker node.
 */
data class WorkerSolutionRef(val nodeId: String, val solutionId: String) : Serializable {

    companion object {
        /**
         * The attribute key used to store a [WorkerSolutionRef] on a MOEA Framework solution.
         */
        const val ATTRIBUTE_KEY = "workerSolutionRef"

        private const val serialVersionUID: Long = 1L
    }
}
