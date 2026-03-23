package com.mdeo.optimizerexecution

import com.mdeo.execution.common.auth.configureJwtAuth
import com.mdeo.execution.common.config.configureSerialization
import com.mdeo.execution.common.config.configureStatusPages
import com.mdeo.execution.common.database.DatabaseFactory
import com.mdeo.execution.common.routes.healthRoutes
import com.mdeo.optimizerexecution.config.AppConfig
import com.mdeo.optimizerexecution.database.OptimizerTables
import com.mdeo.optimizerexecution.routes.optimizerRoutes
import com.mdeo.optimizerexecution.routes.workerRoutes
import com.mdeo.optimizerexecution.service.OptimizerApiClient
import com.mdeo.optimizerexecution.service.OptimizerExecutionService
import com.mdeo.optimizerexecution.worker.WorkerService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val config = AppConfig.load()
    embeddedServer(Netty, port = config.serverPort, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(appConfig: AppConfig) {
    val databaseFactory = DatabaseFactory()
    databaseFactory.init(appConfig.database)
    OptimizerTables.createTables()

    val apiClient = OptimizerApiClient(appConfig.backendApiUrl)
    val executionService = OptimizerExecutionService(apiClient, appConfig.executionTimeoutMs, this, appConfig)
    val workerService = WorkerService(appConfig.workerThreads)

    monitor.subscribe(ApplicationStopped) {
        runBlocking { workerService.close() }
        databaseFactory.close()
        apiClient.close()
    }

    install(CallLogging) { level = Level.INFO }
    install(WebSockets) {
        pingPeriodMillis = 30_000L
        timeoutMillis = 15_000L
    }
    configureSerialization()
    configureStatusPages()
    configureJwtAuth(appConfig.backendApiUrl, appConfig.jwtIssuer)

    routing {
        healthRoutes { databaseFactory.checkConnection() }
        optimizerRoutes(executionService)
        workerRoutes(workerService)
    }

    logger.info("Optimizer execution service started on port ${appConfig.serverPort}")
}
