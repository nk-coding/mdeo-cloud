package com.mdeo.expression.ast.expressions

import kotlinx.serialization.SerialName

/**
 * Base interface for call expressions.
 */
interface TypedCallExpression : TypedExpression {
    /**
     * List of arguments, each wrapping the expression and its expected parameter type
     * from the resolved function signature.
     */
    @SerialName("arguments")
    val arguments: List<TypedCallArgument>
}
