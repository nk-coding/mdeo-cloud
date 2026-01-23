package com.mdeo.scriptexecution

import com.mdeo.scriptexecution.auth.configureJwtAuth
import com.mdeo.scriptexecution.auth.fetchJwks
import com.mdeo.scriptexecution.config.*
import com.mdeo.scriptexecution.database.DatabaseFactory
import com.mdeo.scriptexecution.routes.executionRoutes
import com.mdeo.scriptexecution.routes.healthRoutes
import com.mdeo.scriptexecution.service.BackendApiService
import com.mdeo.scriptexecution.service.ExecutionService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val logger = LoggerFactory.getLogger("Application")

/**
 * Application entry point.
 * Loads configuration and starts the embedded Netty server.
 */
fun main() {
    val config = AppConfig.load()
    
    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

/**
 * Main application module that configures all services, plugins, and routing.
 *
 * @param appConfig Application configuration
 */
fun Application.module(appConfig: AppConfig) {
    DatabaseFactory.init(appConfig.database)
    
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    
    // Fetch JWKS from backend
    val jwksKeys = runBlocking {
        fetchJwks(httpClient, appConfig.backendApiUrl)
    }
    
    if (jwksKeys.isEmpty()) {
        logger.error("Failed to fetch JWKS from backend. Authentication will not work!")
    } else {
        logger.info("Successfully fetched ${jwksKeys.size} keys from JWKS endpoint")
    }
    
    val backendApiService = BackendApiService(appConfig.backendApiUrl)
    val executionService = ExecutionService(
        backendApiService,
        appConfig.executionTimeoutMs
    )
    
    monitor.subscribe(ApplicationStopped) {
        DatabaseFactory.close()
        httpClient.close()
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
    
    configureSerialization()
    configureStatusPages()
    configureJwtAuth(httpClient, appConfig.backendApiUrl, jwksKeys)
    
    routing {
        healthRoutes()
        executionRoutes(executionService)
    }
    
    logger.info("Script execution service started on port ${appConfig.serverPort}")
}
