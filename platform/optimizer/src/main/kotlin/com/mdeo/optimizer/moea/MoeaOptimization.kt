package com.mdeo.optimizer.moea

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
     * @param config Algorithm configuration.
     * @return The search result containing the final population.
     */
    fun execute(config: AlgorithmConfiguration): SearchResult {
        val instrumenter = createInstrumenter()
        val algorithm = config.createAlgorithmFactory()
            .getAlgorithm(config.algorithmName, config.getProperties(), config.createProblem())
        val instrumentedAlgorithm = instrumenter.instrument(algorithm)

        try {
            instrumentedAlgorithm.run(config.createTerminationCondition())
        } catch (e: Exception) {
            val msg = "Unable to run search. Encountered exception: ${e.message}"
            logger.error(msg, e)
            throw RuntimeException(msg, e)
        }

        return SearchResult(config, instrumentedAlgorithm.getSeries(), instrumentedAlgorithm.getResult())
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
