package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Unary expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param operator The unary operator being applied.
 * @param expression The operand expression.
 */
@Serializable
data class TypedUnaryExpression(
    override val kind: String = "unary",
    override val evalType: Int,
    val operator: String,
    @Contextual
    val expression: TypedExpression
) : TypedExpression
