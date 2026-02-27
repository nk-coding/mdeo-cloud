package com.mdeo.optimizerexecution.config

import com.mdeo.execution.common.config.BaseExecutionConfig
import com.mdeo.execution.common.config.DatabaseConfig
import com.mdeo.execution.common.config.ExecutionServiceConfig

data class AppConfig(
    override val serverPort: Int,
    override val database: DatabaseConfig,
    override val backendApiUrl: String,
    override val jwtIssuer: String,
    override val executionTimeoutMs: Long
) : ExecutionServiceConfig {
    companion object {
        private const val DEFAULT_PORT = 8083
        private const val DEFAULT_BACKEND_URL = "http://localhost:8080/api"
        private const val DEFAULT_JWT_ISSUER = "mdeo-platform"
        // Optimization runs can take longer than script or transformation executions
        private const val DEFAULT_TIMEOUT_MS = 600000L // 10 minutes

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
