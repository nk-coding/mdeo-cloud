package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import kotlinx.serialization.Serializable

/**
 * Variable declaration statement.
 *
 * @param kind The kind of statement.
 * @param name Name of the variable being declared.
 * @param type Index into the types array for the variable type.
 * @param initialValue Optional initial value expression.
 */
@Serializable
data class TypedVariableDeclarationStatement(
    override val kind: TypedStatementKind = TypedStatementKind.VariableDeclaration,
    val name: String,
    val type: Int,
    val initialValue: @Serializable(with = TypedExpressionSerializer::class) TypedExpression? = null
) : TypedStatement
