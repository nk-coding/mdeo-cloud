package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import kotlinx.serialization.Serializable

/**
 * Return statement.
 *
 * @param kind The kind of statement.
 * @param value Optional expression to return.
 */
@Serializable
data class TypedReturnStatement(
    override val kind: TypedStatementKind = TypedStatementKind.Return,
    val value: @Serializable(with = TypedExpressionSerializer::class) TypedExpression? = null
) : TypedStatement
