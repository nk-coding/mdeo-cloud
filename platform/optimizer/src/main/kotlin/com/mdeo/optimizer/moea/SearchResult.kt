package com.mdeo.optimizer.moea

import com.mdeo.optimizer.metrics.OptimizationMetricsCollector
import org.moeaframework.analysis.series.ResultSeries
import org.moeaframework.core.population.NondominatedPopulation

/**
 * Container for the results of an optimization search run.
 *
 * @param series The per-step runtime metrics from the instrumented algorithm.
 * @param finalPopulation The final Pareto-optimal solutions from the algorithm.
 * @param metricsCollector The collected per-generation metrics.
 */
class SearchResult(
    private val series: ResultSeries,
    private val finalPopulation: NondominatedPopulation,
    private val metricsCollector: OptimizationMetricsCollector = OptimizationMetricsCollector()
) {
    /**
     * Gets the collected runtime series from the instrumented run.
     */
    fun getObservations(): ResultSeries = series

    /**
     * Returns pairs of (objective values, constraint values) for each final Pareto-optimal solution.
     */
    fun getFinalSolutions(): List<SolutionResult> {
        return finalPopulation.map { solution ->
            SolutionResult(
                objectives = solution.getObjectiveValues().toList(),
                constraints = solution.getConstraintValues().toList()
            )
        }
    }

    /**
     * Returns the raw MOEA population; each solution carries a [com.mdeo.optimizer.evaluation.WorkerSolutionRef]
     * that identifies its owning evaluator node.
     */
    fun getRawPopulation(): NondominatedPopulation = finalPopulation

    /**
     * Returns the metrics collector containing all recorded per-generation data for this run.
     *
     * @return The [OptimizationMetricsCollector] populated during the search.
     */
    fun getMetrics(): OptimizationMetricsCollector = metricsCollector
}

/**
 * Objective and constraint values for a single Pareto-optimal solution.
 *
 * @param objectives Objective function values in declaration order.
 * @param constraints Constraint violation values (zero means satisfied).
 */
data class SolutionResult(
    val objectives: List<Double>,
    val constraints: List<Double>
)
