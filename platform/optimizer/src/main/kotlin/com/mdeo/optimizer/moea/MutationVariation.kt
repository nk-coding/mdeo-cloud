package com.mdeo.optimizer.moea

import org.moeaframework.core.Solution
import org.moeaframework.core.operator.Variation

/**
 * MOEA Framework variation operator that applies mutation only (no crossover/breed).
 *
 * Ported from MoeaOptimisationMutationVariation.java.
 */
class MutationVariation(
    private val solutionGenerator: SolutionGenerator
) : Variation {

    override fun getName(): String = "MdeoMutation"

    override fun evolve(parents: Array<out Solution>): Array<Solution> {
        val parent = parents[0] as OptimizationSolution
        val newSolution = parent.copy() as OptimizationSolution

        val mutated = solutionGenerator.mutate(newSolution.getOptimizationSolution())
        newSolution.setOptimizationSolution(mutated)

        return arrayOf(newSolution)
    }

    override fun getArity(): Int = 1
}
