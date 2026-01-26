package com.mdeo.expression.ast.expressions

/**
 * Base interface for all expressions.
 */
interface TypedExpression {
    /**
     * The kind of expression.
     */
    val kind: String
    
    /**
     * Index into the types array for the type this expression evaluates to.
     */
    val evalType: Int
}
