package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import kotlinx.serialization.Serializable

/**
 * Break statement.
 *
 * @param kind The kind of statement.
 */
@Serializable
data class TypedBreakStatement(
    override val kind: TypedStatementKind = TypedStatementKind.Break
) : TypedStatement
