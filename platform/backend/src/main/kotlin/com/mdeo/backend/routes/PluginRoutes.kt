package com.mdeo.backend.routes

import com.mdeo.backend.plugins.*
import com.mdeo.backend.service.PluginService
import com.mdeo.backend.service.ProjectPermission
import com.mdeo.backend.service.ProjectService
import com.mdeo.common.model.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Configures plugin management routes for both global and project-specific plugins.
 *
 * @param pluginService Service for plugin operations
 * @param projectService Service for project access validation
 */
fun Route.pluginRoutes(
    pluginService: PluginService, 
    projectService: ProjectService
) {
    route("/api/plugins") {
        /**
         * Gets all available plugins.
         *
         * @return ApiResult.Success with list of all plugins
         */
        get {
            val plugins = pluginService.getPlugins()
            call.respond(plugins)
        }
        
        /**
         * Creates a new plugin (admin only).
         *
         * @param body CreatePluginRequest with plugin URL
         * @return ApiResult.Success with plugin ID (201 Created) or ApiResult.Failure, 403 if not admin
         */
        post {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@post
            }
            
            val request = call.receive<CreatePluginRequest>()
            val result = pluginService.createPlugin(request.url)
            
            call.respondApiResult(result)
        }
        
        /**
         * Deletes a plugin (admin only).
         *
         * @param pluginId Path parameter for plugin UUID
         * @return ApiResult indicating success or failure, 403 if not admin
         */
        delete("/{pluginId}") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@delete
            }
            
            val pluginId = call.parameters["pluginId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (pluginId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid plugin ID"))
                return@delete
            }
            
            val result = pluginService.deletePlugin(pluginId)
            call.respondApiResult(result)
        }
        
        /**
         * Refreshes a plugin's data by re-fetching its manifest (admin only).
         *
         * @param pluginId Path parameter for plugin UUID
         * @return ApiResult indicating success or failure, 403 if not admin
         */
        post("/{pluginId}/refresh") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@post
            }
            
            val pluginId = call.parameters["pluginId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (pluginId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid plugin ID"))
                return@post
            }
            
            val result = pluginService.refreshPluginData(pluginId)
            
            call.respondApiResult(result)
        }

        /**
         * Refreshes all plugins by re-fetching each plugin manifest (admin only).
         *
         * @return ApiResult indicating success or failure, 403 if not admin
         */
        post("/refresh") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@post
            }

            val result = pluginService.refreshAllPluginsData()
            call.respondApiResult(result)
        }
        
        /**
         * Gets a specific plugin by ID.
         *
         * @param pluginId Path parameter for plugin UUID
         * @return ApiResult with plugin information
         */
        get("/{pluginId}") {
            val pluginId = call.parameters["pluginId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (pluginId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid plugin ID"))
                return@get
            }
            
            val result = pluginService.getPlugin(pluginId)
            call.respondApiResult(result)
        }
        
        /**
         * Updates the default status of a plugin (admin only).
         *
         * @param pluginId Path parameter for plugin UUID
         * @param body UpdatePluginDefaultRequest with default flag
         * @return ApiResult indicating success or failure, 403 if not admin
         */
        patch("/{pluginId}/default") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                return@patch
            }
            
            val pluginId = call.parameters["pluginId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (pluginId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid plugin ID"))
                return@patch
            }
            
            val request = call.receive<UpdatePluginDefaultRequest>()
            val result = pluginService.updatePluginDefault(pluginId, request.default)
            call.respondApiResult(result)
        }
    }
    
    route("/api/projects/{projectId}/plugins") {
        /**
         * Gets all plugins associated with a project.
         *
         * @param projectId Path parameter for project UUID
         * @return ApiResult.Success with list of project plugins, 401/403 on errors
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
            
            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.READ)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }
            
            val plugins = pluginService.getProjectPlugins(projectId)
            call.respond(plugins)
        }
        
        /**
         * Adds a plugin to a project.
         *
         * @param projectId Path parameter for project UUID
         * @param body AddPluginToProjectRequest with pluginId
         * @return ApiResult indicating success or failure, 401/403 on errors
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
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@post
            }
            
            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.ADMIN)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@post
            }
            
            val request = call.receive<AddPluginToProjectRequest>()
            val pluginId = try { UUID.fromString(request.pluginId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid plugin ID"))
                return@post
            }
            
            val result = pluginService.addPluginToProject(projectId, pluginId)
            
            call.respondApiResult(result)
        }
        
        /**
         * Removes a plugin from a project.
         *
         * @param projectId Path parameter for project UUID
         * @param pluginId Path parameter for plugin UUID
         * @return ApiResult indicating success or failure, 401/403 on errors
         */
        delete("/{pluginId}") {
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
            
            val pluginId = call.parameters["pluginId"]?.let { 
                try { UUID.fromString(it) } catch (e: Exception) { null }
            }
            if (pluginId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid plugin ID"))
                return@delete
            }
            
            val userId = try { UUID.fromString(session.userId) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                return@delete
            }
            
            if (!projectService.hasProjectPermission(projectId, userId, session.isAdmin, ProjectPermission.ADMIN)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@delete
            }
            
            val result = pluginService.removePluginFromProject(projectId, pluginId)
            
            call.respondApiResult(result)
        }
    }
}
