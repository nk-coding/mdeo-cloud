package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Member access expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param expression The expression whose member is being accessed.
 * @param member The name of the member being accessed.
 * @param isNullChaining Whether this uses null-safe chaining (?.).
 */
@Serializable
data class TypedMemberAccessExpression(
    override val kind: String = "memberAccess",
    override val evalType: Int,
    @Contextual
    val expression: TypedExpression,
    val member: String,
    val isNullChaining: Boolean
) : TypedExpression
