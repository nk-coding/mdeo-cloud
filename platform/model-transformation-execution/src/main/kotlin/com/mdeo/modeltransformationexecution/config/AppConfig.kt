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

        /**
         * Loads configuration from environment variables with fallback defaults.
         *
         * @return Fully configured AppConfig instance
         */
        fun load(): AppConfig {
            val baseConfig = BaseExecutionConfig.fromEnvironment()
            
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
