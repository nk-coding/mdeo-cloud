package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Unary expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param operator The unary operator being applied.
 * @param expression The operand expression.
 */
@Serializable
data class TypedUnaryExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.Unary,
    override val evalType: Int,
    val operator: String,
    val expression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression
) : TypedExpression
