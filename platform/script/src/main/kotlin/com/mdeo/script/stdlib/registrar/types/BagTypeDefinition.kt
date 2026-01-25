package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

/**
 * Creates the Bag type definition.
 *
 * Bag is a mutable collection that allows duplicates with count tracking.
 */
fun createBagType(): TypeDefinition {
    return typeDefinition("builtin.Bag") {
        extends("builtin.ReadonlyBag")
        extends("builtin.Collection")
    }
}
