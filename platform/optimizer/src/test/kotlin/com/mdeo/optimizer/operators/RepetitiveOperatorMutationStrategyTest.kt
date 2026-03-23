package com.mdeo.optimizer.operators

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.graph.TinkerModelGraph
import com.mdeo.optimizer.solution.Solution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [RepetitiveOperatorMutationStrategy].
 *
 * Uses a no-op [TypedAst] (empty statements) for operators that should succeed,
 * and an AST containing an unrecognised statement kind for operators that should
 * fail. The key property under test is that the repetitive strategy reuses a
 * successful operator and only fetches a new one on failure.
 */
class RepetitiveOperatorMutationStrategyTest {

    private val metamodel = Metamodel.compile(MetamodelData())

    /**
     * A TypedAst with no statements — executes as a successful no-op. 
     */
    private val noOpAst = TypedAst(types = emptyList(), metamodelPath = "", statements = emptyList())

    /**
     * A TypedAst whose statement has an unregistered kind, causing a Failure result. 
     */
    private val failAst = TypedAst(
        types = emptyList(),
        metamodelPath = "",
        statements = listOf(object : TypedTransformationStatement {
            override val kind: String = "__test_unknown_kind__"
        })
    )

    /**
     * Step-size strategy that always returns a fixed value.
     */
    private class FixedStepSize(private val size: Int) : MutationStepSizeStrategy {
        override fun getNextStepSize(solution: Solution): Int = size
    }

    /**
     * Controllable operator selection that returns operators from a predefined list.
     * Tracks how many times [getNextOperator] is called and how many flushes occur.
     */
    private class FakeOperatorSelection(
        private val operators: List<String>
    ) : OperatorSelectionStrategy {
        var getNextOperatorCalls = 0
            private set
        var flushCalls = 0
            private set

        private var index = 0
        private val triedThisStep = mutableSetOf<String>()

        override fun getNextOperator(solution: Solution): String? {
            getNextOperatorCalls++
            if (index >= operators.size) return null
            val op = operators[index++]
            triedThisStep.add(op)
            return op
        }

        override fun hasUntriedOperators(): Boolean = index < operators.size

        override fun flushTriedOperators() {
            flushCalls++
            triedThisStep.clear()
        }
    }

    private fun createSolution(): Solution {
        return Solution(
            TinkerModelGraph.create(
                ModelData(metamodelPath = "", instances = emptyList(), links = emptyList()),
                metamodel
            )
        )
    }

    @Test
    fun `reuses same operator when it keeps succeeding`() {
        // Only "opA" is a real transformation (no-op); opB and opC are decoys.
        val transformations = mapOf("opA" to noOpAst)
        val selection = FakeOperatorSelection(listOf("opA", "opB", "opC"))
        val strategy = RepetitiveOperatorMutationStrategy(
            transformations = transformations,
            stepSizeStrategy = FixedStepSize(3),
            operatorSelectionStrategy = selection
        )

        val solution = createSolution()
        strategy.mutate(solution)

        // Operator was fetched once then reused for all 3 steps
        assertEquals(1, selection.getNextOperatorCalls)
        assertEquals(listOf("opA", "opA", "opA"), solution.transformationsChain.last())
    }

    @Test
    fun `fetches new operator when current one fails`() {
        // "opA" maps to a failing AST, "opB" is a valid no-op.
        val transformations = mapOf("opA" to failAst, "opB" to noOpAst)
        val selection = FakeOperatorSelection(listOf("opA", "opB"))
        val strategy = RepetitiveOperatorMutationStrategy(
            transformations = transformations,
            stepSizeStrategy = FixedStepSize(1),
            operatorSelectionStrategy = selection
        )

        val solution = createSolution()
        strategy.mutate(solution)

        // opA tried and failed (unregistered statement kind), then opB tried and succeeded
        assertEquals(2, selection.getNextOperatorCalls)
        assertEquals(listOf("opB"), solution.transformationsChain.last())
    }

    @Test
    fun `operator persists across multiple steps`() {
        val transformations = mapOf("opA" to noOpAst, "opB" to noOpAst)
        val selection = FakeOperatorSelection(listOf("opA", "opB"))
        val strategy = RepetitiveOperatorMutationStrategy(
            transformations = transformations,
            stepSizeStrategy = FixedStepSize(5),
            operatorSelectionStrategy = selection
        )

        val solution = createSolution()
        strategy.mutate(solution)

        // opA succeeds on step 1 and is reused for all 5 steps
        assertEquals(1, selection.getNextOperatorCalls)
        val recorded = solution.transformationsChain.last()
        assertEquals(5, recorded.size)
        assertTrue(recorded.all { it == "opA" })
    }

    @Test
    fun `flushTriedOperators called once per step`() {
        val transformations = mapOf("opA" to noOpAst)
        val selection = FakeOperatorSelection(listOf("opA"))
        val strategy = RepetitiveOperatorMutationStrategy(
            transformations = transformations,
            stepSizeStrategy = FixedStepSize(3),
            operatorSelectionStrategy = selection
        )

        val solution = createSolution()
        strategy.mutate(solution)

        assertEquals(3, selection.flushCalls)
    }

    @Test
    fun `handles no operators available`() {
        val transformations = emptyMap<String, TypedAst>()
        val selection = FakeOperatorSelection(emptyList())
        val strategy = RepetitiveOperatorMutationStrategy(
            transformations = transformations,
            stepSizeStrategy = FixedStepSize(2),
            operatorSelectionStrategy = selection
        )

        val solution = createSolution()
        val result = strategy.mutate(solution)

        assertSame(solution, result)
        assertEquals(listOf(emptyList<String>()), result.transformationsChain)
    }

    @Test
    fun `step size of zero produces empty transformation step`() {
        val transformations = mapOf("opA" to noOpAst)
        val selection = FakeOperatorSelection(listOf("opA"))
        val strategy = RepetitiveOperatorMutationStrategy(
            transformations = transformations,
            stepSizeStrategy = FixedStepSize(0),
            operatorSelectionStrategy = selection
        )

        val solution = createSolution()
        strategy.mutate(solution)

        assertEquals(0, selection.getNextOperatorCalls)
        assertEquals(listOf(emptyList<String>()), solution.transformationsChain)
    }
}
