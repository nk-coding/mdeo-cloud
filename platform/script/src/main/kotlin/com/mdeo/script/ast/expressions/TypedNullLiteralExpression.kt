package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Null literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 */
@Serializable
data class TypedNullLiteralExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.NullLiteral,
    override val evalType: Int
) : TypedExpression
