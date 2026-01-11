package com.mdeo.common.model

import kotlinx.serialization.Serializable

/**
 * File type constants matching VS Code's FileType enum.
 */
object FileType {
    const val UNKNOWN = 0
    const val FILE = 1
    const val DIRECTORY = 2
    const val SYMBOLIC_LINK = 64
}

/**
 * Represents a file system entry.
 *
 * @property name Name of the file or directory
 * @property type Type of the entry as defined in FileType constants
 */
@Serializable
data class FileEntry(
    val name: String,
    val type: Int
)

/**
 * Request options for writing a file.
 *
 * @property create Flag indicating whether to create the file if it doesn't exist
 * @property overwrite Flag indicating whether to overwrite the file if it already exists
 */
@Serializable
data class WriteFileRequest(
    val create: Boolean = false,
    val overwrite: Boolean = false
)

/**
 * Request options for deleting a file or directory.
 *
 * @property recursive Flag indicating whether to delete directories recursively
 * @property useTrash Flag indicating whether to move the file to trash instead of permanent deletion
 */
@Serializable
data class DeleteFileRequest(
    val recursive: Boolean = false,
    val useTrash: Boolean = false
)

/**
 * Request options for renaming or moving a file.
 *
 * @property overwrite Flag indicating whether to overwrite the destination if it already exists
 */
@Serializable
data class RenameFileRequest(
    val overwrite: Boolean = false
)
