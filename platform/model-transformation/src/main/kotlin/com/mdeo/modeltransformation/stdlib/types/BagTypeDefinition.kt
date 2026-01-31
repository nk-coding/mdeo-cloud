package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType

/**
 * Creates the Bag type definition.
 *
 * Extends ReadonlyCollection. A bag allows duplicate elements.
 */
fun createBagType(): GremlinTypeDefinition {
    return gremlinType("collection.bag")
        .extends("collection.readonly")
        .build()
}
