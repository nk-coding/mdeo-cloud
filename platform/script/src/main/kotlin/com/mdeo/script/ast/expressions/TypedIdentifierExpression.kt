package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.Serializable

/**
 * Identifier expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param name The name of the identifier.
 * @param scope Scope level where this identifier is defined. 0 = global scope, increases with nesting.
 */
@Serializable
data class TypedIdentifierExpression(
    override val kind: TypedExpressionKind = TypedExpressionKind.Identifier,
    override val evalType: Int,
    val name: String,
    val scope: Int
) : TypedExpression
