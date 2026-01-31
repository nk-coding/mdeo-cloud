package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedBooleanLiteralExpression] nodes.
 *
 * Compiles boolean literal expressions into a [TraversalCompilationResult] containing
 * a GraphTraversal that produces the constant boolean value using `__.constant()`.
 *
 * ## Example
 * The expression `true` compiles to a traversal equivalent to `__.constant(true)`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 */
class BooleanLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedBooleanLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedBooleanLiteralExpression
    }

    /**
     * Compiles a boolean literal expression into a traversal result.
     *
     * Uses the boolean value directly from the expression.
     *
     * @param expression The boolean literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [TraversalCompilationResult] producing the boolean value
     */
    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val booleanExpression = expression as TypedBooleanLiteralExpression
        val booleanValue = booleanExpression.value
        return TraversalCompilationResult.constant(booleanValue, initialTraversal)
    }
}
