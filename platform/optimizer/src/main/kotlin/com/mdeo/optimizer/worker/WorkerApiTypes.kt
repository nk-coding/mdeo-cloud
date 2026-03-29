package com.mdeo.optimizer.worker

import com.mdeo.metamodel.SerializedModel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.optimizer.config.GoalConfig
import com.mdeo.optimizer.config.SolverConfig
import kotlinx.serialization.Serializable

/**
 * Request sent by the orchestrator to allocate a worker for a new optimization run.
 *
 * Contains everything the worker needs to set up its local evaluation environment:
 * the metamodel, an initial model, pre-serialized transformation and script ASTs,
 * goal/solver configuration, and how many initial solutions to generate.
 *
 * @param executionId Unique identifier for the optimization execution.
 * @param metamodelData The metamodel that governs model structure.
 * @param initialModelData The seed model from which initial solutions derive.
 * @param transformationAstJsons Map of transformation path to its JSON-serialized
 *        [com.mdeo.modeltransformation.ast.TypedAst]. Workers deserialize these with
 *        the appropriate serializer modules on their side.
 * @param scriptAstJsons Map of script path to its JSON-serialized
 *        [com.mdeo.script.ast.TypedAst]. Workers deserialize these with the
 *        appropriate contextual serializer modules on their side.
 * @param goalConfig Objective and constraint definitions for fitness evaluation.
 * @param solverConfig Solver parameters (algorithm, population size, etc.).
 * @param initialSolutionCount Number of initial solutions the worker should generate
 *        and evaluate during allocation.
 */
@Serializable
data class WorkerAllocationRequest(
    val executionId: String,
    val metamodelData: MetamodelData,
    val initialModelData: ModelData,
    val transformationAstJsons: Map<String, String>,
    val scriptAstJsons: Map<String, String>,
    val goalConfig: GoalConfig,
    val solverConfig: SolverConfig,
    val initialSolutionCount: Int
)

/**
 * Response returned by a worker after successful allocation.
 *
 * @param initialSolutions Evaluated initial solutions produced by the worker.
 */
@Serializable
data class WorkerAllocationResponse(
    val initialSolutions: List<InitialSolutionData>
)

/**
 * Compact representation of an evaluated solution, carrying only objective and
 * constraint values (not the full model graph).
 *
 * @param solutionId Unique identifier for this solution within the execution.
 * @param objectives Objective function values (one per goal, in declaration order).
 * @param constraints Constraint violation values (zero means satisfied).
 */
@Serializable
data class InitialSolutionData(
    val solutionId: String,
    val objectives: List<Double>,
    val constraints: List<Double>
)

/**
 * A single mutation task within a [com.mdeo.optimizer.worker.NodeWorkBatchRequest].
 *
 * @param solutionId Identifier of the parent solution to mutate.
 */
@Serializable
data class BatchTask(
    val solutionId: String
)

/**
 * Result of a single mutation-and-evaluation task.
 *
 * @param parentSolutionId Identifier of the solution that was mutated.
 * @param newSolutionId Identifier assigned to the newly created offspring solution.
 * @param objectives Objective values of the new solution.
 * @param constraints Constraint values of the new solution.
 * @param succeeded Whether the mutation and evaluation completed without error.
 */
@Serializable
data class BatchResult(
    val parentSolutionId: String,
    val newSolutionId: String,
    val objectives: List<Double>,
    val constraints: List<Double>,
    val succeeded: Boolean
)

/**
 * A single solution with its serialized model, used for rebalancing imports
 * embedded inline in a [com.mdeo.optimizer.worker.NodeWorkBatchRequest].
 *
 * @param solutionId Identifier of the solution being transferred.
 * @param serializedModel Serialized model graph for reconstitution on the receiving worker.
 */
@Serializable
data class SolutionTransferItem(
    val solutionId: String,
    val serializedModel: SerializedModel
)
