package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

/**
 * Creates the OrderedSet type definition.
 *
 * OrderedSet is a mutable ordered collection that maintains insertion order
 * and does not allow duplicates.
 */
fun createOrderedSetType(): TypeDefinition {
    return typeDefinition("builtin.OrderedSet") {
        extends("builtin.ReadonlyOrderedSet")
        extends("builtin.OrderedCollection")

        // OrderedSet inherits all methods from ReadonlyOrderedSet and OrderedCollection
    }
}
