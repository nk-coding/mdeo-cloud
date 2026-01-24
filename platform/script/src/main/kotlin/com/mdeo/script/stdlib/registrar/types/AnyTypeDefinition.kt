package com.mdeo.script.stdlib.registrar.types

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
                ANY_HELPER
            )
        }

        staticMethod("asInteger") {
            overload(
                "",
                "(Ljava/lang/Object;)I",
                ANY_HELPER
            )
        }

        staticMethod("asReal") {
            overload(
                "",
                "(Ljava/lang/Object;)D",
                ANY_HELPER
            )
        }

        staticMethod("asDouble") {
            overload(
                "",
                "(Ljava/lang/Object;)D",
                ANY_HELPER
            )
        }

        staticMethod("asFloat") {
            overload(
                "",
                "(Ljava/lang/Object;)F",
                ANY_HELPER
            )
        }

        staticMethod("asString") {
            overload(
                "",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                ANY_HELPER
            )
        }

        staticMethod("format") {
            overload(
                "",
                "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;",
                ANY_HELPER
            )
        }

        staticMethod("hasProperty") {
            overload(
                "",
                "(Ljava/lang/Object;Ljava/lang/String;)Z",
                ANY_HELPER
            )
        }
    }
}
