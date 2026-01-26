package com.mdeo.expression.ast

import com.mdeo.expression.ast.statements.TypedStatement
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Body of a callable (function or lambda).
 *
 * @param body Statements in the body of the callable.
 */
@Serializable
data class TypedCallableBody(
    val body: List<@Contextual TypedStatement>
)
