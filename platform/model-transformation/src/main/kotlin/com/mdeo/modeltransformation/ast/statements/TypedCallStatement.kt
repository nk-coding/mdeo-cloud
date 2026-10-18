package com.mdeo.modeltransformation.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * Call of another transformation: `call name(a = expr, ...) -> (b: x, ...)`.
 *
 * The callee runs as an engine of its own on the same model graph, with its inputs bound. On
 * success (also when the callee ends with `stop`) its outputs are bound in the caller's scope,
 * as a match binds its objects. A callee failure fails the call, and `kill` in the callee ends
 * the whole transformation.
 *
 * @param callee Absolute path of the callee's file, its key in the root's callees.
 * @param name The callee's name, for messages.
 * @param arguments The arguments passed.
 * @param outputs The outputs bound.
 */
@Serializable
data class TypedCallStatement(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    override val kind: String = "call",
    val callee: String,
    val name: String,
    val arguments: List<TypedTransformationCallArgument> = emptyList(),
    val outputs: List<TypedTransformationCallOutput> = emptyList()
) : TypedTransformationStatement

/**
 * A named argument of a call.
 *
 * @param name The callee parameter the argument is passed to.
 * @param value The value, evaluated in the caller.
 */
@Serializable
data class TypedTransformationCallArgument(
    val name: String,
    @Contextual val value: TypedExpression
)

/**
 * The binding of a callee output to a caller name.
 *
 * @param parameter The callee's `out` parameter.
 * @param name The caller's name.
 */
@Serializable
data class TypedTransformationCallOutput(
    val parameter: String,
    val name: String
)
