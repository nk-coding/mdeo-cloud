package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Serializable

/**
 * Double literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param value The double value as a string.
 */
@Serializable
data class TypedDoubleLiteralExpression(
    override val kind: String = "doubleLiteral",
    override val evalType: Int,
    val value: String
) : TypedExpression
