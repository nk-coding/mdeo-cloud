package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.ast.types.BuiltinTypes
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
                isInterface = true,
                parameterTypes = listOf(BuiltinTypes.INT),
                returnType = BuiltinTypes.NULLABLE_ANY
            )
        }

        instanceMethod("first") {
            overload(
                "",
                "()Ljava/lang/Object;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.NULLABLE_ANY
            )
        }

        instanceMethod("last") {
            overload(
                "",
                "()Ljava/lang/Object;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.NULLABLE_ANY
            )
        }

        instanceMethod("indexOf") {
            overload(
                "",
                "(Ljava/lang/Object;)I",
                READONLY_ORDERED_COLLECTION,
                isInterface = true,
                parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY),
                returnType = BuiltinTypes.INT
            )
        }

        instanceMethod("invert") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/ReadonlyOrderedCollection;",
                READONLY_ORDERED_COLLECTION,
                isInterface = true,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.READONLY_ORDERED_COLLECTION
            )
        }
    }
}
