package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Binary expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param operator The binary operator being applied.
 * @param left The left operand expression.
 * @param right The right operand expression.
 */
@Serializable
data class TypedBinaryExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.Binary,
    override val evalType: Int,
    val operator: String,
    val left: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val right: @Serializable(with = TypedExpressionSerializer::class) TypedExpression
) : TypedExpression
