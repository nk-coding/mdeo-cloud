package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Expression statement.
 *
 * @param kind The kind of statement.
 * @param expression The expression to evaluate.
 */
@Serializable
data class TypedExpressionStatement(
    override val kind: String = "expression",
    @Contextual
    val expression: TypedExpression
) : TypedStatement
