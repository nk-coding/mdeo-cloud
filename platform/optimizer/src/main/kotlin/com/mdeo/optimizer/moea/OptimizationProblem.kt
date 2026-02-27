package com.mdeo.optimizer.moea

import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.solution.Solution
import org.moeaframework.core.Solution as MoeaSolution
import org.moeaframework.problem.AbstractProblem

/**
 * MOEA Framework problem definition for the optimizer.
 *
 * Evaluates candidate solutions using configured objective and constraint functions.
 * Ported from MoeaOptimisationProblem.java.
 *
 * @param objectives The objective fitness functions.
 * @param constraints The constraint fitness functions.
 * @param solutionGenerator Generates new/initial solutions.
 */
class OptimizationProblem(
    private val objectives: List<GuidanceFunction>,
    private val constraints: List<GuidanceFunction>,
    private val solutionGenerator: SolutionGenerator
) : AbstractProblem(1, objectives.size, constraints.size) {

    override fun evaluate(solution: MoeaSolution) {
        val optSolution = (solution as OptimizationSolution)
        val candidate = optSolution.getOptimizationSolution()

        // Evaluate objectives
        objectives.forEachIndexed { index, objective ->
            optSolution.setObjectiveValue(index, objective.computeFitness(candidate))
        }

        // Evaluate constraints
        constraints.forEachIndexed { index, constraint ->
            optSolution.setConstraintValue(index, constraint.computeFitness(candidate))
        }
    }

    override fun newSolution(): MoeaSolution {
        return solutionGenerator.createNewSolution(numberOfObjectives, numberOfConstraints)
    }
}
