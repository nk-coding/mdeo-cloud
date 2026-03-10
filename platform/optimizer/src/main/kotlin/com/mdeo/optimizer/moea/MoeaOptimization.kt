package com.mdeo.optimizer.moea

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.moeaframework.algorithm.extension.Frequency
import org.moeaframework.analysis.runtime.Instrumenter
import org.moeaframework.analysis.series.ResultSeries
import org.moeaframework.core.population.NondominatedPopulation
import org.slf4j.LoggerFactory

/**
 * MOEA Framework-based optimizer execution.
 *
 * Ported from MoeaOptimisation.java.
 * Drives the evolutionary search using MOEA Framework's Executor.
 */
class MoeaOptimization {

    private val logger = LoggerFactory.getLogger(MoeaOptimization::class.java)

    /**
     * Runs the optimization and returns the result.
     *
     * Iterates the algorithm one generation at a time so that [onGenerationComplete] can update
     * progress and check for cancellation after every generation.  The callback is invoked as a
     * suspend function, allowing lightweight IO work (e.g. a progress API call) between steps
     * without blocking the compute thread.
     *
     * @param config Algorithm configuration.
     * @param onGenerationComplete Suspend callback invoked after each completed generation.
     *   Receives the 1-based generation index.  Throwing [kotlinx.coroutines.CancellationException]
     *   from this callback will abort the search.
     * @return The search result containing the final population.
     */
    suspend fun execute(
        config: AlgorithmConfiguration,
        onGenerationComplete: suspend (generation: Int) -> Unit = {}
    ): SearchResult = withContext(Dispatchers.Default) {
        val instrumenter = createInstrumenter()
        val algorithm = config.createAlgorithmFactory()
            .getAlgorithm(config.algorithmName, config.getProperties(), config.createProblem())
        val instrumentedAlgorithm = instrumenter.instrument(algorithm)

        val terminationCondition = config.createTerminationCondition()
        terminationCondition.initialize(instrumentedAlgorithm)

        var generation = 0
        try {
            while (!terminationCondition.shouldTerminate(instrumentedAlgorithm)) {
                instrumentedAlgorithm.step()
                generation++
                onGenerationComplete(generation)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = "Unable to run search. Encountered exception: ${e.message}"
            logger.error(msg, e)
            throw RuntimeException(msg, e)
        }

        SearchResult(config, instrumentedAlgorithm.getSeries(), instrumentedAlgorithm.getResult())
    }

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
