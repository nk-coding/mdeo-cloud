package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

/**
 * Creates the List type definition.
 *
 * List is a mutable ordered collection that allows duplicates.
 */
fun createListType(): TypeDefinition {
    return typeDefinition("builtin.List") {
        extends("builtin.OrderedCollection")

        // List inherits all methods from OrderedCollection
    }
}
