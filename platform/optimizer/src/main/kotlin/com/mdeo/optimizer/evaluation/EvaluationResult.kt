package com.mdeo.optimizer.evaluation

/**
 * Represents the result of a mutation and evaluation operation performed by a worker node.
 *
 * After a worker deep-copies a parent solution, applies a mutation, and evaluates
 * fitness, it returns this result containing both the lineage information and the
 * computed objective/constraint values.
 *
 * @param parentSolutionId The identifier of the parent solution that was mutated.
 * @param newSolutionId The identifier assigned to the newly created mutated solution.
 * @param workerNodeId The identifier of the worker node that performed the operation.
 * @param objectives The computed objective function values for the new solution.
 * @param constraints The computed constraint function values for the new solution.
 * @param succeeded Whether the mutation and evaluation completed successfully.
 */
data class EvaluationResult(
    val parentSolutionId: String,
    val newSolutionId: String,
    val workerNodeId: String,
    val objectives: List<Double>,
    val constraints: List<Double>,
    val succeeded: Boolean
)
