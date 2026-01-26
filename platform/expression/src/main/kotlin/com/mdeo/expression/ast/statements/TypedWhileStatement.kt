package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * While statement.
 *
 * @param kind The kind of statement.
 * @param condition The condition expression to evaluate.
 * @param body Statements to execute while the condition is true.
 */
@Serializable
data class TypedWhileStatement(
    override val kind: String = "while",
    @Contextual
    val condition: TypedExpression,
    val body: List<@Contextual TypedStatement>
) : TypedStatement
