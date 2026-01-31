package com.mdeo.modeltransformation.compiler

import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Utility functions for working with GraphTraversals in the expression compiler.
 *
 * This object provides helper methods for common traversal operations,
 * ensuring consistent behavior across the compiler infrastructure.
 */
object TraversalUtils {

    /**
     * Creates an identity traversal.
     *
     * Returns a traversal that passes through its input unchanged.
     * This is the default starting point when no initial traversal is provided.
     *
     * @return An identity traversal `__.identity()`
     */
    @Suppress("UNCHECKED_CAST")
    fun <S> identity(): GraphTraversal<S, S> {
        return AnonymousTraversal.identity<S>()
    }

    /**
     * Creates a constant traversal.
     *
     * Returns a traversal that ignores its input and produces the given constant value.
     *
     * @param value The constant value to produce
     * @return A constant traversal `__.constant(value)`
     */
    @Suppress("UNCHECKED_CAST")
    fun <S, E> constant(value: E): GraphTraversal<S, E> {
        return AnonymousTraversal.constant(value) as GraphTraversal<S, E>
    }

    /**
     * Creates a traversal starting with `__.as()` for match contexts.
     *
     * In Gremlin's match() clause, traversals need to start with `__.as()` to
     * bind to a variable. This method creates such a traversal.
     *
     * @param label The label to bind to
     * @return A traversal starting with `__.as(label)`
     */
    @Suppress("UNCHECKED_CAST")
    fun <S> asLabel(label: String): GraphTraversal<S, S> {
        return AnonymousTraversal.`as`<S>(label)
    }

    /**
     * Gets the starting traversal based on context.
     *
     * If an initial traversal is provided, returns it. Otherwise, returns
     * an identity traversal.
     *
     * @param initialTraversal Optional initial traversal
     * @return The initial traversal or identity
     */
    @Suppress("UNCHECKED_CAST")
    fun <S> getStartingTraversal(initialTraversal: GraphTraversal<*, *>?): GraphTraversal<S, S> {
        return if (initialTraversal != null) {
            initialTraversal as GraphTraversal<S, S>
        } else {
            identity()
        }
    }

    /**
     * Creates a traversal that produces a constant, optionally building on an initial traversal.
     *
     * This is the preferred way to create constant traversals when an initial
     * traversal might be present (e.g., in match contexts).
     *
     * @param value The constant value
     * @param initialTraversal Optional initial traversal to build upon
     * @return A traversal that produces the constant value
     */
    @Suppress("UNCHECKED_CAST")
    fun <S, E> constantWithInitial(value: E, initialTraversal: GraphTraversal<*, *>?): GraphTraversal<S, E> {
        return if (initialTraversal != null) {
            (initialTraversal as GraphTraversal<S, *>).constant(value) as GraphTraversal<S, E>
        } else {
            constant(value)
        }
    }

    /**
     * Wraps a boolean-producing traversal in a choose step for predicate contexts.
     *
     * This converts a traversal that produces a boolean value into one that
     * can be used with Gremlin's choose() step for conditional logic.
     *
     * @param booleanTraversal A traversal that produces boolean values
     * @param ifTrue The value/traversal to use when true
     * @param ifFalse The value/traversal to use when false
     * @return A choose traversal based on the boolean result
     */
    @Suppress("UNCHECKED_CAST")
    fun <S, E> choose(
        booleanTraversal: GraphTraversal<S, Boolean>,
        ifTrue: E,
        ifFalse: E
    ): GraphTraversal<S, E> {
        return booleanTraversal.choose(
            AnonymousTraversal.`is`(P.eq(true)),
            AnonymousTraversal.constant(ifTrue),
            AnonymousTraversal.constant(ifFalse)
        ) as GraphTraversal<S, E>
    }

    /**
     * Creates a select traversal for referencing a bound variable.
     *
     * In Gremlin, previously bound elements can be referenced using select().
     * This is used for accessing match-defined variables.
     *
     * @param label The label to select
     * @return A select traversal `__.select(label)`
     */
    @Suppress("UNCHECKED_CAST")
    fun <S, E> select(label: String): GraphTraversal<S, E> {
        return AnonymousTraversal.select<S, E>(label)
    }
}
