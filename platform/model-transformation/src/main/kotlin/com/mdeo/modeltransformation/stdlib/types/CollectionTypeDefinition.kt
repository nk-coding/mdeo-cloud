package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Collection type definition.
 *
 * Extends ReadonlyCollection - the mutable collection type.
 */
fun createCollectionType(): GremlinTypeDefinition {
    return gremlinType("collection.collection")
        .extends("collection.readonly")
        .build()
}
