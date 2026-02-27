package com.mdeo.optimizer.operators

import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Determines how many transformation steps to apply per mutation.
 *
 * Ported from MutationStepSizeStrategy.java.
 */
interface MutationStepSizeStrategy {
    /**
     * Returns the number of transformation steps to apply in the next mutation.
     */
    fun getNextStepSize(solution: Solution): Int
}

/**
 * Fixed step size: always applies the configured number of steps.
 *
 * Ported from FixedMutationStepSizeStrategy.java.
 */
class FixedMutationStepSizeStrategy(private val step: Int) : MutationStepSizeStrategy {
    override fun getNextStepSize(solution: Solution): Int = step
}

/**
 * Interval step size: randomly chooses a step count from [lower, upper).
 *
 * Ported from IntervalMutationStepSizeStrategy.java.
 */
class IntervalMutationStepSizeStrategy(
    private val lower: Int,
    private val upper: Int
) : MutationStepSizeStrategy {
    private val random = java.util.Random()

    override fun getNextStepSize(solution: Solution): Int {
        return if (upper > lower) {
            lower + random.nextInt(upper - lower)
        } else {
            lower
        }
    }
}
