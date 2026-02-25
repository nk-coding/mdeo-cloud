package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedIntLiteralExpression] nodes.
 *
 * Compiles integer literal expressions into a [CompilationResult] containing
 * a GraphTraversal that produces the constant integer value using `__.constant()`.
 *
 * ## Example
 * The expression `42` compiles to a traversal equivalent to `__.constant(42)`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 */
class IntLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedIntLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedIntLiteralExpression
    }

    /**
     * Compiles an integer literal expression into a traversal result.
     *
     * Parses the string value from the expression and creates a constant traversal.
     *
     * @param expression The integer literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [CompilationResult] producing the integer value
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val intExpression = expression as TypedIntLiteralExpression
        val intValue = intExpression.value.toInt()
        return CompilationResult.constant(intValue, initialTraversal)
    }
}
