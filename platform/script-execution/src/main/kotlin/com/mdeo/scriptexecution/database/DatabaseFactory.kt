package com.mdeo.scriptexecution.database

import com.mdeo.execution.common.config.DatabaseConfig
import com.mdeo.execution.common.database.DatabaseFactory as CommonDatabaseFactory
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Factory object responsible for database connection management and schema initialization.
 * Delegates core functionality to execution-common's DatabaseFactory while adding
 * script-execution specific schema initialization.
 */
object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private val commonFactory = CommonDatabaseFactory()
    
    /**
     * Initializes the database connection pool and creates tables.
     *
     * @param config Database configuration containing connection parameters
     */
    fun init(config: DatabaseConfig) {
        logger.info("Initializing script-execution database")
        
        // Use execution-common's database factory for connection management
        commonFactory.init(config)
        
        // Create script-execution specific tables
        transaction {
            SchemaUtils.create(ExecutionsTable)
        }
        
        logger.info("Script-execution database initialized successfully")
    }
    
    /**
     * Checks database connectivity.
     *
     * @return true if database is accessible, false otherwise
     */
    fun checkConnection(): Boolean = commonFactory.checkConnection()
    
    /**
     * Closes the database connection pool.
     */
    fun close() = commonFactory.close()
}
