package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Function call expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param name The name of the function being called.
 * @param overload Overload identifier for the function being called.
 * @param arguments Array of argument expressions.
 */
@Serializable
data class TypedFunctionCallExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.FunctionCall,
    override val evalType: Int,
    val name: String,
    val overload: String,
    @SerialName("arguments")
    override val arguments: List<@Serializable(with = TypedExpressionSerializer::class) TypedExpression>
) : TypedCallExpression
