package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

/**
 * Creates the Set type definition.
 *
 * Set is a mutable collection that does not allow duplicates.
 */
fun createSetType(): TypeDefinition {
    return typeDefinition("builtin", "Set") {
        jvmClass("java/util/Set")
        extends("builtin", "Collection")
    }
}
