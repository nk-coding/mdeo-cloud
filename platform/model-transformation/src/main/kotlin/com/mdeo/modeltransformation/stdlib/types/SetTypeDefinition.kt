package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Set type definition.
 *
 * Extends ReadonlyCollection.
 */
fun createSetType(): GremlinTypeDefinition {
    return gremlinType("collection.set")
        .extends("collection.readonly")
        .build()
}
