package com.mdeo.backend.config

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the application.
 *
 * Sets up allowed HTTP methods, headers, credentials handling, and permitted origins.
 *
 * @param config The CORS configuration containing allowed hosts
 */
fun Application.configureCors(config: CorsConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        
        allowCredentials = true
        
        config.allowedHosts.forEach { host ->
            allowHost(host, schemes = listOf("http", "https"))
        }
    }
}
