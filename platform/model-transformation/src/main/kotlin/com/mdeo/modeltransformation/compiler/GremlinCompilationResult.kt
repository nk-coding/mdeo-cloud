package com.mdeo.modeltransformation.compiler

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Represents the result of compiling a typed expression to a Gremlin traversal.
 *
 * The compiler can produce different types of results depending on how the
 * expression will be used:
 * - [FilterResult]: Used when the expression should filter/match existing graph elements
 * - [ValueResult]: Used when the expression should produce a value (for creating nodes or comparisons)
 * - [TraversalResult]: Used when the expression produces a complete traversal step
 *
 * This sealed interface enables type-safe handling of compilation results and
 * supports the dual-mode compilation required for graph transformations.
 */
sealed interface GremlinCompilationResult {
    
    /**
     * Result type for expressions compiled in filter mode.
     *
     * Filter mode generates Gremlin steps that filter or match existing graph elements.
     * This is used when expressions appear in pattern matching contexts, such as
     * conditions that must be satisfied by existing nodes.
     *
     * @param T The type of graph element being filtered (Vertex, Edge, etc.)
     * @param S The type of value being compared
     * @param filter A function that takes a traversal and applies filter steps to it.
     *               The function receives the current traversal and returns a modified
     *               traversal with the filter conditions applied.
     */
    data class FilterResult<T, S>(
        val filter: (GraphTraversal<T, S>) -> GraphTraversal<T, S>
    ) : GremlinCompilationResult
    
    /**
     * Result type for expressions compiled in value mode.
     *
     * Value mode produces a constant value that can be used directly in Gremlin
     * operations. This is used when expressions appear in contexts where a value
     * is needed, such as property assignments or literal comparisons.
     *
     * @param value The compiled value. Can be any type that Gremlin supports
     *              (primitives, strings, lists, etc.)
     */
    data class ValueResult(
        val value: Any?
    ) : GremlinCompilationResult
    
    /**
     * Result type for expressions that compile to traversal steps.
     *
     * Traversal mode generates Gremlin steps that transform or navigate the graph.
     * This is used for more complex expressions that require graph operations
     * beyond simple filtering or value production.
     *
     * @param T The input type of the traversal
     * @param S The output type of the traversal
     * @param step A function that takes a traversal and applies transformation steps to it.
     *             The function receives the current traversal and returns a modified
     *             traversal with the transformation applied.
     */
    data class TraversalResult<T, S>(
        val step: (GraphTraversal<T, *>) -> GraphTraversal<T, S>
    ) : GremlinCompilationResult
}
