package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

/**
 * Creates the ReadonlyBag type definition.
 *
 * ReadonlyBag is a readonly collection that allows duplicates with count tracking.
 */
fun createReadonlyBagType(): TypeDefinition {
    return typeDefinition("builtin.ReadonlyBag") {
        extends("builtin.ReadonlyCollection")

        // ReadonlyBag inherits all methods from ReadonlyCollection
    }
}
