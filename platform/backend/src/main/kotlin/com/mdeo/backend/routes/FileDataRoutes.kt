package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.FileDataService
import com.mdeo.backend.service.JwtService
import com.mdeo.backend.service.ProjectService
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Configures file data computation routes.
 *
 * @param fileDataService Service for file data computation
 * @param projectService Service for project access validation
 * @param jwtService Service for JWT operations
 */
fun Route.fileDataRoutes(
    fileDataService: FileDataService,
    projectService: ProjectService,
    jwtService: JwtService
) {
    route("/api/projects/{projectId}/file-data/{key}") {
        /**
         * Gets computed file data for a specific file.
         *
         * @param projectId Path parameter for project UUID
         * @param key Path parameter for data key (e.g., "ast")
         * @param path Query parameter for file path
         * @return ApiResult with computed data or failure
         */
        get {
            val session = call.getUserSession()
            val jwtPrincipal = call.getJwtPrincipal()
            
            val projectId = call.parameters["projectId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (projectId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                return@get
            }
            
            if (session != null) {
                val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                    return@get
                }
                
                if (!projectService.isOwnerOrAdmin(projectId, userId, session.isAdmin)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }
            } else if (jwtPrincipal != null) {
                if (jwtPrincipal.projectId != projectId.toString()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token not valid for this project"))
                    return@get
                }
                if (JwtService.SCOPE_FILE_DATA_READ !in jwtPrincipal.scopes) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Token missing required scope"))
                    return@get
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                return@get
            }
            
            val key = call.parameters["key"]
            if (key.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing data key"))
                return@get
            }
            
            val path = call.request.queryParameters["path"]
            if (path.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing path parameter"))
                return@get
            }
            
            val result = fileDataService.getFileData(projectId, path, key)
            
            when (result) {
                is ApiResult.Success -> call.respond(result.value)
                is ApiResult.Failure -> call.respond(result)
            }
        }
    }
}

/**
 * Configures public routes for JWT public key.
 * This should be called outside of authentication blocks.
 *
 * @param jwtService Service for JWT operations
 */
fun Route.publicKeyRoute(jwtService: JwtService) {
    route("/api/auth/public-key") {
        /**
         * Gets the public key for JWT verification.
         * This endpoint is public and can be accessed by plugins to verify tokens.
         *
         * @return Public key in PEM format
         */
        get {
            val publicKey = jwtService.getPublicKeyPem()
            call.respond(mapOf("publicKey" to publicKey))
        }
    }
}
