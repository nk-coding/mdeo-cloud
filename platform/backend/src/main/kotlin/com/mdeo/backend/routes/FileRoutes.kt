package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.FileService
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Configures file and directory management routes.
 *
 * @param fileService Service for file operations
 */
fun Route.fileRoutes(fileService: FileService) {
    route("/api/projects/{projectId}/files") {
        /**
         * Reads file contents.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @return File contents as byte array or ApiResult.Failure on error
         */
        get("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@get
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            when (val result = fileService.readFile(projectId, path)) {
                is ApiResult.Success -> {
                    call.respondBytes(result.value, ContentType.Application.OctetStream)
                }
                is ApiResult.Failure -> {
                    call.respond(result)
                }
            }
        }
        
        /**
         * Creates or updates a file (POST variant).
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @param create Query parameter to allow creation (default true)
         * @param overwrite Query parameter to allow overwrite (default false)
         * @param body File contents as byte array
         * @return ApiResult indicating success or failure
         */
        post("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@post
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val content = call.receive<ByteArray>()
            val create = call.request.queryParameters["create"]?.toBoolean() ?: true
            val overwrite = call.request.queryParameters["overwrite"]?.toBoolean() ?: false
            
            val result = fileService.writeFile(projectId, path, content, create, overwrite)
            call.respond(result)
        }
        
        /**
         * Updates or creates a file (PUT variant).
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @param create Query parameter to allow creation (default false)
         * @param overwrite Query parameter to allow overwrite (default true)
         * @param body File contents as byte array
         * @return ApiResult indicating success or failure
         */
        put("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@put
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val content = call.receive<ByteArray>()
            val create = call.request.queryParameters["create"]?.toBoolean() ?: false
            val overwrite = call.request.queryParameters["overwrite"]?.toBoolean() ?: true
            
            val result = fileService.writeFile(projectId, path, content, create, overwrite)
            call.respond(result)
        }
        
        /**
         * Deletes a file or directory.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @param recursive Query parameter to enable recursive deletion (default false)
         * @return ApiResult indicating success or failure
         */
        delete("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@delete
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val recursive = call.request.queryParameters["recursive"]?.toBoolean() ?: false
            
            val result = fileService.delete(projectId, path, recursive)
            call.respond(result)
        }
    }
    
    route("/api/projects/{projectId}/dirs") {
        /**
         * Creates a directory.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for directory path
         * @return ApiResult indicating success or failure
         */
        post("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@post
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val result = fileService.mkdir(projectId, path)
            call.respond(result)
        }
        
        /**
         * Reads directory contents.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for directory path (defaults to root)
         * @return ApiResult with directory contents or failure
         */
        get("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@get
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/").ifEmpty { "/" }
            
            val result = fileService.readdir(projectId, path)
            call.respond(result)
        }
    }
    
    route("/api/projects/{projectId}/stat") {
        /**
         * Gets file or directory metadata.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file/directory path
         * @return ApiResult with file stats or failure
         */
        get("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@get
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val result = fileService.stat(projectId, path)
            call.respond(result)
        }
    }
    
    route("/api/projects/{projectId}/version") {
        /**
         * Gets the version of a file.
         *
         * @param projectId Path parameter for project UUID
         * @param path Variable path segments for file path
         * @return ApiResult with file version or failure
         */
        get("{path...}") {
            val (projectId, _) = call.validateProjectAccess() ?: return@get
            
            val pathParts = call.parameters.getAll("path") ?: emptyList()
            val path = pathParts.joinToString("/")
            
            val result = fileService.getFileVersion(projectId, path)
            call.respond(result)
        }
    }
    
    route("/api/projects/{projectId}/rename") {
        /**
         * Renames or moves a file or directory.
         *
         * @param projectId Path parameter for project UUID
         * @param from Query parameter for source path
         * @param to Query parameter for destination path
         * @param overwrite Query parameter to allow overwrite (default false)
         * @return ApiResult indicating success or failure
         */
        post {
            val (projectId, _) = call.validateProjectAccess() ?: return@post
            
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]
            val overwrite = call.request.queryParameters["overwrite"]?.toBoolean() ?: false
            
            if (from == null || to == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing 'from' or 'to' parameter"))
                return@post
            }
            
            val result = fileService.rename(projectId, from, to, overwrite)
            call.respond(result)
        }
    }
}

/**
 * Validates project access and extracts project ID from path parameters.
 *
 * @return Pair of project UUID and user ID if validation succeeds, null otherwise
 */
private suspend fun ApplicationCall.validateProjectAccess(): Pair<UUID, String>? {
    val session = getUserSession()
    if (session == null) {
        respond(HttpStatusCode.Unauthorized)
        return null
    }
    
    val projectId = parameters["projectId"]?.let { 
        try { UUID.fromString(it) } catch (e: Exception) { null }
    }
    if (projectId == null) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
        return null
    }
    
    val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
        return null
    }
    
    return Pair(projectId, session.userId)
}
