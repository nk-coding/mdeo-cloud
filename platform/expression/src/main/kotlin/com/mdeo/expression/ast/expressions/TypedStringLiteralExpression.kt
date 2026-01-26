package com.mdeo.expression.ast.expressions

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
    override val kind: String = "stringLiteral",
    override val evalType: Int,
    val value: String
) : TypedExpression
