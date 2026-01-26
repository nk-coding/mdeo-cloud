package com.mdeo.expression.ast.expressions

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
    override val kind: String = "intLiteral",
    override val evalType: Int,
    val value: String
) : TypedExpression
