package com.mdeo.optimizerexecution.worker

import com.mdeo.metamodel.SerializedModel
import com.mdeo.optimizer.worker.*
import com.mdeo.optimizerexecution.service.OrchestratorRegistry
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
 * **WebSocket mode** (default): After HTTP allocation, the worker's subprocess opens a
 * WebSocket connection back to the orchestrator. This client registers with the
 * [OrchestratorRegistry] and waits for the subprocess to connect. All ongoing traffic
 * (work batches, solution fetches) flows over this reverse-connected session.
 *
 * **Local-channel mode** ([useLocalChannel] = `true`): The subprocess uses the subprocess
 * stdin/stdout pipe for orchestrator communication instead of a WebSocket. In this mode
 * the client does not register with the [OrchestratorRegistry] and instead opens a
 * WebSocket directly to the worker's legacy `/ws/worker/executions/{id}` endpoint so that
 * [WorkerService] can act as the forwarder between the session and the subprocess channel.
 * This eliminates the extra network connection for the local node in federated setups.
 *
 * **Legacy mode**: When no [OrchestratorRegistry] is provided and [useLocalChannel] is
 * `false`, the client opens a WebSocket connection to the worker (the old approach) after
 * HTTP allocation.
 *
 * In all modes, the WebSocket reading loop is launched as a child coroutine of [scope].
 * When that scope is cancelled, the loop terminates and the connection is closed cleanly.
 *
 * @param nodeId Unique identifier for the worker node (used as a key in solution refs).
 * @param baseUrl Base URL of the worker node (e.g. `http://worker-1:8080`).
 * @param scope Coroutine scope that owns the background WebSocket reading loop.
 * @param orchestratorRegistry Optional registry for reverse-connected subprocess WebSockets.
 * @param orchestratorWsBaseUrl The base WS URL that subprocesses should connect back to
 *        (e.g. `ws://orchestrator-host:8080`). Required when [orchestratorRegistry] is set.
 * @param useLocalChannel When `true`, the subprocess communicates with the orchestrator
 *        via the subprocess stdio pipe rather than a WebSocket. The client opens a WS to
 *        the worker's legacy endpoint so [WorkerService] acts as forwarder. This is more
 *        efficient for the local node in a federated setup.
 */
@OptIn(ExperimentalSerializationApi::class)
class WorkerClient(
    val nodeId: String,
    val baseUrl: String,
    private val scope: CoroutineScope,
    private val orchestratorRegistry: OrchestratorRegistry? = null,
    private val orchestratorWsBaseUrl: String? = null,
    private val useLocalChannel: Boolean = false
) : AutoCloseable {

    /**
     * Timeout constants for WebSocket session management. 
     */
    private companion object {
        /**
         * Max time to wait for the subprocess to connect back via the registry.
         */
        const val SESSION_READY_TIMEOUT_MS = 30_000L
        /**
         * Default timeout for a full request/response cycle over the WS.
         */
        const val OPERATION_TIMEOUT_MS = 600_000L
    }

    /**
     * HTTP client used for REST allocation, metadata, cleanup, and (in legacy mode) WebSocket upgrade. 
     */
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
        install(WebSockets)
    }

    /**
     * Logger instance for this client. 
     */
    private val logger = LoggerFactory.getLogger(WorkerClient::class.java)
    /**
     * CBOR codec for encoding/decoding [WorkerWsMessage] frames. 
     */
    private val cbor = Cbor { ignoreUnknownKeys = true }

    /**
     * Pending request correlation: requestId → deferred result.
     */
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<WorkerWsMessage>>()

    /**
     * The reason provided in a [WorkerShutdownNotice] received from the subprocess, or
     * `null` if no notice has been received.
     *
     * Set atomically when the orchestrator dispatch loop processes a
     * [WorkerShutdownNotice]; callers can inspect this to report a precise error instead
     * of a generic "worker not reachable" message.
     */
    @Volatile
    var shutdownReason: String? = null
        private set

    /**
     * The active WebSocket session (resolved once the connection is established).
     *
     * In registry mode this is a [DefaultWebSocketSession] from the server side;
     * in legacy mode it is a [DefaultClientWebSocketSession].
     */
    private val wsSessionDeferred = CompletableDeferred<DefaultWebSocketSession>()

    /**
     * Background job running the WebSocket read loop.
     */
    private var wsJob: Job? = null

    /**
     * Whether the WebSocket connection to the worker is still active.
     *
     * Returns `false` after the WS session closes (e.g. subprocess crash or timeout).
     * Used by the evaluator to distinguish transient evaluation failures from
     * permanent worker death.
     */
    val isAlive: Boolean get() = wsJob?.isActive ?: false

    /**
     * `ws://` / `wss://` equivalent of [baseUrl].
     */
    private val wsBaseUrl = when {
        baseUrl.startsWith("https://") -> {
            baseUrl.replace("https://", "wss://")
        }
        baseUrl.startsWith("http://") -> {
            baseUrl.replace("http://", "ws://")
        }
        else -> {
            baseUrl
        }
    }

    /**
     * Returns the full WebSocket URL of this worker's legacy session endpoint for the given execution.
     *
     * @param executionId The execution identifier shared by all nodes in the run.
     * @return Full WS URL, e.g. `ws://host:8080/ws/worker/executions/{id}`.
     */
    fun wsUrl(executionId: String): String =
        "$wsBaseUrl/ws/worker/executions/$executionId"

    /**
     * Retrieves the serialized model for a specific solution from the worker.
     *
     * In **registry mode** ([orchestratorRegistry] set and [useLocalChannel] = `false`): the
     * orchestrator WS URL is passed in the HTTP request so the worker can forward it to its
     * subprocess. After the HTTP response, the client registers with the [OrchestratorRegistry]
     * and waits for the subprocess to connect back, then starts the WS read loop on that session.
     *
     * In **local-channel mode** ([useLocalChannel] = `true`): the allocation request carries
     * `useLocalChannel = true` instead of an orchestratorWsUrl, signalling the subprocess to
     * use the stdio pipe. After the HTTP response, the client opens a WebSocket to the worker's
     * legacy endpoint so [WorkerService] can act as the forwarder.
     *
     * In **legacy mode**: behaves as before — opens a WS connection to the worker.
     *
     * @param request The allocation request containing all resources and configuration.
     * @return The allocation response with initial solution fitness data.
     */
    suspend fun allocate(request: WorkerAllocationRequest): WorkerAllocationResponse {
        return if (useLocalChannel) {
            val httpResp = httpClient.post("$baseUrl/api/worker/executions") {
                contentType(ContentType.Application.Json)
                setBody(request.copy(useLocalChannel = true, orchestratorWsUrl = null))
            }
            if (!httpResp.status.isSuccess()) {
                val errorBody = httpResp.bodyAsText()
                throw IllegalStateException("Worker allocation failed (${httpResp.status.value}): $errorBody")
            }
            val response = httpResp.body<WorkerAllocationResponse>()
            startWsSession(request.executionId)
            response
        } else {
            allocateRegistry(request)
        }
    }

    /**
     * Registry-mode (or legacy) allocation: the orchestrator WS URL is embedded in the
     * HTTP request for subprocess reverse-connection, or omitted for legacy mode.
     *
     * @param request The allocation request containing all resources and configuration.
     * @return The allocation response with initial solution fitness data.
     */
    private suspend fun allocateRegistry(request: WorkerAllocationRequest): WorkerAllocationResponse {
        val registryKey = orchestratorRegistry?.let { OrchestratorRegistry.key(request.executionId, nodeId) }

        val orchestratorWsUrl = if (orchestratorRegistry != null && orchestratorWsBaseUrl != null) {
            "$orchestratorWsBaseUrl/ws/subprocess/executions/${request.executionId}/$nodeId"
        } else null

        val wsDeferred = if (orchestratorRegistry != null && registryKey != null) {
            logger.info(
                "Registering subprocess WS slot for node {} execution {} (key={})",
                nodeId, request.executionId, registryKey
            )
            orchestratorRegistry.register(registryKey)
        } else null

        val response = try {
            val httpResp = httpClient.post("$baseUrl/api/worker/executions") {
                contentType(ContentType.Application.Json)
                setBody(request.copy(orchestratorWsUrl = orchestratorWsUrl))
            }
            if (!httpResp.status.isSuccess()) {
                val errorBody = httpResp.bodyAsText()
                throw IllegalStateException("Worker allocation failed (${httpResp.status.value}): $errorBody")
            }
            httpResp.body<WorkerAllocationResponse>()
        } catch (e: Exception) {
            if (registryKey != null) orchestratorRegistry.remove(registryKey)
            throw e
        }

        if (wsDeferred != null && registryKey != null) {
            try {
                logger.info(
                    "Waiting up to {}ms for subprocess WS connect-back (node {} execution {})",
                    SESSION_READY_TIMEOUT_MS, nodeId, request.executionId
                )
                val session = withTimeout(SESSION_READY_TIMEOUT_MS) { wsDeferred.await() }
                logger.info("Subprocess WS session established for node {} execution {}", nodeId, request.executionId)
                startRegistryWsReadLoop(request.executionId, session)
            } catch (e: Exception) {
                orchestratorRegistry.remove(registryKey)
                throw e
            }
        } else {
            startWsSession(request.executionId)
        }

        return response
    }

    /**
     * Sends a unified work batch to the worker: mutation tasks, evaluation-only tasks,
     * solution discards, and any pending solution relocations — all in a single
     * [NodeWorkBatchRequest] message. The subprocess handles the relocations by
     * opening (or reusing) a direct WebSocket to each destination worker and pushing
     * the model bytes there, without involving the orchestrator.
     *
     * @param executionId The execution identifier on the worker (used for logging/tracing only).
     * @param tasks Mutation tasks referencing existing solutions.
     * @param evaluationTasks Evaluation-only tasks for fitness computation without mutation.
     * @param discards Solution IDs to discard after processing the batch.
     * @param relocations Solutions to push to other nodes before or during mutation.
     * @return The batch response with evaluation results for each task.
     */
    suspend fun executeNodeBatch(
        executionId: String,
        tasks: List<BatchTask>,
        evaluationTasks: List<BatchEvaluationTask> = emptyList(),
        discards: List<String>,
        relocations: List<SolutionRelocation> = emptyList()
    ): NodeWorkBatchResponse {
        val requestId = newRequestId()
        return sendAndReceive(NodeWorkBatchRequest(requestId, tasks, evaluationTasks, discards, relocations)) as NodeWorkBatchResponse
    }

    /**
     * Retrieves the serialized model for a specific solution from the worker.
     *
     * Delegates to [getSolutionDataBatch] with a single-element list.
     *
     * @param executionId The execution identifier on the worker.
     * @param solutionId The identifier of the solution whose model is requested.
     * @return The serialized model for the solution.
     */
    suspend fun getSolutionData(executionId: String, solutionId: String): SerializedModel {
        logger.info("[node={}] Fetching model for solution {} (execution {})", nodeId, solutionId, executionId)
        return getSolutionDataBatch(executionId, listOf(solutionId))[solutionId]
            ?: throw IllegalStateException("Solution $solutionId not found on node $nodeId (execution $executionId)")
    }

    /**
     * Retrieves serialized models for multiple solutions in a single batched request.
     *
     * Sends one [SolutionBatchFetchRequest] and receives a single [SolutionBatchFetchResponse]
     * carrying all requested models at once.
     *
     * @param executionId The execution identifier on the worker.
     * @param solutionIds The solution identifiers to fetch.
     * @return A map from solution ID to its [SerializedModel].
     */
    suspend fun getSolutionDataBatch(
        executionId: String,
        solutionIds: List<String>
    ): Map<String, SerializedModel> {
        if (solutionIds.isEmpty()) return emptyMap()
        logger.info(
            "[node={}] Fetching models for {} solution(s) in batch (execution {})",
            nodeId, solutionIds.size, executionId
        )
        val requestId = newRequestId()
        val response = sendAndReceive(SolutionBatchFetchRequest(requestId, solutionIds)) as SolutionBatchFetchResponse
        return response.solutions.associate { it.solutionId to it.serializedModel }
    }

    /**
     * Fetches metadata describing the worker node's thread capacity and supported algorithm backends.
     *
     * Performs an HTTP GET to `GET /api/worker/metadata` on this node. Useful for orchestrators
     * that need to know actual worker capabilities before committing thread budgets.
     *
     * @return The [WorkerMetadata] reported by the remote worker.
     */
    suspend fun getMetadata(): WorkerMetadata {
        return httpClient.get("$baseUrl/api/worker/metadata").body<WorkerMetadata>()
    }


    /**
     * Cleans up the execution on the worker and closes the WebSocket connection.
     *
     * An HTTP DELETE is sent first so the worker can release its local state.
     * The background WebSocket job is then cancelled and any registry entry removed.
     *
     * @param executionId The execution identifier to clean up.
     */
    suspend fun cleanup(executionId: String) {
        httpClient.delete("$baseUrl/api/worker/executions/$executionId")
        val registryKey = OrchestratorRegistry.key(executionId, nodeId)
        orchestratorRegistry?.remove(registryKey)
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


    /**
     * Launches the background coroutine that establishes the WebSocket connection to the worker.
     *
     * Used in legacy mode where the orchestrator opens the connection to the worker.
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
                        if (frame !is Frame.Binary) {
                            continue
                        }
                        val msg = cbor.decodeFromByteArray<WorkerWsMessage>(frame.readBytes())
                        if (handleUnsolicitedMessage(msg, this)) continue
                        pendingRequests.remove(msg.requestId)?.complete(msg)
                    }
                    logger.info("Worker WS disconnected from {} (execution {})", baseUrl, executionId)
                    drainPendingRequests(IllegalStateException("WS session closed (node $nodeId, execution $executionId)"))
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
     * Starts the WS read loop on a server-side session obtained via [OrchestratorRegistry].
     *
     * Unlike [startWsSession] this does not establish a new connection — the session was
     * already opened by the subprocess connecting back to the orchestrator.
     *
     * @param executionId The execution identifier for logging.
     * @param session The WebSocket session to read from.
     */
    private fun startRegistryWsReadLoop(executionId: String, session: DefaultWebSocketSession) {
        wsSessionDeferred.complete(session)
        wsJob = scope.launch {
            try {
                logger.info("Subprocess WS connected for node {} (execution {})", nodeId, executionId)
                for (frame in session.incoming) {
                    if (frame !is Frame.Binary) {
                        continue
                    }
                    val msg = cbor.decodeFromByteArray<WorkerWsMessage>(frame.readBytes())
                    if (handleUnsolicitedMessage(msg, session)) continue
                    pendingRequests.remove(msg.requestId)?.complete(msg)
                }
                logger.info("Subprocess WS disconnected for node {} (execution {})", nodeId, executionId)
                drainPendingRequests(IllegalStateException("WS session closed (node $nodeId, execution $executionId)"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Subprocess WS read loop failed for node {} (execution {}): {}", nodeId, executionId, e.message)
                drainPendingRequests(e)
            }
        }
    }

    /**
     * Handles unsolicited messages sent by the worker outside of normal request-response
     * cycles (e.g. [WorkerShutdownNotice]).
     *
     * @param msg The incoming message.
     * @param session The WebSocket session used to send back an acknowledgement.
     * @return `true` if the message was consumed here and must not be dispatched to pending
     *   request deferreds; `false` if it should be passed to the regular dispatch logic.
     */
    private suspend fun handleUnsolicitedMessage(
        msg: WorkerWsMessage,
        session: DefaultWebSocketSession
    ): Boolean {
        if (msg is WorkerShutdownNotice) {
            logger.warn(
                "[node={}] Received WorkerShutdownNotice: {}",
                nodeId, msg.reason
            )
            shutdownReason = msg.reason
            drainPendingRequests(WorkerShutdownException(msg.reason))
            val ack = WorkerShutdownAck(requestId = msg.requestId)
            try {
                session.send(Frame.Binary(true, cbor.encodeToByteArray<WorkerWsMessage>(ack)))
            } catch (e: Exception) {
                logger.warn("[node={}] Failed to send WorkerShutdownAck: {}", nodeId, e.message)
            }
            return true
        }
        return false
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

    /**
     * Completes all pending request deferreds exceptionally and clears the map.
     *
     * Called both on error paths and on normal WS close so that callers never
     * wait for the full [OPERATION_TIMEOUT_MS] when the connection has already dropped.
     */
    private fun drainPendingRequests(cause: Exception) {
        val keys = pendingRequests.keys.toList()
        if (keys.isNotEmpty()) {
            logger.warn(
                "[node={}] Draining {} pending request(s) due to WS close: {}",
                nodeId, keys.size, cause.message
            )
        }
        for (key in keys) {
            pendingRequests.remove(key)?.completeExceptionally(cause)
        }
    }

    /**
     * Generates a unique random request correlation identifier.
     *
     * @return A new random UUID string.
     */
    private fun newRequestId() = UUID.randomUUID().toString()
}

