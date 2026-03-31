package com.mdeo.optimizer.evaluation

/**
 * Represents the result of creating an initial solution during population initialization.
 *
 * When the evaluator initializes the population, each solution is created and stored
 * on a worker node. Fitness evaluation is performed separately via [EvaluationTask]s
 * dispatched through [MutationEvaluator.executeNodeBatches], which provides consistent
 * timeout protection for all evaluation work.
 *
 * @param solutionId The identifier assigned to the newly created solution.
 * @param workerNodeId The identifier of the worker node that created and stores the solution.
 */
data class InitialSolutionResult(
    val solutionId: String,
    val workerNodeId: String
)
