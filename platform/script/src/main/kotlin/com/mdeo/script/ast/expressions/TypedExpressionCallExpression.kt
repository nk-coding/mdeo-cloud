package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Expression call expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression being called (function or lambda).
 * @param arguments Array of argument expressions.
 */
@Serializable
data class TypedExpressionCallExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.ExpressionCall,
    override val evalType: Int,
    val expression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    @SerialName("arguments")
    override val arguments: List<@Serializable(with = TypedExpressionSerializer::class) TypedExpression>
) : TypedCallExpression
