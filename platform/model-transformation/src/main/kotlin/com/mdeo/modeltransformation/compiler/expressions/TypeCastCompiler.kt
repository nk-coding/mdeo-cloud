package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTypeCastExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedTypeCastExpression] nodes.
 *
 * Compiles type cast expressions (expr as Type, expr as? Type) into
 * [TraversalCompilationResult] containing GraphTraversals.
 *
 * ## Gremlin Implementation
 * Since Gremlin is dynamically typed, type casts are essentially no-ops at runtime.
 * The expression traversal is returned directly without modification.
 *
 * For safe casts (as?), the semantic is also a pass-through in Gremlin since
 * there's no static type system to validate against at runtime. If type validation
 * is required, it should be handled at a higher level.
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
class TypeCastCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedTypeCastExpression
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val castExpr = expression as TypedTypeCastExpression
        return compileTypeCast(castExpr, context, initialTraversal)
    }

    /**
     * Compiles a type cast expression.
     *
     * In Gremlin's dynamic type system, casts are pass-throughs. The inner
     * expression is compiled and returned directly since there's no runtime
     * type conversion needed for graph traversals.
     */
    private fun compileTypeCast(
        expr: TypedTypeCastExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        return registry.compile(expr.expression, context, initialTraversal)
    }
}
