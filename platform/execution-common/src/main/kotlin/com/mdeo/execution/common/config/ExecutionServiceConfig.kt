package com.mdeo.execution.common.config

/**
 * Base configuration interface for execution services.
 * Defines the common configuration properties required by all execution services.
 * Service-specific timeout settings are managed by each service individually.
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
}

/**
 * Default implementation of execution service configuration.
 * Provides common configuration loading from environment variables.
 *
 * @property serverPort The port number on which the server will listen
 * @property database Database connection configuration
 * @property backendApiUrl Base URL for the backend API
 * @property jwtIssuer JWT issuer identifier
 */
data class BaseExecutionConfig(
    override val serverPort: Int,
    override val database: DatabaseConfig,
    override val backendApiUrl: String,
    override val jwtIssuer: String
) : ExecutionServiceConfig {
    companion object {
        /**
         * Loads base configuration from environment variables with fallback defaults.
         *
         * @param defaultPort Default port if SERVER_PORT environment variable is not set
         * @param defaultBackendUrl Default backend API URL
         * @param defaultJwtIssuer Default JWT issuer
         * @return A fully configured BaseExecutionConfig instance
         */
        fun fromEnvironment(
            defaultPort: Int = 8080,
            defaultBackendUrl: String = "http://localhost:8080/api",
            defaultJwtIssuer: String = "mdeo-platform"
        ): BaseExecutionConfig {
            return BaseExecutionConfig(
                serverPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: defaultPort,
                database = DatabaseConfig.fromEnvironment(),
                backendApiUrl = System.getenv("BACKEND_API_URL") ?: defaultBackendUrl,
                jwtIssuer = System.getenv("JWT_ISSUER") ?: defaultJwtIssuer
            )
        }
    }
}
