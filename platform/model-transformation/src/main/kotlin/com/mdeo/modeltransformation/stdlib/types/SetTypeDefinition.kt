package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Set type definition.
 *
 * Extends ReadonlyCollection.
 */
fun createSetType(): GremlinTypeDefinition {
    return gremlinType("builtin.Set")
        .extends("builtin.ReadonlyCollection")
        .cardinality(VertexProperty.Cardinality.set)
        .build()
}
