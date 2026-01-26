package com.mdeo.expression.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Return statement.
 *
 * @param kind The kind of statement.
 * @param value Optional expression to return.
 */
@Serializable
data class TypedReturnStatement(
    override val kind: String = "return",
    @Contextual
    val value: TypedExpression? = null
) : TypedStatement
