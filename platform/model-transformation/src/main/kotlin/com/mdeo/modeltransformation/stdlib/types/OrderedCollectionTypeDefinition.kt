package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Creates the OrderedCollection type definition.
 *
 * An OrderedCollection extends Collection with ordered-specific operations.
 * Elements maintain their insertion order when stored and retrieved.
 *
 * Uses list cardinality for vertex properties to maintain order.
 * Inherits all methods from Collection including filter, map, exists, all, etc.
 *
 * @return The OrderedCollection type definition for the Gremlin type registry
 */
@Suppress("UNCHECKED_CAST")
fun createOrderedCollectionType(): GremlinTypeDefinition {
    return gremlinType("builtin.OrderedCollection")
        .extends("builtin.Collection")
        .cardinality(VertexProperty.Cardinality.list)
        .build()
}
