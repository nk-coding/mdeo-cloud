package com.mdeo.scriptexecution.config

/**
 * Main application configuration for the script execution service.
 *
 * @property serverPort The port number on which the server will listen
 * @property database Database connection configuration
 * @property backendApiUrl Base URL for the backend API to fetch typed ASTs and JWKS
 * @property executionTimeoutMs Timeout for script execution in milliseconds
 */
data class AppConfig(
    val serverPort: Int,
    val database: DatabaseConfig,
    val backendApiUrl: String,
    val executionTimeoutMs: Long
) {
    companion object {
        /**
         * Loads application configuration from environment variables with fallback defaults.
         *
         * @return A fully configured AppConfig instance
         */
        fun load(): AppConfig {
            return AppConfig(
                serverPort = System.getenv("PORT")?.toIntOrNull() ?: 8081,
                database = DatabaseConfig(
                    url = System.getenv("DATABASE_URL") 
                        ?: "jdbc:postgresql://localhost:5433/mdeo",
                    user = System.getenv("DATABASE_USER") ?: "mdeo",
                    password = System.getenv("DATABASE_PASSWORD") ?: "mdeo",
                    maxPoolSize = System.getenv("DATABASE_MAX_POOL_SIZE")?.toIntOrNull() ?: 10
                ),
                backendApiUrl = System.getenv("BACKEND_API_URL") ?: "http://localhost:8080/api",
                executionTimeoutMs = System.getenv("EXECUTION_TIMEOUT_MS")?.toLongOrNull() ?: 30000L
            )
        }
    }
}

/**
 * Database configuration.
 *
 * @property url JDBC connection URL
 * @property user Database username
 * @property password Database password
 * @property maxPoolSize Maximum number of connections in the pool
 */
data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)

