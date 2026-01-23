package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Int literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param value The integer value as a string.
 */
@Serializable
data class TypedIntLiteralExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.IntLiteral,
    override val evalType: Int,
    val value: String
) : TypedExpression
