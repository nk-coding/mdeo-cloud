package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import kotlinx.serialization.Serializable

/**
 * Assignment statement.
 *
 * @param kind The kind of statement.
 * @param left The left-hand side target of the assignment (identifier or member access).
 * @param right The right-hand side value expression to assign.
 */
@Serializable
data class TypedAssignmentStatement(
    override val kind: TypedStatementKind = TypedStatementKind.Assignment,
    val left: @Serializable(with = TypedExpressionSerializer::class) TypedExpression,
    val right: @Serializable(with = TypedExpressionSerializer::class) TypedExpression
) : TypedStatement
