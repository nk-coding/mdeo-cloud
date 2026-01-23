package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import kotlinx.serialization.Serializable

/**
 * Else-if clause.
 *
 * @param condition The condition expression to evaluate.
 * @param thenBlock Statements to execute if the condition is true.
 */
@Serializable
data class TypedElseIfClause(
    val condition: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val thenBlock: List<@Serializable(with = TypedStatementSerializer::class) TypedStatement>
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
    override val kind: TypedStatementKind = TypedStatementKind.If,
    val condition: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val thenBlock: List<@Serializable(with = TypedStatementSerializer::class) TypedStatement>,
    val elseIfs: List<TypedElseIfClause>,
    val elseBlock: List<@Serializable(with = TypedStatementSerializer::class) TypedStatement>? = null
) : TypedStatement
