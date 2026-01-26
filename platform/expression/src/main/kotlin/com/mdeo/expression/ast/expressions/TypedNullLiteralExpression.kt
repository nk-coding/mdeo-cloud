package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Serializable

/**
 * Null literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 */
@Serializable
data class TypedNullLiteralExpression(
    override val kind: String = "nullLiteral",
    override val evalType: Int
) : TypedExpression
