package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Mutation strategy that reuses the same operator as long as it succeeds.
 *
 * Unlike [RandomOperatorMutationStrategy], which selects a new operator for every
 * step, this strategy keeps applying the same operator across all steps until it
 * fails. When the current operator fails, it is discarded and a new one is
 * requested from the [OperatorSelectionStrategy].
 *
 * On each mutation:
 * 1. Determines the step size (how many operators to apply).
 * 2. For each step, reuses the current operator if one exists, otherwise fetches a new one.
 * 3. If the operator succeeds, it is retained for the next step.
 * 4. If the operator fails, it is cleared and a new one is selected.
 * 5. Returns the mutated solution.
 *
 * @param transformations Map of transformation path to its compiled TypedAst.
 * @param stepSizeStrategy Strategy for determining how many operators to apply per mutation.
 * @param operatorSelectionStrategy Strategy for choosing which operator to try next.
 */
class RepetitiveOperatorMutationStrategy(
    private val transformations: Map<String, TypedAst>,
    private val stepSizeStrategy: MutationStepSizeStrategy,
    private val operatorSelectionStrategy: OperatorSelectionStrategy
) : MutationStrategy {

    private val logger = LoggerFactory.getLogger(RepetitiveOperatorMutationStrategy::class.java)
    private val attemptRunner = TransformationAttemptRunner(transformations)

    /**
     * Mutates the given solution by repeatedly applying the same operator until it fails.
     *
     * The operator path persists across steps: it is only replaced when the current
     * operator fails to apply. Each successful application adds the operator to the
     * recorded transformation step.
     *
     * @param solution The candidate solution to mutate.
     * @return The mutated solution with recorded transformation steps.
     */
    override fun mutate(solution: Solution): Solution {
        val stepSize = stepSizeStrategy.getNextStepSize(solution)
        var operatorPath: String? = null
        val stepTransformations = mutableListOf<String>()

        for (step in 1..stepSize) {
            do {
                if (operatorPath == null) {
                    operatorPath = operatorSelectionStrategy.getNextOperator(solution)
                        ?: break
                }

                if (attemptRunner.tryApply(solution, operatorPath)) {
                    stepTransformations.add(operatorPath)
                } else {
                    operatorPath = null
                }
            } while (operatorPath == null && operatorSelectionStrategy.hasUntriedOperators())

            operatorSelectionStrategy.flushTriedOperators()
        }

        solution.recordTransformationStep(stepTransformations)
        return solution
    }
}
