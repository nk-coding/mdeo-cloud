package com.mdeo.optimizer.moea

import com.mdeo.optimizer.graph.TinkerGraphBackend
import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.operators.MutationStrategy
import com.mdeo.optimizer.solution.Solution
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.moeaframework.core.initialization.RandomInitialization

/**
 * Tests for [OptimizationProblem], [OptimizationSolution], [OptimizationVariable],
 * [SolutionGenerator], and [MutationVariation].
 */
class OptimizationProblemTest {

    private class CountingObjective : GuidanceFunction {
        override val name = "VertexCount"
        override fun computeFitness(solution: Solution): Double {
            return solution.graphBackend.traversal().V().count().next().toDouble()
        }
    }

    private class NoOpMutationStrategy : MutationStrategy {
        override fun mutate(solution: Solution): Solution = solution.deepCopy()
    }

    private class AddVertexMutationStrategy : MutationStrategy {
        override fun mutate(solution: Solution): Solution {
            val copy = solution.deepCopy()
            copy.graphBackend.traversal().addV("node").next()
            return copy
        }
    }

    private fun createSolution(): Solution {
        val backend = TinkerGraphBackend()
        backend.traversal().addV("root").next()
        return Solution(backend)
    }

    @Test
    fun `SolutionGenerator creates OptimizationSolution`() {
        val gen = SolutionGenerator(
            initialSolutionProvider = { createSolution() },
            mutationStrategy = NoOpMutationStrategy()
        )

        val solution = gen.createNewSolution(1, 0)

        assertNotNull(solution)
        assertTrue(solution is OptimizationSolution)
        assertNotNull(solution.getOptimizationSolution())
    }

    @Test
    fun `OptimizationSolution copy produces independent solution`() {
        val gen = SolutionGenerator(
            initialSolutionProvider = { createSolution() },
            mutationStrategy = NoOpMutationStrategy()
        )
        val original = gen.createNewSolution(1, 0)

        val copy = original.copy() as OptimizationSolution

        // Both should have independent graph backends
        val origGraph = original.getOptimizationSolution().graphBackend
        val copyGraph = copy.getOptimizationSolution().graphBackend

        // Modify copy's graph — original should be unaffected
        copyGraph.traversal().addV("extra").next()

        val origCount = origGraph.traversal().V().count().next()
        val copyCount = copyGraph.traversal().V().count().next()

        assertNotEquals(origCount, copyCount, "Copy should be independent of original")
    }

    @Test
    fun `OptimizationVariable randomize applies mutation`() {
        val gen = SolutionGenerator(
            initialSolutionProvider = { createSolution() },
            mutationStrategy = AddVertexMutationStrategy()
        )
        val solution = gen.createNewSolution(1, 0)
        val variable = solution.getVariable(0) as OptimizationVariable

        val countBefore = variable.solution.graphBackend.traversal().V().count().next()
        variable.randomize()
        val countAfter = variable.solution.graphBackend.traversal().V().count().next()

        assertTrue(countAfter > countBefore, "Randomize should add a vertex via mutation")
    }

    @Test
    fun `OptimizationProblem evaluate computes objective values`() {
        val objectives = listOf(CountingObjective())
        val gen = SolutionGenerator(
            initialSolutionProvider = { createSolution() },
            mutationStrategy = NoOpMutationStrategy()
        )
        val problem = OptimizationProblem(objectives, emptyList(), gen)

        val solution = problem.newSolution() as OptimizationSolution
        problem.evaluate(solution)

        val objectiveValue = solution.getObjectiveValue(0)
        assertTrue(objectiveValue >= 1.0, "Should count at least 1 vertex (the root)")
    }

    @Test
    fun `OptimizationProblem newSolution returns valid solution`() {
        val gen = SolutionGenerator(
            initialSolutionProvider = { createSolution() },
            mutationStrategy = NoOpMutationStrategy()
        )
        val problem = OptimizationProblem(listOf(CountingObjective()), emptyList(), gen)

        val solution = problem.newSolution()

        assertNotNull(solution)
        assertEquals(1, solution.numberOfVariables)
        assertEquals(1, solution.numberOfObjectives)
        assertEquals(0, solution.numberOfConstraints)
    }

    @Test
    fun `MutationVariation evolve produces mutated offspring`() {
        val gen = SolutionGenerator(
            initialSolutionProvider = { createSolution() },
            mutationStrategy = AddVertexMutationStrategy()
        )

        val variation = MutationVariation(gen)
        assertEquals(1, variation.arity, "MutationVariation should have arity 1")

        val parent = gen.createNewSolution(1, 0)
        val parentCount = parent.getOptimizationSolution()
            .graphBackend.traversal().V().count().next()

        val offspring = variation.evolve(arrayOf(parent))

        assertEquals(1, offspring.size, "Should produce exactly one offspring")
        val childCount = (offspring[0] as OptimizationSolution)
            .getOptimizationSolution()
            .graphBackend.traversal().V().count().next()

        assertTrue(childCount > parentCount, "Offspring should have more vertices than parent")
    }
}
