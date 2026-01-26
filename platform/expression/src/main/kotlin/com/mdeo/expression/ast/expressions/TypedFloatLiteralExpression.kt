package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Serializable

/**
 * Float literal expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param value The float value as a string.
 */
@Serializable
data class TypedFloatLiteralExpression(
    override val kind: String = "floatLiteral",
    override val evalType: Int,
    val value: String
) : TypedExpression
