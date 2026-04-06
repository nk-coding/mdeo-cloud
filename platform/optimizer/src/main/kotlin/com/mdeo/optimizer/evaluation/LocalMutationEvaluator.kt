package com.mdeo.optimizer.evaluation

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.SerializedModel
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.graph.MdeoModelGraph
import com.mdeo.modeltransformation.graph.TinkerModelGraph
import com.mdeo.optimizer.config.GraphBackendType
import com.mdeo.optimizer.guidance.GuidanceFunction
import com.mdeo.optimizer.operators.MutationStrategy
import com.mdeo.optimizer.solution.Solution
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Single-node, in-process implementation of [MutationEvaluator].
 *
 * All mutation and evaluation work is performed locally within the current JVM.
 * Solutions are stored in a thread-safe map keyed by generated identifiers.
 *
 * @param initialSolutionProvider Factory that creates a fresh initial [Solution].
 * @param mutationStrategy The mutation strategy applied to solutions.
 * @param objectives The objective guidance functions used for fitness evaluation.
 * @param constraints The constraint guidance functions used for feasibility checks.
 * @param metamodel The compiled metamodel used to reconstitute solutions from [ModelData].
 * @param nodeId Optional identifier for this evaluator node (defaults to [DEFAULT_NODE_ID]).
 * @param graphBackendType The graph backend to use when reconstituting imported solutions.
 */
class LocalMutationEvaluator(
    private val initialSolutionProvider: () -> Solution,
    private val mutationStrategy: MutationStrategy,
    private val objectives: List<GuidanceFunction>,
    private val constraints: List<GuidanceFunction>,
    private val metamodel: Metamodel? = null,
    private val nodeId: String = DEFAULT_NODE_ID,
    private val graphBackendType: GraphBackendType = GraphBackendType.MDEO
) : MutationEvaluator {

    private val solutions: ConcurrentHashMap<String, Solution> = ConcurrentHashMap()

    override fun getNodeIds(): Set<String> = setOf(nodeId)

    override suspend fun initialize(count: Int): List<InitialSolutionResult> {
        return (0 until count).map {
            val solution = initialSolutionProvider()
            val mutated = mutationStrategy.mutate(solution)
            try {
                val id = generateId()
                solutions[id] = mutated
                InitialSolutionResult(
                    solutionId = id,
                    workerNodeId = nodeId
                )
            } catch (e: Exception) {
                mutated.close()
                throw e
            }
        }
    }

    override suspend fun executeNodeBatches(batches: List<NodeBatch>): List<EvaluationResult> {
        val batch = batches.firstOrNull { it.nodeId == nodeId } ?: return emptyList()

        for (import in batch.imports) {
            receiveSolution(import.solutionId, import.serializedModel)
        }

        val mutationResults = batch.tasks.map { task -> evaluateSingle(task) }

        val evaluationResults = batch.evaluationTasks.map { task -> evaluateExisting(task) }

        for (solutionId in batch.discards) {
            solutions.remove(solutionId)?.close()
        }

        return mutationResults + evaluationResults
    }

    override suspend fun getSolutionData(ref: WorkerSolutionRef): SerializedModel {
        val solution = requireSolution(ref.solutionId)
        return solution.modelGraph.toSerializedModel()
    }

    override suspend fun cleanup() {
        for (solution in solutions.values) {
            solution.close()
        }
        solutions.clear()
    }

    /**
     * Reconstitutes a solution from a [SerializedModel] and stores it under [solutionId].
     *
     * Requires that a non-null [metamodel] was provided at construction time.
     */
    fun receiveSolution(solutionId: String, serializedModel: SerializedModel) {
        val mm = checkNotNull(metamodel) { "Cannot receive solutions without a metamodel" }
        val modelGraph = when (graphBackendType) {
            GraphBackendType.MDEO -> MdeoModelGraph.create(serializedModel, mm)
            GraphBackendType.Tinker -> TinkerModelGraph.create(serializedModel.toModelData(mm), mm)
        }
        solutions[solutionId] = Solution(modelGraph)
    }

    /**
     * Deep-copies the parent solution, applies the mutation strategy, stores the offspring,
     * and returns an [EvaluationResult] with fitness values.
     *
     * @param task The mutation task identifying the parent solution.
     * @return An [EvaluationResult] with [EvaluationResult.succeeded] set accordingly.
     */
    private fun evaluateSingle(task: MutationTask): EvaluationResult {
        val parent = requireSolution(task.solutionId)
        val copy = parent.deepCopy()
        val mutated = try {
            mutationStrategy.mutate(copy)
        } catch (e: Throwable) {
            copy.close()
            return EvaluationResult(
                parentSolutionId = task.solutionId,
                newSolutionId = "",
                workerNodeId = nodeId,
                objectives = emptyList(),
                constraints = emptyList(),
                succeeded = false
            )
        }
        val newId = generateId()
        solutions[newId] = mutated
        val objectives: List<Double>
        val constraints: List<Double>
        try {
            objectives = evaluateObjectives(mutated)
            constraints = evaluateConstraints(mutated)
        } catch (e: Throwable) {
            solutions.remove(newId)?.close()
            return EvaluationResult(
                parentSolutionId = task.solutionId,
                newSolutionId = "",
                workerNodeId = nodeId,
                objectives = emptyList(),
                constraints = emptyList(),
                succeeded = false,
                errorMessage = "Guidance function evaluation failed: ${e.message}"
            )
        }
        return EvaluationResult(
            parentSolutionId = task.solutionId,
            newSolutionId = newId,
            workerNodeId = nodeId,
            objectives = objectives,
            constraints = constraints,
            succeeded = true
        )
    }

    /**
     * Evaluates an existing solution without mutation, returning fitness values.
     *
     * Used for initial population evaluation where solutions have already been created
     * and stored but need their objective and constraint values computed.
     *
     * @param task The evaluation task identifying the solution to evaluate.
     * @return An [EvaluationResult] with the same solution ID for both parent and new.
     */
    private fun evaluateExisting(task: EvaluationTask): EvaluationResult {
        val solution = requireSolution(task.solutionId)
        val objectives: List<Double>
        val constraints: List<Double>
        try {
            objectives = evaluateObjectives(solution)
            constraints = evaluateConstraints(solution)
        } catch (e: Throwable) {
            return EvaluationResult(
                parentSolutionId = task.solutionId,
                newSolutionId = "",
                workerNodeId = nodeId,
                objectives = emptyList(),
                constraints = emptyList(),
                succeeded = false,
                errorMessage = "Guidance function evaluation failed: ${e.message}"
            )
        }
        return EvaluationResult(
            parentSolutionId = task.solutionId,
            newSolutionId = task.solutionId,
            workerNodeId = nodeId,
            objectives = objectives,
            constraints = constraints,
            succeeded = true
        )
    }

    /**
     * Evaluates all objective functions against the given solution.
     *
     * @param solution The candidate solution to evaluate.
     * @return Fitness values in the order the objectives were configured.
     */
    private fun evaluateObjectives(solution: Solution): List<Double> =
        objectives.map { it.computeFitness(solution) }

    /**
     * Evaluates all constraint functions against the given solution.
     *
     * @param solution The candidate solution to evaluate.
     * @return Constraint violation values in the order the constraints were configured.
     */
    private fun evaluateConstraints(solution: Solution): List<Double> =
        constraints.map { it.computeFitness(solution) }

    /**
     * Looks up a solution by ID, throwing if not found.
     *
     * @param solutionId The solution identifier to look up.
     * @return The stored [Solution].
     * @throws IllegalArgumentException if no solution with that ID exists.
     */
    private fun requireSolution(solutionId: String): Solution =
        solutions[solutionId] ?: throw IllegalArgumentException("Solution not found: $solutionId")

    /**
     * Generates a unique identifier for a new solution, scoped to this node.
     *
     * @return A string of the form `"<nodeId>-<uuid>"` that is unique across all nodes.
     */
    private fun generateId(): String = "$nodeId-${UUID.randomUUID()}"

    companion object {
        /**
         * Default node identifier used when no [nodeId] is provided. 
         */
        const val DEFAULT_NODE_ID = "local"
    }
}
