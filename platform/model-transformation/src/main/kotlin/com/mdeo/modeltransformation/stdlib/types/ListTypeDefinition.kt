package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the List type definition.
 *
 * Extends ReadonlyOrderedCollection.
 */
fun createListType(): GremlinTypeDefinition {
    return gremlinType("builtin.List")
        .extends("builtin.ReadonlyOrderedCollection")
        .cardinality(VertexProperty.Cardinality.list)
        .build()
}
