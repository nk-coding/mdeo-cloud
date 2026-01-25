package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val ANY_HELPER = "com/mdeo/script/stdlib/impl/primitives/AnyHelper"

/**
 * Creates the Any type definition.
 *
 * The Any type is the root of the type hierarchy.
 */
fun createAnyType(): TypeDefinition {
    return typeDefinition("builtin.any") {
        staticMethod("asBoolean") {
            overload(
                "",
                "(Ljava/lang/Object;)Z",
                ANY_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("asInteger") {
            overload(
                "",
                "(Ljava/lang/Object;)I",
                ANY_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.INT
            )
        }

        staticMethod("asReal") {
            overload(
                "",
                "(Ljava/lang/Object;)D",
                ANY_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.DOUBLE
            )
        }

        staticMethod("asDouble") {
            overload(
                "",
                "(Ljava/lang/Object;)D",
                ANY_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.DOUBLE
            )
        }

        staticMethod("asFloat") {
            overload(
                "",
                "(Ljava/lang/Object;)F",
                ANY_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.FLOAT
            )
        }

        staticMethod("asString") {
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
