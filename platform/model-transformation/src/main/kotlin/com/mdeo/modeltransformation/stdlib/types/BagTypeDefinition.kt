package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Bag type definition.
 *
 * A Bag is an unordered collection that allows duplicate elements (also known as a multiset).
 * It extends the builtin.Collection type and uses list cardinality for vertex properties
 * to support duplicates.
 *
 * Inherits all methods from Collection including filter, map, exists, all, etc.
 *
 * @return The Bag type definition for the Gremlin type registry
 */
fun createBagType(): GremlinTypeDefinition {
    return gremlinType("builtin", "Bag")
        .extends("builtin", "Collection")
        .cardinality(VertexProperty.Cardinality.list)
        .build()
}
