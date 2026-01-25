package com.mdeo.scriptexecution.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import com.mdeo.common.model.ExecutionState

/**
 * Database table for script executions.
 * Stores only data needed by the script-execution service itself.
 */
object ExecutionsTable : Table("script_executions") {
    /**
     * Unique identifier for the execution.
     */
    val id = uuid("id")
    
    /**
     * ID of the project this execution belongs to.
     */
    val projectId = uuid("project_id")
    
    /**
     * Path to the source file being executed.
     */
    val filePath = varchar("file_path", 1024)
    
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
     * Arbitrary JSON data for the execution.
     */
    val data = text("data")
    
    /**
     * Execution result as text.
     */
    val result = text("result").nullable()
    
    /**
     * Captured output from the execution.
     */
    val output = text("output").nullable()
    
    /**
     * Error message if execution failed.
     */
    val error = text("error").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, projectId)
    }
}

