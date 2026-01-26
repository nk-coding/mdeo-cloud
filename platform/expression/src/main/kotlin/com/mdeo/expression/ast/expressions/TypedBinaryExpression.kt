package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Binary expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param operator The binary operator being applied.
 * @param left The left operand expression.
 * @param right The right operand expression.
 */
@Serializable
data class TypedBinaryExpression(
    override val kind: String = "binary",
    override val evalType: Int,
    val operator: String,
    @Contextual
    val left: TypedExpression,
    @Contextual
    val right: TypedExpression
) : TypedExpression
