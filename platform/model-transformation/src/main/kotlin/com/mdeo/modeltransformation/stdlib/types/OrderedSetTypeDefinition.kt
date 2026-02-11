package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the OrderedSet type definition.
 *
 * Extends both ReadonlyOrderedCollection and Set.
 */
fun createOrderedSetType(): GremlinTypeDefinition {
    return gremlinType("builtin.OrderedSet")
        .extends("builtin.ReadonlyOrderedCollection")
        .cardinality(VertexProperty.Cardinality.set)
        // Note: Also conceptually extends Set
        .build()
}
