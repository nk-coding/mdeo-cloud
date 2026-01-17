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
