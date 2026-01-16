package com.mdeo.backend.service

import com.mdeo.backend.config.FileDataConfig
import com.mdeo.backend.database.DataDependenciesTable
import com.mdeo.backend.database.FileDependenciesTable
import com.mdeo.backend.database.FileDataTable
import com.mdeo.backend.database.FilesTable
import com.mdeo.common.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
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
) : BaseService() {
    private val logger = LoggerFactory.getLogger(FileDataService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(if (pluginService.config.forceHttp1) HttpClient.Version.HTTP_1_1 else HttpClient.Version.HTTP_2)
        .build()

    /**
     * Gets computed file data for a specific file and key.
     * Checks cache validity and computes if necessary.
     *
     * @param projectId The UUID of the project
     * @param path The normalizedPath to the file
     * @param key The data key (e.g., "ast")
     * @return ApiResult containing the computed data with version or an error
     */
    suspend fun getFileData(projectId: UUID, path: String, key: String): ApiResult<FileDataResponse> {
        val normalizedPath = normalizePath(path)
        val computingCheck = transaction {
            FileDataTable.selectAll()
                .where {
                    (FileDataTable.projectId eq projectId) and
                            (FileDataTable.path eq normalizedPath) and
                            (FileDataTable.dataKey eq key)
                }
                .firstOrNull()
        }

        if (computingCheck != null && isDataCurrent(projectId, computingCheck)) {
            val cachedJson: JsonElement = computingCheck[FileDataTable.data]
            return success(
                FileDataResponse(
                    data = cachedJson,
                    version = computingCheck[FileDataTable.sourceVersion]
                )
            )
        }

        val fileContent = when (val result = fileService.readFile(projectId, normalizedPath)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return ApiResult.Failure(result.error)
        }

        val pluginInfo = pluginService.findPluginForFile(projectId, normalizedPath)
            ?: return fileDataFailure(
                ErrorCodes.FILE_DATA_NO_PLUGIN_FOUND,
                "No plugin found to compute data for file: $normalizedPath"
            )

        val (pluginId, languagePlugin) = pluginInfo
        val pluginUrl = pluginService.getPluginUrl(pluginId)
            ?: return fileDataFailure(
                ErrorCodes.PLUGIN_NOT_FOUND,
                "Plugin URL not found"
            )

        val contributionPlugins = pluginService.getContributionPluginsForLanguage(projectId, languagePlugin.id)

        val fileVersion = when (val result = fileService.getFileVersion(projectId, normalizedPath)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return ApiResult.Failure(result.error)
        }

        try {
            val token = jwtService.generateProjectToken(projectId)

            val computedData =
                computeFromPlugin(pluginUrl, key, normalizedPath, projectId, fileVersion, fileContent, token, contributionPlugins)

            storeFileData(projectId, normalizedPath, key, computedData, fileVersion)

            for (additional in computedData.additionalFileData) {
                storeFileData(
                    projectId,
                    normalizePath(additional.path),
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

            return success(
                FileDataResponse(
                    data = computedData.data,
                    version = fileVersion
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to compute file data for $normalizedPath:$key", e)
            return fileDataFailure(
                ErrorCodes.FILE_DATA_COMPUTATION_FAILED,
                "Failed to compute file data: ${e.message}"
            )
        }
    }

    /**
     * Checks if cached file data is still current based on transitive version checking.
     */
    private fun isDataCurrent(projectId: UUID, row: ResultRow): Boolean {
        val path = row[FileDataTable.path]
        val dataKey = row[FileDataTable.dataKey]

        if (!checkSourceFileVersion(projectId, row)) {
            deleteFileData(projectId, path, dataKey)
            return false
        }

        if (!checkFileDependencies(projectId, path, dataKey)) {
            deleteFileData(projectId, path, dataKey)
            return false
        }

        if (!checkDataDependenciesTransitively(projectId, path, dataKey)) {
            deleteFileData(projectId, path, dataKey)
            return false
        }

        return true
    }

    /**
     * Checks if the source file version matches the cached version.
     */
    private fun checkSourceFileVersion(projectId: UUID, row: ResultRow): Boolean {
        val cachedSourceVersion = row[FileDataTable.sourceVersion]
        val path = row[FileDataTable.path]

        val currentVersion = transaction {
            FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId) and (FilesTable.path eq path) }
                .firstOrNull()
                ?.get(FilesTable.version)
        } ?: return false

        return currentVersion == cachedSourceVersion
    }

    /**
     * Checks all file dependencies using a single LEFT JOIN query.
     */
    private fun checkFileDependencies(projectId: UUID, path: String, dataKey: String): Boolean {
        return transaction {
            val results = FileDependenciesTable
                .leftJoin(
                    FilesTable,
                    { FileDependenciesTable.dependencyPath },
                    { FilesTable.path },
                    additionalConstraint = { FilesTable.projectId eq projectId }
                )
                .selectAll()
                .where {
                    (FileDependenciesTable.projectId eq projectId) and
                            (FileDependenciesTable.path eq path) and
                            (FileDependenciesTable.dataKey eq dataKey)
                }

            for (result in results) {
                val expectedVersion = result[FileDependenciesTable.dependencyVersion]
                val currentVersion = result.getOrNull(FilesTable.version)

                if (currentVersion == null || currentVersion != expectedVersion) {
                    return@transaction false
                }
            }

            true
        }
    }

    /**
     * Checks data dependencies transitively using a queue-based approach.
     * Data dependencies can themselves have file and data dependencies that must be checked.
     */
    private fun checkDataDependenciesTransitively(projectId: UUID, path: String, dataKey: String): Boolean {
        val queue = ArrayDeque<DataKey>()
        val visited = mutableSetOf<DataKey>()

        val initialDeps = getDataDependenciesWithVersionCheck(projectId, path, dataKey)
            ?: return false

        queue.addAll(initialDeps)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (!visited.add(current)) {
                continue
            }

            if (!checkFileDependencies(projectId, current.path, current.key)) {
                return false
            }

            val deps = getDataDependenciesWithVersionCheck(projectId, current.path, current.key)
                ?: return false

            queue.addAll(deps)
        }

        return true
    }

    /**
     * Fetches data dependencies for a given file data entry and validates their versions.
     * Returns null if any version check fails, otherwise returns the list of dependencies.
     */
    private fun getDataDependenciesWithVersionCheck(
        projectId: UUID,
        path: String,
        dataKey: String
    ): List<DataKey>? {
        return transaction {
            val results = DataDependenciesTable
                .leftJoin(
                    FilesTable,
                    { DataDependenciesTable.dependencyPath },
                    { FilesTable.path },
                    additionalConstraint = { FilesTable.projectId eq projectId }
                )
                .selectAll()
                .where {
                    (DataDependenciesTable.projectId eq projectId) and
                            (DataDependenciesTable.path eq path) and
                            (DataDependenciesTable.dataKey eq dataKey)
                }

            val dependencies = mutableListOf<DataKey>()

            for (result in results) {
                val expectedVersion = result[DataDependenciesTable.dependencyVersion]
                val currentVersion = result.getOrNull(FilesTable.version)

                if (currentVersion == null || currentVersion != expectedVersion) {
                    return@transaction null
                }

                dependencies.add(
                    DataKey(
                        result[DataDependenciesTable.dependencyPath],
                        result[DataDependenciesTable.dependencyKey]
                    )
                )
            }

            dependencies
        }
    }

    /**
     * Computes file data by calling the responsible plugin.
     */
    private suspend fun computeFromPlugin(
        pluginUrl: String,
        key: String,
        path: String,
        project: UUID,
        version: Int,
        content: ByteArray,
        token: String,
        contributionPlugins: List<JsonObject>
    ): FileDataComputeResponse {
        return withContext(Dispatchers.IO) {
            val requestBody = json.encodeToString(
                FileDataComputeRequest(
                    path = path,
                    project = project.toString(),
                    version = version,
                    content = String(content, Charsets.UTF_8),
                    contributionPlugins = contributionPlugins
                )
            )

            val dataUrl = URI.create(pluginUrl).resolve("data/$key")

            val request = HttpRequest.newBuilder()
                .uri(dataUrl)
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

            FileDataTable.insert {
                it[FileDataTable.projectId] = projectId
                it[FileDataTable.path] = path
                it[FileDataTable.dataKey] = key
                it[FileDataTable.data] = response.data
                it[FileDataTable.sourceVersion] = sourceVersion
                it[FileDataTable.createdAt] = now
                it[FileDataTable.updatedAt] = now
            }

            for (fileDep in response.fileDependencies) {
                FileDependenciesTable.insert {
                    it[FileDependenciesTable.projectId] = projectId
                    it[FileDependenciesTable.path] = path
                    it[FileDependenciesTable.dataKey] = key
                    it[FileDependenciesTable.dependencyPath] = normalizePath(fileDep.path)
                    it[FileDependenciesTable.dependencyVersion] = fileDep.version
                }
            }

            for (dataDep in response.dataDependencies) {
                DataDependenciesTable.insert {
                    it[DataDependenciesTable.projectId] = projectId
                    it[DataDependenciesTable.path] = path
                    it[DataDependenciesTable.dataKey] = key
                    it[DataDependenciesTable.dependencyPath] = normalizePath(dataDep.path)
                    it[DataDependenciesTable.dependencyKey] = dataDep.key
                    it[DataDependenciesTable.dependencyVersion] = dataDep.version
                }
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

/**
 * Data class representing a file data key (path + key combination).
 */
private data class DataKey(val path: String, val key: String)