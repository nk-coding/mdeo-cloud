package com.mdeo.expression.ast.expressions

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
    override val kind: String = "longLiteral",
    override val evalType: Int,
    val value: String
) : TypedExpression
