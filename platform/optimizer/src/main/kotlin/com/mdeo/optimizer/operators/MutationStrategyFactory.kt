package com.mdeo.optimizer.operators

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.optimizer.config.MutationParameters
import com.mdeo.optimizer.config.MutationStepConfig
import com.mdeo.optimizer.config.MutationStrategy as MutationStrategyEnum

/**
 * Factory for creating mutation-related strategy instances from config.
 */
object MutationStrategyFactory {

    /**
     * Creates a fully-configured [MutationStrategy] from the optimizer config.
     *
     * @param params Mutation parameters from config.
     * @param transformations Map of path to compiled TypedAst for all mutation operators.
     * @return A configured MutationStrategy.
     */
    fun create(
        params: MutationParameters,
        transformations: Map<String, TypedAst>
    ): com.mdeo.optimizer.operators.MutationStrategy {
        val operatorPaths = transformations.keys.toList()
        val stepSizeStrategy = createStepSizeStrategy(params.step)
        val selectionFactory: () -> OperatorSelectionStrategy = { RandomOperatorSelection(operatorPaths) }

        return when (params.strategy) {
            MutationStrategyEnum.RANDOM -> RandomOperatorMutationStrategy(
                transformations = transformations,
                stepSizeStrategy = stepSizeStrategy,
                operatorSelectionStrategyFactory = selectionFactory
            )
            MutationStrategyEnum.REPETITIVE -> {
                RepetitiveOperatorMutationStrategy(
                    transformations = transformations,
                    stepSizeStrategy = stepSizeStrategy,
                    operatorSelectionStrategyFactory = selectionFactory
                )
            }
        }
    }

    /**
     * Creates a [MutationStepSizeStrategy] from the given step configuration.
     *
     * @param config The step size configuration (fixed or interval).
     * @return A configured [MutationStepSizeStrategy].
     */
    private fun createStepSizeStrategy(config: MutationStepConfig): MutationStepSizeStrategy {
        return when (config) {
            is MutationStepConfig.Fixed -> FixedMutationStepSizeStrategy(config.n)
            is MutationStepConfig.Interval -> IntervalMutationStepSizeStrategy(config.lower, config.upper)
        }
    }
}
