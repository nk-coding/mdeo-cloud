package com.mdeo.optimizer.guidance

import com.mdeo.optimizer.solution.Solution

/**
 * Interface for fitness/guidance functions used by the optimizer.
 *
 * Objectives and constraints both implement this interface.
 * Ported from IGuidanceFunction.java, adapted to use the new Solution type.
 */
interface GuidanceFunction {
    /**
     * Computes the fitness value for the given candidate solution.
     *
     * For objectives: returns a numeric fitness value (higher = better or lower = better
     * depending on the objective tendency).
     * For constraints: returns 0.0 if satisfied, non-zero if violated.
     *
     * @param solution The candidate solution to evaluate.
     * @return The computed fitness value.
     */
    fun computeFitness(solution: Solution): Double

    /**
     * Human-readable name of this guidance function.
     */
    val name: String
}
