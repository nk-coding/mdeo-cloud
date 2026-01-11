package com.mdeo.backend.config

import java.util.concurrent.TimeUnit

/**
 * Main application configuration containing all configuration sections.
 *
 * @property serverPort The port number on which the server will listen
 * @property database Database connection configuration
 * @property session Session management configuration
 * @property cors Cross-Origin Resource Sharing configuration
 * @property defaultAdmin Default administrator account configuration
 * @property plugin Plugin system configuration
 */
data class AppConfig(
    val serverPort: Int,
    val database: DatabaseConfig,
    val session: SessionConfig,
    val cors: CorsConfig,
    val defaultAdmin: DefaultAdminConfig,
    val plugin: PluginConfig
) {
    companion object {
        /**
         * Loads application configuration from environment variables with fallback defaults.
         *
         * @return A fully configured AppConfig instance
         */
        fun load(): AppConfig {
            val environment = System.getenv("ENVIRONMENT") ?: "development"
            val isProduction = environment.equals("production", ignoreCase = true)
            
            return AppConfig(
                serverPort = System.getenv("SERVER_PORT")?.toIntOrNull() ?: 8080,
                database = DatabaseConfig(
                    url = System.getenv("DATABASE_URL") 
                        ?: "jdbc:postgresql://localhost:5432/mdeo",
                    user = System.getenv("DATABASE_USER") ?: "mdeo",
                    password = System.getenv("DATABASE_PASSWORD") ?: "mdeo",
                    maxPoolSize = System.getenv("DATABASE_MAX_POOL_SIZE")?.toIntOrNull() ?: 10
                ),
                session = SessionConfig(
                    maxIdleSeconds = System.getenv("SESSION_MAX_IDLE_SECONDS")?.toLongOrNull()
                        ?: TimeUnit.DAYS.toSeconds(7),
                    cookieSecure = System.getenv("COOKIE_SECURE")?.toBoolean() ?: true,
                    encryptionKey = System.getenv("SESSION_ENCRYPTION_KEY") 
                        ?: "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff",
                    sameSite = System.getenv("COOKIE_SAMESITE") ?: "Strict"
                ),
                cors = CorsConfig(
                    allowedHosts = run {
                        val corsHosts = System.getenv("CORS_ALLOWED_HOSTS")
                            ?.split(",")
                            ?.map { it.trim() }
                            ?: listOf("localhost:4242", "localhost:5173", "127.0.0.1:4242", "127.0.0.1:5173")
                        
                        // Validate CORS configuration in production
                        if (isProduction) {
                            val hasLocalhostOrigins = corsHosts.any { host ->
                                host.contains("localhost") || host.contains("127.0.0.1")
                            }
                            require(!hasLocalhostOrigins) {
                                "Production environment detected but CORS configuration contains localhost origins. " +
                                "Please set CORS_ALLOWED_HOSTS environment variable with production domains only."
                            }
                        }
                        
                        corsHosts
                    }
                ),
                defaultAdmin = DefaultAdminConfig(
                    username = System.getenv("ADMIN_USERNAME") ?: "admin",
                    password = System.getenv("ADMIN_PASSWORD") ?: "admin"
                ),
                plugin = PluginConfig(
                    cacheTtlSeconds = System.getenv("PLUGIN_CACHE_TTL_SECONDS")?.toLongOrNull()
                        ?: TimeUnit.MINUTES.toSeconds(5)
                )
            )
        }
    }
}

/**
 * Database connection configuration.
 *
 * @property url The JDBC connection URL
 * @property user The database username
 * @property password The database password
 * @property maxPoolSize Maximum number of connections in the pool
 */
data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)

/**
 * Session management configuration.
 *
 * @property maxIdleSeconds Maximum seconds a session can be idle before expiration
 * @property cookieSecure Whether to use secure cookies (HTTPS only)
 * @property encryptionKey The key used for session encryption/signing
 * @property sameSite The SameSite attribute for session cookies
 */
data class SessionConfig(
    val maxIdleSeconds: Long,
    val cookieSecure: Boolean,
    val encryptionKey: String,
    val sameSite: String
)

/**
 * Cross-Origin Resource Sharing (CORS) configuration.
 *
 * @property allowedHosts List of allowed host origins
 */
data class CorsConfig(
    val allowedHosts: List<String>
)

/**
 * Default administrator account configuration.
 *
 * @property username The default admin username
 * @property password The default admin password
 */
data class DefaultAdminConfig(
    val username: String,
    val password: String
)

/**
 * Plugin system configuration.
 *
 * @property cacheTtlSeconds Cache time-to-live in seconds for plugin data
 */
data class PluginConfig(
    val cacheTtlSeconds: Long
)
