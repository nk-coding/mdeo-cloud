package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Boolean type definition.
 *
 * Boolean type extends any with no additional methods.
 * Boolean operations are handled by BinaryOperatorCompiler.
 */
fun createBooleanType(): GremlinTypeDefinition {
    return gremlinType("builtin.boolean")
        .extends("builtin.any")
        .build()
}
