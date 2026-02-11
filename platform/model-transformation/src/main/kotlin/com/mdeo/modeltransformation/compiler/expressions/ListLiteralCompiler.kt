package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedListLiteralExpression] nodes.
 *
 * Compiles list literal expressions into a [GremlinCompilationResult] containing
 * a GraphTraversal that produces a list of constant values.
 *
 * ## Compilation Strategy
 * This compiler handles list literals by recursively compiling each element:
 *
 * 1. If all elements are constants (isConstant = true), the list is compiled
 *    as a single constant value using `__.constant(listOf(...))`.
 *
 * 2. If any element is non-constant (dynamic), the compiler throws an exception
 *    as dynamic list construction requires fold/unfold operations not yet supported.
 *
 * ## Example
 * The expression `[1, 2, 3]` compiles to `__.constant(listOf(1, 2, 3))`.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided (e.g., in match contexts), the constant
 * step is appended to it. Otherwise, a new anonymous traversal is created.
 *
 * @param registry The traversal compiler registry for compiling element expressions
 */
class ListLiteralCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedListLiteralExpression]
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedListLiteralExpression
    }

    /**
     * Compiles a list literal expression into a traversal result that emits multiple values.
     *
     * This compiler creates a traversal that emits each list element as a separate value,
     * rather than creating a single List object. This approach supports:
     * 1. Constant elements: `[10, 20]` → emits 10, then 20
     * 2. Computed elements: `[10 * 20, 100 - 20]` → evaluates expressions and emits results
     *
     * The traversal uses `union()` to combine all element traversals, ensuring
     * compatibility with remote graph databases (no lambdas required).
     *
     * For storage with Cardinality.list, each emitted value will be added as a
     * separate property value, not as a single List object.
     *
     * @param expression The list literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [GremlinCompilationResult] that emits each list element sequentially
     */
    @Suppress("UNCHECKED_CAST")
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val listExpression = expression as TypedListLiteralExpression
        val elementResults = compileElements(listExpression.elements, context)

        val traversal: GraphTraversal<Any, Any> = if (elementResults.isEmpty()) {
            if (initialTraversal != null) {
                (initialTraversal as GraphTraversal<Any, Any>).not(AnonymousTraversal.identity<Any>()) as GraphTraversal<Any, Any>
            } else {
                (AnonymousTraversal.not<Any>(AnonymousTraversal.identity<Any>()) as GraphTraversal<Any, Any>)
            }
        } else if (elementResults.size == 1) {
            val elementTraversal = elementResults[0].traversal as GraphTraversal<Any, Any>
            if (initialTraversal != null) {
                (initialTraversal as GraphTraversal<Any, Any>).flatMap(elementTraversal) as GraphTraversal<Any, Any>
            } else {
                elementTraversal
            }
        } else {
            val elementTraversals = elementResults.map { 
                it.traversal as GraphTraversal<Any, Any>
            }.toTypedArray()
            
            if (initialTraversal != null) {
                (initialTraversal as GraphTraversal<Any, Any>).union(*elementTraversals) as GraphTraversal<Any, Any>
            } else {
                AnonymousTraversal.union<Any, Any>(*elementTraversals) as GraphTraversal<Any, Any>
            }
        }

        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Compiles each element expression to a traversal result.
     */
    private fun compileElements(
        elements: List<TypedExpression>,
        context: CompilationContext
    ): List<GremlinCompilationResult> {
        return elements.map { element ->
            registry.compile(element, context, null)
        }
    }

}
