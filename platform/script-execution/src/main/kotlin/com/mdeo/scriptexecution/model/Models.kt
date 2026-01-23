package com.mdeo.scriptexecution.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Request to create a new execution (from backend via service-script).
 * Matches PluginCreateExecutionRequest from common module.
 *
 * @param executionId Unique identifier for the new execution
 * @param project Project ID
 * @param filePath Path to the file being executed
 * @param data Arbitrary JSON data for the execution
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
 * @param name The display name for the execution
 */
@Serializable
data class CreateExecutionResponse(
    val name: String
)

/**
 * Response from file tree endpoint.
 * Matches ExecutionFileTreeResponse from common module.
 *
 * @param files List of file entries
 */
@Serializable
data class ExecutionFileTreeResponse(
    val files: List<FileEntry>
)

/**
 * File entry for file tree.
 * Matches FileEntry from common module.
 *
 * @param name Name of the file or directory
 * @param type Type of the entry (FILE=1, DIRECTORY=2)
 */
@Serializable
data class FileEntry(
    val name: String,
    val type: Int
)

/**
 * Response from summary endpoint.
 * Matches ExecutionSummaryResponse from common module.
 *
 * @param summary Markdown-formatted summary
 */
@Serializable
data class ExecutionSummaryResponse(
    val summary: String
)
