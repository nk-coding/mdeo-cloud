package com.mdeo.execution.common.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * Factory for creating backend API clients.
 * Allows for customization of serialization and other client settings.
 */
object BackendApiClientFactory {
    /**
     * Creates a basic backend API client with default settings.
     *
     * @param baseUrl Base URL of the backend API
     * @return A configured BackendApiClient instance
     */
    fun create(baseUrl: String): BackendApiClient {
        return BackendApiClient(baseUrl)
    }

    /**
     * Creates a backend API client with custom serializers.
     *
     * @param baseUrl Base URL of the backend API
     * @param serializersModule Custom serializers module
     * @return A configured BackendApiClient instance
     */
    fun create(baseUrl: String, serializersModule: SerializersModule): BackendApiClient {
        return BackendApiClient(baseUrl, serializersModule)
    }
}
