package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import kotlinx.serialization.Serializable

/**
 * Expression statement.
 *
 * @param kind The kind of statement.
 * @param expression The expression to evaluate.
 */
@Serializable
data class TypedExpressionStatement(
    override val kind: TypedStatementKind = TypedStatementKind.Expression,
    val expression: @Serializable(with = TypedExpressionSerializer::class) TypedExpression
) : TypedStatement
