package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Boolean literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param value The boolean value.
 */
@Serializable
data class TypedBooleanLiteralExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.BooleanLiteral,
    override val evalType: Int,
    val value: Boolean
) : TypedExpression
