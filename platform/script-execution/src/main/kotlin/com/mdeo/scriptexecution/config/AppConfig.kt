package com.mdeo.scriptexecution.config

import com.mdeo.execution.common.config.DatabaseConfig
import com.mdeo.execution.common.config.ExecutionServiceConfig

/**
 * Main application configuration for the script execution service.
 * Implements ExecutionServiceConfig from execution-common for standardized configuration.
 *
 * @property serverPort The port number on which the server will listen
 * @property database Database connection configuration
 * @property backendApiUrl Base URL for the backend API to fetch typed ASTs and JWKS
 * @property jwtIssuer JWT issuer identifier
 * @property executionTimeoutMs Timeout for script execution in milliseconds
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
         * Loads application configuration from environment variables with fallback defaults.
         *
         * @return A fully configured AppConfig instance
         */
        fun load(): AppConfig {
            return AppConfig(
                serverPort = System.getenv("PORT")?.toIntOrNull() ?: 8081,
                database = DatabaseConfig.fromEnvironment(),
                backendApiUrl = System.getenv("BACKEND_API_URL") ?: "http://localhost:8080/api",
                jwtIssuer = System.getenv("JWT_ISSUER") ?: "mdeo-platform",
                executionTimeoutMs = System.getenv("EXECUTION_TIMEOUT_MS")?.toLongOrNull() ?: 30000L
            )
        }
    }
}

