package com.mdeo.scriptexecution.service

import com.mdeo.script.ast.TypedAst
import com.mdeo.common.model.ApiError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlinx.serialization.Serializable
import io.ktor.client.request.*

/**
 * Service for interacting with the backend API.
 *
 * @param baseUrl Base URL of the backend API
 */
class BackendApiService(private val baseUrl: String) {
    private val logger = LoggerFactory.getLogger(BackendApiService::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Fetches the typed AST for a file from the backend API.
     *
     * @param projectId UUID of the project
     * @param filePath Path to the file
     * @param jwtToken JWT token to pass through to the backend
     * @return TypedAst object or null if not found
     */
    suspend fun getTypedAst(projectId: String, filePath: String, jwtToken: String): TypedAst? {
        return try {
            logger.debug("Fetching typed AST for $filePath in project $projectId")

            val response = client.get("$baseUrl/projects/$projectId/file-data/typed-ast") {
                parameter("path", filePath)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }

            if (response.status == HttpStatusCode.OK) {
                println(response.body<String>())
                val result = response.body<TypedAstFileDataResponse>()
                result.data
            } else {
                logger.warn("Failed to fetch typed AST: ${response.status}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching typed AST", e)
            null
        }
    }

    /**
     * Updates execution state on the backend.
     *
     * @param executionId UUID of the execution
     * @param state New state string
     * @param progressText Optional progress text
     * @param jwtToken JWT token to authenticate the request
     * @return true if update was successful, false otherwise
     */
    suspend fun updateExecutionState(
        executionId: String,
        state: String,
        progressText: String?,
        jwtToken: String
    ): Boolean {
        @Serializable
        data class UpdateExecutionStateRequest(val state: String, val progressText: String?)

        return try {
            logger.debug("Updating backend state for execution $executionId to $state")

            val response = client.patch("$baseUrl/executions/$executionId/state") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
                setBody(UpdateExecutionStateRequest(state, progressText))
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) {
                true
            } else {
                logger.warn("Failed to update execution state on backend: ${response.status}")
                false
            }
        } catch (e: Exception) {
            logger.error("Error updating execution state on backend", e)
            false
        }
    }
}

/**
 * Response for the typed-ast file data request.
 */
@Serializable
data class TypedAstFileDataResponse(
    val data: TypedAst,
    val version: Int? = null
)