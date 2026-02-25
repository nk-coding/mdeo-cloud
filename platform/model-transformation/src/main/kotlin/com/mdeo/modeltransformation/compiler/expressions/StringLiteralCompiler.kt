package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedStringLiteralExpression] nodes.
 *
 * Compiles string literal expressions into a [CompilationResult] containing
 * a GraphTraversal that produces the constant string value using `__.constant()`.
 *
 * ## Example
 * The expression `"hello"` compiles to a traversal equivalent to `__.constant("hello")`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 */
class StringLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedStringLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedStringLiteralExpression
    }

    /**
     * Compiles a string literal expression into a traversal result.
     *
     * Uses the string value directly from the expression.
     *
     * @param expression The string literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [CompilationResult] producing the string value
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val stringExpression = expression as TypedStringLiteralExpression
        val stringValue = stringExpression.value
        return CompilationResult.constant(stringValue, initialTraversal)
    }
}
