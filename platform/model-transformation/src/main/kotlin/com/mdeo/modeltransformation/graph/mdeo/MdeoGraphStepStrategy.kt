package com.mdeo.modeltransformation.graph.mdeo

import org.apache.tinkerpop.gremlin.process.traversal.Step
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.step.HasContainerHolder
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.HasStep
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStepContract
import org.apache.tinkerpop.gremlin.process.traversal.step.map.NoOpBarrierStep
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper
import org.apache.tinkerpop.gremlin.structure.Element

/**
 * Traversal strategy that replaces generic [GraphStep] instances with
 * optimized [MdeoGraphStep] instances for [MdeoGraph].
 *
 * This strategy absorbs adjacent [HasStep] containers into the [MdeoGraphStep]
 * for more efficient filtering at the graph level rather than the traversal level.
 */
class MdeoGraphStepStrategy private constructor() :
    AbstractTraversalStrategy<TraversalStrategy.ProviderOptimizationStrategy>(),
    TraversalStrategy.ProviderOptimizationStrategy {

    companion object {
        private val INSTANCE = MdeoGraphStepStrategy()

        /**
         * Returns the singleton instance.
         *
         * @return The strategy instance.
         */
        @JvmStatic
        fun instance(): MdeoGraphStepStrategy = INSTANCE
    }

    @Suppress("UNCHECKED_CAST")
    override fun apply(traversal: Traversal.Admin<*, *>) {
        for (originalGraphStep in TraversalHelper.getStepsOfClass(GraphStepContract::class.java, traversal)) {
            @Suppress("UNCHECKED_CAST")
            val mdeoGraphStep = MdeoGraphStep(originalGraphStep as GraphStepContract<Any, Element>)
            TraversalHelper.replaceStep(originalGraphStep as Step<Any, Element>, mdeoGraphStep, traversal)
            var currentStep: Step<*, *> = mdeoGraphStep.nextStep
            while (currentStep is HasStep<*> || currentStep is NoOpBarrierStep<*>) {
                if (currentStep is HasStep<*>) {
                    val hasContainers = (currentStep as HasContainerHolder<*, *>).hasContainers
                    for (hasContainer in hasContainers) {
                        if (!GraphStep.processHasContainerIds(mdeoGraphStep, hasContainer)) {
                            mdeoGraphStep.addHasContainer(hasContainer)
                        }
                    }
                    TraversalHelper.copyLabels(currentStep as Step<*, *>, currentStep.previousStep as Step<*, *>, false)
                    traversal.removeStep<Any, Any>(currentStep)
                }
                currentStep = currentStep.nextStep
            }
        }
    }
}
