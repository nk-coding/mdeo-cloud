package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import kotlinx.serialization.Serializable

/**
 * Continue statement.
 *
 * @param kind The kind of statement.
 */
@Serializable
data class TypedContinueStatement(
    override val kind: TypedStatementKind = TypedStatementKind.Continue
) : TypedStatement
