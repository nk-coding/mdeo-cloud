package com.mdeo.modeltransformationexecution.config

import com.mdeo.execution.common.config.BaseExecutionConfig
import com.mdeo.execution.common.config.DatabaseConfig
import com.mdeo.execution.common.config.ExecutionServiceConfig

/**
 * Application configuration for the model transformation execution service.
 * Extends the base execution config with transformation-specific settings.
 *
 * @property serverPort The port number on which the server will listen
 * @property database Database connection configuration
 * @property backendApiUrl Base URL for the backend API
 * @property jwtIssuer JWT issuer identifier
 * @property executionTimeoutMs Timeout for execution in milliseconds
 */
data class AppConfig(
    override val serverPort: Int,
    override val database: DatabaseConfig,
    override val backendApiUrl: String,
    override val jwtIssuer: String,
    override val executionTimeoutMs: Long
) : ExecutionServiceConfig {
    companion object {
        private const val DEFAULT_PORT = 8082
        private const val DEFAULT_BACKEND_URL = "http://localhost:8080/api"
        private const val DEFAULT_JWT_ISSUER = "mdeo-platform"
        private const val DEFAULT_TIMEOUT_MS = 60000L

        /**
         * Loads configuration from environment variables with fallback defaults.
         *
         * @return Fully configured AppConfig instance
         */
        fun load(): AppConfig {
            val baseConfig = BaseExecutionConfig.fromEnvironment(
                defaultPort = DEFAULT_PORT,
                defaultBackendUrl = DEFAULT_BACKEND_URL,
                defaultJwtIssuer = DEFAULT_JWT_ISSUER,
                defaultTimeoutMs = DEFAULT_TIMEOUT_MS
            )
            
            return AppConfig(
                serverPort = baseConfig.serverPort,
                database = baseConfig.database,
                backendApiUrl = baseConfig.backendApiUrl,
                jwtIssuer = baseConfig.jwtIssuer,
                executionTimeoutMs = baseConfig.executionTimeoutMs
            )
        }
    }
}
