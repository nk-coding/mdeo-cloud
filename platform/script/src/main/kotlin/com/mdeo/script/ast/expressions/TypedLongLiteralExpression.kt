package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Long literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param value The long value as a string.
 */
@Serializable
data class TypedLongLiteralExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.LongLiteral,
    override val evalType: Int,
    val value: String
) : TypedExpression
