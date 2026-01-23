package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * String literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param value The string value.
 */
@Serializable
data class TypedStringLiteralExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.StringLiteral,
    override val evalType: Int,
    val value: String
) : TypedExpression
