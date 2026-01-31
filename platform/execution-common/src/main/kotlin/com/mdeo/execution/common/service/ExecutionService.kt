package com.mdeo.execution.common.service

import com.mdeo.execution.common.routes.FileEntry
import kotlinx.serialization.json.JsonElement
import java.util.*

/**
 * Interface defining the core operations for execution services.
 * Implementations provide specific execution logic for different file types.
 */
interface ExecutionService {
    /**
     * Creates and starts a new execution.
     *
     * @param executionId Unique identifier for the execution
     * @param projectId Project containing the file
     * @param filePath Path to the file to execute
     * @param data Execution configuration data
     * @param jwtToken JWT token for backend API authentication
     * @return The display name of the created execution
     */
    suspend fun createAndStartExecution(
        executionId: UUID,
        projectId: UUID,
        filePath: String,
        data: JsonElement,
        jwtToken: String
    ): String

    /**
     * Cancels a running execution.
     *
     * @param executionId Unique identifier of the execution to cancel
     */
    suspend fun cancelExecution(executionId: UUID)

    /**
     * Deletes an execution and its associated data.
     *
     * @param executionId Unique identifier of the execution to delete
     */
    suspend fun deleteExecution(executionId: UUID)

    /**
     * Gets a summary of the execution results.
     *
     * @param executionId Unique identifier of the execution
     * @return Markdown-formatted summary, or null if not found
     */
    suspend fun getSummary(executionId: UUID): String?
}

/**
 * Extended execution service interface that includes file tree support.
 * Used by services that produce file-based output (like transformations).
 */
interface ExecutionServiceWithFileTree : ExecutionService {
    /**
     * Gets the file tree of execution results.
     *
     * @param executionId Unique identifier of the execution
     * @param path Optional path prefix to filter results
     * @return List of file entries, or null if not found
     */
    suspend fun getFileTree(executionId: UUID, path: String?): List<FileEntry>?

    /**
     * Gets the contents of a specific file from execution results.
     *
     * @param executionId Unique identifier of the execution
     * @param filePath Path to the file within execution results
     * @return File contents, or null if not found
     */
    suspend fun getFileContents(executionId: UUID, filePath: String): String?
}
