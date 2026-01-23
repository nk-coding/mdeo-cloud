package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Member access expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression on which the member is accessed.
 * @param member The name of the member being accessed.
 * @param isNullChaining Whether this uses null-safe chaining (?.).
 */
@Serializable
data class TypedMemberAccessExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.MemberAccess,
    override val evalType: Int,
    val expression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val member: String,
    val isNullChaining: Boolean
) : TypedExpression
