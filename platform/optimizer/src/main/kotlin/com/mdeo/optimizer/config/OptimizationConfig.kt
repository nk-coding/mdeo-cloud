package com.mdeo.optimizer.config

import com.mdeo.optimizer.rulegen.MutationRuleSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level optimizer execution payload, normalized from config problem/goal/search/solver sections.
 * This is the single data class that the optimizer-execution service receives.
 */
@Serializable
data class OptimizationConfig(
    val problem: ProblemConfig,
    val goal: GoalConfig,
    val search: SearchConfig,
    val solver: SolverConfig
)

/**
 * Problem section: metamodel + initial model references.
 */
@Serializable
data class ProblemConfig(
    val metamodelPath: String,
    val modelPath: String
)

/**
 * Goal section: objectives, constraints, refinements.
 */
@Serializable
data class GoalConfig(
    val objectives: List<ObjectiveConfig>,
    val constraints: List<ConstraintConfig>,
    val refinements: List<RefinementConfig> = emptyList()
)

@Serializable
data class ObjectiveConfig(
    val type: ObjectiveTendency,
    val path: String,
    val functionName: String
)

@Serializable
enum class ObjectiveTendency {
    MAXIMIZE,
    MINIMIZE;

    /**
     * Returns the numerical direction: -1 for minimise, +1 for maximise.
     * MOEA framework uses minimisation internally, so maximisation objectives are negated.
     */
    fun numericalDirection(): Double = when (this) {
        MINIMIZE -> 1.0
        MAXIMIZE -> -1.0
    }
}

@Serializable
data class ConstraintConfig(
    val path: String,
    val functionName: String
)

/**
 * Multiplicity refinement that tightens the lower or upper bound of a metamodel reference
 * for the purpose of rule generation.
 *
 * When a non-empty list of refinements is passed to
 * [com.mdeo.optimizer.rulegen.MutationRuleGenerator.generate], a second generation pass is
 * performed using a modified [com.mdeo.optimizer.rulegen.MetamodelInfo] where the specified
 * reference multiplicities are replaced by the values declared here.  Rules produced in the
 * second pass are prefixed with `"S_"` to distinguish them from the base rules.
 *
 * @param className The metamodel class that owns the reference.
 * @param fieldName The reference (field) name to refine.
 * @param lower     The tightened lower bound (>= original lower).
 * @param upper     The tightened upper bound (<= original upper, or -1 for unbounded).
 */
@Serializable
data class RefinementConfig(
    val className: String,
    val fieldName: String,
    val lower: Int,
    val upper: Int
)

/**
 * Search section: mutation transformations.
 */
@Serializable
data class SearchConfig(
    val mutations: MutationsConfig
)

@Serializable
data class MutationsConfig(
    val usingPaths: List<String> = emptyList(),
    /**
     * Auto-generation specs: when non-empty, [MutationRuleGenerator] is invoked at run-time
     * to synthesise additional mutation operators directly from the metamodel structure.
     * Generated operators are merged with any hand-written transformations from [usingPaths].
     */
    val generate: List<MutationRuleSpec> = emptyList()
)

/**
 * The underlying solver framework to use.
 * Currently only MOEA Framework is supported.
 */
@Serializable
enum class SolverProvider { MOEA }

/**
 * Solver section: algorithm configuration + termination.
 *
 * @property provider Underlying solver framework.
 * @property algorithm Evolutionary algorithm to use.
 * @property parameters Algorithm hyper-parameters.
 * @property termination Termination conditions for the search.
 * @property batches Number of independent optimization runs.
 * @property scriptTimeout Per-evaluation timeout for constraint and objective scripts, in seconds
 *   (same unit as [TerminationConfig.time]).  Optional; defaults to
 *   [DEFAULT_SCRIPT_TIMEOUT_SECONDS] when `null`.  Capped at [MAX_SCRIPT_TIMEOUT_SECONDS] to
 *   prevent accidental denial-of-service.  The combined timeout for a single solution evaluation
 *   is `scriptTimeout × (objectives + constraints)`.
 */
@Serializable
data class SolverConfig(
    val provider: SolverProvider = SolverProvider.MOEA,
    val algorithm: AlgorithmType = AlgorithmType.NSGAII,
    val parameters: AlgorithmParameters = AlgorithmParameters(),
    val termination: TerminationConfig = TerminationConfig(),
    val batches: Int = 1,
    val scriptTimeout: Int? = null
) {
    companion object {
        /**
         * Default per-script timeout when none is configured: 30 seconds. 
         */
        const val DEFAULT_SCRIPT_TIMEOUT_SECONDS: Int = 30

        /**
         * Maximum allowed per-script timeout: 10 minutes (600 seconds). 
         */
        const val MAX_SCRIPT_TIMEOUT_SECONDS: Int = 600
    }

    /**
     * Returns the effective per-script evaluation timeout in milliseconds.
     * Applies the default when [scriptTimeout] is `null` and enforces the maximum cap.
     *
     * @return Timeout in milliseconds.
     */
    fun effectiveScriptTimeoutMs(): Long {
        val seconds = (scriptTimeout ?: DEFAULT_SCRIPT_TIMEOUT_SECONDS)
            .coerceIn(1, MAX_SCRIPT_TIMEOUT_SECONDS)
        return seconds.toLong() * 1000L
    }
}

@Serializable
enum class AlgorithmType {
    NSGAII, IBEA, SPEA2, SMSMOEA, VEGA, PESA2, PAES, RANDOM
}

@Serializable
data class AlgorithmParameters(
    val population: Int = 40,
    val variation: VariationType = VariationType.MUTATION,
    val mutation: MutationParameters = MutationParameters(),
    val bisections: Int? = null,
    val archiveSize: Int? = null
)

@Serializable
enum class VariationType { MUTATION, GENETIC, PROBABILISTIC }

@Serializable
data class MutationParameters(
    val step: MutationStepConfig = MutationStepConfig.Fixed(1),
    val strategy: MutationStrategy = MutationStrategy.RANDOM
)

@Serializable
enum class MutationStrategy { RANDOM, REPETITIVE }

@Serializable
sealed class MutationStepConfig {
    @Serializable
    @SerialName("Fixed")
    data class Fixed(val n: Int) : MutationStepConfig()

    @Serializable
    @SerialName("Interval")
    data class Interval(val lower: Int, val upper: Int) : MutationStepConfig()
}

@Serializable
data class TerminationConfig(
    val evolutions: Int? = null,
    val time: Int? = null,
    val delta: Double? = null,
    val iterations: Int? = null
)

/**
 * Configuration option for how the ScriptModel should convert model data.
 */
@Serializable
enum class ModelConversionStrategy {
    /**
     * Convert all model data to ScriptModel instances eagerly at binding time. 
     */
    EAGER,
    /**
     * Convert model data lazily on first access. 
     */
    LAZY
}
