package com.mdeo.optimizer

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.optimizer.config.OptimizationConfig
import com.mdeo.optimizer.config.SolverConfig
import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.moea.AlgorithmConfiguration
import com.mdeo.optimizer.moea.MoeaOptimization
import com.mdeo.optimizer.moea.SearchResult
import com.mdeo.optimizer.moea.SolutionGenerator
import com.mdeo.optimizer.operators.MutationStrategyFactory
import com.mdeo.optimizer.solution.Solution
import org.slf4j.LoggerFactory

/**
 * Main orchestrator for optimization runs.
 *
 * Wires together the solver configuration, mutation strategy, guidance functions,
 * and the initial solution provider, then runs the evolutionary search using the
 * MOEA Framework.
 *
 * @param config Full optimization configuration (problem, goal, search, solver).
 * @param objectives Objective guidance functions.
 * @param constraints Constraint guidance functions.
 * @param transformations Pre-fetched and validated transformation typed ASTs, keyed by file path.
 * @param initialSolutionProvider Factory for creating an initial candidate solution
 *   (model loaded into a [ModelGraph][com.mdeo.modeltransformation.graph.ModelGraph]).
 */
class OptimizationOrchestrator(
    private val config: OptimizationConfig,
    private val objectives: List<GuidanceFunction>,
    private val constraints: List<GuidanceFunction>,
    private val transformations: Map<String, TypedAst>,
    private val initialSolutionProvider: () -> Solution
) {
    private val logger = LoggerFactory.getLogger(OptimizationOrchestrator::class.java)

    /**
     * Runs the optimization and returns the search result.
     *
     * Creates the mutation strategy from configuration, builds the MOEA algorithm
     * configuration, and executes the specified number of batches.  The
     * [onGenerationComplete] callback is invoked after every generation so that callers
     * can report progress and check for cancellation.
     *
     * The combined per-solution evaluation timeout is derived from
     * [SolverConfig.effectiveScriptTimeoutMs] multiplied by the total number of objective and
     * constraint functions, ensuring each individual script gets a fair share of the budget.
     *
     * @param onGenerationComplete Suspend callback that receives the 1-based generation counter.
     *   Throwing [kotlinx.coroutines.CancellationException] from this callback aborts the search.
     * @return The [SearchResult] containing the final approximation set and metrics.
     */
    suspend fun run(onGenerationComplete: suspend (generation: Int) -> Unit = {}): SearchResult {
        logger.info(
            "Starting optimization: algorithm=${config.solver.algorithm}, " +
                "objectives=${objectives.size}, constraints=${constraints.size}, " +
                "transformations=${transformations.size}, batches=${config.solver.batches}"
        )

        val perScriptTimeoutMs = config.solver.effectiveScriptTimeoutMs()
        val guidanceFunctionCount = (objectives.size + constraints.size).coerceAtLeast(1)
        val combinedScriptTimeoutMs = perScriptTimeoutMs * guidanceFunctionCount

        logger.debug(
            "Script evaluation timeout: ${perScriptTimeoutMs}ms per script × " +
                "$guidanceFunctionCount function(s) = ${combinedScriptTimeoutMs}ms combined"
        )

        val mutationStrategy = MutationStrategyFactory.create(
            config.solver.parameters.mutation,
            transformations
        )

        val solutionGenerator = SolutionGenerator(
            initialSolutionProvider = initialSolutionProvider,
            mutationStrategy = mutationStrategy
        )

        val algorithmConfig = AlgorithmConfiguration(
            solverConfig = config.solver,
            solutionGenerator = solutionGenerator,
            objectives = objectives,
            constraints = constraints,
            scriptTimeoutMs = combinedScriptTimeoutMs
        )

        val batches = config.solver.batches.coerceAtLeast(1)
        var bestResult: SearchResult? = null

        for (batch in 1..batches) {
            logger.info("Running optimization batch $batch/$batches with algorithm ${config.solver.algorithm}")
            val optimization = MoeaOptimization()
            val result = optimization.execute(algorithmConfig, onGenerationComplete)

            if (bestResult == null) {
                bestResult = result
            }
            // TODO: Compare results across batches and keep the best approximation set
        }

        logger.info("Optimization completed after $batches batch(es)")
        return bestResult!!
    }
}
