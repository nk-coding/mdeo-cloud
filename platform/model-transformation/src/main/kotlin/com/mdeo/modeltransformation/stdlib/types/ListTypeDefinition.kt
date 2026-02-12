package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the List type definition.
 *
 * A List is an ordered collection that allows duplicate elements and maintains insertion order.
 * It extends the builtin.OrderedCollection type and uses list cardinality for vertex properties.
 *
 * Inherits all methods from OrderedCollection and Collection including filter, map, exists, all, etc.
 *
 * @return The List type definition for the Gremlin type registry
 */
fun createListType(): GremlinTypeDefinition {
    return gremlinType("builtin.List")
        .extends("builtin.OrderedCollection")
        .cardinality(VertexProperty.Cardinality.list)
        .build()
}
