package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
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
    override val kind: String = "assignment",
    @Contextual
    val left: TypedExpression,
    @Contextual
    val right: TypedExpression
) : TypedStatement
