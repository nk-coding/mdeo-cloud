package com.mdeo.optimizerexecution.worker

import com.mdeo.common.model.ExecutionState
import com.mdeo.execution.common.subprocess.SubprocessPool
import com.mdeo.execution.common.subprocess.SubprocessResult
import com.mdeo.execution.common.subprocess.SubprocessRunner
import com.mdeo.metamodel.SerializedModel
import com.mdeo.optimizer.worker.*
import com.mdeo.optimizerexecution.database.OptimizerExecutionsTable
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.uuid.toKotlinUuid

/**
 * Service that manages worker-side execution state for multi-node optimization.
 *
 * Each optimization execution runs its mutations and evaluations in a dedicated child JVM
 * ([WorkerSubprocessMain]) started via [SubprocessRunner].
 *
 * **WebSocket mode** (default for federated / remote nodes): The subprocess opens a WebSocket
 * connection directly back to the orchestrator and handles all generation traffic
 * (imports, mutations, discards) in-process. The parent stores solution bytes for crash
 * recovery and keeps discards in sync via [SubprocessChannelMessage]s.
 *
 * **Local-channel mode** (for the local node in a federated run): The subprocess receives
 * orchestrator requests through the subprocess stdin/stdout pipe as
 * [SubprocessChannelMessage.OrchestratorRequest] channel messages and replies via
 * [SubprocessChannelMessage.OrchestratorResponses]. This service acts as a forwarder:
 * it accepts the orchestrator's WebSocket connection, serialises each incoming [WorkerWsMessage]
 * into an [OrchestratorRequest], forwards it to the subprocess, collects the
 * [OrchestratorResponses] reply, and echoes each response frame back to the orchestrator.
 * This avoids a second network connection (even to localhost).
 *
 * In both modes, solutions are stored as raw CBOR bytes in the parent process for model
 * fetch requests and crash diagnostics.
 *
 * @param workerThreads Number of blocking I/O threads per execution for subprocess communication.
 * @param scriptTimeoutMs Per-script-invocation timeout budget in milliseconds.
 * @param transformationTimeoutMs Transformation-step timeout budget in milliseconds.
 * @param serverPort The local HTTP/WS server port, used to build the subprocess WS URL.
 * @param subprocessPool Pool of reusable subprocess runners. Completed executions return
 *        their subprocesses to the pool (after a protocol-level reset) instead of destroying
 *        them, amortising JVM startup and class-loading costs across executions.
 */
@OptIn(ExperimentalSerializationApi::class)
class WorkerService(
    private val workerThreads: Int,
    private val scriptTimeoutMs: Long,
    private val transformationTimeoutMs: Long,
    private val serverPort: Int = 0,
    private val subprocessPool: SubprocessPool = buildDefaultPool()
) {

    /**
     * Logger instance for this service. 
     */
    private val logger = LoggerFactory.getLogger(WorkerService::class.java)
    /**
     * Map of active execution states keyed by execution ID string. 
     */
    private val executions = ConcurrentHashMap<String, WorkerExecutionState>()

    /**
     * Pending local-channel response deferreds, keyed by [WorkerWsMessage.requestId].
     *
     * When [WorkerService] acts as a forwarder in local-channel mode it registers a
     * [CompletableDeferred] here before sending an [OrchestratorRequest] to the subprocess.
     * The channel message handler completes it when the matching [OrchestratorResponses]
     * arrives, unblocking [sendViaLocalChannel].
     */
    private val localChannelPending =
        ConcurrentHashMap<String, CompletableDeferred<List<WorkerWsMessage>>>()

    /**
     * Returns metadata describing this worker node's local thread capacity and
     * the algorithm backends it supports.
     *
     * @return A [WorkerMetadata] instance with current configuration values.
     */
    fun getMetadata(): WorkerMetadata = WorkerMetadata(
        threadCount = workerThreads,
        supportedBackends = SUPPORTED_BACKENDS
    )

    /**
     * CBOR codec used to encode/decode subprocess and WebSocket messages. 
     */
    private val cbor = Cbor { ignoreUnknownKeys = true }

    /**
     * Allocates resources for a new optimization execution on this worker.
     *
     * Starts a [WorkerSubprocessMain] subprocess, sends the [WorkerSubprocessRequest.Setup]
     * command carrying all compilation inputs, and stores the returned initial solutions.
     *
     * When [orchestratorWsUrl] is provided, the subprocess will connect back to the
     * orchestrator via WebSocket and handle all generation traffic directly.
     *
     * When [request.useLocalChannel] is `true`, the subprocess uses the stdin/stdout pipe
     * for orchestrator communication and this service acts as a forwarder via
     * [WorkerService.handleOrchestratorSession].
     *
     * @param request Allocation request from the orchestrator.
     * @param orchestratorWsUrl WebSocket URL for the subprocess to connect back to, or `null`.
     * @return Allocation response with initial solution fitness data.
     */
    suspend fun allocate(
        request: WorkerAllocationRequest,
        orchestratorWsUrl: String? = null
    ): WorkerAllocationResponse {
        val effectiveThreads = minOf(request.threadsPerNode, workerThreads)
        val useLocalChannel = request.useLocalChannel
        logger.info(
            "Allocating execution {} with {} initial solutions ({} threads, cap {}, wsMode={}, localChannel={})",
            request.executionId, request.initialSolutionCount, effectiveThreads,
            request.threadsPerNode, orchestratorWsUrl != null, useLocalChannel
        )

        val solutions = ConcurrentHashMap<String, ByteArray>()
        val dispatcher = Executors.newFixedThreadPool(effectiveThreads).asCoroutineDispatcher()
        val needsChannelHandler = orchestratorWsUrl != null || useLocalChannel
        val pooledRunner = subprocessPool.acquire()
        val runner: SubprocessRunner
        if (pooledRunner != null) {
            runner = pooledRunner
            configureChannelHandler(runner, solutions, request.executionId, needsChannelHandler)
        } else {
            runner = createSubprocessRunner(solutions, request.executionId, needsChannelHandler)
            if (!runner.start()) {
                dispatcher.close()
                throw IllegalStateException("Failed to start worker subprocess for execution ${request.executionId}")
            }
        }

        val setupRequest = WorkerSubprocessRequest.Setup(
            metamodelData = request.metamodelData,
            initialModelData = request.initialModelData,
            transformationAstJsons = request.transformationAstJsons,
            scriptAstJsons = request.scriptAstJsons,
            goalConfig = request.goalConfig,
            solverConfig = request.solverConfig,
            initialSolutionCount = request.initialSolutionCount,
            skipInitialization = false,
            orchestratorWsUrl = orchestratorWsUrl,
            useLocalChannel = useLocalChannel,
            scriptTimeoutMs = scriptTimeoutMs,
            transformationTimeoutMs = transformationTimeoutMs,
            workerThreads = effectiveThreads
        )

        val setupResult = withContext(dispatcher) {
            runner.sendCommand(cbor.encodeToByteArray<WorkerSubprocessRequest>(setupRequest))
        }

        if (setupResult !is SubprocessResult.Success) {
            runner.destroy()
            dispatcher.close()
            throw IllegalStateException("Worker subprocess Setup failed for execution ${request.executionId}: $setupResult")
        }

        val setupOk = cbor.decodeFromByteArray<WorkerSubprocessResponse>(setupResult.data) as? WorkerSubprocessResponse.SetupOk
            ?: throw IllegalStateException("Unexpected subprocess response type for execution ${request.executionId}")

        for (initial in setupOk.solutions) {
            solutions[initial.solutionId] = initial.modelBytes
        }

        executions[request.executionId] = WorkerExecutionState(
            executionId = request.executionId,
            runner = runner,
            solutions = solutions,
            dispatcher = dispatcher
        )

        logger.info("Execution {} allocated with {} initial solutions", request.executionId, setupOk.solutions.size)

        return WorkerAllocationResponse(
            initialSolutions = setupOk.solutions.map { s ->
                InitialSolutionData(
                    solutionId = s.solutionId
                )
            },
            threadCount = effectiveThreads
        )
    }


    /**
     * Retrieves the serialised model for a specific solution from the parent's byte store.
     *
     * @param executionId The execution identifier.
     * @param solutionId The solution identifier to retrieve.
     * @return The [SerializedModel] for the solution.
     */
    suspend fun getSolutionData(executionId: String, solutionId: String): SerializedModel {
        val state = requireExecution(executionId)
        val bytes = state.solutions[solutionId]
            ?: throw IllegalArgumentException("Solution not found: $solutionId (execution $executionId)")
        return cbor.decodeFromByteArray<SerializedModel>(bytes)
    }


    /**
     * Stops the subprocess and releases all resources for the specified execution.
     *
     * If a [SubprocessPool] is configured and the subprocess is still healthy,
     * it is reset and returned to the pool for reuse. Otherwise, it is destroyed.
     *
     * @param executionId The execution identifier to clean up.
     */
    suspend fun cleanup(executionId: String) {
        val state = executions.remove(executionId)
            ?: throw IllegalArgumentException("Execution not found: $executionId")

        if (state.runner.isRunning) {
            subprocessPool.resetAndRelease(state.runner)
        } else {
            state.runner.destroy()
        }
        state.dispatcher.close()
        logger.info("Execution {} cleaned up", executionId)
    }


    /**
     * Services a long-lived WebSocket connection from the orchestrator.
     *
     * **Legacy mode**: Each incoming [WorkerWsMessage] is dispatched via
     * [handleOrchestratorMessage], which drives the subprocess through stdin/stdout commands.
     *
     * **Local-channel mode**: Each message is forwarded to the subprocess as a
     * [SubprocessChannelMessage.OrchestratorRequest] and the matching
     * [SubprocessChannelMessage.OrchestratorResponses] is awaited and echoed back.
     *
     * In WebSocket subprocess mode the subprocess connects directly to the orchestrator
     * and this method is not used.
     *
     * @param executionId The execution this session is associated with.
     * @param session The Ktor WebSocket session opened by the orchestrator.
     */
    suspend fun handleOrchestratorSession(executionId: String, session: DefaultWebSocketSession) {
        logger.info("Orchestrator WebSocket connected for execution {}", executionId)
        val state = executions[executionId]
        state?.orchestratorSession = session
        try {
            supervisorScope {
                for (frame in session.incoming) {
                    if (frame !is Frame.Binary) {
                        continue
                    }
                    val bytes = frame.readBytes()
                    val msg = try {
                        cbor.decodeFromByteArray<WorkerWsMessage>(bytes)
                    } catch (e: Exception) {
                        logger.warn("Ignoring malformed WS frame for execution {}: {}", executionId, e.message)
                        continue
                    }
                    launch {
                        // WorkerShutdownAck is a one-way forward: the subprocess will halt
                        // immediately after processing it, so no response is expected.
                        if (msg is WorkerShutdownAck && state != null) {
                            forwardAckToSubprocess(state, bytes)
                            return@launch
                        }
                        val responses = if (state != null) {
                            sendViaLocalChannel(state, msg)
                        } else {
                            emptyList()
                        }
                        for (response in responses) {
                            session.send(Frame.Binary(true, cbor.encodeToByteArray<WorkerWsMessage>(response)))
                        }
                    }
                }
            }
        } finally {
            state?.orchestratorSession = null
            logger.info("Orchestrator WebSocket disconnected for execution {}", executionId)
        }
    }

    /**
     * Forwards a raw WS frame to the subprocess as a [SubprocessChannelMessage.OrchestratorRequest]
     * without registering a response deferred.
     *
     * Used for one-way acknowledgements such as [WorkerShutdownAck] where the subprocess
     * will halt immediately after receiving the message and no response should be expected.
     *
     * @param state The execution state providing the subprocess runner.
     * @param rawPayload The raw CBOR bytes of the [WorkerWsMessage] to forward.
     */
    private fun forwardAckToSubprocess(state: WorkerExecutionState, rawPayload: ByteArray) {
        try {
            state.runner.sendChannelMessage(
                cbor.encodeToByteArray<SubprocessChannelMessage>(
                    SubprocessChannelMessage.OrchestratorRequest(rawPayload)
                )
            )
        } catch (e: Exception) {
            logger.warn(
                "Failed to forward ack to subprocess for execution {}: {}",
                state.executionId, e.message
            )
        }
    }

    /**
     * Dispatches [msg] to the subprocess for [executionId] via the local channel and returns
     * the responses. Returns an empty list if the subprocess has died. Used by tests.
     *
     * @param executionId The execution identifier to look up.
     * @param msg The [WorkerWsMessage] to forward.
     * @return The list of response messages from the subprocess, or empty on subprocess death.
     */
    internal suspend fun dispatchToSubprocess(executionId: String, msg: WorkerWsMessage): List<WorkerWsMessage> =
        sendViaLocalChannel(requireExecution(executionId), msg)

    /**
     * Sends [msg] to the subprocess via the local-channel mechanism and awaits all
     * response messages. Returns an empty list if the subprocess has died before
     * or during the operation (e.g. due to an internal timeout).
     *
     * Encodes [msg] into a [SubprocessChannelMessage.OrchestratorRequest], registers a
     * pending [CompletableDeferred] in [localChannelPending] keyed by [msg.requestId], and
     * sends the channel message to the subprocess. The channel handler completes the
     * deferred when the matching [SubprocessChannelMessage.OrchestratorResponses] arrives.
     *
     * @param state The execution state (provides the [SubprocessRunner]).
     * @param msg The [WorkerWsMessage] to forward.
     * @return The list of response messages from the subprocess, or empty on subprocess death.
     */
    private suspend fun sendViaLocalChannel(
        state: WorkerExecutionState,
        msg: WorkerWsMessage
    ): List<WorkerWsMessage> {
        val requestPayload = cbor.encodeToByteArray<WorkerWsMessage>(msg)
        val channelMsgBytes = cbor.encodeToByteArray<SubprocessChannelMessage>(
            SubprocessChannelMessage.OrchestratorRequest(requestPayload)
        )
        val deferred = CompletableDeferred<List<WorkerWsMessage>>()
        localChannelPending[msg.requestId] = deferred
        state.runner.sendChannelMessage(channelMsgBytes)
        return try {
            withTimeout(LOCAL_CHANNEL_TIMEOUT_MS) { deferred.await() }
        } catch (_: Exception) {
            logger.warn("Local-channel request {} for execution did not complete (subprocess may have died)", msg.requestId)
            emptyList()
        } finally {
            localChannelPending.remove(msg.requestId)
        }
    }


    /**
     * Builds the channel message callback for a subprocess in WS or local-channel mode.
     *
     * Synchronises the parent's solution byte store, routes [OrchestratorResponses] back
     * to the pending [CompletableDeferred] registered by [sendViaLocalChannel], and
     * forwards unsolicited [OrchestratorNotice] messages to the current orchestrator
     * WebSocket session (if one is open).
     *
     * @param solutions The parent-side solution byte store.
     * @param executionId Execution identifier for logging.
     * @return The channel message callback.
     */
    private fun buildChannelHandler(
        solutions: ConcurrentHashMap<String, ByteArray>,
        executionId: String
    ): (ByteArray, (ByteArray) -> Unit) -> Unit {
        return { payload, _ ->
            try {
                when (val msg = cbor.decodeFromByteArray<SubprocessChannelMessage>(payload)) {
                    is SubprocessChannelMessage.SolutionStored -> {
                        solutions[msg.solutionId] = msg.modelBytes
                    }
                    is SubprocessChannelMessage.SolutionsDiscarded -> {
                        for (id in msg.solutionIds) {
                            solutions.remove(id)
                        }
                    }
                    is SubprocessChannelMessage.OrchestratorResponses -> {
                        val responses = msg.payloads.map { cbor.decodeFromByteArray<WorkerWsMessage>(it) }
                        val requestId = responses.firstOrNull()?.requestId
                        if (requestId != null) {
                            localChannelPending.remove(requestId)?.complete(responses)
                        }
                    }
                    is SubprocessChannelMessage.OrchestratorNotice -> {
                        val state = executions[executionId]
                        val session = state?.orchestratorSession
                        if (session != null) {
                            kotlinx.coroutines.runBlocking {
                                try {
                                    session.send(Frame.Binary(true, msg.payload))
                                } catch (e: Exception) {
                                    logger.warn(
                                        "Failed to forward OrchestratorNotice for execution {}: {}",
                                        executionId, e.message
                                    )
                                }
                            }
                        } else {
                            logger.warn(
                                "Received OrchestratorNotice for execution {} but no active session",
                                executionId
                            )
                        }
                    }
                    is SubprocessChannelMessage.OrchestratorRequest -> {
                        // Direction: parent→subprocess. Silently ignore if going the wrong way.
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error processing channel message for execution {}: {}", executionId, e.message)
            }
        }
    }

    /**
     * Configures the channel message handler and process-exit callback on an existing [SubprocessRunner].
     *
     * Used when reusing a pooled subprocess for a new execution.
     *
     * @param runner The subprocess runner to configure.
     * @param solutions The parent-side solution byte store to keep in sync.
     * @param executionId Execution identifier for logging.
     * @param wsMode Whether the subprocess operates in WebSocket or local-channel mode.
     */
    private fun configureChannelHandler(
        runner: SubprocessRunner,
        solutions: ConcurrentHashMap<String, ByteArray>,
        executionId: String,
        wsMode: Boolean
    ) {
        logger.info("Configuring pooled subprocess for execution {} (cancellation check already set)", executionId)
        runner.executionId = executionId
        runner.onChannelMessage = if (wsMode) buildChannelHandler(solutions, executionId) else null
        runner.onProcessExited = {
            localChannelPending.values.forEach { it.completeExceptionally(RuntimeException("Subprocess exited")) }
        }
    }

    /**
     * Creates a new [SubprocessRunner] with channel message handling configured.
     *
     * @param solutions The parent-side solution byte store to keep in sync.
     * @param executionId Execution identifier for logging.
     * @param wsMode Whether the subprocess operates in WebSocket or local-channel mode.
     */
    private fun createSubprocessRunner(
        solutions: ConcurrentHashMap<String, ByteArray>,
        executionId: String,
        wsMode: Boolean
    ): SubprocessRunner {
        logger.info("Creating subprocess runner for execution {} with cancellation check (interval={}ms)", executionId, CANCELLATION_CHECK_INTERVAL_MS)
        val runner = SubprocessRunner(
            mainClass = WorkerSubprocessMain::class.java.name,
            cancellationCheck = { id -> isExecutionCancelled(id) },
            cancellationCheckIntervalMs = CANCELLATION_CHECK_INTERVAL_MS,
            onChannelMessage = if (wsMode) buildChannelHandler(solutions, executionId) else null
        )
        runner.executionId = executionId
        runner.onProcessExited = {
            localChannelPending.values.forEach { it.completeExceptionally(RuntimeException("Subprocess exited")) }
        }
        return runner
    }

    /**
     * Looks up the execution state for [executionId], throwing if not found.
     *
     * @param executionId The execution identifier to look up.
     * @return The [WorkerExecutionState] for the execution.
     */
    private fun requireExecution(executionId: String): WorkerExecutionState {
        return executions[executionId]
            ?: throw IllegalArgumentException("Execution not found: $executionId")
    }

    /**
     * Releases all active executions, their associated resources, and any pooled subprocesses.
     *
     * Should be called when the application is shutting down.
     */
    suspend fun close() {
        for ((id, state) in executions) {
            try {
                state.suspendClose()
            } catch (e: Exception) {
                logger.warn("Failed to close execution {}: {}", id, e.message)
            }
        }
        executions.clear()
        subprocessPool.close()
    }

    /**
     * Per-execution runtime state held in the parent process.
     *
     * @param executionId The execution identifier.
     * @param runner The subprocess runner.
     * @param solutions Map from solution ID to CBOR-encoded [SerializedModel] bytes.
     * @param dispatcher Thread pool used for blocking subprocess I/O.
     */
    inner class WorkerExecutionState(
        val executionId: String,
        @Volatile var runner: SubprocessRunner,
        val solutions: ConcurrentHashMap<String, ByteArray>,
        val dispatcher: ExecutorCoroutineDispatcher
    ) {
        /**
         * The currently active orchestrator WebSocket session, or `null` when no session
         * is open (e.g. in WebSocket-subprocess mode where the subprocess connects directly
         * to the remote orchestrator and [WorkerService] is not involved).
         *
         * Set by [handleOrchestratorSession] when the orchestrator connects and cleared
         * when it disconnects. Used by the channel message handler to forward unsolicited
         * messages (e.g. [SubprocessChannelMessage.OrchestratorNotice]) back to the
         * orchestrator without going through the normal request-response path.
         */
        @Volatile
        var orchestratorSession: DefaultWebSocketSession? = null

        /**
         * Stops subprocess and releases thread pool. 
         */
        suspend fun suspendClose() {
            runner.stop()
            dispatcher.close()
        }
    }


    /**
     * Checks whether the execution has been cancelled or deleted in the database.
     *
     * This is called from the [SubprocessRunner] cancellation polling thread (not a coroutine),
     * so it uses a blocking [transaction] call.
     *
     * @param executionId The execution identifier (string form).
     * @return `true` if the execution record is missing or its state is [ExecutionState.CANCELLED].
     */
    private fun isExecutionCancelled(executionId: String): Boolean {
        return try {
            val uuid = UUID.fromString(executionId)
            val state = transaction {
                OptimizerExecutionsTable.selectAll()
                    .where { OptimizerExecutionsTable.id eq uuid.toKotlinUuid() }
                    .firstOrNull()
                    ?.get(OptimizerExecutionsTable.state)
            }
            val cancelled = state == null || state == ExecutionState.CANCELLED
            logger.warn("Cancellation check for execution {}: {} (dbState={})", executionId, if (cancelled) { "CANCELLED" } else { "not cancelled" }, state)
            if (cancelled) {
                logger.info("Cancellation check for execution {}: CANCELLED (dbState={})", executionId, state)
            } else {
                logger.debug("Cancellation check for execution {}: not cancelled (dbState={})", executionId, state)
            }
            cancelled
        } catch (e: Exception) {
            logger.warn("Cancellation check for execution {} failed: {}", executionId, e.message)
            false
        }
    }

    companion object {
        /**
         * Algorithm backend identifiers supported by this worker.
         * Must stay in sync with [com.mdeo.optimizer.moea.DelegatingAlgorithmProvider].
         */
        val SUPPORTED_BACKENDS: List<String> = listOf(
            "NSGAII", "SPEA2", "IBEA", "SMSEMOA", "VEGA", "PESA2", "PAES", "RANDOM"
        )

        /**
         * Interval between cancellation checks in the subprocess runner's polling thread.
         */
        private const val CANCELLATION_CHECK_INTERVAL_MS = 5000L

        /**
         * Timeout for local-channel request/response round-trips.
         * Matches the WebSocket operation timeout in [WorkerClient].
         */
        const val LOCAL_CHANNEL_TIMEOUT_MS = 600_000L

        /**
         * Builds the default [SubprocessPool] for worker executions.
         *
         * Uses the CBOR-encoded [WorkerSubprocessRequest.Reset] / [WorkerSubprocessResponse.ResetOk]
         * protocol to reset subprocesses before returning them to the pool.
         *
         * @param maxSize Maximum number of idle subprocesses the pool will hold.
         * @return A configured [SubprocessPool] with the CBOR reset protocol.
         */
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        fun buildDefaultPool(maxSize: Int = SubprocessPool.DEFAULT_POOL_SIZE): SubprocessPool {
            val cbor = Cbor { ignoreUnknownKeys = true }
            return SubprocessPool(maxSize = maxSize) { runner ->
                val result = runner.sendCommand(
                    cbor.encodeToByteArray<WorkerSubprocessRequest>(WorkerSubprocessRequest.Reset)
                )
                if (result !is SubprocessResult.Success) return@SubprocessPool false
                runCatching {
                    cbor.decodeFromByteArray<WorkerSubprocessResponse>(result.data) is WorkerSubprocessResponse.ResetOk
                }.getOrDefault(false)
            }
        }
    }
}
