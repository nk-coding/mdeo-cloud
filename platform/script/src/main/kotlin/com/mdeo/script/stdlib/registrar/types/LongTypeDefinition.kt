package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val LONG_HELPER = "com/mdeo/script/stdlib/impl/primitives/LongHelper"

/**
 * Creates the Long type definition.
 */
fun createLongType(): TypeDefinition {
    return typeDefinition("builtin.long") {
        extends("builtin.any")

        staticMethod("abs") {
            overload(
                "",
                "(J)J",
                LONG_HELPER
            )
        }

        staticMethod("ceiling") {
            overload(
                "",
                "(J)J",
                LONG_HELPER
            )
        }

        staticMethod("floor") {
            overload(
                "",
                "(J)J",
                LONG_HELPER
            )
        }

        staticMethod("log") {
            overload(
                "",
                "(J)D",
                LONG_HELPER
            )
        }

        staticMethod("log10") {
            overload(
                "",
                "(J)D",
                LONG_HELPER
            )
        }

        staticMethod("max") {
            overload(
                "int",
                "(JI)J",
                LONG_HELPER,
                "max"
            )
            overload(
                "long",
                "(JJ)J",
                LONG_HELPER,
                "max"
            )
            overload(
                "float",
                "(JF)F",
                LONG_HELPER,
                "max"
            )
            overload(
                "double",
                "(JD)D",
                LONG_HELPER,
                "max"
            )
        }

        staticMethod("min") {
            overload(
                "int",
                "(JI)J",
                LONG_HELPER,
                "min"
            )
            overload(
                "long",
                "(JJ)J",
                LONG_HELPER,
                "min"
            )
            overload(
                "float",
                "(JF)F",
                LONG_HELPER,
                "min"
            )
            overload(
                "double",
                "(JD)D",
                LONG_HELPER,
                "min"
            )
        }

        staticMethod("pow") {
            overload(
                "",
                "(JD)D",
                LONG_HELPER
            )
        }

        staticMethod("round") {
            overload(
                "",
                "(J)J",
                LONG_HELPER
            )
        }

        staticMethod("iota") {
            overload(
                "",
                "(JJJ)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                LONG_HELPER
            )
        }

        staticMethod("mod") {
            overload(
                "",
                "(JJ)J",
                LONG_HELPER
            )
        }

        staticMethod("to") {
            overload(
                "",
                "(JJ)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                LONG_HELPER
            )
        }

        staticMethod("toBinary") {
            overload(
                "",
                "(J)Ljava/lang/String;",
                LONG_HELPER
            )
        }

        staticMethod("toHex") {
            overload(
                "",
                "(J)Ljava/lang/String;",
                LONG_HELPER
            )
        }

        staticMethod("asString") {
            overload(
                "",
                "(J)Ljava/lang/String;",
                LONG_HELPER
            )
        }

        staticMethod("asDouble") {
            overload(
                "",
                "(J)D",
                LONG_HELPER
            )
        }
    }
}
