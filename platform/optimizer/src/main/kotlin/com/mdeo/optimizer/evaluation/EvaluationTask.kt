package com.mdeo.optimizer.evaluation

/**
 * Represents a request to evaluate (but not mutate) an existing solution on a specific worker node.
 *
 * Unlike [MutationTask], which deep-copies and mutates a parent solution before evaluating,
 * an evaluation task runs the objective and constraint functions directly against the
 * solution as it currently exists. This is used during population initialization, where
 * solutions have already been created and stored on workers but have not yet been evaluated
 * for fitness.
 *
 * @param solutionId The identifier of the solution to evaluate.
 * @param workerNodeId The identifier of the worker node where the solution resides.
 */
data class EvaluationTask(val solutionId: String, val workerNodeId: String)
