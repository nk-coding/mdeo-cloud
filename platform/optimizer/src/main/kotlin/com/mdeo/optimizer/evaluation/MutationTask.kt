package com.mdeo.optimizer.evaluation

/**
 * Represents a request to mutate and evaluate a specific solution on a specific worker node.
 *
 * The orchestrator creates mutation tasks during each generation and dispatches them
 * to the appropriate worker nodes. Each task identifies the parent solution to be
 * deep-copied, mutated, and evaluated.
 *
 * @param solutionId The identifier of the parent solution to mutate.
 * @param workerNodeId The identifier of the worker node where the solution resides.
 */
data class MutationTask(val solutionId: String, val workerNodeId: String)
