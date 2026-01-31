package com.mdeo.execution.common.config

/**
 * Base configuration interface for execution services.
 * Defines the common configuration properties required by all execution services.
 */
interface ExecutionServiceConfig {
    /**
     * The port number on which the server will listen.
     */
    val serverPort: Int

    /**
     * Database connection configuration.
     */
    val database: DatabaseConfig

    /**
     * Base URL for the backend API to fetch data and JWKS.
     */
    val backendApiUrl: String

    /**
     * JWT issuer identifier for token validation.
     */
    val jwtIssuer: String

    /**
     * Timeout for execution operations in milliseconds.
     */
    val executionTimeoutMs: Long
}

/**
 * Default implementation of execution service configuration.
 * Provides common configuration loading from environment variables.
 *
 * @property serverPort The port number on which the server will listen
 * @property database Database connection configuration
 * @property backendApiUrl Base URL for the backend API
 * @property jwtIssuer JWT issuer identifier
 * @property executionTimeoutMs Timeout for execution in milliseconds
 */
data class BaseExecutionConfig(
    override val serverPort: Int,
    override val database: DatabaseConfig,
    override val backendApiUrl: String,
    override val jwtIssuer: String,
    override val executionTimeoutMs: Long
) : ExecutionServiceConfig {
    companion object {
        /**
         * Loads base configuration from environment variables with fallback defaults.
         *
         * @param defaultPort Default port if PORT environment variable is not set
         * @param defaultBackendUrl Default backend API URL
         * @param defaultJwtIssuer Default JWT issuer
         * @param defaultTimeoutMs Default execution timeout in milliseconds
         * @return A fully configured BaseExecutionConfig instance
         */
        fun fromEnvironment(
            defaultPort: Int = 8081,
            defaultBackendUrl: String = "http://localhost:8080/api",
            defaultJwtIssuer: String = "mdeo-platform",
            defaultTimeoutMs: Long = 30000L
        ): BaseExecutionConfig {
            return BaseExecutionConfig(
                serverPort = System.getenv("PORT")?.toIntOrNull() ?: defaultPort,
                database = DatabaseConfig.fromEnvironment(),
                backendApiUrl = System.getenv("BACKEND_API_URL") ?: defaultBackendUrl,
                jwtIssuer = System.getenv("JWT_ISSUER") ?: defaultJwtIssuer,
                executionTimeoutMs = System.getenv("EXECUTION_TIMEOUT_MS")?.toLongOrNull() ?: defaultTimeoutMs
            )
        }
    }
}
