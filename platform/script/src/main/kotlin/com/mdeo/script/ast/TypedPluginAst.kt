package com.mdeo.script.ast

import com.mdeo.expression.ast.TypedCallableBody
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ReturnTypeSerializer
import kotlinx.serialization.Serializable

/**
 * Root of the TypedAST for plugin contributions.
 *
 * Represents all functions contributed by script contribution plugins, as produced
 * by the `typedAstHandler` in the `service-script` frontend package. All plugin
 * function signatures share a single merged [types] array.
 *
 * @param types Array of all types used across all plugin functions. Indexed by typeIndex
 *              in expressions and parameter/return-type declarations.
 * @param functions All functions contributed by plugins, each potentially carrying
 *                  multiple overloads.
 */
@Serializable
data class TypedPluginAst(
    val types: List<@Serializable(with = ReturnTypeSerializer::class) ReturnType>,
    val functions: List<TypedPluginFunction>
)

/**
 * A single contributed function with all of its overloads.
 *
 * @param name The function name as visible to script code.
 * @param signatures Map from overload identifier to the concrete signature and body.
 *                   Extension-call expressions carry `overload = ""`, so a single-overload
 *                   contribution plugin should use `""` as the key; the compiler also
 *                   falls back to the first available overload when the exact key is absent.
 */
@Serializable
data class TypedPluginFunction(
    val name: String,
    val signatures: Map<String, TypedPluginFunctionSignature>
)

/**
 * One overload of a contributed function.
 *
 * All type indices refer to positions in [TypedPluginAst.types].
 *
 * @param parameters Parameters of this overload in declaration order.
 * @param returnType Index into [TypedPluginAst.types] for the return type.
 * @param body Compiled callable body that implements this overload.
 */
@Serializable
data class TypedPluginFunctionSignature(
    val parameters: List<TypedParameter>,
    val returnType: Int,
    val body: TypedCallableBody
)
