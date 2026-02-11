package com.mdeo.modeltransformation.compiler

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Represents the result of compiling a typed expression to a Gremlin traversal.
 *
 * The compiler can produce different types of results depending on how the
 * expression will be used:
 * - [TraversalResult]: Used when the expression produces a complete traversal
 * - [ValueResult]: Used when the expression produces a known constant primitive value
 *
 * This sealed interface enables type-safe handling of compilation results and
 * supports optimizations for constant values.
 *
 * All result types provide a [traversal] property containing the compiled GraphTraversal.
 */
sealed interface GremlinCompilationResult {
    
    /**
     * The compiled GraphTraversal representing this expression.
     */
    val traversal: GraphTraversal<*, *>
    
    /**
     * Result type for expressions that compile to traversal steps.
     *
     * This is used for expressions that require graph operations or produce
     * dynamic values that cannot be determined at compile time.
     *
     * @param traversal The compiled GraphTraversal
     */
    data class TraversalResult(
        override val traversal: GraphTraversal<*, *>
    ) : GremlinCompilationResult
    
    /**
     * Result type for expressions that produce a known constant primitive value.
     *
     * ValueResult is used when the compiler knows the expression evaluates to a
     * constant primitive value (string, number, boolean, null). This allows
     * for optimizations like inlining constants in property assignments or
     * Gremlin math expressions.
     *
     * IMPORTANT: Only primitive-like values may be stored in [value]:
     * - null
     * - String
     * - Number (Int, Long, Float, Double, etc.)
     * - Boolean
     * - Char
     *
     * Collections (List, Set, Map, etc.) are NOT allowed and will cause an exception.
     *
     * @param traversal The compiled GraphTraversal that produces the constant
     * @param value The known constant value (must be a primitive type or null)
     * @throws IllegalArgumentException if value is a collection type
     */
    data class ValueResult(
        override val traversal: GraphTraversal<*, *>,
        val value: Any?
    ) : GremlinCompilationResult {
        init {
            if (value != null) {
                val isAllowedType = value is String ||
                    value is Number ||
                    value is Boolean ||
                    value is Char
                
                if (!isAllowedType) {
                    throw IllegalArgumentException(
                        "ValueResult can only hold primitive values (String, Number, Boolean, Char, null). " +
                        "Got ${value::class.simpleName}: $value. " +
                        "Use TraversalResult for complex values or collections."
                    )
                }
            }
        }
    }
    
    companion object {
        /**
         * Creates a result for a constant value.
         *
         * @param value The constant value
         * @param initialTraversal Optional initial traversal to prepend
         * @return A [GremlinCompilationResult.ValueResult] that produces the constant value
         */
        @Suppress("UNCHECKED_CAST")
        fun <S, E> constant(value: E, initialTraversal: GraphTraversal<S, *>? = null): GremlinCompilationResult {
            val traversal = (initialTraversal ?: 
                org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__`.identity<Any>())
                .constant(value) as GraphTraversal<Any, Any>
            
            return ValueResult(
                traversal = traversal,
                value = value
            )
        }

        /**
         * Creates a result from an existing traversal.
         *
         * @param traversal The traversal to wrap
         * @return A [GremlinCompilationResult.TraversalResult] wrapping the traversal
         */
        fun <S, E> of(traversal: GraphTraversal<S, E>): GremlinCompilationResult {
            return TraversalResult(traversal = traversal)
        }

        /**
         * Creates an identity result that passes through the input unchanged.
         *
         * @param initialTraversal Optional initial traversal to use instead
         * @return A [GremlinCompilationResult] that passes through input
         */
        @Suppress("UNCHECKED_CAST")
        fun <S> identity(initialTraversal: GraphTraversal<S, *>? = null): GremlinCompilationResult {
            val traversal = initialTraversal ?: 
                org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__`.identity<Any>()
            
            return TraversalResult(traversal = traversal as GraphTraversal<Any, Any>)
        }
    }
}
