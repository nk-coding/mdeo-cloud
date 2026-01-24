package com.mdeo.scriptexecution.routes

import com.mdeo.scriptexecution.plugins.AUTH_JWT
import com.mdeo.scriptexecution.plugins.getJwtPrincipal
import com.mdeo.scriptexecution.plugins.getJwtToken
import com.mdeo.scriptexecution.model.*
import com.mdeo.scriptexecution.service.ExecutionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Configures execution-related routes.
 * All routes require JWT authentication for security.
 * Routes match the backend's plugin API expectations.
 *
 * @param executionService Service for execution operations
 */
fun Route.executionRoutes(executionService: ExecutionService) {
    val logger = LoggerFactory.getLogger("ExecutionRoutes")
    
    authenticate(AUTH_JWT) {
        route("/api/executions") {
            post {
                val principal = call.getJwtPrincipal()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                    return@post
                }
                
                val jwtToken = call.getJwtToken()
                if (jwtToken == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "JWT token required"))
                    return@post
                }
                
                if (!principal.scopes.contains("execution:write")) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                    return@post
                }
                
                val request = try {
                    call.receive<CreateExecutionRequest>()
                } catch (e: Exception) {
                    logger.error("Invalid request body", e)
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
                    return@post
                }
                
                val executionId = try {
                    UUID.fromString(request.executionId)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid execution ID"))
                    return@post
                }
                
                val projectId = try {
                    UUID.fromString(request.project)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                    return@post
                }
                
                if (principal.projectId != request.project) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token project mismatch"))
                    return@post
                }
                
                if (principal.executionId != null && principal.executionId != request.executionId) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token execution mismatch"))
                    return@post
                }
                
                try {
                    val name = executionService.createAndStartExecution(
                        executionId,
                        projectId,
                        request.filePath,
                        request.data,
                        jwtToken
                    )
                    call.respond(HttpStatusCode.Created, CreateExecutionResponse(name))
                } catch (e: Exception) {
                    logger.error("Failed to create execution", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
                }
            }
            
            post("{id}/cancel") {
                val principal = call.getJwtPrincipal()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                    return@post
                }
                
                if (!principal.scopes.contains("plugin:execution:cancel")) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                    return@post
                }
                
                val executionId = call.parameters["id"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                if (executionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid execution ID"))
                    return@post
                }
                
                if (principal.executionId != null && principal.executionId != executionId.toString()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token execution mismatch"))
                    return@post
                }
                
                try {
                    executionService.cancelExecution(executionId)
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    logger.error("Failed to cancel execution", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
                }
            }
            
            delete("{id}") {
                val principal = call.getJwtPrincipal()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                    return@delete
                }
                
                if (!principal.scopes.contains("plugin:execution:delete")) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                    return@delete
                }
                
                val executionId = call.parameters["id"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                if (executionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid execution ID"))
                    return@delete
                }
                
                if (principal.executionId != null && principal.executionId != executionId.toString()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token execution mismatch"))
                    return@delete
                }
                
                try {
                    executionService.deleteExecution(executionId)
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    logger.error("Failed to delete execution", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
                }
            }
            
            get("{id}/summary") {
                val principal = call.getJwtPrincipal()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                    return@get
                }
                
                if (!principal.scopes.contains("plugin:execution:read")) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                    return@get
                }
                
                val executionId = call.parameters["id"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                if (executionId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid execution ID"))
                    return@get
                }
                
                if (principal.executionId != null && principal.executionId != executionId.toString()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token execution mismatch"))
                    return@get
                }
                
                try {
                    val summary = executionService.getSummary(executionId)
                    if (summary == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Execution not found"))
                    } else {
                        call.respond(ExecutionSummaryResponse(summary))
                    }
                } catch (e: Exception) {
                    logger.error("Failed to get summary", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Internal error")))
                }
            }
        }
    }
}
