package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val STRING_HELPER = "com/mdeo/script/stdlib/impl/primitives/StringHelper"

/**
 * Creates the String type definition.
 */
fun createStringType(): TypeDefinition {
    return typeDefinition("builtin.string") {
        extends("builtin.any")

        staticMethod("characterAt") {
            overload(
                "",
                "(Ljava/lang/String;I)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("concat") {
            overload(
                "",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("endsWith") {
            overload(
                "",
                "(Ljava/lang/String;Ljava/lang/String;)Z",
                STRING_HELPER
            )
        }

        staticMethod("firstToLowerCase") {
            overload(
                "",
                "(Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("firstToUpperCase") {
            overload(
                "",
                "(Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("isInteger") {
            overload(
                "",
                "(Ljava/lang/String;)Z",
                STRING_HELPER
            )
        }

        staticMethod("isReal") {
            overload(
                "",
                "(Ljava/lang/String;)Z",
                STRING_HELPER
            )
        }

        staticMethod("isSubstringOf") {
            overload(
                "",
                "(Ljava/lang/String;Ljava/lang/String;)Z",
                STRING_HELPER
            )
        }

        staticMethod("length") {
            overload(
                "",
                "(Ljava/lang/String;)I",
                STRING_HELPER
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
                "",
                "(Ljava/lang/String;Ljava/lang/String;)Z",
                STRING_HELPER
            )
        }

        staticMethod("pad") {
            overload(
                "",
                "(Ljava/lang/String;ILjava/lang/String;Z)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("replace") {
            overload(
                "",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("split") {
            overload(
                "",
                "(Ljava/lang/String;Ljava/lang/String;)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                STRING_HELPER
            )
        }

        staticMethod("startsWith") {
            overload(
                "",
                "(Ljava/lang/String;Ljava/lang/String;)Z",
                STRING_HELPER
            )
        }

        staticMethod("substring") {
            overload(
                "1",
                "(Ljava/lang/String;I)Ljava/lang/String;",
                STRING_HELPER
            )
            overload(
                "2",
                "(Ljava/lang/String;II)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("toCharSequence") {
            overload(
                "",
                "(Ljava/lang/String;)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                STRING_HELPER
            )
        }

        staticMethod("toLowerCase") {
            overload(
                "",
                "(Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("toUpperCase") {
            overload(
                "",
                "(Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("trim") {
            overload(
                "",
                "(Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }

        staticMethod("asInteger") {
            overload(
                "",
                "(Ljava/lang/String;)I",
                STRING_HELPER
            )
        }

        staticMethod("asDouble") {
            overload(
                "",
                "(Ljava/lang/String;)D",
                STRING_HELPER
            )
        }

        staticMethod("asString") {
            overload(
                "",
                "(Ljava/lang/String;)Ljava/lang/String;",
                STRING_HELPER
            )
        }
    }
}
