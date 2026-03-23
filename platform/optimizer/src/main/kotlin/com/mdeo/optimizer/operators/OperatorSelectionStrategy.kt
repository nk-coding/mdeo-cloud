package com.mdeo.optimizer.operators

import com.mdeo.optimizer.solution.Solution

/**
 * Strategy for selecting which transformation operator to apply next.
 */
interface OperatorSelectionStrategy {
    /**
     * Returns the path of the next transformation operator to try,
     * or null if all operators have been tried in this step.
     */
    fun getNextOperator(solution: Solution): String?

    /**
     * Whether there are untried operators remaining in this step.
     */
    fun hasUntriedOperators(): Boolean

    /**
     * Resets the tried operators for the next step.
     */
    fun flushTriedOperators()
}

/**
 * Randomly selects operators, trying each at most once per step before declaring exhaustion.
 *
 * @param operatorPaths The full list of available transformation operator paths.
 */
class RandomOperatorSelection(
    private val operatorPaths: List<String>
) : OperatorSelectionStrategy {

    private val triedOperators = mutableSetOf<String>()

    override fun getNextOperator(solution: Solution): String? {
        val remaining = operatorPaths.filter { it !in triedOperators }
        if (remaining.isEmpty()) return null

        val selected = remaining.random()
        triedOperators.add(selected)
        return selected
    }

    override fun hasUntriedOperators(): Boolean {
        return triedOperators.size < operatorPaths.size
    }

    override fun flushTriedOperators() {
        triedOperators.clear()
    }
}
