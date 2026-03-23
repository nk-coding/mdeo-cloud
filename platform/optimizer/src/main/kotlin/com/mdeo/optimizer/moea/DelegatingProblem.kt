package com.mdeo.optimizer.moea

import org.moeaframework.core.Solution
import org.moeaframework.problem.AbstractProblem

/**
 * MOEA Framework problem definition for delegating-evaluation mode.
 *
 * In this mode, actual models live externally — either on remote worker nodes or
 * managed by a [com.mdeo.optimizer.evaluation.MutationEvaluator]. This problem
 * creates empty MOEA solutions with no decision variables, and its [evaluate]
 * method is a no-op because evaluation is handled by [EvaluationCoordinator]
 * through the overridden `evaluateAll`/`evaluate` methods in the delegating
 * algorithm subclasses.
 *
 * @param numberOfObjectives The number of objective functions.
 * @param numberOfConstraints The number of constraint functions.
 */
class DelegatingProblem(
    numberOfObjectives: Int,
    numberOfConstraints: Int
) : AbstractProblem(0, numberOfObjectives, numberOfConstraints) {

    override fun evaluate(solution: Solution) {}

    override fun newSolution(): Solution {
        return Solution(0, numberOfObjectives, numberOfConstraints)
    }
}
