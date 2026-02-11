package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedLongLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedLongLiteralExpression] nodes.
 *
 * Compiles long literal expressions into a [GremlinCompilationResult] containing
 * a GraphTraversal that produces the constant long value using `__.constant()`.
 *
 * ## Example
 * The expression `9223372036854775807L` compiles to a traversal equivalent to
 * `__.constant(9223372036854775807L)`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 */
class LongLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedLongLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedLongLiteralExpression
    }

    /**
     * Compiles a long literal expression into a traversal result.
     *
     * Parses the string value from the expression and creates a constant traversal.
     *
     * @param expression The long literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [GremlinCompilationResult] producing the long value
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val longExpression = expression as TypedLongLiteralExpression
        val longValue = longExpression.value.toLong()
        return GremlinCompilationResult.constant(longValue, initialTraversal)
    }
}
