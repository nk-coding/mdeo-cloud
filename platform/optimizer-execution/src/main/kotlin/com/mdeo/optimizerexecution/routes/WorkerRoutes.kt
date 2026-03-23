package com.mdeo.optimizerexecution.routes

import com.mdeo.optimizer.worker.*
import com.mdeo.optimizerexecution.worker.WorkerService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("WorkerRoutes")

@OptIn(ExperimentalSerializationApi::class)
private val cbor = Cbor { ignoreUnknownKeys = true }

/**
 * Registers worker API routes under `/api/worker` and `/ws/worker`.
 *
 * HTTP routes are used for the initial allocation handshake, solution data retrieval,
 * and cleanup. All ongoing per-generation traffic (imports + mutations + discards) is
 * served over a persistent binary-CBOR WebSocket connection opened by the orchestrator
 * after successful allocation.
 *
 * No authentication is required between worker nodes.
 *
 * @param workerService The service managing worker-side execution state.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Route.workerRoutes(workerService: WorkerService) {
    // HTTP routes (allocation + solution retrieval + cleanup)
    route("/api/worker/executions") {
        allocateRoute(workerService)

        route("/{id}") {
            getSolutionDataRoute(workerService)
            cleanupRoute(workerService)
        }
    }

    // WebSocket route (orchestrator ↔ worker ongoing communication)
    route("/ws/worker/executions/{id}") {
        orchestratorWsRoute(workerService)
    }
}

/**
 * POST `/api/worker/executions` — allocates resources for a new optimization execution.
 *
 * @param workerService The worker service to delegate allocation to.
 */
private fun Route.allocateRoute(workerService: WorkerService) {
    post {
        val request = call.receive<WorkerAllocationRequest>()
        try {
            val response = workerService.allocate(request)
            call.respond(HttpStatusCode.Created, response)
        } catch (e: Exception) {
            logger.error("Allocation failed for execution {}", request.executionId, e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Allocation failed")))
        }
    }
}

/**
 * GET `/api/worker/executions/{id}/solutions/{solutionId}` — returns model data as CBOR.
 *
 * The orchestrator calls this to pre-fetch model data for solutions it needs to transfer
 * to another node during rebalancing, before embedding the data inline in that node's
 * [NodeWorkBatchRequest].
 *
 * @param workerService The worker service to retrieve solution data from.
 */
@OptIn(ExperimentalSerializationApi::class)
private fun Route.getSolutionDataRoute(workerService: WorkerService) {
    get("/solutions/{solutionId}") {
        val executionId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val solutionId = call.parameters["solutionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        try {
            val modelData = workerService.getSolutionData(executionId, solutionId)
            val bytes = cbor.encodeToByteArray(modelData)
            call.respondBytes(bytes, ContentType.Application.Cbor)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Not found")))
        } catch (e: Exception) {
            logger.error("Failed to get solution {} for execution {}", solutionId, executionId, e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed")))
        }
    }
}

/**
 * DELETE `/api/worker/executions/{id}` — cleans up an execution and frees resources.
 *
 * @param workerService The worker service to delegate cleanup to.
 */
private fun Route.cleanupRoute(workerService: WorkerService) {
    delete {
        val executionId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
        try {
            workerService.cleanup(executionId)
            call.respond(HttpStatusCode.OK)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "Not found")))
        } catch (e: Exception) {
            logger.error("Failed to cleanup execution {}", executionId, e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed")))
        }
    }
}

// ─── WebSocket routes ───────────────────────────────────────────────────────────

/**
 * WebSocket `/ws/worker/executions/{id}` — long-lived orchestrator session.
 *
 * The orchestrator connects here after successful HTTP allocation and keeps this
 * connection open for the entire execution. Binary CBOR frames carry [WorkerWsMessage]
 * values encoding unified per-generation work batches ([NodeWorkBatchRequest]) and
 * solution fetch requests ([SolutionFetchRequest]).
 *
 * @param workerService The worker service that drives the session.
 */
private fun Route.orchestratorWsRoute(workerService: WorkerService) {
    webSocket {
        val executionId = call.parameters["id"]
        if (executionId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing execution ID"))
            return@webSocket
        }
        workerService.handleOrchestratorSession(executionId, this)
    }
}
