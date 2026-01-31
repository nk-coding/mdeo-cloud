package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedAssertNonNullExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedAssertNonNullExpression] nodes.
 *
 * Compiles assert non-null expressions (expr!!) into [TraversalCompilationResult]
 * containing GraphTraversals.
 *
 * ## Gremlin Implementation
 * In Gremlin's runtime, null handling is implicit. The traversal continues with
 * the value if present, and the traverser simply doesn't continue if the value
 * is null (no matching elements). Therefore, the non-null assertion is effectively
 * a pass-through of the inner expression.
 *
 * The expression is compiled and returned directly since Gremlin's traversal
 * semantics naturally handle null values by filtering them out.
 *
 * ## Initial Traversal Propagation
 * The [initialTraversal] is passed directly to the inner expression since
 * there is only one sub-expression.
 *
 * ## Portability
 * Uses pure Gremlin (no lambdas) for maximum portability across graph databases.
 *
 * @param registry The traversal compiler registry for compiling the inner expression
 */
class AssertNonNullCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedAssertNonNullExpression
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val assertExpr = expression as TypedAssertNonNullExpression
        return compileAssertNonNull(assertExpr, context, initialTraversal)
    }

    /**
     * Compiles an assert non-null expression.
     *
     * In Gremlin, null values naturally don't produce traversers, so the
     * non-null assertion is a semantic pass-through. The inner expression
     * is compiled and returned directly.
     */
    private fun compileAssertNonNull(
        expr: TypedAssertNonNullExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        return registry.compile(expr.expression, context, initialTraversal)
    }
}
