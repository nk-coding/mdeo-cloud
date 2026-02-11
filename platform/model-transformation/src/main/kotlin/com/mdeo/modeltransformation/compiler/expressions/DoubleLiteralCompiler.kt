package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedDoubleLiteralExpression] nodes.
 *
 * Compiles double literal expressions into a [GremlinCompilationResult] containing
 * a GraphTraversal that produces the constant double value using `__.constant()`.
 *
 * ## Example
 * The expression `3.14159` compiles to a traversal equivalent to `__.constant(3.14159)`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 */
class DoubleLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedDoubleLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedDoubleLiteralExpression
    }

    /**
     * Compiles a double literal expression into a traversal result.
     *
     * Parses the string value from the expression and creates a constant traversal.
     *
     * @param expression The double literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [GremlinCompilationResult] producing the double value
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val doubleExpression = expression as TypedDoubleLiteralExpression
        val doubleValue = doubleExpression.value.toDouble()
        return GremlinCompilationResult.constant(doubleValue, initialTraversal)
    }
}
