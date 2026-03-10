package com.mdeo.optimizer.moea

import com.mdeo.optimizer.config.*
import com.mdeo.optimizer.graph.TinkerGraphBackend
import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.operators.MutationStrategy
import com.mdeo.optimizer.solution.Solution
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Tests for [MoeaOptimization] — the core MOEA Framework integration layer.
 *
 * These tests verify that the evolutionary search executes correctly without
 * reflection errors (InaccessibleObjectException) and produces valid results.
 */
class MoeaOptimizationTest {

    /**
     * Simple objective that counts vertices in the graph. 
     */
    private class VertexCountObjective : GuidanceFunction {
        override val name = "VertexCount"
        override fun computeFitness(solution: Solution): Double {
            return solution.graphBackend.traversal().V().count().next().toDouble()
        }
    }

    /**
     * Objective that always returns a constant — for testing search mechanics. 
     */
    private class ConstantObjective(private val value: Double) : GuidanceFunction {
        override val name = "Constant"
        override fun computeFitness(solution: Solution): Double = value
    }

    /**
     * Identity mutation strategy — returns deep copy without modification. 
     */
    private class IdentityMutationStrategy : MutationStrategy {
        override fun mutate(solution: Solution): Solution = solution.deepCopy()
    }

    /**
     * Mutation strategy that adds a random vertex to the graph. 
     */
    private class AddVertexMutationStrategy : MutationStrategy {
        override fun mutate(solution: Solution): Solution {
            val copy = solution.deepCopy()
            copy.graphBackend.traversal().addV("node").next()
            return copy
        }
    }

    private fun createInitialSolution(): Solution {
        val backend = TinkerGraphBackend()
        // Seed the graph with a single vertex
        backend.traversal().addV("root").property("name", "initial").next()
        return Solution(backend)
    }

    private fun createAlgorithmConfig(
        algorithmType: AlgorithmType = AlgorithmType.NSGAII,
        objectives: List<GuidanceFunction> = listOf(VertexCountObjective()),
        constraints: List<GuidanceFunction> = emptyList(),
        mutationStrategy: MutationStrategy = AddVertexMutationStrategy(),
        populationSize: Int = 10,
        evolutions: Int = 5
    ): AlgorithmConfiguration {
        val solverConfig = SolverConfig(
            provider = SolverProvider.MOEA,
            algorithm = algorithmType,
            parameters = AlgorithmParameters(population = populationSize),
            termination = TerminationConfig(evolutions = evolutions),
            batches = 1
        )
        val solutionGenerator = SolutionGenerator(
            initialSolutionProvider = { createInitialSolution() },
            mutationStrategy = mutationStrategy
        )
        return AlgorithmConfiguration(
            solverConfig = solverConfig,
            solutionGenerator = solutionGenerator,
            objectives = objectives,
            constraints = constraints
        )
    }

    @Test
    fun `execute completes without InaccessibleObjectException`() = runBlocking {
        val config = createAlgorithmConfig()
        val optimization = MoeaOptimization()

        val result = assertDoesNotThrow { runBlocking { optimization.execute(config) } }

        assertNotNull(result)
        assertNotNull(result.getObservations())
    }

    @Test
    fun `execute returns non-empty result series`() = runBlocking {
        val config = createAlgorithmConfig(evolutions = 3)
        val optimization = MoeaOptimization()

        val result = optimization.execute(config)
        val series = result.getObservations()

        assertTrue(series.size() > 0, "Expected at least one observation in the result series")
    }

    @Test
    fun `execute returns final solutions`() = runBlocking {
        val config = createAlgorithmConfig()
        val optimization = MoeaOptimization()

        val result = optimization.execute(config)
        val solutions = result.getFinalSolutions()

        assertFalse(solutions.isEmpty(), "Expected at least one solution in the result")
        solutions.forEach { sol ->
            assertEquals(1, sol.objectives.size, "Expected one objective value per solution")
        }
    }

    @Test
    fun `execute with identity mutation does not crash`() = runBlocking {
        val config = createAlgorithmConfig(mutationStrategy = IdentityMutationStrategy())
        val optimization = MoeaOptimization()

        val result = assertDoesNotThrow { runBlocking { optimization.execute(config) } }
        assertNotNull(result)
    }

    @Test
    fun `execute with multiple objectives works`() = runBlocking {
        val objectives = listOf(
            VertexCountObjective(),
            ConstantObjective(42.0)
        )
        val config = createAlgorithmConfig(objectives = objectives)
        val optimization = MoeaOptimization()

        val result = optimization.execute(config)
        val solutions = result.getFinalSolutions()

        assertFalse(solutions.isEmpty())
        solutions.forEach { sol ->
            assertEquals(2, sol.objectives.size, "Expected two objective values per solution")
        }
    }

    @Test
    fun `execute with constraints works`() = runBlocking {
        val constraints = listOf(ConstantObjective(0.0)) // Always satisfied
        val config = createAlgorithmConfig(constraints = constraints)
        val optimization = MoeaOptimization()

        val result = optimization.execute(config)
        val solutions = result.getFinalSolutions()

        assertFalse(solutions.isEmpty())
        solutions.forEach { sol ->
            assertEquals(1, sol.constraints.size, "Expected one constraint value")
        }
    }

    @ParameterizedTest
    @EnumSource(value = AlgorithmType::class, names = ["NSGAII", "SPEA2", "RANDOM"])
    fun `execute works with different algorithms`(algorithmType: AlgorithmType) = runBlocking {
        val config = createAlgorithmConfig(
            algorithmType = algorithmType,
            populationSize = 10,
            evolutions = 3
        )
        val optimization = MoeaOptimization()

        val result = assertDoesNotThrow { runBlocking { optimization.execute(config) } }
        assertNotNull(result)
        assertFalse(result.getFinalSolutions().isEmpty())
    }
}
