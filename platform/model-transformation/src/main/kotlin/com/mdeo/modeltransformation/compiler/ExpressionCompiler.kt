package com.mdeo.modeltransformation.compiler

import com.mdeo.expression.ast.expressions.TypedExpression
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Interface for expression compilers that produce traversal-based results.
 *
 * This is the new expression compiler interface that always produces a
 * [TraversalCompilationResult]. All expressions are compiled to GraphTraversals,
 * ensuring uniformity and composability across the compiler infrastructure.
 *
 * ## Initial Traversal Handling
 * The [compile] method accepts an optional [initialTraversal] parameter.
 * This is necessary for match contexts where traversals must start with
 * `__.as("label")`. The initial traversal should only be passed to the
 * leftmost sub-expression in compound expressions.
 *
 * ## Implementation Guidelines
 * 1. When compiling sub-expressions, pass [initialTraversal] only to the
 *    leftmost/first sub-expression.
 * 2. Other sub-expressions should receive null for [initialTraversal].
 * 3. If [initialTraversal] is null, use `__.identity()` or similar.
 *
 * Example implementation for literals:
 * ```kotlin
 * class IntLiteralCompiler : ExpressionCompiler {
 *     override fun canCompile(expression: TypedExpression) =
 *         expression is TypedIntLiteralExpression
 *
 *     override fun compile(
 *         expression: TypedExpression,
 *         context: TraversalCompilationContext,
 *         initialTraversal: GraphTraversal<*, *>?
 *     ): TraversalCompilationResult<*, *> {
 *         val intExpr = expression as TypedIntLiteralExpression
 *         return TraversalCompilationResult.constant(
 *             intExpr.value.toInt(),
 *             initialTraversal
 *         )
 *     }
 * }
 * ```
 *
 * @see TraversalCompilationResult
 * @see TraversalCompilationContext
 * @see ExpressionCompilerRegistry
 */
interface ExpressionCompiler {

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * Implementations should check the expression type and any other relevant
     * criteria to determine if this compiler is the appropriate one to use.
     * This method should be fast and avoid complex computations.
     *
     * @param expression The expression to check
     * @return `true` if this compiler can compile the expression, `false` otherwise
     */
    fun canCompile(expression: TypedExpression): Boolean

    /**
     * Compiles the given expression into a traversal compilation result.
     *
     * This method transforms a typed expression into a GraphTraversal.
     * The result is always a [TraversalCompilationResult] containing the
     * traversal that represents the expression.
     *
     * ## Initial Traversal Usage
     * When [initialTraversal] is provided, it should be used as the base
     * for the resulting traversal. This is essential for match contexts
     * where all traversals must start with a common prefix (like `__.as()`).
     *
     * For compound expressions (binary, ternary, etc.), the initial traversal
     * should only be passed to the leftmost sub-expression. Other sub-expressions
     * should start fresh.
     *
     * @param expression The expression to compile
     * @param context The compilation context with type info and settings
     * @param initialTraversal Optional initial traversal to build upon.
     *                         Pass to the leftmost sub-expression only.
     * @return The compilation result containing the GraphTraversal
     * @throws CompilationException If the expression cannot be compiled
     */
    fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>? = null
    ): TraversalCompilationResult<*, *>
}
