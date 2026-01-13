package com.mdeo.backend

import com.mdeo.backend.config.AppConfig
import com.mdeo.backend.config.configureCors
import com.mdeo.backend.config.configureSerialization
import com.mdeo.backend.config.configureStatusPages
import com.mdeo.backend.database.DatabaseFactory
import com.mdeo.backend.plugins.*
import com.mdeo.backend.routes.*
import com.mdeo.backend.service.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.*
import kotlinx.coroutines.*
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
 * @param config Application configuration
 */
fun Application.module(config: AppConfig) {
    DatabaseFactory.init(config.database)
    
    val userService = UserService()
    val projectService = ProjectService()
    val fileService = FileService()
    val metadataService = MetadataService()
    val pluginService = PluginService(config.plugin)
    val jwtService = JwtService(config.jwt)
    val fileDataService = FileDataService(config.fileData, pluginService, fileService, jwtService)
    
    jwtService.init()
    
    runBlocking {
        userService.createDefaultAdmin(config.defaultAdmin.username, config.defaultAdmin.password)
    }
    
    monitor.subscribe(ApplicationStopped) {
        DatabaseFactory.close()
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
    
    install(Sessions) {
        val secretSignKey = hex(config.session.encryptionKey)
        cookie<UserSession>("MDEO_SESSION") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = config.session.cookieSecure
            cookie.maxAgeInSeconds = config.session.maxIdleSeconds
            cookie.extensions["SameSite"] = config.session.sameSite
            transform(SessionTransportTransformerMessageAuthentication(secretSignKey))
        }
    }
    
    configureSerialization()
    configureCors(config.cors)
    configureStatusPages()
    configureAuthentication(jwtService)
    
    routing {
        authRoutes(userService)
        publicKeyRoute(jwtService)
        
        authenticate(AUTH_SESSION, AUTH_JWT, optional = true) {
            fileRoutes(fileService, projectService)
            fileDataRoutes(fileDataService, projectService, jwtService)
        }
        
        authenticate(AUTH_SESSION) {
            projectRoutes(projectService)
            metadataRoutes(metadataService)
            pluginRoutes(pluginService, projectService, fileDataService)
            adminRoutes(userService)
            userRoutes(userService, projectService)
        }
    }
}
