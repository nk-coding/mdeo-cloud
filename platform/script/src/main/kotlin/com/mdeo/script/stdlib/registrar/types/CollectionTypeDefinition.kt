package com.mdeo.script.stdlib.registrar.types

import com.mdeo.expression.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val COLLECTION = "com/mdeo/script/stdlib/impl/collections/Collection"

/**
 * Creates the Collection type definition.
 *
 * Collection extends ReadonlyCollection and adds mutation operations.
 */
fun createCollectionType(): TypeDefinition {
    return typeDefinition("builtin", "Collection") {
        extends("builtin", "ReadonlyCollection")

        instanceMethod("add") {
            overload(
                "", "(Ljava/lang/Object;)Z", COLLECTION,
                isInterface = true,
                parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        instanceMethod("clear") {
            overload(
                "", "()V", COLLECTION,
                isInterface = true,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.VOID
            )
        }

        instanceMethod("remove") {
            overload(
                "", "(Ljava/lang/Object;)Z", COLLECTION,
                isInterface = true,
                parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        instanceMethod("addAll") {
            overload(
                "", "(Lcom/mdeo/script/stdlib/impl/collections/Collection;)Z", COLLECTION,
                isInterface = true,
                parameterTypes = listOf(BuiltinTypes.COLLECTION),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        instanceMethod("removeAll") {
            overload(
                "", "(Lcom/mdeo/script/stdlib/impl/collections/Collection;)Z", COLLECTION,
                isInterface = true,
                parameterTypes = listOf(BuiltinTypes.COLLECTION),
                returnType = BuiltinTypes.BOOLEAN
            )
        }
    }
}
