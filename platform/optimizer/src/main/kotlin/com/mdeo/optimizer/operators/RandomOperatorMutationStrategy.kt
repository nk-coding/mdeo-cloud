package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Mutation strategy that randomly selects and applies transformation operators.
 *
 * On each mutation:
 * 1. Deep-copies the candidate solution (copy-before-transform).
 * 2. Determines the step size (how many operators to apply).
 * 3. For each step, randomly selects an operator and attempts to apply it.
 * 4. Returns the mutated copy (original is never modified).
 *
 * @param transformations Map of transformation path to its compiled TypedAst.
 * @param stepSizeStrategy Strategy for determining how many operators to apply per mutation.
 * @param operatorSelectionStrategy Strategy for choosing which operator to try next.
 */
class RandomOperatorMutationStrategy(
    private val transformations: Map<String, TypedAst>,
    private val stepSizeStrategy: MutationStepSizeStrategy,
    private val operatorSelectionStrategy: OperatorSelectionStrategy
) : MutationStrategy {

    private val logger = LoggerFactory.getLogger(RandomOperatorMutationStrategy::class.java)
    private val attemptRunner = TransformationAttemptRunner(transformations)

    override fun mutate(solution: Solution): Solution {
        val stepSize = stepSizeStrategy.getNextStepSize(solution)
        val stepTransformations = mutableListOf<String>()

        for (step in 1..stepSize) {
            var operatorApplied = false

            do {
                val operatorPath = operatorSelectionStrategy.getNextOperator(solution)
                    ?: break

                if (attemptRunner.tryApply(solution, operatorPath)) {
                    stepTransformations.add(operatorPath)
                    operatorApplied = true
                }
            } while (!operatorApplied && operatorSelectionStrategy.hasUntriedOperators())

            operatorSelectionStrategy.flushTriedOperators()
        }

        solution.recordTransformationStep(stepTransformations)
        return solution
    }
}
