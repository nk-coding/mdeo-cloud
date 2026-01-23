package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Ternary expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param condition The condition expression to evaluate.
 * @param trueExpression Expression to evaluate if condition is true.
 * @param falseExpression Expression to evaluate if condition is false.
 */
@Serializable
data class TypedTernaryExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.Ternary,
    override val evalType: Int,
    val condition: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val trueExpression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val falseExpression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression
) : TypedExpression
