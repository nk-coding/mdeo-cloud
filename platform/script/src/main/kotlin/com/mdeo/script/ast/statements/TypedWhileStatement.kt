package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
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
    override val kind: TypedStatementKind = TypedStatementKind.While,
    val condition: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val body: List<@Serializable(with = TypedStatementSerializer::class) TypedStatement>
) : TypedStatement
