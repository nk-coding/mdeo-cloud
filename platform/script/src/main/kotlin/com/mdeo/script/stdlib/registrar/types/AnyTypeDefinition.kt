package com.mdeo.script.stdlib.registrar.types

import com.mdeo.expression.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val ANY_HELPER = "com/mdeo/script/stdlib/impl/primitives/AnyHelper"

/**
 * Creates the Any type definition.
 *
 * The Any type is the root of the type hierarchy.
 */
fun createAnyType(): TypeDefinition {
    return typeDefinition("builtin", "Any") {
        jvmClass("java/lang/Object")
        staticMethod("toString") {
            overload(
                "",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                ANY_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("format") {
            overload(
                "",
                "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",
                ANY_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("hasProperty") {
            overload(
                "",
                "(Ljava/lang/Object;Ljava/lang/String;)Z",
                ANY_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.BOOLEAN
            )
        }
    }
}
