package com.mdeo.modeltransformation.compiler

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Represents the result of compiling an expression to a Gremlin traversal.
 *
 * All expressions compile to a [GraphTraversal], ensuring uniformity and composability.
 * The traversal can be used in various contexts:
 * - As a value producer (for property assignments, creating nodes)
 * - As a predicate (for where conditions, match clauses)
 * - As a traversal step (for navigation, filtering)
 *
 * The result also tracks metadata about how the traversal should be used:
 * - [isConstant]: Whether the traversal produces a constant value
 * - [constantValue]: The constant value if [isConstant] is true
 *
 * ## Usage in Match Context
 * When used in a match clause, traversals typically need to start with `__.as()`.
 * The [TraversalCompiler] handles this by providing an initial traversal that
 * already has the necessary prefix.
 *
 * ## Evaluation
 * - As a value: Use [traversal] directly, it produces the expression value
 * - As a predicate: The traversal produces a boolean, or can be used with P.eq()
 *
 * @param S The start type of the traversal
 * @param E The end type (result type) of the traversal
 * @param traversal The compiled GraphTraversal that represents this expression
 * @param isConstant Whether this traversal produces a constant value
 * @param constantValue The constant value, if [isConstant] is true
 */
data class TraversalCompilationResult<S, E>(
    val traversal: GraphTraversal<S, E>,
    val isConstant: Boolean = false,
    val constantValue: Any? = null
) {
    companion object {
        /**
         * Creates a result for a constant value.
         *
         * The traversal uses `__.constant(value)` to produce the value.
         * This allows constants to be used uniformly with other traversals.
         *
         * @param value The constant value
         * @param initialTraversal Optional initial traversal to prepend
         * @return A [TraversalCompilationResult] that produces the constant value
         */
        @Suppress("UNCHECKED_CAST")
        fun <S, E> constant(value: E, initialTraversal: GraphTraversal<S, *>? = null): TraversalCompilationResult<S, E> {
            val traversal: GraphTraversal<S, E> = if (initialTraversal != null) {
                initialTraversal.constant(value) as GraphTraversal<S, E>
            } else {
                AnonymousTraversal.constant(value) as GraphTraversal<S, E>
            }
            return TraversalCompilationResult(
                traversal = traversal,
                isConstant = true,
                constantValue = value
            )
        }

        /**
         * Creates a result from an existing traversal.
         *
         * @param traversal The traversal to wrap
         * @return A [TraversalCompilationResult] wrapping the traversal
         */
        fun <S, E> of(traversal: GraphTraversal<S, E>): TraversalCompilationResult<S, E> {
            return TraversalCompilationResult(traversal = traversal)
        }

        /**
         * Creates an identity result that passes through the input unchanged.
         *
         * Uses `__.identity()` to create a no-op traversal.
         *
         * @param initialTraversal Optional initial traversal to use instead
         * @return A [TraversalCompilationResult] that passes through input
         */
        @Suppress("UNCHECKED_CAST")
        fun <S> identity(initialTraversal: GraphTraversal<S, *>? = null): TraversalCompilationResult<S, S> {
            val traversal = if (initialTraversal != null) {
                initialTraversal as GraphTraversal<S, S>
            } else {
                AnonymousTraversal.identity<S>()
            }
            return TraversalCompilationResult(traversal = traversal)
        }
    }

    /**
     * Maps this result's traversal to a new type.
     *
     * Applies a transformation function to the traversal, producing a new result.
     * This is useful for chaining operations on the traversal.
     *
     * @param R The new end type
     * @param transform The transformation to apply to the traversal
     * @return A new [TraversalCompilationResult] with the transformed traversal
     */
    fun <R> map(transform: (GraphTraversal<S, E>) -> GraphTraversal<S, R>): TraversalCompilationResult<S, R> {
        return TraversalCompilationResult(
            traversal = transform(traversal),
            isConstant = false,
            constantValue = null
        )
    }

    /**
     * Appends additional steps to the traversal.
     *
     * Convenience method for adding steps without changing the constant status.
     *
     * @param R The new end type after appending
     * @param steps The steps to append
     * @return A new [TraversalCompilationResult] with the appended steps
     */
    fun <R> append(steps: (GraphTraversal<S, E>) -> GraphTraversal<S, R>): TraversalCompilationResult<S, R> {
        return TraversalCompilationResult(
            traversal = steps(traversal),
            isConstant = false,
            constantValue = null
        )
    }

    /**
     * Gets the traversal for use in a predicate context.
     *
     * When the expression is used in a where() or filter() context,
     * this returns the traversal that can be used for comparison.
     *
     * @return The traversal suitable for predicate use
     */
    fun asPredicateTraversal(): GraphTraversal<S, E> {
        return traversal
    }

    /**
     * Gets the traversal for use as a value producer.
     *
     * When the expression is used for property assignment or value creation,
     * this returns the traversal that produces the value.
     *
     * @return The traversal suitable for value production
     */
    fun asValueTraversal(): GraphTraversal<S, E> {
        return traversal
    }
}
