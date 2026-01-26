package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
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
    override val kind: String = "variableDeclaration",
    val name: String,
    val type: Int,
    @Contextual
    val initialValue: TypedExpression? = null
) : TypedStatement
