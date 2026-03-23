package com.mdeo.optimizer.evaluation

/**
 * Represents the result of creating an initial solution during population initialization.
 *
 * When the evaluator initializes the population, each created solution is evaluated
 * and this result captures the solution's identity and its computed fitness values.
 *
 * @param solutionId The identifier assigned to the newly created solution.
 * @param workerNodeId The identifier of the worker node that created and stores the solution.
 * @param objectives The computed objective function values for the solution.
 * @param constraints The computed constraint function values for the solution.
 */
data class InitialSolutionResult(
    val solutionId: String,
    val workerNodeId: String,
    val objectives: List<Double>,
    val constraints: List<Double>
)
