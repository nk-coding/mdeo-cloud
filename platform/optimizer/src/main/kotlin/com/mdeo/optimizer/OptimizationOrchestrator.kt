package com.mdeo.optimizer

import com.mdeo.optimizer.config.OptimizationConfig
import com.mdeo.optimizer.config.SolverConfig
import com.mdeo.optimizer.evaluation.EvaluationFailedException
import com.mdeo.optimizer.evaluation.MutationEvaluator
import com.mdeo.optimizer.metrics.OptimizationMetricsCollector
import com.mdeo.optimizer.moea.DelegatingAlgorithmProvider
import com.mdeo.optimizer.moea.DelegatingProblem
import com.mdeo.optimizer.moea.EvaluationCoordinator
import com.mdeo.optimizer.moea.ProgressCallbackExtension
import com.mdeo.optimizer.moea.SearchResult
import com.mdeo.optimizer.moea.TerminationConditionAdapter
import com.mdeo.optimizer.provider.OptimizationProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.moeaframework.algorithm.extension.Frequency
import org.moeaframework.analysis.runtime.Instrumenter
import org.slf4j.LoggerFactory

/**
 * Main orchestrator for optimization runs.
 *
 * Drives the evolutionary search using the delegating algorithm pattern: a
 * [DelegatingAlgorithmProvider] creates MOEA algorithm subclasses whose evaluation
 * is routed through an [EvaluationCoordinator] to the supplied [MutationEvaluator].
 * This design is backend-agnostic — the same orchestration logic works with both
 * local (in-process) and federated (multi-node) evaluators.
 *
 * @param config Full optimization configuration (problem, goal, search, solver).
 * @param evaluator The mutation evaluator that performs the actual work (local or federated).
 */
class OptimizationOrchestrator(
    private val config: OptimizationConfig,
    private val evaluator: MutationEvaluator
) {
    private val logger = LoggerFactory.getLogger(OptimizationOrchestrator::class.java)

    /**
     * Runs the optimization and returns the search result.
     *
     * Creates the [EvaluationCoordinator], [DelegatingProblem], and
     * [DelegatingAlgorithmProvider], then executes the configured number of batches.
     * The [onGenerationComplete] callback is invoked after every generation so that
     * callers can report progress and check for cancellation.
     *
     * @param onGenerationComplete Suspend callback that receives the 1-based generation counter.
     *   Throwing [kotlinx.coroutines.CancellationException] from this callback aborts the search.
     * @return The [SearchResult] containing the final approximation set and metrics.
     */
    suspend fun run(onGenerationComplete: suspend (generation: Int) -> Unit = {}): SearchResult =
        withContext(Dispatchers.IO) {
            logger.info(
                "Starting optimization: algorithm=${config.solver.algorithm}, " +
                    "objectives=${config.goal.objectives.size}, " +
                    "constraints=${config.goal.constraints.size}"
            )

            val coordinator = EvaluationCoordinator(evaluator)
            val problem = DelegatingProblem(
                numberOfObjectives = config.goal.objectives.size,
                numberOfConstraints = config.goal.constraints.size
            )
            val provider = DelegatingAlgorithmProvider(coordinator)
            val properties = provider.buildProperties(config.solver)

            val batches = config.solver.batches.coerceAtLeast(1)
            var bestResult: SearchResult? = null

            for (batch in 1..batches) {
                logger.info("Running optimization batch $batch/$batches with algorithm ${config.solver.algorithm}")

                val instrumenter = createInstrumenter()
                val algorithm = provider.getAlgorithm(config.solver.algorithm.name, properties, problem)

                val metricsCollector = OptimizationMetricsCollector()

                val progressListener = OptimizationProgressListener { generation ->
                    onGenerationComplete(generation)
                }
                algorithm.addExtension(ProgressCallbackExtension(progressListener, coordinator, metricsCollector))

                val instrumentedAlgorithm = instrumenter.instrument(algorithm)
                val terminationCondition = TerminationConditionAdapter(config.solver).create()

                try {
                    instrumentedAlgorithm.run(terminationCondition)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: EvaluationFailedException) {
                    throw e
                } catch (e: Throwable) {
                    val msg = "Unable to run search. Encountered exception: ${e.message}"
                    logger.error(msg, e)
                    throw RuntimeException(msg, e)
                }

                val result = SearchResult(instrumentedAlgorithm.getSeries(), instrumentedAlgorithm.getResult(), metricsCollector)
                if (bestResult == null) {
                    bestResult = result
                }
            }

            logger.info("Optimization completed after $batches batch(es)")
            bestResult!!
        }

    /**
     * Creates a MOEA [Instrumenter] that tracks elapsed time and population size per iteration.
     * Common JDK/framework packages are excluded to reduce instrumentation overhead.
     *
     * @return A configured [Instrumenter].
     */
    private fun createInstrumenter(): Instrumenter {
        return Instrumenter()
            .attachElapsedTimeCollector()
            .attachPopulationSizeCollector()
            .withFrequency(Frequency.ofIterations(1))
            .addExcludedPackage("jdk")
            .addExcludedPackage("sun")
            .addExcludedPackage("org.xml")
            .addExcludedPackage("javax")
            .addExcludedPackage("com.sun")
            .addExcludedPackage("org.apache")
            .addExcludedPackage("kotlinx")
            .addExcludedPackage("io.netty")
    }
}
