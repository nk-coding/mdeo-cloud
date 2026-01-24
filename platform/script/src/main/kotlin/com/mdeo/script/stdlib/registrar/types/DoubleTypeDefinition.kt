package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val DOUBLE_HELPER = "com/mdeo/script/stdlib/impl/primitives/DoubleHelper"

/**
 * Creates the Double type definition.
 */
fun createDoubleType(): TypeDefinition {
    return typeDefinition("builtin.double") {
        extends("builtin.any")

        staticMethod("abs") {
            overload(
                "",
                "(D)D",
                DOUBLE_HELPER
            )
        }

        staticMethod("ceiling") {
            overload(
                "",
                "(D)J",
                DOUBLE_HELPER
            )
        }

        staticMethod("floor") {
            overload(
                "",
                "(D)J",
                DOUBLE_HELPER
            )
        }

        staticMethod("log") {
            overload(
                "",
                "(D)D",
                DOUBLE_HELPER
            )
        }

        staticMethod("log10") {
            overload(
                "",
                "(D)D",
                DOUBLE_HELPER
            )
        }

        staticMethod("max") {
            overload(
                "int",
                "(DI)D",
                DOUBLE_HELPER,
                "max"
            )
            overload(
                "long",
                "(DJ)D",
                DOUBLE_HELPER,
                "max"
            )
            overload(
                "float",
                "(DF)D",
                DOUBLE_HELPER,
                "max"
            )
            overload(
                "double",
                "(DD)D",
                DOUBLE_HELPER,
                "max"
            )
        }

        staticMethod("min") {
            overload(
                "int",
                "(DI)D",
                DOUBLE_HELPER,
                "min"
            )
            overload(
                "long",
                "(DJ)D",
                DOUBLE_HELPER,
                "min"
            )
            overload(
                "float",
                "(DF)D",
                DOUBLE_HELPER,
                "min"
            )
            overload(
                "double",
                "(DD)D",
                DOUBLE_HELPER,
                "min"
            )
        }

        staticMethod("pow") {
            overload(
                "",
                "(DD)D",
                DOUBLE_HELPER
            )
        }

        staticMethod("round") {
            overload(
                "",
                "(D)J",
                DOUBLE_HELPER
            )
        }

        staticMethod("asString") {
            overload(
                "",
                "(D)Ljava/lang/String;",
                DOUBLE_HELPER
            )
        }
    }
}
