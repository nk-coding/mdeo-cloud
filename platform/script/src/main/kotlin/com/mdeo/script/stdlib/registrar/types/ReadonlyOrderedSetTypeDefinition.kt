package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

/**
 * Creates the ReadonlyOrderedSet type definition.
 *
 * ReadonlyOrderedSet is a readonly ordered collection that does not allow duplicates.
 */
fun createReadonlyOrderedSetType(): TypeDefinition {
    return typeDefinition("builtin", "ReadonlyOrderedSet") {
        extends("builtin", "ReadonlyOrderedCollection")
    }
}
