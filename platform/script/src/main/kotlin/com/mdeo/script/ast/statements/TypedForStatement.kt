package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import kotlinx.serialization.Serializable

/**
 * For statement.
 *
 * @param kind The kind of statement.
 * @param variableName Name of the loop variable.
 * @param variableType Index into the types array for the loop variable type.
 * @param iterable Expression providing the iterable collection.
 * @param body Statements to execute in each iteration.
 */
@Serializable
data class TypedForStatement(
    override val kind: TypedStatementKind = TypedStatementKind.For,
    val variableName: String,
    val variableType: Int,
    val iterable: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val body: List<@Serializable(with = TypedStatementSerializer::class) TypedStatement>
) : TypedStatement
