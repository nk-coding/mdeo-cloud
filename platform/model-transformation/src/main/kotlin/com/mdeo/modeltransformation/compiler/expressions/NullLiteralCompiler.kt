package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedNullLiteralExpression] nodes.
 *
 * Compiles null literal expressions into a [CompilationResult] containing
 * a GraphTraversal that produces `null` using `__.constant(null)`.
 *
 * ## Example
 * The expression `null` compiles to a traversal equivalent to `__.constant(null)`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 *
 * ## Null Semantics
 * In Gremlin, null values can be propagated through traversals. The `constant(null)`
 * step produces a null value that can be used in comparisons, property assignments,
 * or other operations where null semantics apply.
 */
class NullLiteralCompiler : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedNullLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedNullLiteralExpression
    }

    /**
     * Compiles a null literal expression into a traversal result.
     *
     * Creates a constant traversal producing null.
     *
     * @param expression The null literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [CompilationResult] producing null
     */
    @Suppress("UNCHECKED_CAST")
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val typedInitial = initialTraversal as? GraphTraversal<Any, *>
        return CompilationResult.constant<Any, Any?>(null, typedInitial)
    }
}
