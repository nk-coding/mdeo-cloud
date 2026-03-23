package com.mdeo.optimizer.operators

import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Determines how many transformation steps to apply per mutation.
 */
interface MutationStepSizeStrategy {
    /**
     * Returns the number of transformation steps to apply in the next mutation.
     */
    fun getNextStepSize(solution: Solution): Int
}

/**
 * Fixed step size: always applies [step] transformation steps per mutation.
 *
 * @param step The fixed number of steps to apply.
 */
class FixedMutationStepSizeStrategy(private val step: Int) : MutationStepSizeStrategy {
    override fun getNextStepSize(solution: Solution): Int = step
}

/**
 * Interval step size: randomly picks a step count from [[lower], [upper]).
 *
 * @param lower Minimum number of steps (inclusive).
 * @param upper Maximum number of steps (exclusive); falls back to [lower] if <= [lower].
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
