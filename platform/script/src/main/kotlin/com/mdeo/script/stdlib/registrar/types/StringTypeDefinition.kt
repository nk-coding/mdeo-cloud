package com.mdeo.script.stdlib.registrar.types

import com.mdeo.expression.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val STRING_HELPER = "com/mdeo/script/stdlib/impl/primitives/StringHelper"

/**
 * Creates the String type definition.
 */
fun createStringType(): TypeDefinition {
    return typeDefinition("builtin.string") {
        extends("builtin.any")
        jvmClass("java/lang/String")

        staticMethod("characterAt") {
            overload(
                "", "(Ljava/lang/String;I)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.INT),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("concat") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("endsWith") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;)Z", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("firstToLowerCase") {
            overload(
                "", "(Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("firstToUpperCase") {
            overload(
                "", "(Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("isInteger") {
            overload(
                "", "(Ljava/lang/String;)Z", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("isReal") {
            overload(
                "", "(Ljava/lang/String;)Z", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("isSubstringOf") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;)Z", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("length") {
            overload(
                "", "(Ljava/lang/String;)I", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.INT
            )
        }

        staticProperty("length") {
            returns("I")
            owner(STRING_HELPER)
            getter("length")
            receiver("Ljava/lang/String;")
        }

        staticMethod("matches") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;)Z", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("pad") {
            overload(
                "", "(Ljava/lang/String;ILjava/lang/String;Z)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.STRING, BuiltinTypes.BOOLEAN),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("replace") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING, BuiltinTypes.STRING),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("split") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.LIST
            )
        }

        staticMethod("startsWith") {
            overload(
                "", "(Ljava/lang/String;Ljava/lang/String;)Z", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.STRING),
                returnType = BuiltinTypes.BOOLEAN
            )
        }

        staticMethod("substring") {
            overload(
                "1", "(Ljava/lang/String;I)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.INT),
                returnType = BuiltinTypes.STRING
            )
            overload(
                "2", "(Ljava/lang/String;II)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.INT),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("toCharSequence") {
            overload(
                "", "(Ljava/lang/String;)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.LIST
            )
        }

        staticMethod("toLowerCase") {
            overload(
                "", "(Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("toUpperCase") {
            overload(
                "", "(Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("trim") {
            overload(
                "", "(Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }

        staticMethod("asInteger") {
            overload(
                "", "(Ljava/lang/String;)I", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.INT
            )
        }

        staticMethod("asDouble") {
            overload(
                "", "(Ljava/lang/String;)D", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.DOUBLE
            )
        }

        staticMethod("toString") {
            overload(
                "", "(Ljava/lang/String;)Ljava/lang/String;", STRING_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }
    }
}
