package com.mdeo.expression.ast.expressions

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
    override val kind: String = "booleanLiteral",
    override val evalType: Int,
    val value: Boolean
) : TypedExpression
