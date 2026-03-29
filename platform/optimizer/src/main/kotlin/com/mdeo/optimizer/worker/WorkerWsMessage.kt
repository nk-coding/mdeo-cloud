package com.mdeo.optimizer.worker

import com.mdeo.metamodel.SerializedModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Polymorphic message type for WebSocket-based orchestrator-to-worker communication.
 *
 * After the initial HTTP handshake that allocates a worker, all ongoing traffic between
 * the orchestrator and each worker node is carried as binary CBOR frames of this sealed
 * hierarchy. The [requestId] present on every message is echoed back in the reply,
 * enabling many in-flight requests to share one persistent WebSocket connection.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed class WorkerWsMessage {

    /**
     * Correlation identifier echoed verbatim in the corresponding response. 
     */
    abstract val requestId: String
}

// ─── Unified generation batch ────────────────────────────────────────────────

/**
 * Orchestrator → Worker: execute one generation of work atomically.
 *
 * Combines solution imports (from rebalancing), mutation tasks, and discards into
 * a single message so that the orchestrator requires exactly one round trip per
 * worker per generation. The worker processes these in order: store imports, execute
 * mutations, drop discards.
 *
 * @param requestId Correlation identifier echoed in [NodeWorkBatchResponse].
 * @param imports Solutions being transferred to this node from other nodes;
 *   model data is pre-fetched by the orchestrator and embedded inline.
 * @param tasks Existing solutions on this node to mutate and evaluate.
 * @param discards Solution IDs on this node to release after evaluation.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("node_work_batch_request")
data class NodeWorkBatchRequest(
    override val requestId: String,
    val imports: List<SolutionTransferItem>,
    val tasks: List<BatchTask>,
    val discards: List<String>
) : WorkerWsMessage()

/**
 * Worker → Orchestrator: results for all mutation tasks in the batch.
 *
 * @param requestId Matches [NodeWorkBatchRequest.requestId].
 * @param results One entry per task in [NodeWorkBatchRequest.tasks], in request order.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("node_work_batch_response")
data class NodeWorkBatchResponse(
    override val requestId: String,
    val results: List<BatchResult>
) : WorkerWsMessage()

// ─── Solution data retrieval ───────────────────────────────────────────────────

/**
 * Orchestrator → Worker: retrieve the full model data for a specific solution.
 *
 * Used when the orchestrator needs to pre-fetch model data for solutions that are
 * being rebalanced to another node (to embed inline in the destination's batch).
 * Also used at the end of an execution to fetch final population model data.
 *
 * @param requestId Correlation identifier echoed in [SolutionFetchResponse].
 * @param solutionId Identifier of the solution whose model data is requested.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("solution_fetch_request")
data class SolutionFetchRequest(
    override val requestId: String,
    val solutionId: String
) : WorkerWsMessage()

/**
 * Orchestrator → Worker: retrieve model data for multiple solutions in one request.
 *
 * Batches several solution IDs into a single message to reduce round-trip overhead
 * during rebalancing. The worker responds with one [SolutionFetchResponse] per
 * requested solution, all sharing the same [requestId].
 *
 * @param requestId Correlation identifier echoed in each [SolutionFetchResponse].
 * @param solutionIds Identifiers of the solutions whose model data is requested.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("solution_batch_fetch_request")
data class SolutionBatchFetchRequest(
    override val requestId: String,
    val solutionIds: List<String>
) : WorkerWsMessage()

/**
 * Worker → Orchestrator: serialized model for a requested solution.
 *
 * @param requestId Matches [SolutionFetchRequest.requestId] or [SolutionBatchFetchRequest.requestId].
 * @param solutionId The solution this response carries data for.
 * @param serializedModel Serialized model graph for the solution.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("solution_fetch_response")
data class SolutionFetchResponse(
    override val requestId: String,
    val solutionId: String,
    val serializedModel: SerializedModel
) : WorkerWsMessage()
