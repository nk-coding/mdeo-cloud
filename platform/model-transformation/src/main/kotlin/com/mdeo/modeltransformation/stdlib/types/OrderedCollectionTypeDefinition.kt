package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Creates the OrderedCollection type definition.
 *
 * Extends Collection with ordered-specific operations.
 *
 */
@Suppress("UNCHECKED_CAST")
fun createOrderedCollectionType(): GremlinTypeDefinition {
    return gremlinType("builtin.OrderedCollection")
        .extends("builtin.Collection")
        .cardinality(VertexProperty.Cardinality.list)
        .build()
}
