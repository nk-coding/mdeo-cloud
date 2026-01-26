package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Ternary expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param condition The condition expression.
 * @param thenExpression The expression to evaluate if condition is true.
 * @param elseExpression The expression to evaluate if condition is false.
 */
@Serializable
data class TypedTernaryExpression(
    override val kind: String = "ternary",
    override val evalType: Int,
    @Contextual
    val condition: TypedExpression,
    @Contextual
    val trueExpression: TypedExpression,
    @Contextual
    val falseExpression: TypedExpression
) : TypedExpression
