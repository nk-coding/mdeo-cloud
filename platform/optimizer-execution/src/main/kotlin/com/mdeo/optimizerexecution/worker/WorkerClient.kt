package com.mdeo.optimizerexecution.worker

import com.mdeo.metamodel.data.ModelData
import com.mdeo.optimizer.worker.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Client that communicates with a single worker node in a multi-node optimization setup.
 *
 * **Initial allocation** uses an HTTP POST to transfer the full metamodel, model, and
 * compiled ASTs to the worker (the payload is too large for a WebSocket frame and must
 * be delivered reliably before any ongoing traffic begins).  Once allocation succeeds
 * the client establishes a persistent binary-CBOR WebSocket connection and uses it for
 * all subsequent requests: per-node work batches (imports + mutations + discards) and solution data retrieval.
 *
 * The WebSocket reading loop is launched as a child coroutine of [scope].  When that
 * scope is cancelled (e.g. because the execution finished or was aborted), the loop
 * terminates automatically and the connection is closed cleanly.  Any unexpected
 * disconnect fails all in-flight requests immediately; callers are expected to handle
 * the resulting exceptions.
 *
 * @param nodeId Unique identifier for the worker node (used as a key in solution refs).
 * @param baseUrl Base URL of the worker node (e.g. `http://worker-1:8080`).
 * @param scope Coroutine scope that owns the background WebSocket reading loop.
 */
@OptIn(ExperimentalSerializationApi::class)
class WorkerClient(
    val nodeId: String,
    val baseUrl: String,
    private val scope: CoroutineScope
) : AutoCloseable {

    private companion object {
        /** Max time to wait for the initial WS handshake to complete. */
        const val SESSION_READY_TIMEOUT_MS = 30_000L
        /** Default timeout for a full request/response cycle over the WS. */
        const val OPERATION_TIMEOUT_MS = 600_000L
    }

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
        install(WebSockets)
    }

    private val logger = LoggerFactory.getLogger(WorkerClient::class.java)
    private val cbor = Cbor { ignoreUnknownKeys = true }

    /** Pending request correlation: requestId → deferred result. */
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<WorkerWsMessage>>()

    /** Resolved once when the initial WebSocket handshake completes. */
    private val wsSessionDeferred = CompletableDeferred<DefaultClientWebSocketSession>()

    /** Background job running the WebSocket read loop. */
    private var wsJob: Job? = null

    /** `ws://` / `wss://` equivalent of [baseUrl]. */
    private val wsBaseUrl = when {
        baseUrl.startsWith("https://") -> baseUrl.replace("https://", "wss://")
        baseUrl.startsWith("http://") -> baseUrl.replace("http://", "ws://")
        else -> baseUrl
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Allocates resources on the worker for a new optimization execution.
     *
     * Sends the metamodel, initial model, transformation/script ASTs, and configuration
     * to the worker over HTTP.  After a successful response the client opens a persistent
     * WebSocket connection that is used for all subsequent operations.
     *
     * @param request The allocation request containing all resources and configuration.
     * @return The allocation response with initial solution fitness data.
     */
    suspend fun allocate(request: WorkerAllocationRequest): WorkerAllocationResponse {
        val response = httpClient.post("$baseUrl/api/worker/executions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<WorkerAllocationResponse>()

        startWsSession(request.executionId)
        return response
    }

    /**
     * Sends a unified work batch to the worker: solution imports (rebalancing), mutation tasks,
     * and solution discards, all in a single [NodeWorkBatchRequest] message.
     *
     * The worker processes the three phases atomically — imports first, then mutations, then discards —
     * and returns a [NodeWorkBatchResponse] with evaluation results for each task.
     *
     * @param executionId The execution identifier on the worker (used for logging/tracing only; the
     *        WebSocket session is already scoped to this execution).
     * @param imports Solutions to import onto this worker (inline model data from the orchestrator).
     * @param tasks Mutation tasks referencing existing (or just-imported) solutions.
     * @param discards Solution IDs to discard after processing the batch.
     * @return The batch response with evaluation results for each task.
     */
    suspend fun executeNodeBatch(
        executionId: String,
        imports: List<SolutionTransferItem>,
        tasks: List<BatchTask>,
        discards: List<String>
    ): NodeWorkBatchResponse {
        val requestId = newRequestId()
        return sendAndReceive(NodeWorkBatchRequest(requestId, imports, tasks, discards)) as NodeWorkBatchResponse
    }

    /**
     * Retrieves the full model data for a specific solution from the worker.
     *
     * @param executionId The execution identifier on the worker.
     * @param solutionId The identifier of the solution whose model data is requested.
     * @return The deserialized model data for the solution.
     */
    suspend fun getSolutionData(executionId: String, solutionId: String): ModelData {
        val requestId = newRequestId()
        val response = sendAndReceive(SolutionFetchRequest(requestId, solutionId)) as SolutionFetchResponse
        return response.modelData
    }


    /**
     * Cleans up the execution on the worker and closes the WebSocket connection.
     *
     * An HTTP DELETE is sent first so the worker can release its local state.
     * The background WebSocket job is then cancelled.
     *
     * @param executionId The execution identifier to clean up.
     */
    suspend fun cleanup(executionId: String) {
        httpClient.delete("$baseUrl/api/worker/executions/$executionId")
        wsJob?.cancel()
        wsJob = null
    }

    /**
     * Closes the underlying HTTP client and cancels the WebSocket connection if still active.
     */
    override fun close() {
        wsJob?.cancel()
        httpClient.close()
    }

    // ─── Internal WebSocket machinery ─────────────────────────────────────────

    /**
     * Launches the background coroutine that establishes the WebSocket connection to the worker.
     *
     * The session deferred is resolved once on successful handshake, unblocking [sendAndReceive]
     * callers. On any disconnect or error, all in-flight requests are failed immediately and
     * the deferred is completed exceptionally so that subsequent callers also fail fast.
     *
     * @param executionId The execution to connect to (`/ws/worker/executions/{id}`).
     */
    private fun startWsSession(executionId: String) {
        wsJob = scope.launch {
            try {
                httpClient.webSocket("$wsBaseUrl/ws/worker/executions/$executionId") {
                    logger.info("Worker WS connected to {} for execution {}", baseUrl, executionId)
                    wsSessionDeferred.complete(this)
                    for (frame in incoming) {
                        if (frame !is Frame.Binary) continue
                        val msg = cbor.decodeFromByteArray<WorkerWsMessage>(frame.readBytes())
                        pendingRequests.remove(msg.requestId)?.complete(msg)
                    }
                    logger.info("Worker WS disconnected from {} (execution {})", baseUrl, executionId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Worker WS to {} failed for execution {}: {}", baseUrl, executionId, e.message)
                if (!wsSessionDeferred.isCompleted) wsSessionDeferred.completeExceptionally(e)
                drainPendingRequests(e)
            }
        }
    }

    /**
     * Sends a [WorkerWsMessage] to the worker and suspends until the matching response arrives.
     *
     * @param msg The message to send.
     * @param timeoutMs Maximum time to wait for the response (default [OPERATION_TIMEOUT_MS]).
     * @return The response message from the worker.
     */
    private suspend fun sendAndReceive(msg: WorkerWsMessage, timeoutMs: Long = OPERATION_TIMEOUT_MS): WorkerWsMessage {
        val session = withTimeout(SESSION_READY_TIMEOUT_MS) { wsSessionDeferred.await() }
        val deferred = CompletableDeferred<WorkerWsMessage>()
        pendingRequests[msg.requestId] = deferred
        try {
            session.send(Frame.Binary(true, cbor.encodeToByteArray<WorkerWsMessage>(msg)))
        } catch (e: Exception) {
            pendingRequests.remove(msg.requestId)
            throw e
        }
        return withTimeout(timeoutMs) { deferred.await() }
    }

    /** Completes all pending request deferreds exceptionally and clears the map. */
    private fun drainPendingRequests(cause: Exception) {
        val keys = pendingRequests.keys.toList()
        for (key in keys) {
            pendingRequests.remove(key)?.completeExceptionally(cause)
        }
    }

    private fun newRequestId() = UUID.randomUUID().toString()
}

