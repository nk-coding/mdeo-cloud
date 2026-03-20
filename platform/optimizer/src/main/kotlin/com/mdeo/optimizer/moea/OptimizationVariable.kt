package com.mdeo.optimizer.moea

import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.solution.Solution
import org.moeaframework.core.Defined
import org.moeaframework.core.variable.Variable

/**
 * MOEA Framework variable wrapping an optimizer [Solution].
 *
 * Ported from MoeaOptimisationVariable.java.
 */
class OptimizationVariable(
    var solution: Solution,
    private val mutator: (Solution) -> Solution
) : Variable {

    override fun copy(): Variable {
        return OptimizationVariable(solution, mutator)
    }

    /**
     * Called by MOEA to generate initial population members.
     */
    override fun randomize() {
        solution = mutator(solution)
    }

    override fun encode(): String = "optimization-variable"

    override fun decode(value: String) {
        // Not needed — our variables are not string-encodable
    }

    override fun getName(): String = "OptimizationVariable"

    override fun getDefinition(): String =
        Defined.createUnsupportedDefinition(OptimizationVariable::class.java)
}
