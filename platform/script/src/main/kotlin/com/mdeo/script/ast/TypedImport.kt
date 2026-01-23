package com.mdeo.script.ast

import kotlinx.serialization.Serializable

/**
 * Import declaration.
 */
@Serializable
data class TypedImport(
    /**
     * Name under which it is registered in the global scope.
     */
    val name: String,
    /**
     * The name of the function in its source file.
     */
    val ref: String,
    /**
     * URI from where it is imported.
     */
    val uri: String
)
