package com.mdeo.scriptexecution.service

import com.mdeo.script.ast.TypedAst
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

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
            
            val response = client.get("$baseUrl/api/projects/$projectId/file-data/typed-ast") {
                parameter("path", filePath)
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }
            
            if (response.status == HttpStatusCode.OK) {
                response.body<TypedAst>()
            } else {
                logger.warn("Failed to fetch typed AST: ${response.status}")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching typed AST", e)
            null
        }
    }
}
