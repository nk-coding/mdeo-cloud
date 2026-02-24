package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Set type definition.
 *
 * A Set is an unordered collection that does not allow duplicate elements.
 * It extends the builtin.Collection type and uses set cardinality for vertex properties.
 *
 * Inherits all methods from Collection including filter, map, exists, all, etc.
 *
 * @return The Set type definition for the Gremlin type registry
 */
fun createSetType(): GremlinTypeDefinition {
    return gremlinType("builtin", "Set")
        .extends("builtin", "Collection")
        .cardinality(VertexProperty.Cardinality.set)
        .build()
}
