package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Bag type definition.
 *
 * Extends ReadonlyCollection. A bag allows duplicate elements.
 */
fun createBagType(): GremlinTypeDefinition {
    return gremlinType("builtin.Bag")
        .extends("builtin.Collection")
        .cardinality(VertexProperty.Cardinality.list)
        .build()
}
