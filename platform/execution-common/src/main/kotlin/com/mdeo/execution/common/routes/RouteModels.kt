package com.mdeo.execution.common.routes

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Request to create a new execution (from backend via service).
 * Matches PluginCreateExecutionRequest from common module.
 *
 * @property executionId Unique identifier for the new execution
 * @property project Project ID
 * @property filePath Path to the file being executed
 * @property data Arbitrary JSON data for the execution
 */
@Serializable
data class CreateExecutionRequest(
    val executionId: String,
    val project: String,
    val filePath: String,
    val data: JsonElement
)

/**
 * Response from creating an execution.
 * Matches CreateExecutionResponse from common module.
 *
 * @property name The display name for the execution
 */
@Serializable
data class CreateExecutionResponse(
    val name: String
)

/**
 * Response from file tree endpoint.
 * Provides the list of files in the execution result.
 *
 * @property files List of file entries
 */
@Serializable
data class ExecutionFileTreeResponse(
    val files: List<FileEntry>
)

/**
 * File entry for file tree responses.
 * Represents a file or directory in execution results.
 *
 * @property name Name of the file or directory
 * @property type Type of the entry (FILE=1, DIRECTORY=2)
 */
@Serializable
data class FileEntry(
    val name: String,
    val type: Int
) {
    companion object {
        const val TYPE_FILE = 1
        const val TYPE_DIRECTORY = 2
    }
}

/**
 * Response from summary endpoint.
 * Provides a markdown-formatted summary of execution results.
 *
 * @property summary Markdown-formatted summary
 */
@Serializable
data class ExecutionSummaryResponse(
    val summary: String
)
