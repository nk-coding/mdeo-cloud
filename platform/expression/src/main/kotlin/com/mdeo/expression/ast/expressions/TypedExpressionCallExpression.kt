package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
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
    override val kind: String = "call",
    override val evalType: Int,
    @Contextual
    val expression: TypedExpression,
    @SerialName("arguments")
    override val arguments: List<@Contextual TypedExpression>
) : TypedCallExpression
