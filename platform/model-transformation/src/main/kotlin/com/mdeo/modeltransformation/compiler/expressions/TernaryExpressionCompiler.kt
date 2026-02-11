package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedTernaryExpression] nodes.
 *
 * Compiles ternary expressions (condition ? then : else) into [GremlinCompilationResult]
 * containing GraphTraversals that use Gremlin's `choose()` step for conditional branching.
 *
 * ## Gremlin Implementation
 * The ternary expression is implemented using Gremlin's choose step:
 * ```
 * conditionTraversal.choose(
 *     __.is(P.eq(true)),
 *     thenTraversal,
 *     elseTraversal
 * )
 * ```
 *
 * ## Initial Traversal Propagation
 * The [initialTraversal] is passed ONLY to the condition expression.
 * The then and else branches start fresh (null initialTraversal) since they
 * are executed within the choose step's anonymous traversals.
 *
 * ## Constant Folding
 * If the condition is a constant, the compiler optimizes by returning only
 * the appropriate branch traversal, avoiding unnecessary choose steps.
 *
 * ## Portability
 * Uses pure Gremlin (no lambdas) for maximum portability across graph databases.
 *
 * @param registry The traversal compiler registry for compiling sub-expressions
 */
class TernaryExpressionCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedTernaryExpression
    }

    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val ternaryExpr = expression as TypedTernaryExpression
        return compileTernary(ternaryExpr, context, initialTraversal)
    }

    /**
     * Compiles a ternary expression using Gremlin's choose step.
     *
     * Evaluates the condition and branches to either the true or false expression
     * based on the condition's result.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileTernary(
        expr: TypedTernaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val conditionResult = registry.compile(expr.condition, context, initialTraversal)

        val trueResult = registry.compile(expr.trueExpression, context, null)
        val falseResult = registry.compile(expr.falseExpression, context, null)

        val traversal = buildChooseTraversal(
            conditionResult.traversal as GraphTraversal<Any, Any>,
            trueResult.traversal as GraphTraversal<Any, Any>,
            falseResult.traversal as GraphTraversal<Any, Any>
        )

        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Builds the choose traversal for conditional branching.
     *
     * Uses is(P.eq(true)) as the condition predicate to check if the
     * condition evaluates to true.
     */
    private fun buildChooseTraversal(
        conditionTraversal: GraphTraversal<Any, Any>,
        trueTraversal: GraphTraversal<Any, Any>,
        falseTraversal: GraphTraversal<Any, Any>
    ): GraphTraversal<Any, Any> {
        return conditionTraversal.choose(
            AnonymousTraversal.`is`(P.eq(true)),
            trueTraversal,
            falseTraversal
        )
    }
}
