package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.*
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Configures project management routes.
 *
 * @param projectService Service for project operations
 */
fun Route.projectRoutes(projectService: ProjectService) {
    route("/api/projects") {
        /**
         * Gets all projects accessible by the current user.
         *
         * @return ApiResult.Success with list of projects, 401 if not authenticated
         */
        get {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@get
            }
            
            val projects = projectService.getProjectsForUser(userId, session.isAdmin)
            call.respond(projects)
        }
        
        /**
         * Creates a new project.
         *
         * @param body CreateProjectRequest with project name
         * @return ApiResult.Success with project ID on success (201 Created), 401 if not authenticated
         */
        post {
            val session = call.getUserSession()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }
            
            val request = call.receive<CreateProjectRequest>()
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@post
            }
            val project = projectService.createProject(request.name, userId)
            call.respond(HttpStatusCode.Created, project)
        }
        
        route("/{projectId}") {
            /**
             * Gets a single project by ID.
             *
             * @param projectId Path parameter for project UUID
             * @return ApiResult.Success with project details, 401/403/404 on errors
             */
            get {
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
                
                if (!projectService.isOwnerOrAdmin(projectId, userId, session.isAdmin)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }
                
                val project = projectService.getProject(projectId)
                if (project == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project not found"))
                    return@get
                }
                
                call.respond(project)
            }
            
            /**
             * Updates a project's details.
             *
             * @param projectId Path parameter for project UUID
             * @param body UpdateProjectRequest with updated project data
             * @return ApiResult.Success on success, 401/403/404 on errors
             */
            put {
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
                
                if (!projectService.isOwnerOrAdmin(projectId, userId, session.isAdmin)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@put
                }
                
                val request = call.receive<UpdateProjectRequest>()
                val updated = projectService.updateProject(projectId, request)

                if (!updated) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project not found"))
                    return@put
                }

                val updatedProject = projectService.getProject(projectId)
                if (updatedProject == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project not found"))
                    return@put
                }

                call.respond(updatedProject)
            }
            
            /**
             * Deletes a project.
             *
             * @param projectId Path parameter for project UUID
             * @return ApiResult.Success on success, 401/403/404 on errors
             */
            delete {
                val session = call.getUserSession()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@delete
                }
                
                val projectId = call.parameters["projectId"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                if (projectId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                    return@delete
                }
                
                val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                    return@delete
                }
                
                if (!projectService.isOwnerOrAdmin(projectId, userId, session.isAdmin)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@delete
                }
                
                val deleted = projectService.deleteProject(projectId)
                
                if (!deleted) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project not found"))
                    return@delete
                }
                
                call.respond(Unit)
            }
            
            route("/owners") {
                /**
                 * Gets all owners of a project.
                 *
                 * @param projectId Path parameter for project UUID
                 * @return ApiResult.Success with list of owners, 401/403 on errors
                 */
                get {
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
                    
                    if (!projectService.isOwnerOrAdmin(projectId, userId, session.isAdmin)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                        return@get
                    }
                    
                    val owners = projectService.getProjectOwners(projectId)
                    call.respond(owners)
                }
                
                /**
                 * Adds an owner to a project.
                 *
                 * @param projectId Path parameter for project UUID
                 * @param body AddOwnerRequest with userId to add
                 * @return ApiResult.Success on success, 401/403/404 on errors
                 */
                post {
                    val session = call.getUserSession()
                    if (session == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }
                    
                    val projectId = call.parameters["projectId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    }
                    if (projectId == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                        return@post
                    }
                    
                    val currentUserId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                        return@post
                    }
                    
                    if (!projectService.isOwnerOrAdmin(projectId, currentUserId, session.isAdmin)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                        return@post
                    }
                    
                    val request = call.receive<AddOwnerRequest>()
                    val userId = try { UUID.fromString(request.userId) } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                        return@post
                    }
                    
                    when (projectService.addOwner(projectId, userId)) {
                        AddOwnerResult.SUCCESS -> call.respond(Unit)
                        AddOwnerResult.PROJECT_NOT_FOUND -> 
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project not found"))
                        AddOwnerResult.USER_NOT_FOUND -> 
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User not found"))
                        AddOwnerResult.ALREADY_OWNER -> 
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User is already an owner"))
                    }
                }
                
                /**
                 * Removes an owner from a project.
                 *
                 * @param projectId Path parameter for project UUID
                 * @param userId Path parameter for user UUID to remove
                 * @return ApiResult.Success on success, 401/403/404 on errors
                 */
                delete("/{userId}") {
                    val session = call.getUserSession()
                    if (session == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@delete
                    }
                    
                    val projectId = call.parameters["projectId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    }
                    if (projectId == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                        return@delete
                    }
                    
                    val currentUserId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                        return@delete
                    }
                    
                    val userId = call.parameters["userId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    }
                    if (userId == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                        return@delete
                    }
                    
                    if (!projectService.isOwnerOrAdmin(projectId, currentUserId, session.isAdmin)) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                        return@delete
                    }
                    
                    when (projectService.removeOwner(projectId, userId)) {
                        RemoveOwnerResult.SUCCESS -> call.respond(Unit)
                        RemoveOwnerResult.PROJECT_NOT_FOUND -> 
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project not found"))
                        RemoveOwnerResult.NOT_OWNER -> 
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User is not an owner"))
                        RemoveOwnerResult.LAST_OWNER -> 
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cannot remove the last owner"))
                    }
                }
            }
        }
    }
}
