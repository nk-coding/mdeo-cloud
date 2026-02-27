package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.optimizer.config.MutationParameters
import com.mdeo.optimizer.config.MutationStepConfig
import com.mdeo.optimizer.config.MutationStrategy as MutationStrategyEnum

/**
 * Factory for creating mutation-related strategy instances from config.
 *
 * Ported from MutationStrategyFactory.java + MutationStepSizeStrategyFactory.java.
 */
object MutationStrategyFactory {

    /**
     * Creates a fully-configured [MutationStrategy] from the optimizer config.
     *
     * @param params Mutation parameters from config.
     * @param transformations Map of path -> compiled TypedAst for all mutation operators.
     * @param metamodelData The metamodel.
     * @return A configured MutationStrategy.
     */
    fun create(
        params: MutationParameters,
        transformations: Map<String, TypedAst>,
        metamodelData: MetamodelData
    ): com.mdeo.optimizer.operators.MutationStrategy {
        val operatorPaths = transformations.keys.toList()
        val stepSizeStrategy = createStepSizeStrategy(params.step)
        val selectionStrategy = RandomOperatorSelection(operatorPaths)

        return when (params.strategy) {
            MutationStrategyEnum.RANDOM -> RandomOperatorMutationStrategy(
                transformations = transformations,
                metamodelData = metamodelData,
                stepSizeStrategy = stepSizeStrategy,
                operatorSelectionStrategy = selectionStrategy
            )
            MutationStrategyEnum.REPETITIVE -> {
                // Repetitive uses the same implementation but with a higher step size.
                // In the original, the only difference was that repetitive didn't shuffle.
                // For now, we use random strategy for both -- repetitive can be differentiated later.
                RandomOperatorMutationStrategy(
                    transformations = transformations,
                    metamodelData = metamodelData,
                    stepSizeStrategy = stepSizeStrategy,
                    operatorSelectionStrategy = selectionStrategy
                )
            }
        }
    }

    private fun createStepSizeStrategy(config: MutationStepConfig): MutationStepSizeStrategy {
        return when (config) {
            is MutationStepConfig.Fixed -> FixedMutationStepSizeStrategy(config.n)
            is MutationStepConfig.Interval -> IntervalMutationStepSizeStrategy(config.lower, config.upper)
        }
    }
}
