package com.mdeo.optimizer.evaluation

import com.mdeo.metamodel.SerializedModel

/**
 * Work to be executed on a single worker node in one generation.
 *
 * Combines imports (solutions arriving from other nodes due to rebalancing),
 * mutation tasks, and discards into a single per-node request so that the
 * orchestrator can communicate with every worker using exactly one message
 * per generation.
 *
 * @param nodeId The worker node that should process this batch.
 * @param imports Solutions being transferred to this node from other nodes;
 *   the orchestrator pre-fetches their serialized models and embeds them inline.
 * @param tasks Parent solutions on this node to mutate and evaluate.
 * @param discards Solution IDs on this node to drop after evaluation.
 */
data class NodeBatch(
    val nodeId: String,
    val imports: List<SolutionImportData>,
    val tasks: List<MutationTask>,
    val discards: List<String>
)

/**
 * Inline serialized model for a solution being imported into a node during rebalancing.
 *
 * @param solutionId The identifier the solution should be registered under.
 * @param serializedModel The serialized model graph to reconstitute on arrival.
 */
data class SolutionImportData(
    val solutionId: String,
    val serializedModel: SerializedModel
)

/**
 * Abstracts the mutation and evaluation pipeline so that both single-node (in-process)
 * and federated (multi-node) execution can be driven through the same contract.
 *
 * The orchestrator interacts exclusively with this interface, remaining agnostic
 * to whether work is performed locally or distributed across remote worker nodes.
 */
interface MutationEvaluator {

    /**
     * Creates and evaluates [count] initial solutions for the starting population.
     *
     * Each solution is generated, optionally mutated, and evaluated against all
     * configured objectives and constraints.
     *
     * @param count The number of initial solutions to create.
     * @return A list of [InitialSolutionResult] entries, one per created solution.
     */
    suspend fun initialize(count: Int): List<InitialSolutionResult>

    /**
     * Executes one generation of work across all nodes in a single coordinated call.
     *
     * Each [NodeBatch] carries the imports (solutions arriving via rebalancing),
     * mutation tasks, and discards for one worker node. The orchestrator sends one
     * message per node and collects all mutation results in a flat list.
     *
     * Import model data is pre-fetched by the orchestrator via [getSolutionData]
     * and embedded inline so that each node can store the arriving solutions before
     * executing mutations.
     *
     * @param batches One [NodeBatch] per node (nodes with nothing to do receive an empty batch).
     * @return A flat list of [EvaluationResult] entries, one per task across all nodes.
     */
    suspend fun executeNodeBatches(batches: List<NodeBatch>): List<EvaluationResult>

    /**
     * Fetches the serialized model for a solution stored on a worker node.
     *
     * @param ref A reference identifying the worker node and solution.
     * @return The [SerializedModel] representation of the solution's model graph.
     */
    suspend fun getSolutionData(ref: WorkerSolutionRef): SerializedModel

    /**
     * Fetches serialized models for multiple solutions in a single batched operation.
     *
     * Implementations that communicate with remote workers should send a single batched
     * request rather than one request per solution. The default implementation falls back
     * to sequential [getSolutionData] calls.
     *
     * @param refs The solution references to fetch.
     * @return A map from solution ID to its [SerializedModel].
     */
    suspend fun getSolutionDataBatch(refs: List<WorkerSolutionRef>): Map<String, SerializedModel> {
        return refs.associate { it.solutionId to getSolutionData(it) }
    }

    /**
     * Releases all resources held by this evaluator.
     *
     * After this call the evaluator must not be used again.
     */
    suspend fun cleanup()

    /**
     * Returns the identifiers of all worker nodes managed by this evaluator.
     *
     * Single-node evaluators return a set with one entry; federated evaluators
     * return one entry per worker.
     */
    fun getNodeIds(): Set<String>
}
