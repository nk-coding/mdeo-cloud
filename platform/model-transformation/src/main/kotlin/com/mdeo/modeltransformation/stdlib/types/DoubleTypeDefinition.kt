package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Creates the Double type definition.
 *
 * Double methods are implemented using Gremlin's `math()` step for pure
 * traversal-based computation. Same operations as Float.
 */
@Suppress("UNCHECKED_CAST")
fun createDoubleType(): GremlinTypeDefinition {
    return gremlinType("builtin.double")
        .extends("builtin.any")
        .method("abs", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("abs(_)")
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("floor", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("floor(_)")
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("ceiling", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("ceil(_)")
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("round", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("floor(_ + 0.5)")
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("log", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("log(_)")
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("log10", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).math("log10(_)")
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        .method("pow", "", 1) { receiver, args ->
            val expValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && expValue != null) {
                "_ ^ $expValue"
            } else {
                "_ ^ exp"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.int", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "max(_, $otherValue)"
            } else {
                "max(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.long", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "max(_, $otherValue)"
            } else {
                "max(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.float", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "max(_, $otherValue)"
            } else {
                "max(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("max", "builtin.double", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "max(_, $otherValue)"
            } else {
                "max(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.int", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "min(_, $otherValue)"
            } else {
                "min(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.long", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "min(_, $otherValue)"
            } else {
                "min(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.float", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "min(_, $otherValue)"
            } else {
                "min(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .method("min", "builtin.double", 1) { receiver, args ->
            val otherValue = (args.firstOrNull() as? GremlinCompilationResult.ValueResult)?.value
            val expr = if (args.firstOrNull() is GremlinCompilationResult.ValueResult == true && otherValue != null) {
                "min(_, $otherValue)"
            } else {
                "min(_, other)"
            }
            val traversal = (receiver as GraphTraversal<Any, Any>).math(expr)
            val result = if (args.firstOrNull() is GremlinCompilationResult.ValueResult != true && args.isNotEmpty()) {
                traversal.by(args.first().traversal)
            } else {
                traversal
            }
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .build()
}
