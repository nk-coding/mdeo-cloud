package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Any type definition.
 *
 * The base type that all other types extend. In the traversal-based
 * implementation, no methods are available since value conversions
 * cannot be implemented as pure Gremlin traversals.
 *
 * This serves as the root of the type hierarchy for all builtin types.
 *
 * @return The Any type definition for the Gremlin type registry
 */
fun createAnyType(): GremlinTypeDefinition {
    return gremlinType("builtin.any")
        .build()
}
