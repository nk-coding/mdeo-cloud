package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val COLLECTION = "com/mdeo/script/stdlib/impl/collections/Collection"

/**
 * Creates the Collection type definition.
 *
 * Collection extends ReadonlyCollection and adds mutation operations.
 */
fun createCollectionType(): TypeDefinition {
    return typeDefinition("builtin.Collection") {
        extends("builtin.ReadonlyCollection")

        instanceMethod("add") {
            overload(
                "",
                "(Ljava/lang/Object;)Z",
                COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?")
            )
        }

        instanceMethod("clear") {
            overload(
                "",
                "()V",
                COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("remove") {
            overload(
                "",
                "(Ljava/lang/Object;)Z",
                COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?")
            )
        }

        instanceMethod("addAll") {
            overload(
                "",
                "(Lcom/mdeo/script/stdlib/impl/collections/Collection;)Z",
                COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.Collection")
            )
        }

        instanceMethod("removeAll") {
            overload(
                "",
                "(Lcom/mdeo/script/stdlib/impl/collections/Collection;)Z",
                COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.Collection")
            )
        }
    }
}
