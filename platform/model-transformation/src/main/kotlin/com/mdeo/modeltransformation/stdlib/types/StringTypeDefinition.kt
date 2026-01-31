package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the String type definition.
 *
 * String type has minimal methods since string operations cannot be
 * implemented as pure Gremlin traversals. All string manipulation
 * requires Kotlin String methods which aren't portable.
 */
fun createStringType(): GremlinTypeDefinition {
    return gremlinType("builtin.string")
        .extends("builtin.any")
        // Note: String methods removed as they require Kotlin lambdas
        // Methods like toLowerCase, toUpperCase, trim, substring, etc.
        // cannot be implemented as pure Gremlin traversals
        .build()
}
