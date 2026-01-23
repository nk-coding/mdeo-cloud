package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
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
 * @param arguments Array of argument expressions.
 */
@Serializable
data class TypedMemberCallExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.MemberCall,
    override val evalType: Int,
    val expression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val member: String,
    val isNullChaining: Boolean,
    val overload: String,
    @SerialName("arguments")
    override val arguments: List<@Serializable(with = TypedExpressionSerializer::class) TypedExpression>
) : TypedCallExpression
