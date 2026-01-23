package com.mdeo.script.ast

import com.mdeo.script.ast.statements.TypedStatement
import com.mdeo.script.ast.statements.TypedStatementSerializer
import kotlinx.serialization.Serializable

/**
 * Body of a callable (function or lambda).
 *
 * @param body Statements in the body of the callable.
 */
@Serializable
data class TypedCallableBody(
    val body: List<@Serializable(with = TypedStatementSerializer::class) TypedStatement>
)
