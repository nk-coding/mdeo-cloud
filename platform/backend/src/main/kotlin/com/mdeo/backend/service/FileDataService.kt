package com.mdeo.backend.service

import com.mdeo.backend.config.FileDataConfig
import com.mdeo.backend.database.DataDependenciesTable
import com.mdeo.backend.database.FileDependenciesTable
import com.mdeo.backend.database.FileDataTable
import com.mdeo.backend.database.FilesTable
import com.mdeo.backend.database.ProjectPluginsTable
import com.mdeo.common.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Service for computing and caching file data (e.g., AST) with dependency tracking.
 *
 * @param config File data computation configuration
 * @param pluginService Plugin service for finding responsible plugins
 * @param fileService File service for reading file contents and versions
 * @param jwtService JWT service for generating plugin authentication tokens
 */
class FileDataService(
    private val config: FileDataConfig,
    private val pluginService: PluginService,
    private val fileService: FileService,
    private val jwtService: JwtService
) {
    private val logger = LoggerFactory.getLogger(FileDataService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    /**
     * Gets computed file data for a specific file and key.
     * Checks cache validity and computes if necessary.
     *
     * @param projectId The UUID of the project
     * @param path The path to the file
     * @param key The data key (e.g., "ast")
     * @return ApiResult containing the computed data or an error
     */
    suspend fun getFileData(projectId: UUID, path: String, key: String): ApiResult<String> {
        val computingCheck = transaction {
            FileDataTable.selectAll()
                .where { 
                    (FileDataTable.projectId eq projectId) and 
                    (FileDataTable.path eq path) and 
                    (FileDataTable.dataKey eq key)
                }
                .firstOrNull()
        }
        
        if (computingCheck != null) {
            val computingAt = computingCheck[FileDataTable.computingAt]
            if (computingAt != null) {
                val elapsed = Duration.between(computingAt, Instant.now()).seconds
                if (elapsed < config.computationTimeoutSeconds) {
                    return fileDataFailure(
                        ErrorCodes.FILE_DATA_CIRCULAR_DEPENDENCY,
                        "Circular dependency detected: computation already in progress for $path:$key"
                    )
                }
                logger.warn("Clearing stale computation flag for $path:$key (elapsed: ${elapsed}s)")
                clearComputingFlag(projectId, path, key)
            }
        }
        
        if (computingCheck != null && isDataCurrent(projectId, computingCheck)) {
            return success(String(computingCheck[FileDataTable.data]))
        }
        
        val pluginInfo = pluginService.findPluginForFile(projectId, path)
            ?: return fileDataFailure(
                ErrorCodes.FILE_DATA_NO_PLUGIN_FOUND,
                "No plugin found to compute data for file: $path"
            )
        
        val (pluginId, languagePlugin) = pluginInfo
        val pluginUrl = pluginService.getPluginUrl(pluginId)
            ?: return fileDataFailure(
                ErrorCodes.PLUGIN_NOT_FOUND,
                "Plugin URL not found"
            )
        
        val contributionPlugins = pluginService.getContributionPluginsForLanguage(projectId, languagePlugin.id)
        
        val fileContent = when (val result = fileService.readFile(projectId, path)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return ApiResult.Failure(result.error)
        }
        
        val fileVersion = when (val result = fileService.getFileVersion(projectId, path)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return ApiResult.Failure(result.error)
        }
        
        setComputingFlag(projectId, path, key)
        
        try {
            val token = jwtService.generateProjectToken(projectId)
            
            val computedData = computeFromPlugin(pluginUrl, key, path, fileVersion, fileContent, token, contributionPlugins)
            
            storeFileData(projectId, path, key, computedData, fileVersion)
            
            for (additional in computedData.additionalFileData) {
                storeFileData(
                    projectId,
                    additional.path,
                    additional.key,
                    FileDataComputeResponse(
                        data = additional.data,
                        fileDependencies = additional.fileDependencies,
                        dataDependencies = additional.dataDependencies,
                        additionalFileData = emptyList()
                    ),
                    additional.sourceVersion
                )
            }
            
            return success(computedData.data)
        } catch (e: Exception) {
            logger.error("Failed to compute file data for $path:$key", e)
            return fileDataFailure(
                ErrorCodes.FILE_DATA_COMPUTATION_FAILED,
                "Failed to compute file data: ${e.message}"
            )
        } finally {
            clearComputingFlag(projectId, path, key)
        }
    }
    
    /**
     * Checks if cached file data is still current based on transitive version checking.
     */
    private fun isDataCurrent(projectId: UUID, row: ResultRow): Boolean {
        val cachedSourceVersion = row[FileDataTable.sourceVersion]
        val path = row[FileDataTable.path]
        val dataKey = row[FileDataTable.dataKey]
        
        val currentVersion = transaction {
            FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId) and (FilesTable.path eq path) }
                .firstOrNull()
                ?.get(FilesTable.version)
        } ?: return false
        
        if (currentVersion != cachedSourceVersion) {
            deleteFileData(projectId, path, dataKey)
            return false
        }
        
        val fileDeps = transaction {
            FileDependenciesTable.selectAll()
                .where { 
                    (FileDependenciesTable.projectId eq projectId) and 
                    (FileDependenciesTable.path eq path) and 
                    (FileDependenciesTable.dataKey eq dataKey)
                }
                .map { 
                    FileDependency(
                        path = it[FileDependenciesTable.dependencyPath],
                        version = it[FileDependenciesTable.dependencyVersion]
                    )
                }
        }
        
        for (dep in fileDeps) {
            val depVersion = transaction {
                FilesTable.selectAll()
                    .where { (FilesTable.projectId eq projectId) and (FilesTable.path eq dep.path) }
                    .firstOrNull()
                    ?.get(FilesTable.version)
            }
            if (depVersion == null || depVersion != dep.version) {
                deleteFileData(projectId, path, dataKey)
                return false
            }
        }
        
        val dataDeps = transaction {
            DataDependenciesTable.selectAll()
                .where { 
                    (DataDependenciesTable.projectId eq projectId) and 
                    (DataDependenciesTable.path eq path) and 
                    (DataDependenciesTable.dataKey eq dataKey)
                }
                .map { 
                    DataDependency(
                        path = it[DataDependenciesTable.dependencyPath],
                        key = it[DataDependenciesTable.dependencyKey],
                        version = it[DataDependenciesTable.dependencyVersion]
                    )
                }
        }
        
        for (dep in dataDeps) {
            val depData = transaction {
                FileDataTable.selectAll()
                    .where { 
                        (FileDataTable.projectId eq projectId) and 
                        (FileDataTable.path eq dep.path) and 
                        (FileDataTable.dataKey eq dep.key)
                    }
                    .firstOrNull()
            }
            if (depData == null || depData[FileDataTable.sourceVersion] != dep.version) {
                deleteFileData(projectId, path, dataKey)
                return false
            }
        }
        
        return true
    }
    
    /**
     * Computes file data by calling the responsible plugin.
     */
    private suspend fun computeFromPlugin(
        pluginUrl: String,
        key: String,
        path: String,
        version: Int,
        content: ByteArray,
        token: String,
        contributionPlugins: List<JsonObject>
    ): FileDataComputeResponse {
        return withContext(Dispatchers.IO) {
            val requestBody = json.encodeToString(
                FileDataComputeRequest(
                    path = path,
                    version = version,
                    content = String(content, Charsets.UTF_8),
                    contributionPlugins = contributionPlugins
                )
            )
            
            val dataUrl = if (pluginUrl.endsWith("/")) {
                "${pluginUrl}data/$key"
            } else {
                "$pluginUrl/data/$key"
            }
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create(dataUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofMinutes(5))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() != 200) {
                throw RuntimeException("Plugin returned status ${response.statusCode()}: ${response.body()}")
            }
            
            json.decodeFromString<FileDataComputeResponse>(response.body())
        }
    }
    
    /**
     * Stores computed file data in the database with dependencies.
     */
    private fun storeFileData(
        projectId: UUID,
        path: String,
        key: String,
        response: FileDataComputeResponse,
        sourceVersion: Int
    ) {
        val now = Instant.now()
        
        transaction {
            FileDataTable.deleteWhere { 
                (FileDataTable.projectId eq projectId) and 
                (FileDataTable.path eq path) and 
                (FileDataTable.dataKey eq key)
            }
            
            FileDependenciesTable.deleteWhere {
                (FileDependenciesTable.projectId eq projectId) and 
                (FileDependenciesTable.path eq path) and 
                (FileDependenciesTable.dataKey eq key)
            }
            
            DataDependenciesTable.deleteWhere {
                (DataDependenciesTable.projectId eq projectId) and 
                (DataDependenciesTable.path eq path) and 
                (DataDependenciesTable.dataKey eq key)
            }
            
            FileDataTable.insert {
                it[FileDataTable.projectId] = projectId
                it[FileDataTable.path] = path
                it[dataKey] = key
                it[data] = response.data.toByteArray()
                it[FileDataTable.sourceVersion] = sourceVersion
                it[computingAt] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
            
            for (fileDep in response.fileDependencies) {
                FileDependenciesTable.insert {
                    it[FileDependenciesTable.projectId] = projectId
                    it[FileDependenciesTable.path] = path
                    it[dataKey] = key
                    it[dependencyPath] = fileDep.path
                    it[dependencyVersion] = fileDep.version
                }
            }
            
            for (dataDep in response.dataDependencies) {
                DataDependenciesTable.insert {
                    it[DataDependenciesTable.projectId] = projectId
                    it[DataDependenciesTable.path] = path
                    it[dataKey] = key
                    it[dependencyPath] = dataDep.path
                    it[dependencyKey] = dataDep.key
                    it[dependencyVersion] = dataDep.version
                }
            }
        }
    }
    
    /**
     * Sets the computing flag for a file data entry.
     */
    private fun setComputingFlag(projectId: UUID, path: String, key: String) {
        val now = Instant.now()
        transaction {
            val existing = FileDataTable.selectAll()
                .where { 
                    (FileDataTable.projectId eq projectId) and 
                    (FileDataTable.path eq path) and 
                    (FileDataTable.dataKey eq key)
                }
                .firstOrNull()
            
            if (existing != null) {
                FileDataTable.update({
                    (FileDataTable.projectId eq projectId) and 
                    (FileDataTable.path eq path) and 
                    (FileDataTable.dataKey eq key)
                }) {
                    it[computingAt] = now
                    it[updatedAt] = now
                }
            } else {
                FileDataTable.insert {
                    it[FileDataTable.projectId] = projectId
                    it[FileDataTable.path] = path
                    it[dataKey] = key
                    it[data] = ByteArray(0)
                    it[sourceVersion] = 0
                    it[computingAt] = now
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }
    
    /**
     * Clears the computing flag for a file data entry.
     */
    private fun clearComputingFlag(projectId: UUID, path: String, key: String) {
        transaction {
            FileDataTable.update({
                (FileDataTable.projectId eq projectId) and 
                (FileDataTable.path eq path) and 
                (FileDataTable.dataKey eq key)
            }) {
                it[computingAt] = null
                it[updatedAt] = Instant.now()
            }
        }
    }
    
    /**
     * Deletes cached file data for a specific entry along with its dependencies.
     */
    private fun deleteFileData(projectId: UUID, path: String, key: String) {
        transaction {
            FileDataTable.deleteWhere { 
                (FileDataTable.projectId eq projectId) and 
                (FileDataTable.path eq path) and 
                (FileDataTable.dataKey eq key)
            }
            
            FileDependenciesTable.deleteWhere {
                (FileDependenciesTable.projectId eq projectId) and 
                (FileDependenciesTable.path eq path) and 
                (FileDependenciesTable.dataKey eq key)
            }
            
            DataDependenciesTable.deleteWhere {
                (DataDependenciesTable.projectId eq projectId) and 
                (DataDependenciesTable.path eq path) and 
                (DataDependenciesTable.dataKey eq key)
            }
        }
    }
    
    /**
     * Invalidates all file data for a project.
     * Cascading deletes automatically handle dependencies.
     *
     * @param projectId The UUID of the project
     */
    fun invalidateProjectData(projectId: UUID) {
        logger.info("Invalidating all file data for project $projectId")
        transaction {
            FileDataTable.deleteWhere { FileDataTable.projectId eq projectId }
        }
    }
    
    /**
     * Invalidates all file data for projects using a specific plugin.
     *
     * @param pluginId The UUID of the plugin
     */
    fun invalidatePluginData(pluginId: UUID) {
        logger.info("Invalidating file data for all projects using plugin $pluginId")
        val projectIds = pluginService.getProjectsUsingPlugin(pluginId)
        for (projectId in projectIds) {
            invalidateProjectData(projectId)
        }
    }
}
