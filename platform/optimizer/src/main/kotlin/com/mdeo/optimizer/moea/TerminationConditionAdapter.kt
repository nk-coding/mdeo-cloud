package com.mdeo.optimizer.moea

import com.mdeo.optimizer.config.SolverConfig
import java.time.Duration
import org.moeaframework.core.termination.CompoundTerminationCondition
import org.moeaframework.core.termination.MaxElapsedTime
import org.moeaframework.core.termination.MaxFunctionEvaluations
import org.moeaframework.core.termination.TerminationCondition

/**
 * Builds a MOEA Framework [TerminationCondition] from [SolverConfig] settings.
 *
 * This mirrors the MDE Optimiser's `TerminationConditionAdapter` pattern: termination
 * configuration is encapsulated in its own class rather than being embedded inside the
 * algorithm configuration, making it reusable across different optimization providers.
 *
 * @param solverConfig The solver configuration whose termination section drives condition creation.
 */
class TerminationConditionAdapter(
    private val solverConfig: SolverConfig
) {

    /**
     * Creates a compound [TerminationCondition] from the configured evolutions and/or time limits.
     *
     * When no termination criteria are configured, a default of 100 evolutions is applied.
     *
     * @return A [TerminationCondition] representing all configured stopping criteria.
     */
    fun create(): TerminationCondition {
        val conditions = mutableListOf<TerminationCondition>()
        val termination = solverConfig.termination

        termination.evolutions?.let { evolutions ->
            val populationSize = solverConfig.parameters.population
            val maxEvaluations = if (populationSize > 0) evolutions * populationSize else evolutions
            conditions.add(MaxFunctionEvaluations(maxEvaluations))
        }

        termination.time?.let { seconds ->
            conditions.add(MaxElapsedTime(Duration.ofSeconds(seconds.toLong())))
        }

        if (conditions.isEmpty()) {
            conditions.add(MaxFunctionEvaluations(100 * solverConfig.parameters.population))
        }

        return CompoundTerminationCondition(*conditions.toTypedArray())
    }
}
