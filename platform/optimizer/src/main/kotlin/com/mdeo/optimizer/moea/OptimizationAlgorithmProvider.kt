package com.mdeo.optimizer.moea

import com.mdeo.optimizer.config.*
import org.moeaframework.algorithm.*
import org.moeaframework.core.TypedProperties
import org.moeaframework.core.fitness.HypervolumeFitnessEvaluator
import org.moeaframework.core.initialization.RandomInitialization
import org.moeaframework.core.operator.Variation
import org.moeaframework.core.population.NondominatedPopulation
import org.moeaframework.core.population.NondominatedSortingPopulation
import org.moeaframework.core.population.Population
import org.moeaframework.core.spi.AlgorithmProvider
import org.moeaframework.problem.Problem

/**
 * Custom MOEA Framework algorithm provider for the MDEO optimizer.
 *
 * Ported from MoeaOptimisationAlgorithmProvider.java.
 * Produces algorithm instances wired with our mutation-only variation operator.
 * Crossover/breed is intentionally removed.
 *
 * @param solutionGenerator The solution generator used to create the mutation-only variation.
 */
class OptimizationAlgorithmProvider(
    private val solutionGenerator: SolutionGenerator
) : AlgorithmProvider() {

    override fun getAlgorithm(
        algorithm: String,
        properties: TypedProperties,
        problem: Problem
    ): Algorithm {
        val populationSize = properties.getInt("populationSize", 40)
        val variation = MutationVariation(solutionGenerator)
        val initialization = RandomInitialization(problem)

        return when (algorithm) {
            "NSGAII" -> NSGAII(
                problem,
                populationSize,
                NondominatedSortingPopulation(),
                null,   // no epsilon box archive
                null,   // default selection
                variation,
                initialization
            )
            "SPEA2" -> SPEA2(
                problem,
                populationSize,
                initialization,
                variation,
                populationSize,
                1
            )
            "IBEA" -> IBEA(
                problem,
                populationSize,
                null,   // default archive
                initialization,
                variation,
                HypervolumeFitnessEvaluator(problem)
            )
            "SMSEMOA" -> SMSEMOA(
                problem,
                populationSize,
                initialization,
                variation,
                HypervolumeFitnessEvaluator(problem)
            )
            "VEGA" -> VEGA(
                problem,
                populationSize,
                Population(),
                NondominatedPopulation(),
                initialization,
                variation
            )
            "PESA2" -> {
                val bisections = properties.getInt("bisections", 8)
                val archiveSize = properties.getInt("archive.size", 100)
                PESA2(problem, populationSize, variation, initialization, bisections, archiveSize)
            }
            "PAES" -> {
                // PAES uses Mutation interface; use single-arg constructor and let it configure
                PAES(problem)
            }
            "RANDOM" -> RandomSearch(
                problem,
                populationSize,
                initialization,
                NondominatedPopulation()
            )
            else -> throw IllegalArgumentException("Unknown algorithm: $algorithm")
        }
    }
}
