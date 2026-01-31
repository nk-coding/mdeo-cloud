package com.mdeo.execution.common.config

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * Configures JSON serialization for execution services with default settings.
 * Uses lenient parsing and ignores unknown keys for forward compatibility.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(createDefaultJson())
    }
}

/**
 * Configures JSON serialization with a custom serializers module.
 * Allows services to register their own type-specific serializers.
 *
 * @param serializersModule Custom serializers module for type-specific serialization
 */
fun Application.configureSerialization(serializersModule: SerializersModule) {
    install(ContentNegotiation) {
        json(createJsonWithModule(serializersModule))
    }
}

/**
 * Creates a default JSON configuration for execution services.
 *
 * @return Configured Json instance with lenient settings
 */
fun createDefaultJson(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
}

/**
 * Creates a JSON configuration with a custom serializers module.
 *
 * @param serializersModule Custom serializers module
 * @return Configured Json instance with the custom module
 */
fun createJsonWithModule(serializersModule: SerializersModule): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        this.serializersModule = serializersModule
    }
}
