package com.mdeo.optimizer.moea

import com.mdeo.optimizer.config.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.moeaframework.core.TypedProperties

/**
 * Tests for [AlgorithmConfiguration].
 */
class AlgorithmConfigurationTest {

    private fun createConfig(
        algorithm: AlgorithmType = AlgorithmType.NSGAII,
        population: Int = 40,
        evolutions: Int? = 10,
        time: Int? = null,
        bisections: Int? = null,
        archiveSize: Int? = null
    ): SolverConfig {
        return SolverConfig(
            provider = SolverProvider.MOEA,
            algorithm = algorithm,
            parameters = AlgorithmParameters(
                population = population,
                bisections = bisections,
                archiveSize = archiveSize
            ),
            termination = TerminationConfig(evolutions = evolutions, time = time),
            batches = 1
        )
    }

    @Test
    fun `getProperties sets populationSize`() {
        val solver = createConfig(population = 50)
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(solver, gen, emptyList(), emptyList())

        val props = config.getProperties()

        assertEquals(50, props.getInt("populationSize", 0))
    }

    @Test
    fun `getProperties sets optional bisections and archiveSize`() {
        val solver = createConfig(bisections = 8, archiveSize = 100)
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(solver, gen, emptyList(), emptyList())

        val props = config.getProperties()

        assertEquals(8, props.getInt("bisections", 0))
        assertEquals(100, props.getInt("archive.size", 0))
    }

    @Test
    fun `algorithmName matches config`() {
        val solver = createConfig(algorithm = AlgorithmType.SPEA2)
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(solver, gen, emptyList(), emptyList())

        assertEquals("SPEA2", config.algorithmName)
    }

    @Test
    fun `createTerminationCondition uses evolutions when provided`() {
        val solver = createConfig(evolutions = 10, population = 5)
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(solver, gen, emptyList(), emptyList())

        val condition = config.createTerminationCondition()
        assertNotNull(condition)
    }

    @Test
    fun `createTerminationCondition defaults when no evolutions or time`() {
        val solver = createConfig(evolutions = null, time = null)
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(solver, gen, emptyList(), emptyList())

        val condition = config.createTerminationCondition()
        assertNotNull(condition)
    }

    @Test
    fun `createProblem returns valid problem`() {
        val solver = createConfig()
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(
            solver, gen,
            listOf(DummyGuidanceFunction("obj1")),
            listOf(DummyGuidanceFunction("cons1"))
        )

        val problem = config.createProblem()
        assertEquals(1, problem.numberOfObjectives)
        assertEquals(1, problem.numberOfConstraints)
    }

    @Test
    fun `createAlgorithmFactory returns factory with custom provider`() {
        val solver = createConfig()
        val gen = createDummySolutionGenerator()
        val config = AlgorithmConfiguration(solver, gen, emptyList(), emptyList())

        val factory = config.createAlgorithmFactory()
        assertNotNull(factory)
    }

    // -- helpers --

    private val metamodel = com.mdeo.metamodel.Metamodel.compile(com.mdeo.metamodel.data.MetamodelData())

    private fun createEmptyModelGraph(): com.mdeo.modeltransformation.graph.TinkerModelGraph {
        return com.mdeo.modeltransformation.graph.TinkerModelGraph.create(
            com.mdeo.metamodel.data.ModelData(metamodelPath = "", instances = emptyList(), links = emptyList()),
            metamodel
        )
    }

    private fun createDummySolutionGenerator(): SolutionGenerator {
        val modelGraph = createEmptyModelGraph()
        modelGraph.traversal().addV("test").next()
        return SolutionGenerator(
            initialSolutionProvider = {
                val mg = createEmptyModelGraph()
                mg.traversal().addV("test").next()
                com.mdeo.optimizer.solution.Solution(mg)
            },
            mutationStrategy = object : com.mdeo.optimizer.operators.MutationStrategy {
                override fun mutate(solution: com.mdeo.optimizer.solution.Solution) = solution.deepCopy()
            }
        )
    }

    private class DummyGuidanceFunction(override val name: String) :
        com.mdeo.optimizer.guidance.GuidanceFunction {
        override fun computeFitness(solution: com.mdeo.optimizer.solution.Solution) = 0.0
    }
}
