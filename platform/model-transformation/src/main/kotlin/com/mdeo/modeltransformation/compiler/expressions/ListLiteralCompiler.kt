package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedListLiteralExpression] nodes.
 *
 * Compiles list literal expressions into a [TraversalCompilationResult] containing
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
     * Compiles a list literal expression into a traversal result.
     *
     * @param expression The list literal expression to compile
     * @param context The traversal compilation context
     * @param initialTraversal Optional traversal to build upon
     * @return A [TraversalCompilationResult] producing the list value
     * @throws CompilationException If any element is non-constant
     */
    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val listExpression = expression as TypedListLiteralExpression
        val elementResults = compileElements(listExpression.elements, context)

        validateAllConstant(elementResults, expression)

        val values = elementResults.map { it.constantValue }
        return TraversalCompilationResult.constant(values, initialTraversal)
    }

    /**
     * Compiles each element expression to a traversal result.
     */
    private fun compileElements(
        elements: List<TypedExpression>,
        context: TraversalCompilationContext
    ): List<TraversalCompilationResult<*, *>> {
        return elements.map { element ->
            registry.compile(element, context, null)
        }
    }

    /**
     * Validates that all element results are constants.
     */
    private fun validateAllConstant(
        results: List<TraversalCompilationResult<*, *>>,
        expression: TypedExpression
    ) {
        val nonConstantIndex = results.indexOfFirst { !it.isConstant }
        if (nonConstantIndex >= 0) {
            throw CompilationException(
                "List literal element at index $nonConstantIndex is not a constant. " +
                    "Dynamic list construction is not yet supported.",
                expression
            )
        }
    }
}
