package com.mdeo.optimizer.config

import com.mdeo.optimizer.rulegen.MutationRuleSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level optimizer execution payload, normalised from config problem/goal/search/solver sections.
 */
@Serializable
data class OptimizationConfig(
    val problem: ProblemConfig,
    val goal: GoalConfig,
    val search: SearchConfig,
    val solver: SolverConfig
)

/**
 * Problem section: metamodel and initial model references.
 *
 * @param metamodelPath Path to the metamodel resource.
 * @param modelPath Path to the initial model resource.
 */
@Serializable
data class ProblemConfig(
    val metamodelPath: String,
    val modelPath: String
)

/**
 * Goal section: objectives, constraints, and optional multiplicty refinements.
 *
 * @param objectives Objective functions to optimise.
 * @param constraints Constraint functions that must be satisfied.
 * @param refinements Optional metamodel refinements used for rule generation.
 */
@Serializable
data class GoalConfig(
    val objectives: List<ObjectiveConfig>,
    val constraints: List<ConstraintConfig>,
    val refinements: List<RefinementConfig> = emptyList()
)

/**
 * Single objective or constraint function configuration.
 *
 * @param type Whether to minimise or maximise the function value.
 * @param path Path to the script file containing the function.
 * @param functionName Name of the function within the script.
 */
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

/**
 * Single constraint function configuration.
 *
 * @param path Path to the script file containing the function.
 * @param functionName Name of the function within the script.
 */
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

/** Search section: mutation transformation configuration. */
@Serializable
data class SearchConfig(
    val mutations: MutationsConfig
)

/**
 * Mutation operator source configuration.
 *
 * @param usingPaths Paths to hand-written transformation files.
 * @param generate Auto-generation specs; when non-empty, [com.mdeo.optimizer.rulegen.MutationRuleGenerator]
 *   synthesises additional operators from the metamodel and merges them with [usingPaths].
 */
@Serializable
data class MutationsConfig(
    val usingPaths: List<String> = emptyList(),
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

/** Supported evolutionary algorithm types. */
@Serializable
enum class AlgorithmType {
    NSGAII, IBEA, SPEA2, SMSMOEA, VEGA, PESA2, PAES, RANDOM
}

/**
 * Algorithm hyper-parameters.
 *
 * @param population Population size.
 * @param variation Variation operator type.
 * @param mutation Mutation configuration.
 * @param bisections Bisections for PESA2/PAES archive grid.
 * @param archiveSize Archive capacity for PESA2/PAES.
 */
@Serializable
data class AlgorithmParameters(
    val population: Int = 40,
    val variation: VariationType = VariationType.MUTATION,
    val mutation: MutationParameters = MutationParameters(),
    val bisections: Int? = null,
    val archiveSize: Int? = null
)

/** Variation operator type. */
@Serializable
enum class VariationType { MUTATION, GENETIC, PROBABILISTIC }

/**
 * Mutation operator parameters.
 *
 * @param step How many mutation steps to apply per offspring.
 * @param strategy The operator selection strategy.
 */
@Serializable
data class MutationParameters(
    val step: MutationStepConfig = MutationStepConfig.Fixed(1),
    val strategy: MutationStrategy = MutationStrategy.RANDOM
)

/** Operator selection strategy for mutation. */
@Serializable
enum class MutationStrategy { RANDOM, REPETITIVE }

/** Configures the number of mutation steps applied per offspring, either fixed or random in an interval. */
@Serializable
sealed class MutationStepConfig {
    /** Always apply exactly [n] steps. */
    @Serializable
    @SerialName("Fixed")
    data class Fixed(val n: Int) : MutationStepConfig()

    /** Randomly pick a count from [[lower], [upper]). */
    @Serializable
    @SerialName("Interval")
    data class Interval(val lower: Int, val upper: Int) : MutationStepConfig()
}

/**
 * Termination conditions for the evolutionary search.
 * All non-null conditions form a compound OR termination.
 *
 * @param evolutions Stop after this many generations (multiplied by population size for evaluations).
 * @param time Stop after this many seconds.
 * @param delta Reserved for convergence-based stopping (not yet implemented).
 * @param iterations Reserved for iteration-based stopping (not yet implemented).
 */
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
