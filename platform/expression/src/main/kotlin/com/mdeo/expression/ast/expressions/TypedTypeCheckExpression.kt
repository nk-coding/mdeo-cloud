package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Type check expression (is / !is).
 * Checks whether the expression is of the specified type.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression to check.
 * @param checkType Index into the types array for the type to check against.
 * @param isNegated Whether this is a negated check (!is).
 */
@Serializable
data class TypedTypeCheckExpression(
    override val kind: String = "typeCheck",
    override val evalType: Int,
    @Contextual
    val expression: TypedExpression,
    val checkType: Int,
    val isNegated: Boolean
) : TypedExpression
