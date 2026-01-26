package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Assert non-null expression (!! postfix operator).
 * Asserts that the expression is not null, throwing a NullPointerException if it is.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression to assert as non-null.
 */
@Serializable
data class TypedAssertNonNullExpression(
    override val kind: String = "assertNonNull",
    override val evalType: Int,
    @Contextual
    val expression: TypedExpression
) : TypedExpression
