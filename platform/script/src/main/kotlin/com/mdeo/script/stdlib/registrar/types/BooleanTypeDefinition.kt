package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val BOOLEAN_HELPER = "com/mdeo/script/stdlib/impl/primitives/BooleanHelper"

/**
 * Creates the Boolean type definition.
 */
fun createBooleanType(): TypeDefinition {
    return typeDefinition("builtin.boolean") {
        extends("builtin.any")

        staticMethod("asString") {
            overload(
                "", "(Z)Ljava/lang/String;", BOOLEAN_HELPER,
                parameterTypes = emptyList(),
                returnType = BuiltinTypes.STRING
            )
        }
    }
}
