package com.mdeo.backend.service

import com.mdeo.backend.database.DataDependenciesTable
import com.mdeo.backend.database.FileDependenciesTable
import com.mdeo.backend.database.FileDataTable
import com.mdeo.backend.database.FilesTable
import com.mdeo.common.model.*
import com.mdeo.common.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Service for computing and caching file data (e.g., AST) with dependency tracking.
 *
 * @param services The injected services providing access to configuration and other services
 */
class FileDataService(services: InjectedServices) : BaseService(), InjectedServices by services {
    private val logger = LoggerFactory.getLogger(FileDataService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * File data configuration settings 
     */
    private val fileDataConfig get() = config.fileData

    private val httpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(if (config.plugin.forceHttp1) HttpClient.Version.HTTP_1_1 else HttpClient.Version.HTTP_2)
            .build()
    }

    /**
     * Gets computed file data for a specific file and key.
     * Checks cache validity and computes if necessary.
     * Supports both path-based and language-based lookups.
     *
     * @param projectId The UUID of the project
     * @param path The normalized path to the file (optional if language is provided)
     * @param languageId The language ID (optional if path is provided, assumes root path)
     * @param key The data key (e.g., "ast")
     * @return ApiResult containing the computed data with version or an error
     */
    suspend fun getFileData(
        projectId: UUID,
        path: String?,
        languageId: String?,
        key: String
    ): ApiResult<FileDataResponse> {
        val normalizedPath = when {
            path != null -> normalizePath(path)
            languageId != null -> normalizePath("/")
            else -> return fileDataFailure(
                ErrorCodes.UNKNOWN,
                "Either path or languageId must be provided"
            )
        }

        val computingCheck = transaction {
            FileDataTable.selectAll()
                .where {
                    (FileDataTable.projectId eq projectId.toKotlinUuid()) and
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

        val fileRow = transaction {
            FilesTable.selectAll()
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq normalizedPath) }
                .firstOrNull()
        } ?: return fileDataFailure(
            ErrorCodes.FILE_NOT_FOUND,
            "File not found: $normalizedPath"
        )

        val fileType = fileRow[FilesTable.fileType]
        val isDirectory = fileType == FileType.DIRECTORY

        val fileSource = if (!isDirectory) {
            val fileContent = when (val result = fileService.readFile(projectId, normalizedPath)) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> return ApiResult.Failure(result.error)
            }

            val fileVersion = when (val result = fileService.getFileVersion(projectId, normalizedPath)) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> return ApiResult.Failure(result.error)
            }

            FileSource(
                version = fileVersion,
                content = String(fileContent, Charsets.UTF_8),
                path = normalizedPath
            )
        } else {
            null
        }

        val pluginInfo = if (languageId != null) {
            pluginService.findPluginByLanguage(projectId, languageId)
                ?: return fileDataFailure(
                    ErrorCodes.FILE_DATA_NO_PLUGIN_FOUND,
                    "No plugin found for language: $languageId"
                )
        } else {
            pluginService.findPluginForFile(projectId, normalizedPath)
                ?: return fileDataFailure(
                    ErrorCodes.FILE_DATA_NO_PLUGIN_FOUND,
                    "No plugin found to compute data for file: $normalizedPath"
                )
        }

        val (pluginId, languagePlugin) = pluginInfo
        val pluginUrl = pluginService.getPluginUrl(pluginId, useInternal = true)
            ?: return fileDataFailure(
                ErrorCodes.PLUGIN_NOT_FOUND,
                "Plugin URL not found"
            )

        val contributionPlugins = pluginService.getContributionPluginsForLanguage(projectId, languagePlugin.id)

        try {
            val token = jwtService.generateProjectToken(projectId)

            val computedData =
                computeFromPlugin(pluginUrl, key, projectId, fileSource, token, contributionPlugins)

            storeFileData(projectId, normalizedPath, key, computedData, fileSource?.version)

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
                    version = fileSource?.version ?: -1
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
                .where { (FilesTable.projectId eq projectId.toKotlinUuid()) and (FilesTable.path eq path) }
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
                    additionalConstraint = { FilesTable.projectId eq projectId.toKotlinUuid() }
                )
                .selectAll()
                .where {
                    (FileDependenciesTable.projectId eq projectId.toKotlinUuid()) and
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
                    additionalConstraint = { FilesTable.projectId eq projectId.toKotlinUuid() }
                )
                .selectAll()
                .where {
                    (DataDependenciesTable.projectId eq projectId.toKotlinUuid()) and
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
     * Path is now included inside the FileSource object.
     * For files, fileSource contains version, content, and path.
     * For directories, fileSource is null.
     *
     * @param pluginUrl Base URL of the plugin
     * @param key The data key to compute (e.g., "ast")
     * @param project Project UUID
     * @param fileSource Source data with version, content, and path (null for directories)
     * @param token JWT token for authentication
     * @param contributionPlugins List of contribution plugins to send to the plugin
     * @return Computed data response from the plugin
     */
    private suspend fun computeFromPlugin(
        pluginUrl: String,
        key: String,
        project: UUID,
        fileSource: FileSource?,
        token: String,
        contributionPlugins: List<JsonObject>
    ): FileDataComputeResponse {
        return withContext(Dispatchers.IO) {
            val requestBody = json.encodeToString(
                FileDataComputeRequest(
                    project = project.toString(),
                    source = fileSource,
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
        sourceVersion: Int?
    ) {
        val now = Instant.now()

        transaction {
            FileDataTable.deleteWhere {
                (FileDataTable.projectId eq projectId.toKotlinUuid()) and
                        (FileDataTable.path eq path) and
                        (FileDataTable.dataKey eq key)
            }

            FileDataTable.insert {
                it[FileDataTable.projectId] = projectId.toKotlinUuid()
                it[FileDataTable.path] = path
                it[FileDataTable.dataKey] = key
                it[FileDataTable.data] = response.data
                it[FileDataTable.sourceVersion] = sourceVersion ?: -1
                it[FileDataTable.createdAt] = now
                it[FileDataTable.updatedAt] = now
            }

            for (fileDep in response.fileDependencies) {
                FileDependenciesTable.insert {
                    it[FileDependenciesTable.projectId] = projectId.toKotlinUuid()
                    it[FileDependenciesTable.path] = path
                    it[FileDependenciesTable.dataKey] = key
                    it[FileDependenciesTable.dependencyPath] = normalizePath(fileDep.path)
                    it[FileDependenciesTable.dependencyVersion] = fileDep.version ?: -1
                }
            }

            for (dataDep in response.dataDependencies) {
                DataDependenciesTable.insert {
                    it[DataDependenciesTable.projectId] = projectId.toKotlinUuid()
                    it[DataDependenciesTable.path] = path
                    it[DataDependenciesTable.dataKey] = key
                    it[DataDependenciesTable.dependencyPath] = normalizePath(dataDep.path)
                    it[DataDependenciesTable.dependencyKey] = dataDep.key
                    it[DataDependenciesTable.dependencyVersion] = dataDep.version ?: -1
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
                (FileDataTable.projectId eq projectId.toKotlinUuid()) and
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
            FileDataTable.deleteWhere { FileDataTable.projectId eq projectId.toKotlinUuid() }
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