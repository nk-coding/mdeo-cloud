package com.mdeo.optimizer.operators

import com.mdeo.optimizer.solution.Solution

/**
 * Strategy for applying mutation operators to a candidate solution.
 */
interface MutationStrategy {
    /**
     * Mutates the given solution and returns a new, mutated solution.
     * The original solution is not modified.
     *
     * @param solution The candidate solution to mutate.
     * @return A new mutated solution.
     */
    fun mutate(solution: Solution): Solution
}
