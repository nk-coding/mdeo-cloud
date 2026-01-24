package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val FLOAT_HELPER = "com/mdeo/script/stdlib/impl/primitives/FloatHelper"

/**
 * Creates the Float type definition.
 */
fun createFloatType(): TypeDefinition {
    return typeDefinition("builtin.float") {
        extends("builtin.any")

        staticMethod("abs") {
            overload(
                "",
                "(F)F",
                FLOAT_HELPER
            )
        }

        staticMethod("ceiling") {
            overload(
                "",
                "(F)I",
                FLOAT_HELPER
            )
        }

        staticMethod("floor") {
            overload(
                "",
                "(F)I",
                FLOAT_HELPER
            )
        }

        staticMethod("log") {
            overload(
                "",
                "(F)F",
                FLOAT_HELPER
            )
        }

        staticMethod("log10") {
            overload(
                "",
                "(F)F",
                FLOAT_HELPER
            )
        }

        staticMethod("max") {
            overload(
                "int",
                "(FI)F",
                FLOAT_HELPER,
                "max"
            )
            overload(
                "long",
                "(FJ)F",
                FLOAT_HELPER,
                "max"
            )
            overload(
                "float",
                "(FF)F",
                FLOAT_HELPER,
                "max"
            )
            overload(
                "double",
                "(FD)D",
                FLOAT_HELPER,
                "max"
            )
        }

        staticMethod("min") {
            overload(
                "int",
                "(FI)F",
                FLOAT_HELPER,
                "min"
            )
            overload(
                "long",
                "(FJ)F",
                FLOAT_HELPER,
                "min"
            )
            overload(
                "float",
                "(FF)F",
                FLOAT_HELPER,
                "min"
            )
            overload(
                "double",
                "(FD)D",
                FLOAT_HELPER,
                "min"
            )
        }

        staticMethod("pow") {
            overload(
                "",
                "(FD)D",
                FLOAT_HELPER
            )
        }

        staticMethod("round") {
            overload(
                "",
                "(F)I",
                FLOAT_HELPER
            )
        }

        staticMethod("asString") {
            overload(
                "",
                "(F)Ljava/lang/String;",
                FLOAT_HELPER
            )
        }

        staticMethod("asDouble") {
            overload(
                "",
                "(F)D",
                FLOAT_HELPER
            )
        }
    }
}
