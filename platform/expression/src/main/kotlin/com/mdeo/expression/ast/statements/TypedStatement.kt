package com.mdeo.expression.ast.statements

/**
 * Base interface for all statements.
 */
interface TypedStatement {
    /**
     * The kind of statement.
     */
    val kind: String
}
