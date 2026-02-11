package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Creates the ReadonlyOrderedCollection type definition.
 *
 * Extends ReadonlyCollection with ordered-specific operations.
 *
 * Methods that can be pure traversals:
 * - `first()` - already in ReadonlyCollection via limit(1)
 * - `last()` - already in ReadonlyCollection via tail(1)
 */
@Suppress("UNCHECKED_CAST")
fun createReadonlyOrderedCollectionType(): GremlinTypeDefinition {
    return gremlinType("builtin.ReadonlyOrderedCollection")
        .extends("builtin.ReadonlyCollection")
        // first() and last() already inherited from ReadonlyCollection
        .build()
}
