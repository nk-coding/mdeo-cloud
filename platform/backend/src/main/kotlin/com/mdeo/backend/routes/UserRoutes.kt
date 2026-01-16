package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.ProjectService
import com.mdeo.backend.service.UserService
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Request body for updating user admin status.
 */
@Serializable
data class UpdateUserAdminRequest(
    val isAdmin: Boolean
)

/**
 * Configures user management routes.
 *
 * @param userService Service for user operations
 * @param projectService Service for project operations
 */
fun Route.userRoutes(userService: UserService, projectService: ProjectService) {
    route("/api/users") {
        /**
         * Gets all users in the system (admin only).
         *
         * @return ApiResult.Success with list of users, 401 if not authenticated, 403 if not admin
         */
        get {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            
            if (!session.isAdmin) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@get
            }
            
            val users = userService.getAllUsers()
            call.respond(users)
        }
        
        route("/{userId}") {
            /**
             * Gets all projects owned by a specific user (admin only).
             *
             * @param userId Path parameter for user UUID
             * @return ApiResult.Success with list of projects, 401/403/400 on errors
             */
            get("/projects") {
                val session = call.getUserSession()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@get
                }
                
                if (!session.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@get
                }
                
                val userId = call.parameters["userId"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                    return@get
                }
                
                val projects = projectService.getProjectsByUserId(userId)
                call.respond(projects)
            }
            
            /**
             * Updates a user's admin status (admin only).
             *
             * @param userId Path parameter for user UUID
             * @param body UpdateUserAdminRequest with isAdmin flag
             * @return ApiResult.Success on success, 401/403/400/404 on errors
             */
            put("/admin") {
                val session = call.getUserSession()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@put
                }
                
                if (!session.isAdmin) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                    return@put
                }
                
                val userId = call.parameters["userId"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                    return@put
                }
                
                val request = call.receive<UpdateUserAdminRequest>()
                
                val updated = userService.updateUserAdmin(userId, request.isAdmin)
                if (!updated) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User not found"))
                    return@put
                }
                
                call.respond(Unit)
            }
        }
    }
}
