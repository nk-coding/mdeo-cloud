package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Boolean type definition.
 *
 * Boolean type extends any with no additional methods.
 * Boolean operations (AND, OR, NOT) are handled by BinaryOperatorCompiler
 * and not as methods on the Boolean type itself.
 *
 * @return The Boolean type definition for the Gremlin type registry
 */
fun createBooleanType(): GremlinTypeDefinition {
    return gremlinType("builtin", "boolean")
        .extends("builtin", "Any")
        .build()
}
