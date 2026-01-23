package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind

/**
 * Base interface for all statements.
 */
interface TypedStatement {
    /**
     * The kind of statement.
     */
    val kind: TypedStatementKind
}
