package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the OrderedSet type definition.
 *
 * Extends both ReadonlyOrderedCollection and Set.
 */
fun createOrderedSetType(): GremlinTypeDefinition {
    return gremlinType("collection.ordered-set")
        .extends("collection.readonly-ordered")
        // Note: Also conceptually extends Set
        .build()
}
