package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base interface for call expressions.
 */
interface TypedCallExpression : TypedExpression {
    /**
     * Array of argument expressions.
     */
    @SerialName("arguments")
    val arguments: List<@Serializable(with = TypedExpressionSerializer::class) TypedExpression>
}
