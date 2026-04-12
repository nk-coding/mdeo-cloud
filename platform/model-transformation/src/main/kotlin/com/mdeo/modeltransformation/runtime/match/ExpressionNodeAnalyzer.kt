package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.expressions.*
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression

/**
 * Analyzes expression trees to determine which match nodes they reference.
 *
 * This is used to determine whether a property constraint expression can be
 * inlined into the Gremlin query: if all referenced match nodes are already
 * bound at the current point in the traversal, the constraint can be evaluated
 * inline rather than deferred to a post-match filter.
 *
 * The analyzer respects scope levels: an identifier is considered a match node
 * reference only when its declared scope level is at or below the current
 * analysis scope, and its name is present in the known match node set.
 *
 * @param matchNodeNames Names of all match nodes known in the current pattern.
 * @param currentScopeIndex The scope index at or below which identifiers are considered potential node references.
 */
internal class ExpressionNodeAnalyzer(
    private val matchNodeNames: Set<String>,
    private val currentScopeIndex: Int
) {
    /**
     * Collects all match node names referenced by [expression].
     *
     * Recursively traverses the expression tree, collecting identifiers whose scope
     * is at or below [currentScopeIndex] and whose name appears in [matchNodeNames].
     *
     * @param expression The expression tree to analyze.
     * @return Set of match node names that are directly referenced in the expression.
     */
    fun findReferencedNodes(expression: TypedExpression): Set<String> {
        val result = mutableSetOf<String>()
        collect(expression, result)
        return result
    }

    /**
     * Checks whether [expression] references only nodes in [boundNodes].
     *
     * A return value of `true` means the expression can safely be evaluated when
     * only the given [boundNodes] are available. Expressions that reference no nodes
     * at all are also considered fully bound.
     *
     * @param expression The expression tree to check.
     * @param boundNodes The set of node names that are already bound in the traversal.
     * @return `true` if all referenced nodes are in [boundNodes] (or the expression references no nodes).
     */
    fun allNodesAreBound(expression: TypedExpression, boundNodes: Set<String>): Boolean {
        return findReferencedNodes(expression).all { it in boundNodes }
    }

    /**
     * Checks whether [expression] references no match nodes at all (pure constant or only variables).
     *
     * @param expression The expression tree to check.
     * @return `true` if the expression does not reference any match nodes.
     */
    fun isNodeFree(expression: TypedExpression): Boolean {
        return findReferencedNodes(expression).isEmpty()
    }

    /**
     * Recursively collects match node names from the expression tree into [result].
     *
     * @param expression The expression subtree to traverse.
     * @param result The mutable set to accumulate referenced node names into.
     */
    private fun collect(expression: TypedExpression, result: MutableSet<String>) {
        when (expression) {
            is TypedIdentifierExpression -> {
                if (expression.scope <= currentScopeIndex && expression.name in matchNodeNames) {
                    result.add(expression.name)
                }
            }
            is TypedMemberAccessExpression -> {
                collect(expression.expression, result)
            }
            is TypedBinaryExpression -> {
                collect(expression.left, result)
                collect(expression.right, result)
            }
            is TypedUnaryExpression -> {
                collect(expression.expression, result)
            }
            is TypedMemberCallExpression -> {
                collect(expression.expression, result)
                expression.arguments.forEach { collect(it.value, result) }
            }
            is TypedFunctionCallExpression -> {
                expression.arguments.forEach { collect(it.value, result) }
            }
            is TypedExpressionCallExpression -> {
                collect(expression.expression, result)
                expression.arguments.forEach { collect(it.value, result) }
            }
            is TypedExtensionCallExpression -> {
                expression.arguments.forEach { collect(it.value, result) }
            }
            is TypedTernaryExpression -> {
                collect(expression.condition, result)
                collect(expression.trueExpression, result)
                collect(expression.falseExpression, result)
            }
            is TypedListLiteralExpression -> {
                expression.elements.forEach { collect(it, result) }
            }
            is TypedTypeCheckExpression -> {
                collect(expression.expression, result)
            }
            is TypedTypeCastExpression -> {
                collect(expression.expression, result)
            }
            is TypedAssertNonNullExpression -> {
                collect(expression.expression, result)
            }
            is TypedLambdaExpression -> {
                collect(expression.body, result)
            }
            is TypedStringLiteralExpression,
            is TypedIntLiteralExpression,
            is TypedLongLiteralExpression,
            is TypedFloatLiteralExpression,
            is TypedDoubleLiteralExpression,
            is TypedBooleanLiteralExpression,
            is TypedNullLiteralExpression -> {
                // Literals never reference match nodes
            }
            else -> {
                throw IllegalArgumentException("Unsupported expression type: ${expression::class}")
            }
        }
    }
}
