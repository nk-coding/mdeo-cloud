package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Serializable

/**
 * Identifier expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param name The name of the identifier.
 * @param variableIndex Index into the variables array for the variable being referenced.
 */
@Serializable
data class TypedIdentifierExpression(
    override val kind: String = "identifier",
    override val evalType: Int,
    val name: String,
    val scope: Int
) : TypedExpression
