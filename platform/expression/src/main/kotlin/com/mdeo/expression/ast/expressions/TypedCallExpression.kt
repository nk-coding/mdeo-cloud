package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName

/**
 * Base interface for call expressions.
 */
interface TypedCallExpression : TypedExpression {
    /**
     * Array of argument expressions.
     */
    @SerialName("arguments")
    val arguments: List<@Contextual TypedExpression>
}
