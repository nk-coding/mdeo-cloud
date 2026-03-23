package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Member call expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression on which the member function is called.
 * @param member The name of the member function being called.
 * @param isNullChaining Whether this uses null-safe chaining (?.).
 * @param overload Overload identifier for the member function being called.
 * @param arguments List of arguments with their expected parameter types.
 */
@Serializable
data class TypedMemberCallExpression(
    override val kind: String = "memberCall",
    override val evalType: Int,
    @Contextual
    val expression: TypedExpression,
    val member: String,
    val isNullChaining: Boolean,
    val overload: String,
    @SerialName("arguments")
    override val arguments: List<TypedCallArgument>
) : TypedCallExpression
