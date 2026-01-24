package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val READONLY_ORDERED_COLLECTION = "com/mdeo/script/stdlib/impl/collections/ReadonlyOrderedCollection"

/**
 * Creates the ReadonlyOrderedCollection type definition.
 *
 * ReadonlyOrderedCollection extends ReadonlyCollection and adds index-based access.
 */
fun createReadonlyOrderedCollectionType(): TypeDefinition {
    return typeDefinition("builtin.ReadonlyOrderedCollection") {
        extends("builtin.ReadonlyCollection")

        instanceMethod("at") {
            overload(
                "",
                "(I)Ljava/lang/Object;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("first") {
            overload(
                "",
                "()Ljava/lang/Object;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("last") {
            overload(
                "",
                "()Ljava/lang/Object;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("indexOf") {
            overload(
                "",
                "(Ljava/lang/Object;)I",
                READONLY_ORDERED_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?")
            )
        }

        instanceMethod("invert") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/ReadonlyOrderedCollection;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true
            )
        }
    }
}
