package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedFloatLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedFloatLiteralExpression] nodes.
 *
 * Compiles float literal expressions into a [GremlinCompilationResult] containing
 * a GraphTraversal that produces the constant float value using `__.constant()`.
 *
 * ## Example
 * The expression `3.14f` compiles to a traversal equivalent to `__.constant(3.14f)`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 */
class FloatLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedFloatLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedFloatLiteralExpression
    }

    /**
     * Compiles a float literal expression into a traversal result.
     *
     * Parses the string value from the expression and creates a constant traversal.
     *
     * @param expression The float literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [GremlinCompilationResult] producing the float value
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val floatExpression = expression as TypedFloatLiteralExpression
        val floatValue = floatExpression.value.toFloat()
        return GremlinCompilationResult.constant(floatValue, initialTraversal)
    }
}
