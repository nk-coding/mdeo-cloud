package com.mdeo.expression.ast.expressions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Function call expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param name The name of the function being called.
 * @param overload Overload identifier for the function being called.
 * @param arguments List of arguments with their expected parameter types.
 */
@Serializable
data class TypedFunctionCallExpression(
    override val kind: String = "functionCall",
    override val evalType: Int,
    val name: String,
    val overload: String,
    @SerialName("arguments")
    override val arguments: List<TypedCallArgument>
) : TypedCallExpression
