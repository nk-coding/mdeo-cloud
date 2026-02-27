package com.mdeo.optimizer.moea

import com.mdeo.optimizer.operators.MutationStrategy
import com.mdeo.optimizer.solution.Solution
import org.moeaframework.core.Solution as MoeaSolution

/**
 * MOEA Framework solution wrapping an optimizer [Solution].
 *
 * Ported from MoeaOptimisationSolution.java, minus breed/crossover support.
 */
class OptimizationSolution : MoeaSolution {

    private val mutator: (Solution) -> Solution

    /**
     * Creates a new solution by mutating an initial solution.
     */
    constructor(
        numberOfObjectives: Int,
        numberOfConstraints: Int,
        initialSolution: Solution,
        mutator: (Solution) -> Solution
    ) : super(1, numberOfObjectives, numberOfConstraints) {
        this.mutator = mutator
        val mutated = mutator(initialSolution)
        setVariable(0, OptimizationVariable(mutated, mutator))
    }

    /**
     * Internal constructor for copying.
     */
    private constructor(
        other: OptimizationSolution
    ) : super(other) {
        this.mutator = other.mutator

        // Deep copy the solution variable
        val otherSolution = other.getOptimizationSolution()
        setVariable(0, OptimizationVariable(otherSolution.deepCopy(), mutator))
    }

    override fun copy(): MoeaSolution {
        return OptimizationSolution(this)
    }

    /**
     * Gets the underlying optimizer [Solution].
     */
    fun getOptimizationSolution(): Solution {
        return (getVariable(0) as OptimizationVariable).solution
    }

    /**
     * Sets the underlying optimizer [Solution].
     */
    fun setOptimizationSolution(solution: Solution) {
        setVariable(0, OptimizationVariable(solution, mutator))
    }
}
