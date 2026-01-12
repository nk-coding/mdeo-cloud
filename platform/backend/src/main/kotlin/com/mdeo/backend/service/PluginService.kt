package com.mdeo.backend.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.mdeo.backend.config.PluginConfig
import com.mdeo.backend.database.PluginsTable
import com.mdeo.backend.database.ProjectPluginsTable
import com.mdeo.common.model.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Service for managing plugins and their associations with projects.
 *
 * @param config The plugin configuration settings
 */
class PluginService(config: PluginConfig) {
    private val logger = LoggerFactory.getLogger(PluginService::class.java)
    
    private val pluginCache = Caffeine.newBuilder()
        .expireAfterWrite(config.cacheTtlSeconds, TimeUnit.SECONDS)
        .maximumSize(100)
        .build<String, ResolvedPlugin>()
    
    // Use a single mock Lucide-style icon node for all plugins in the backend.
    // This is a JsonArray of icon children (tag + attributes) that the frontend
    // expects and will convert via `convertIcon`.
    private val mockIconNode = buildJsonArray {
        addJsonArray {
            add("path")
            addJsonObject { put("d", "M 19 15 L 22 18") }
        }
        addJsonArray {
            add("path")
            addJsonObject { put("d", "M 22 18 L 19 21") }
        }
        addJsonArray {
            add("path")
            addJsonObject { put("d", "M2 18h20") }
        }
        addJsonArray {
            add("path")
            addJsonObject { put("d", "M4.8 10.4V8.6a.6.6 0 0 1 .6-.6h7.2a.6.6 0 0 1 .6.6v1.8") }
        }
        addJsonArray {
            add("path")
            addJsonObject { put("d", "M9 8V5.6") }
        }
        addJsonArray {
            add("rect")
            addJsonObject {
                put("x", "11.4")
                put("y", "10.4")
                put("width", "3.6")
                put("height", "3.6")
                put("rx", ".6")
                put("fill", "currentColor")
            }
        }
        addJsonArray {
            add("rect")
            addJsonObject {
                put("x", "3")
                put("y", "10.4")
                put("width", "3.6")
                put("height", "3.6")
                put("rx", ".6")
                put("fill", "currentColor")
            }
        }
        addJsonArray {
            add("rect")
            addJsonObject {
                put("x", "7.2")
                put("y", "2")
                put("width", "3.6")
                put("height", "3.6")
                put("rx", ".6")
                put("fill", "currentColor")
            }
        }
    }
    
    /**
     * Retrieves all registered plugins.
     *
     * @return List of all plugins
     */
    fun getPlugins(): List<BackendPlugin> {
        return transaction {
            PluginsTable.selectAll()
                .map { row -> 
                    val id = row[PluginsTable.id].toString()
                    val url = row[PluginsTable.url]
                    createBackendPlugin(id, url)
                }
        }
    }
    
    /**
     * Retrieves all plugins associated with a specific project.
     *
     * @param projectId The UUID of the project
     * @return List of plugins associated with the project
     */
    fun getProjectPlugins(projectId: UUID): List<BackendPlugin> {
        return transaction {
            (ProjectPluginsTable innerJoin PluginsTable)
                .selectAll()
                .where { ProjectPluginsTable.projectId eq projectId }
                .map { row ->
                    val id = row[PluginsTable.id].toString()
                    val url = row[PluginsTable.url]
                    createBackendPlugin(id, url)
                }
        }
    }
    
    /**
     * Creates a new plugin with the specified URL.
     *
     * @param url The URL of the plugin
     * @return ApiResult containing the plugin ID if successful, or an error
     */
    fun createPlugin(url: String): ApiResult<String> {
        return transaction {
            val existing = PluginsTable.selectAll()
                .where { PluginsTable.url eq url }
                .firstOrNull()
            
            if (existing != null) {
                return@transaction pluginFailure(
                    ErrorCodes.PLUGIN_ALREADY_EXISTS, 
                    "Plugin with URL already exists: $url"
                )
            }
            
            val pluginId = UUID.randomUUID()
            val now = Instant.now()
            
            PluginsTable.insert {
                it[id] = pluginId
                it[PluginsTable.url] = url
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            success(pluginId.toString())
        }
    }
    
    /**
     * Deletes a plugin and removes it from all projects.
     *
     * @param pluginId The UUID of the plugin to delete
     * @return ApiResult indicating success or containing an error
     */
    fun deletePlugin(pluginId: UUID): ApiResult<Unit> {
        return transaction {
            ProjectPluginsTable.deleteWhere { ProjectPluginsTable.pluginId eq pluginId }
            
            val deleted = PluginsTable.deleteWhere { PluginsTable.id eq pluginId }
            
            if (deleted == 0) {
                return@transaction pluginFailure(ErrorCodes.PLUGIN_NOT_FOUND, "Plugin not found")
            }
            
            pluginCache.invalidate(pluginId.toString())
            
            success(Unit)
        }
    }
    
    /**
     * Associates a plugin with a project.
     *
     * @param projectId The UUID of the project
     * @param pluginId The UUID of the plugin
     * @return ApiResult indicating success or containing an error
     */
    fun addPluginToProject(projectId: UUID, pluginId: UUID): ApiResult<Unit> {
        return transaction {
            val pluginExists = PluginsTable.selectAll()
                .where { PluginsTable.id eq pluginId }
                .count() > 0
            
            if (!pluginExists) {
                return@transaction pluginFailure(ErrorCodes.PLUGIN_NOT_FOUND, "Plugin not found")
            }
            
            val alreadyAdded = ProjectPluginsTable.selectAll()
                .where { 
                    (ProjectPluginsTable.projectId eq projectId) and 
                    (ProjectPluginsTable.pluginId eq pluginId) 
                }
                .count() > 0
            
            if (alreadyAdded) {
                return@transaction pluginFailure(
                    ErrorCodes.PLUGIN_ALREADY_ADDED_TO_PROJECT, 
                    "Plugin already added to project"
                )
            }
            
            ProjectPluginsTable.insert {
                it[ProjectPluginsTable.projectId] = projectId
                it[ProjectPluginsTable.pluginId] = pluginId
            }
            
            success(Unit)
        }
    }
    
    /**
     * Removes a plugin association from a project.
     *
     * @param projectId The UUID of the project
     * @param pluginId The UUID of the plugin
     * @return ApiResult indicating success or containing an error
     */
    fun removePluginFromProject(projectId: UUID, pluginId: UUID): ApiResult<Unit> {
        return transaction {
            val deleted = ProjectPluginsTable.deleteWhere { 
                (ProjectPluginsTable.projectId eq projectId) and 
                (ProjectPluginsTable.pluginId eq pluginId) 
            }
            
            if (deleted == 0) {
                return@transaction pluginFailure(
                    ErrorCodes.PLUGIN_NOT_ADDED_TO_PROJECT, 
                    "Plugin not added to project"
                )
            }
            
            success(Unit)
        }
    }
    
    /**
     * Resolves and retrieves detailed information about a plugin.
     * Results are cached to improve performance.
     *
     * @param pluginId The UUID of the plugin
     * @return ApiResult containing the resolved plugin information, or an error
     */
    fun resolvePlugin(pluginId: UUID): ApiResult<ResolvedPlugin> {
        val cachedPlugin = pluginCache.getIfPresent(pluginId.toString())
        if (cachedPlugin != null) {
            return success(cachedPlugin)
        }
        
        return transaction {
            val row = PluginsTable.selectAll()
                .where { PluginsTable.id eq pluginId }
                .firstOrNull()
            
            if (row == null) {
                return@transaction pluginFailure(ErrorCodes.PLUGIN_NOT_FOUND, "Plugin not found")
            }
            
            val url = row[PluginsTable.url]
            val resolved = createResolvedPlugin(pluginId.toString(), url)
            
            pluginCache.put(pluginId.toString(), resolved)
            
            success(resolved)
        }
    }
    
    /**
     * Removes all plugin associations for a project.
     *
     * @param projectId The UUID of the project
     */
    fun deleteAllForProject(projectId: UUID) {
        transaction {
            ProjectPluginsTable.deleteWhere { ProjectPluginsTable.projectId eq projectId }
        }
    }
    
    private fun createBackendPlugin(id: String, url: String): BackendPlugin {
        return BackendPlugin(
            id = id,
            url = url,
            name = extractPluginName(url),
            description = "Plugin loaded from $url",
            icon = createIconNode(url)
        )
    }
    
    private fun createResolvedPlugin(id: String, url: String): ResolvedPlugin {
        return ResolvedPlugin(
            id = id,
            url = url,
            name = extractPluginName(url),
            description = "Plugin loaded from $url",
            icon = createIconNode(url)
        )
    }
    
    /**
     * Extracts a readable plugin name from a URL.
     *
     * @param url The plugin URL
     * @return The extracted plugin name
     */
    private fun extractPluginName(url: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: url
            val lastSegment = path.split("/").lastOrNull { it.isNotBlank() } ?: "Unknown Plugin"
            lastSegment
                .removeSuffix(".js")
                .removeSuffix(".mjs")
                .removeSuffix(".json")
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Unknown Plugin"
        }
    }
    
    /**
     * Creates an icon node for a plugin based on its URL.
     *
     * @param url The plugin URL
     * @return A JsonArray representing the icon node in Lucide format
     */
    private fun createIconNode(url: String): JsonArray {
        // For now return the fixed mock icon node defined above.
        return mockIconNode
    }
}
