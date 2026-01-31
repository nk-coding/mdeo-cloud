package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the List type definition.
 *
 * Extends ReadonlyOrderedCollection.
 */
fun createListType(): GremlinTypeDefinition {
    return gremlinType("collection.list")
        .extends("collection.readonly-ordered")
        .build()
}
