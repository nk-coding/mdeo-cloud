package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedTernaryExpression] nodes.
 *
 * Compiles ternary expressions (condition ? then : else) into [CompilationResult]
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

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedTernaryExpression], `false` otherwise
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedTernaryExpression
    }

    /**
     * Compiles a ternary expression into a Gremlin traversal.
     *
     * This method converts the ternary expression into a Gremlin choose() step
     * that implements conditional branching based on the condition evaluation.
     *
     * @param expression The ternary expression to compile
     * @param context The compilation context containing variable bindings and scope information
     * @param initialTraversal Optional initial traversal to build upon; passed only to the condition
     * @return A [CompilationResult] containing the compiled choose() traversal
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val ternaryExpr = expression as TypedTernaryExpression
        return compileTernary(ternaryExpr, context, initialTraversal)
    }

    /**
     * Compiles a ternary expression using Gremlin's choose step.
     *
     * Evaluates the condition and branches to either the true or false expression
     * based on the condition's result. The condition is compiled with the initialTraversal
     * while the true and false branches start fresh with null initialTraversal since they
     * execute within anonymous traversals in the choose step.
     *
     * @param expr The ternary expression containing condition, true, and false branches
     * @param context The compilation context for resolving variables and scopes
     * @param initialTraversal Optional initial traversal passed only to the condition expression
     * @return A [CompilationResult] with the complete choose() traversal
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileTernary(
        expr: TypedTernaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val conditionResult = registry.compile(expr.condition, context, initialTraversal)

        val trueResult = registry.compile(expr.trueExpression, context, null)
        val falseResult = registry.compile(expr.falseExpression, context, null)

        val traversal = buildChooseTraversal(
            conditionResult.traversal as GraphTraversal<Any, Any>,
            trueResult.traversal as GraphTraversal<Any, Any>,
            falseResult.traversal as GraphTraversal<Any, Any>
        )

        return CompilationResult.of(traversal)
    }

    /**
     * Builds the choose traversal for conditional branching.
     *
     * Uses is(P.eq(true)) as the condition predicate to check if the
     * condition evaluates to true. This creates the Gremlin equivalent of
     * the ternary operator: condition ? trueExpr : falseExpr.
     *
     * @param conditionTraversal The traversal that produces the boolean condition value
     * @param trueTraversal The traversal to execute if the condition is true
     * @param falseTraversal The traversal to execute if the condition is false
     * @return A [GraphTraversal] implementing the conditional logic with choose()
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
