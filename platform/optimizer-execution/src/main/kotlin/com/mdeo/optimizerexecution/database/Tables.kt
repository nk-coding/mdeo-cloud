package com.mdeo.optimizerexecution.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed table mapping for optimizer execution records.
 */
object OptimizerExecutionsTable : Table("optimizer_executions") {
    /**
     * Unique execution identifier (UUID). 
     */
    val id = uuid("id")
    /**
     * Identifier of the project that owns this execution. 
     */
    val projectId = uuid("project_id")
    /**
     * Path to the optimizer config file that triggered this execution. 
     */
    val configPath = varchar("config_path", 1024)
    /**
     * Current lifecycle state (e.g. SUBMITTED, RUNNING, COMPLETED, FAILED). 
     */
    val state = varchar("state", 32)
    /**
     * Human-readable progress message, or null when not yet started. 
     */
    val progress = text("progress").nullable()
    /**
     * Timestamp when the execution record was created. 
     */
    val createdAt = timestamp("created_at")
    /**
     * Timestamp when the execution transitioned to INITIALIZING or RUNNING, or null. 
     */
    val startedAt = timestamp("started_at").nullable()
    /**
     * Timestamp when the execution reached a terminal state, or null. 
     */
    val completedAt = timestamp("completed_at").nullable()
    /**
     * Error message from the last failure, or null when not failed. 
     */
    val error = text("error").nullable()
    /**
     * Raw JSON configuration data stored for debugging purposes, or null. 
     */
    val configData = text("config_data").nullable()
    override val primaryKey = PrimaryKey(id)
    init { index(false, projectId) }
}

/**
 * Exposed table mapping for result files produced by optimizer executions.
 */
object OptimizerResultFilesTable : Table("optimizer_result_files") {
    /**
     * Unique result-file identifier (UUID). 
     */
    val id = uuid("id")
    /**
     * Foreign key to the owning [OptimizerExecutionsTable] record. 
     */
    val executionId = uuid("execution_id") references OptimizerExecutionsTable.id
    /**
     * Relative path of this result file within the execution's output tree. 
     */
    val filePath = varchar("file_path", 1024)
    /**
     * Raw text content of the result file. 
     */
    val content = text("content")
    /**
     * MIME type of the result file (e.g. `application/json`, `text/markdown`). 
     */
    val mimeType = varchar("mime_type", 128)
    override val primaryKey = PrimaryKey(id)
    init {
        index(false, executionId)
        uniqueIndex(executionId, filePath)
    }
}

/**
 * Utility object for creating all optimizer-related database tables.
 */
object OptimizerTables {
    /**
     * Creates [OptimizerExecutionsTable] and [OptimizerResultFilesTable] if they do not exist.
     */
    fun createTables() {
        transaction {
            SchemaUtils.create(OptimizerExecutionsTable, OptimizerResultFilesTable)
        }
    }
}
