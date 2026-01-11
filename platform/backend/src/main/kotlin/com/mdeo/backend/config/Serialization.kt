package com.mdeo.backend.config

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

/**
 * JSON serialization configuration for the application.
 *
 * Configured with compact output, strict parsing, and sensible defaults.
 */
val appJson = Json {
    prettyPrint = false
    isLenient = false
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Configures content negotiation with JSON serialization for the application.
 *
 * Installs ContentNegotiation plugin with the application's JSON configuration.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(appJson)
    }
}
