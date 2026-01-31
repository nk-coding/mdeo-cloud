package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

/**
 * List literal expression (square brackets with comma separated values).
 *
 * @property kind The kind of expression, always "listLiteral".
 * @property evalType Index into the types array for the type this expression evaluates to.
 * @property elements Array of element expressions in the list.
 */
@Serializable
data class TypedListLiteralExpression(
    override val kind: String = "listLiteral",
    override val evalType: Int,
    val elements: List<@Contextual TypedExpression>
) : TypedExpression
