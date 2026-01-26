package com.mdeo.expression.ast.statements

import kotlinx.serialization.Serializable

/**
 * Continue statement.
 *
 * @param kind The kind of statement.
 */
@Serializable
data class TypedContinueStatement(
    override val kind: String = "continue"
) : TypedStatement
