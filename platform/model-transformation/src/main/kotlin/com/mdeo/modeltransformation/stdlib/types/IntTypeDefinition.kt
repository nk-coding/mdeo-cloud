package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Creates the Int type definition.
 *
 * Integer methods are implemented using Gremlin's `math()` step for pure
 * traversal-based computation. All math operations work on the current
 * traverser value.
 *
 * ## Supported Operations
 * - `abs()` - Absolute value
 * - `floor()`, `ceiling()`, `round()` - Identity for integers
 * - `log()`, `log10()` - Natural and base-10 logarithms
 * - `pow(exp)` - Exponentiation
 * - `mod(divisor)` - Modulo operation
 * - `max(other)`, `min(other)` - Comparison operations
 */
@Suppress("UNCHECKED_CAST")
fun createIntType(): GremlinTypeDefinition {
    return gremlinType("builtin.int")
        .extends("builtin.any")
        .method("abs", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("abs(_)")
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("floor", "", 0) { receiver, _ ->
            // Floor is identity for integers
            TraversalCompilationResult.of(receiver as GraphTraversal<Any, Any>)
        }
        .method("ceiling", "", 0) { receiver, _ ->
            // Ceiling is identity for integers
            TraversalCompilationResult.of(receiver as GraphTraversal<Any, Any>)
        }
        .method("round", "", 0) { receiver, _ ->
            // Round is identity for integers
            TraversalCompilationResult.of(receiver as GraphTraversal<Any, Any>)
        }
        .method("log", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("log(_)")
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("log10", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("log10(_)")
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("pow", "", 1) { receiver, args ->
            val expValue = args.firstOrNull()?.constantValue
            val expr = if (expValue != null) {
                "_ ^ $expValue"
            } else {
                // If not constant, we need to use by() modulation
                "_ ^ exp"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (expValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("mod", "", 1) { receiver, args ->
            val divisorValue = args.firstOrNull()?.constantValue
            val expr = if (divisorValue != null) {
                "_ % $divisorValue"
            } else {
                "_ % divisor"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (divisorValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.int", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val expr = if (otherValue != null) {
                "max(_, $otherValue)"
            } else {
                "max(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.long", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("max(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.float", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("max(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.double", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("max(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.int", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("min(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.long", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("min(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.float", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("min(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.double", 1) { receiver, args ->
            val otherValue = args.firstOrNull()?.constantValue
            val traversal = (receiver as GraphTraversal<Any, Any>).math("min(_, ${otherValue ?: "other"})")
            val result = if (otherValue == null && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            TraversalCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .build()
}
