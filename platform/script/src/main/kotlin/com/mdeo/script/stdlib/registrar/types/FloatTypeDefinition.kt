package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.ast.types.BuiltinTypes
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
            overload("", "(F)F", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.FLOAT)
        }

        staticMethod("ceiling") {
            overload("", "(F)I", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.INT)
        }

        staticMethod("floor") {
            overload("", "(F)I", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.INT)
        }

        staticMethod("log") {
            overload("", "(F)F", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.FLOAT)
        }

        staticMethod("log10") {
            overload("", "(F)F", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.FLOAT)
        }

        staticMethod("max") {
            overload("int", "(FI)F", FLOAT_HELPER, "max", parameterTypes = listOf(BuiltinTypes.INT), returnType = BuiltinTypes.FLOAT)
            overload("long", "(FJ)F", FLOAT_HELPER, "max", parameterTypes = listOf(BuiltinTypes.LONG), returnType = BuiltinTypes.FLOAT)
            overload("float", "(FF)F", FLOAT_HELPER, "max", parameterTypes = listOf(BuiltinTypes.FLOAT), returnType = BuiltinTypes.FLOAT)
            overload("double", "(FD)D", FLOAT_HELPER, "max", parameterTypes = listOf(BuiltinTypes.DOUBLE), returnType = BuiltinTypes.DOUBLE)
        }

        staticMethod("min") {
            overload("int", "(FI)F", FLOAT_HELPER, "min", parameterTypes = listOf(BuiltinTypes.INT), returnType = BuiltinTypes.FLOAT)
            overload("long", "(FJ)F", FLOAT_HELPER, "min", parameterTypes = listOf(BuiltinTypes.LONG), returnType = BuiltinTypes.FLOAT)
            overload("float", "(FF)F", FLOAT_HELPER, "min", parameterTypes = listOf(BuiltinTypes.FLOAT), returnType = BuiltinTypes.FLOAT)
            overload("double", "(FD)D", FLOAT_HELPER, "min", parameterTypes = listOf(BuiltinTypes.DOUBLE), returnType = BuiltinTypes.DOUBLE)
        }

        staticMethod("pow") {
            overload("", "(FD)D", FLOAT_HELPER, parameterTypes = listOf(BuiltinTypes.DOUBLE), returnType = BuiltinTypes.DOUBLE)
        }

        staticMethod("round") {
            overload("", "(F)I", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.INT)
        }

        staticMethod("asString") {
            overload("", "(F)Ljava/lang/String;", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.STRING)
        }

        staticMethod("asDouble") {
            overload("", "(F)D", FLOAT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.DOUBLE)
        }
    }
}
