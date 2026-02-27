package com.mdeo.optimizer.moea

import com.mdeo.optimizer.config.SolverConfig
import com.mdeo.optimizer.config.TerminationConfig
import com.mdeo.optimizer.guidance.GuidanceFunction
import java.time.Duration
import org.moeaframework.core.TypedProperties
import org.moeaframework.core.spi.AlgorithmFactory
import org.moeaframework.core.termination.CompoundTerminationCondition
import org.moeaframework.core.termination.MaxElapsedTime
import org.moeaframework.core.termination.MaxFunctionEvaluations
import org.moeaframework.core.termination.TerminationCondition
import org.moeaframework.problem.Problem

/**
 * Configuration container for a MOEA Framework algorithm run.
 *
 * Ported from MoeaFrameworkAlgorithmConfiguration.java.
 *
 * @param solverConfig Solver configuration from the optimizer config.
 * @param solutionGenerator The solution generator to wire into the algorithm.
 * @param objectives List of objective functions.
 * @param constraints List of constraint functions.
 */
class AlgorithmConfiguration(
    private val solverConfig: SolverConfig,
    val solutionGenerator: SolutionGenerator,
    private val objectives: List<GuidanceFunction>,
    private val constraints: List<GuidanceFunction>
) {
    /**
     * Builds MOEA typed properties from the solver config.
     */
    fun getProperties(): TypedProperties {
        val props = TypedProperties()
        val params = solverConfig.parameters

        props.setInt("populationSize", params.population)

        solverConfig.parameters.bisections?.let { props.setInt("bisections", it) }
        solverConfig.parameters.archiveSize?.let { props.setInt("archive.size", it) }

        return props
    }

    /**
     * The algorithm name string for MOEA Framework.
     */
    val algorithmName: String get() = solverConfig.algorithm.name

    /**
     * Creates the MOEA problem.
     */
    fun createProblem(): Problem {
        return OptimizationProblem(objectives, constraints, solutionGenerator)
    }

    /**
     * Creates the MOEA algorithm factory with our custom provider.
     */
    fun createAlgorithmFactory(): AlgorithmFactory {
        val factory = AlgorithmFactory()
        factory.addProvider(OptimizationAlgorithmProvider(solutionGenerator))
        return factory
    }

    /**
     * Creates the termination condition(s) from config.
     */
    fun createTerminationCondition(): TerminationCondition {
        val conditions = mutableListOf<TerminationCondition>()
        val termination = solverConfig.termination

        termination.evolutions?.let { evolutions ->
            val populationSize = solverConfig.parameters.population
            val maxEvaluations = if (populationSize > 0) evolutions * populationSize else evolutions
            conditions.add(MaxFunctionEvaluations(maxEvaluations))
        }

        termination.time?.let { seconds ->
            conditions.add(MaxElapsedTime(Duration.ofSeconds(seconds.toLong())))
        }

        // Delta termination: check if the population is converged
        // For now, skip delta as it requires instrumentation — use evolutions/time instead.
        // This can be added back when the DeltaTerminationCondition is ported.

        if (conditions.isEmpty()) {
            // Default: 100 evolutions
            conditions.add(MaxFunctionEvaluations(100 * solverConfig.parameters.population))
        }

        return CompoundTerminationCondition(
            *conditions.toTypedArray()
        )
    }
}
