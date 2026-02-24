package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the String type definition.
 *
 * String type has minimal methods since string operations cannot be
 * implemented as pure Gremlin traversals. All string manipulation
 * requires Kotlin String methods which aren't portable.
 *
 * The String type extends the builtin.any base type but provides no
 * additional methods due to Gremlin traversal limitations.
 *
 * @return The String type definition for the Gremlin type registry
 */
fun createStringType(): GremlinTypeDefinition {
    return gremlinType("builtin", "string")
        .extends("builtin", "any")
        .build()
}
