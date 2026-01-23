package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind

/**
 * Base interface for all expressions.
 */
interface TypedExpression {
    /**
     * The kind of expression.
     */
    val kind: TypedExpressionKind
    
    /**
     * Index into the types array for the type this expression evaluates to.
     */
    val evalType: Int
}
