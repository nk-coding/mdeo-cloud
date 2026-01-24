package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val ORDERED_COLLECTION = "com/mdeo/script/stdlib/impl/collections/OrderedCollection"

/**
 * Creates the OrderedCollection type definition.
 *
 * OrderedCollection extends ReadonlyOrderedCollection and adds mutation operations.
 */
fun createOrderedCollectionType(): TypeDefinition {
    return typeDefinition("builtin.OrderedCollection") {
        extends("builtin.ReadonlyOrderedCollection")
        extends("builtin.Collection")

        instanceMethod("removeAt") {
            overload(
                "",
                "(I)Ljava/lang/Object;",
                ORDERED_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("sortBy") {
            overload(
                "",
                "(Lcom/mdeo/script/api/Function;)Lcom/mdeo/script/stdlib/impl/collections/OrderedCollection;",
                ORDERED_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.function")
            )
        }
    }
}
