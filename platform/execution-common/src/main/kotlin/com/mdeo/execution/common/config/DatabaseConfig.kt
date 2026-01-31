package com.mdeo.execution.common.config

/**
 * Base configuration for database connections.
 * Used by execution services to configure their database connections.
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
) {
    companion object {
        /**
         * Loads database configuration from environment variables with fallback defaults.
         *
         * @param defaultUrl Default JDBC URL if not specified in environment
         * @param defaultUser Default database user if not specified
         * @param defaultPassword Default database password if not specified
         * @param defaultMaxPoolSize Default max pool size if not specified
         * @return A fully configured DatabaseConfig instance
         */
        fun fromEnvironment(
            defaultUrl: String = "jdbc:postgresql://localhost:5433/mdeo",
            defaultUser: String = "mdeo",
            defaultPassword: String = "mdeo",
            defaultMaxPoolSize: Int = 10
        ): DatabaseConfig {
            return DatabaseConfig(
                url = System.getenv("DATABASE_URL") ?: defaultUrl,
                user = System.getenv("DATABASE_USER") ?: defaultUser,
                password = System.getenv("DATABASE_PASSWORD") ?: defaultPassword,
                maxPoolSize = System.getenv("DATABASE_MAX_POOL_SIZE")?.toIntOrNull() ?: defaultMaxPoolSize
            )
        }
    }
}
