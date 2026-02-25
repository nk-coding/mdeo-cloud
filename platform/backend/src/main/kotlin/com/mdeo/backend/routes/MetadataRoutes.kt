package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.MetadataService
import com.mdeo.backend.service.ProjectPermission
import com.mdeo.backend.service.ProjectService
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonObject
import java.util.*

/**
 * Configures metadata management routes for project files.
 *
 * @param metadataService Service for metadata operations
 * @param projectService Service for project access validation
 */
fun Route.metadataRoutes(metadataService: MetadataService, projectService: ProjectService) {
    route("/api/projects/{projectId}/metadata") {
        /**
         * Reads metadata for a file.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @return ApiResult with metadata as JsonObject or failure
         */
        get("{path...}") {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            
            val projectId = call.parameters["projectId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (projectId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                return@get
            }
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@get
            }

            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.READ)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val result = metadataService.readMetadata(projectId, path)
            call.respondApiResult(result)
        }
        
        /**
         * Writes metadata for a file.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @param body JsonObject containing metadata to write
         * @return ApiResult indicating success or failure
         */
        put("{path...}") {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }
            
            val projectId = call.parameters["projectId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (projectId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                return@put
            }
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@put
            }

            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.WRITE)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@put
            }

            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val metadata = call.receive<JsonObject>()
            val result = metadataService.writeMetadata(projectId, path, metadata)
            call.respondApiResult(result)
        }
    }
    
    route("/api/projects/{projectId}/executions/{executionId}/metadata") {
        /**
         * Reads metadata for an execution result file.
         *
         * @param projectId Path parameter for project UUID
         * @param executionId Path parameter for execution UUID
         * @param path Variable path segments for file path
         * @return ApiResult with metadata as JsonObject or failure
         */
        get("{path...}") {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            
            val projectId = call.parameters["projectId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (projectId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                return@get
            }
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@get
            }

            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.READ)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            val executionId = call.parameters["executionId"]?.let {
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (executionId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid execution ID"))
                return@get
            }
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val result = metadataService.readExecutionFileMetadata(executionId, path)
            call.respondApiResult(result)
        }
        
        /**
         * Writes metadata for an execution result file.
         *
         * @param projectId Path parameter for project UUID
         * @param executionId Path parameter for execution UUID
         * @param path Variable path segments for file path
         * @param body JsonObject containing metadata to write
         * @return ApiResult indicating success or failure
         */
        put("{path...}") {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@put
            }
            
            val projectId = call.parameters["projectId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (projectId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                return@put
            }
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@put
            }

            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.WRITE)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@put
            }

            val executionId = call.parameters["executionId"]?.let {
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (executionId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid execution ID"))
                return@put
            }
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val metadata = call.receive<JsonObject>()
            val result = metadataService.writeExecutionFileMetadata(executionId, path, metadata)
            call.respondApiResult(result)
        }
    }
}
