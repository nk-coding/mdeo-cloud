package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.expression.ast.types.MetamodelData
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
 * Ported from RandomOperatorMutationStrategy.java + AbstractMutationStrategy.java.
 */
class RandomOperatorMutationStrategy(
    private val transformations: Map<String, TypedAst>,
    private val metamodelData: MetamodelData,
    private val stepSizeStrategy: MutationStepSizeStrategy,
    private val operatorSelectionStrategy: OperatorSelectionStrategy
) : MutationStrategy {

    private val logger = LoggerFactory.getLogger(RandomOperatorMutationStrategy::class.java)
    private val attemptRunner = TransformationAttemptRunner(transformations, metamodelData)

    override fun mutate(solution: Solution): Solution {
        // Always deep-copy before mutating
        val candidateSolution = solution.deepCopy()

        val stepSize = stepSizeStrategy.getNextStepSize(candidateSolution)
        val stepTransformations = mutableListOf<String>()

        for (step in 1..stepSize) {
            var operatorApplied = false

            do {
                val operatorPath = operatorSelectionStrategy.getNextOperator(candidateSolution)
                    ?: break // No more operators to try

                if (attemptRunner.tryApply(candidateSolution, operatorPath)) {
                    stepTransformations.add(operatorPath)
                    operatorApplied = true
                }
            } while (!operatorApplied && operatorSelectionStrategy.hasUntriedOperators())

            operatorSelectionStrategy.flushTriedOperators()
        }

        candidateSolution.recordTransformationStep(stepTransformations)
        return candidateSolution
    }
}
