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
 */
@Serializable
data class SolverConfig(
    val provider: SolverProvider = SolverProvider.MOEA,
    val algorithm: AlgorithmType = AlgorithmType.NSGAII,
    val parameters: AlgorithmParameters = AlgorithmParameters(),
    val termination: TerminationConfig = TerminationConfig(),
    val batches: Int = 1
)

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
    /** Convert all model data to ScriptModel instances eagerly at binding time. */
    EAGER,
    /** Convert model data lazily on first access. */
    LAZY
}
