package com.mdeo.optimizer.moea

import com.mdeo.optimizer.solution.Solution
import org.moeaframework.analysis.series.ResultSeries
import org.moeaframework.core.population.NondominatedPopulation

/**
 * Container for the results of an optimization search run.
 *
 * Ported from SearchResult.java.
 *
 * @param configuration The algorithm configuration used for this run.
 * @param instrumenter The MOEA instrumenter collecting per-step metrics.
 */
class SearchResult(
    private val configuration: AlgorithmConfiguration,
    private val series: ResultSeries,
    private val finalPopulation: NondominatedPopulation
) {
    /**
     * Gets the collected runtime series from the instrumented run.
     */
    fun getObservations(): ResultSeries = series

    /**
     * Gets the final Pareto-optimal solutions.
     * Returns pairs of (objective values, constraint values) for each solution.
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
     * Gets the underlying [Solution] graph backends for the final Pareto-optimal solutions.
     *
     * Each entry in the returned list corresponds to the solution at the same index in
     * [getFinalSolutions]. Solutions that cannot be cast to [OptimizationSolution] are skipped.
     */
    fun getFinalSolutionGraphs(): List<Solution> {
        return finalPopulation.mapNotNull { moeaSolution ->
            (moeaSolution as? OptimizationSolution)?.getOptimizationSolution()
        }
    }
}

/**
 * Summary of a single solution in the final population.
 */
data class SolutionResult(
    val objectives: List<Double>,
    val constraints: List<Double>
)
