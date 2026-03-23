package com.mdeo.optimizerexecution.config

import com.mdeo.execution.common.config.BaseExecutionConfig
import com.mdeo.execution.common.config.DatabaseConfig
import com.mdeo.execution.common.config.ExecutionServiceConfig

/**
 * Application configuration for the optimizer execution service.
 *
 * Extends [ExecutionServiceConfig] with multi-node optimizer settings loaded from
 * environment variables.
 *
 * @param serverPort HTTP port the service listens on.
 * @param database JDBC database connection settings.
 * @param backendApiUrl Base URL for the platform backend API.
 * @param jwtIssuer Expected JWT issuer for authentication.
 * @param executionTimeoutMs Maximum wall-clock time for a single optimization run in milliseconds.
 * @param nodeId Zero-based identifier of this node in a multi-node deployment.
 * @param peers Base URLs of peer optimizer-execution instances (excluding self).
 * @param includeSelf Whether this node should also act as a local worker.
 * @param workerThreads Number of concurrent worker threads for local evaluation.
 */
data class AppConfig(
    override val serverPort: Int,
    override val database: DatabaseConfig,
    override val backendApiUrl: String,
    override val jwtIssuer: String,
    override val executionTimeoutMs: Long,
    val nodeId: Int,
    val peers: List<String>,
    val includeSelf: Boolean,
    val workerThreads: Int
) : ExecutionServiceConfig {
    companion object {
        private const val DEFAULT_NODE_ID = 0
        private const val DEFAULT_INCLUDE_SELF = true
        private const val DEFAULT_WORKER_THREADS = 1

        /**
         * Load application configuration from environment variables.
         *
         * Multi-node settings are read from:
         * - `NODE_ID` – integer node identifier (default [DEFAULT_NODE_ID])
         * - `PEERS` – comma-separated list of peer base URLs (default empty)
         * - `INCLUDE_SELF` – whether this node evaluates locally (default [DEFAULT_INCLUDE_SELF])
         * - `WORKER_THREADS` – local worker thread count (default [DEFAULT_WORKER_THREADS])
         *
         * @return A fully populated [AppConfig].
         */
        fun load(): AppConfig {
            val baseConfig = BaseExecutionConfig.fromEnvironment()

            val nodeId = System.getenv("NODE_ID")?.toIntOrNull() ?: DEFAULT_NODE_ID
            val peers = System.getenv("PEERS")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            val includeSelf = System.getenv("INCLUDE_SELF")?.toBooleanStrictOrNull() ?: DEFAULT_INCLUDE_SELF
            val workerThreads = System.getenv("WORKER_THREADS")?.toIntOrNull() ?: DEFAULT_WORKER_THREADS

            return AppConfig(
                serverPort = baseConfig.serverPort,
                database = baseConfig.database,
                backendApiUrl = baseConfig.backendApiUrl,
                jwtIssuer = baseConfig.jwtIssuer,
                executionTimeoutMs = baseConfig.executionTimeoutMs,
                nodeId = nodeId,
                peers = peers,
                includeSelf = includeSelf,
                workerThreads = workerThreads
            )
        }
    }
}
