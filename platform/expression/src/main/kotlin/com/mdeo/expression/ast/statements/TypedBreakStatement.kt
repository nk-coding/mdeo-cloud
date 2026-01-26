package com.mdeo.expression.ast.statements

import kotlinx.serialization.Serializable

/**
 * Break statement.
 *
 * @param kind The kind of statement.
 */
@Serializable
data class TypedBreakStatement(
    override val kind: String = "break"
) : TypedStatement
