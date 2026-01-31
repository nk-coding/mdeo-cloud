package com.mdeo.modeltransformationexecution.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Database table for model transformation executions.
 * Stores execution metadata and state information.
 */
object TransformationExecutionsTable : Table("transformation_executions") {
    /**
     * Unique identifier for the execution.
     */
    val id = uuid("id")
    
    /**
     * ID of the project this execution belongs to.
     */
    val projectId = uuid("project_id")
    
    /**
     * Path to the transformation file being executed.
     */
    val transformationPath = varchar("transformation_path", 1024)
    
    /**
     * Path to the model file being transformed.
     */
    val modelPath = varchar("model_path", 1024)
    
    /**
     * Current state of the execution.
     */
    val state = varchar("state", 32)
    
    /**
     * Progress text (optional).
     */
    val progress = text("progress").nullable()
    
    /**
     * Timestamp when the execution was created.
     */
    val createdAt = timestamp("created_at")
    
    /**
     * Timestamp when the execution started, nullable.
     */
    val startedAt = timestamp("started_at").nullable()
    
    /**
     * Timestamp when the execution completed, nullable.
     */
    val completedAt = timestamp("completed_at").nullable()
    
    /**
     * Error message if execution failed.
     */
    val error = text("error").nullable()
    
    /**
     * Log messages from the execution.
     */
    val logMessages = text("log_messages").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, projectId)
    }
}

/**
 * Database table for result files from transformation executions.
 * Each execution can produce multiple output files (though typically one model file).
 */
object TransformationResultFilesTable : Table("transformation_result_files") {
    /**
     * Unique identifier for the result file.
     */
    val id = uuid("id")
    
    /**
     * Reference to the parent execution.
     */
    val executionId = uuid("execution_id") references TransformationExecutionsTable.id
    
    /**
     * Path of the file within the execution result tree.
     */
    val filePath = varchar("file_path", 1024)
    
    /**
     * Content of the file (JSON for model data).
     */
    val content = text("content")
    
    /**
     * MIME type of the file content.
     */
    val mimeType = varchar("mime_type", 128)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, executionId)
        uniqueIndex(executionId, filePath)
    }
}

/**
 * Container object for table management operations.
 */
object TransformationTables {
    /**
     * Creates all transformation-related tables if they don't exist.
     */
    fun createTables() {
        transaction {
            SchemaUtils.create(TransformationExecutionsTable, TransformationResultFilesTable)
        }
    }
}
