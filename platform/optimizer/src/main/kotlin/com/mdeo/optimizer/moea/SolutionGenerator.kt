package com.mdeo.optimizer.moea

import com.mdeo.optimizer.operators.MutationStrategy
import com.mdeo.optimizer.solution.Solution

/**
 * Generates new and initial solutions for the optimizer.
 *
 * This is the bridge between the optimizer's model loading and MOEA's population initialization.
 * Separating this allows future distribution: initial solution generation could be parallelized
 * across compute nodes.
 *
 * @param initialSolutionProvider Provides the initial solution to mutate from.
 * @param mutationStrategy The mutation strategy for generating new solutions.
 */
class SolutionGenerator(
    private val initialSolutionProvider: () -> Solution,
    private val mutationStrategy: MutationStrategy
) {
    /**
     * Creates a new MOEA solution by mutating the initial model.
     */
    fun createNewSolution(numberOfObjectives: Int, numberOfConstraints: Int): OptimizationSolution {
        val initial = initialSolutionProvider()
        return OptimizationSolution(
            numberOfObjectives = numberOfObjectives,
            numberOfConstraints = numberOfConstraints,
            initialSolution = initial,
            mutator = { solution -> mutationStrategy.mutate(solution) }
        )
    }

    /**
     * Mutates a given solution. Used by MOEA variation operators.
     */
    fun mutate(solution: Solution): Solution {
        return mutationStrategy.mutate(solution)
    }
}
