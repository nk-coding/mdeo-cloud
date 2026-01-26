package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Type cast expression (as / as?).
 * Casts the expression to the specified target type.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression to cast.
 * @param targetType Index into the types array for the target type to cast to.
 * @param isSafe Whether this is a safe cast (as?) that returns null instead of throwing.
 */
@Serializable
data class TypedTypeCastExpression(
    override val kind: String = "typeCast",
    override val evalType: Int,
    @Contextual
    val expression: TypedExpression,
    val targetType: Int,
    val isSafe: Boolean
) : TypedExpression
