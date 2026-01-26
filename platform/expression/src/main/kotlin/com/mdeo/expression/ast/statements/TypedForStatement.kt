package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
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
    override val kind: String = "for",
    val variableName: String,
    val variableType: Int,
    @Contextual
    val iterable: TypedExpression,
    val body: List<@Contextual TypedStatement>
) : TypedStatement
