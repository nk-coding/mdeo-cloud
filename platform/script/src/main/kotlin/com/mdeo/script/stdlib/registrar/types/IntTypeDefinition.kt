package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val INT_HELPER = "com/mdeo/script/stdlib/impl/primitives/IntHelper"

/**
 * Creates the Int type definition.
 */
fun createIntType(): TypeDefinition {
    return typeDefinition("builtin.int") {
        extends("builtin.any")

        staticMethod("abs") {
            overload(
                "",
                "(I)I",
                INT_HELPER
            )
        }

        staticMethod("ceiling") {
            overload(
                "",
                "(I)I",
                INT_HELPER
            )
        }

        staticMethod("floor") {
            overload(
                "",
                "(I)I",
                INT_HELPER
            )
        }

        staticMethod("log") {
            overload(
                "",
                "(I)F",
                INT_HELPER
            )
        }

        staticMethod("log10") {
            overload(
                "",
                "(I)F",
                INT_HELPER
            )
        }

        staticMethod("max") {
            overload(
                "int",
                "(II)I",
                INT_HELPER,
                "max"
            )
            overload(
                "long",
                "(IJ)J",
                INT_HELPER,
                "max"
            )
            overload(
                "float",
                "(IF)F",
                INT_HELPER,
                "max"
            )
            overload(
                "double",
                "(ID)D",
                INT_HELPER,
                "max"
            )
        }

        staticMethod("min") {
            overload(
                "int",
                "(II)I",
                INT_HELPER,
                "min"
            )
            overload(
                "long",
                "(IJ)J",
                INT_HELPER,
                "min"
            )
            overload(
                "float",
                "(IF)F",
                INT_HELPER,
                "min"
            )
            overload(
                "double",
                "(ID)D",
                INT_HELPER,
                "min"
            )
        }

        staticMethod("pow") {
            overload(
                "",
                "(ID)D",
                INT_HELPER
            )
        }

        staticMethod("round") {
            overload(
                "",
                "(I)I",
                INT_HELPER
            )
        }

        staticMethod("iota") {
            overload(
                "",
                "(III)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                INT_HELPER
            )
        }

        staticMethod("mod") {
            overload(
                "",
                "(II)I",
                INT_HELPER
            )
        }

        staticMethod("to") {
            overload(
                "",
                "(II)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                INT_HELPER
            )
        }

        staticMethod("toBinary") {
            overload(
                "",
                "(I)Ljava/lang/String;",
                INT_HELPER
            )
        }

        staticMethod("toHex") {
            overload(
                "",
                "(I)Ljava/lang/String;",
                INT_HELPER
            )
        }

        staticMethod("asString") {
            overload(
                "",
                "(I)Ljava/lang/String;",
                INT_HELPER
            )
        }

        staticMethod("asDouble") {
            overload(
                "",
                "(I)D",
                INT_HELPER
            )
        }

        staticMethod("asFloat") {
            overload(
                "",
                "(I)F",
                INT_HELPER
            )
        }
    }
}
