package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Else-if clause.
 *
 * @param condition The condition expression to evaluate.
 * @param thenBlock Statements to execute if the condition is true.
 */
@Serializable
data class TypedElseIfClause(
    @Contextual
    val condition: TypedExpression,
    val thenBlock: List<@Contextual TypedStatement>
)

/**
 * If statement.
 *
 * @param kind The kind of statement.
 * @param condition The condition expression to evaluate.
 * @param thenBlock Statements to execute if the condition is true.
 * @param elseIfs Array of else-if clauses.
 * @param elseBlock Optional else block statements.
 */
@Serializable
data class TypedIfStatement(
    override val kind: String = "if",
    @Contextual
    val condition: TypedExpression,
    val thenBlock: List<@Contextual TypedStatement>,
    val elseIfs: List<TypedElseIfClause>,
    val elseBlock: List<@Contextual TypedStatement>? = null
) : TypedStatement
