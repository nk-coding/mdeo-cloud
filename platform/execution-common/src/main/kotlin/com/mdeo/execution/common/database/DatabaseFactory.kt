package com.mdeo.execution.common.database

import com.mdeo.execution.common.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Factory for creating and managing database connections.
 * Provides connection pooling using HikariCP and schema initialization support.
 */
class DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var dataSource: HikariDataSource? = null

    /**
     * Initializes the database connection pool.
     *
     * @param config Database configuration containing connection parameters
     */
    fun init(config: DatabaseConfig) {
        logger.info("Initializing database connection to ${config.url}")

        val hikariConfig = createHikariConfig(config)
        dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource!!)

        logger.info("Database initialized successfully")
    }

    /**
     * Creates HikariCP configuration from database config.
     */
    private fun createHikariConfig(config: DatabaseConfig): HikariConfig {
        return HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
    }

    /**
     * Checks database connectivity.
     *
     * @return true if database is accessible, false otherwise
     */
    fun checkConnection(): Boolean {
        return try {
            transaction {
                exec("SELECT 1") { }
            }
            true
        } catch (e: Exception) {
            logger.error("Database health check failed", e)
            false
        }
    }

    /**
     * Closes the database connection pool.
     */
    fun close() {
        dataSource?.close()
        logger.info("Database connection closed")
    }
}
