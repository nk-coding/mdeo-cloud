package com.mdeo.scriptexecution.database

import com.mdeo.scriptexecution.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Factory object responsible for database connection management and schema initialization.
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var dataSource: HikariDataSource? = null
    
    /**
     * Initializes the database connection pool and creates tables.
     *
     * @param config Database configuration containing connection parameters
     */
    fun init(config: DatabaseConfig) {
        logger.info("Initializing database connection to ${config.url}")
        
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        
        dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource!!)
        
        transaction {
            SchemaUtils.create(ExecutionsTable)
        }
        
        logger.info("Database initialized successfully")
    }
    
    /**
     * Checks database connectivity.
     *
     * @return true if database is accessible, false otherwise
     */
    fun checkConnection(): Boolean {
        return try {
            transaction {
                exec("SELECT 1") { /* do nothing */ }
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
