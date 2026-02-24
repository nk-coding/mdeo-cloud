package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the OrderedSet type definition.
 *
 * An OrderedSet is a collection that maintains insertion order and does not allow duplicates.
 * It extends the builtin.OrderedCollection type and uses set cardinality for vertex properties.
 *
 * Inherits all methods from OrderedCollection and Collection including filter, map, exists, all, etc.
 *
 * @return The OrderedSet type definition for the Gremlin type registry
 */
fun createOrderedSetType(): GremlinTypeDefinition {
    return gremlinType("builtin", "OrderedSet")
        .extends("builtin", "OrderedCollection")
        .cardinality(VertexProperty.Cardinality.set)
        .build()
}
