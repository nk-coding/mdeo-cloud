package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import com.mdeo.modeltransformation.stdlib.lambdaMethod
import com.mdeo.modeltransformation.stdlib.withLambdaParam
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Creates the ReadonlyCollection type definition.
 *
 * This is the base collection type with all methods implemented as pure
 * Gremlin traversals. Lambda methods are defined inline using the lambdaMethod builder.
 */
@Suppress("UNCHECKED_CAST")
fun createReadonlyCollectionType(): GremlinTypeDefinition {
    return gremlinType("collection.readonly")
        .extends("builtin.any")
        // Size property
        .property("size") { receiver ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().count()
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // isEmpty()
        .method("isEmpty", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().count().`is`(0L)
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // notEmpty()
        .method("notEmpty", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().count().`is`(P.gt(0L))
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // sum()
        .method("sum", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().sum<Number>()
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // first()
        .method("first", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().limit(1)
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // last()
        .method("last", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().tail(1)
            TraversalCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // Lambda methods - defined inline
        .lambdaMethod("filter") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded else unfolded.filter(AnonymousTraversal.constant(false))
            } else {
                unfolded.filter(predResult.traversal)
            }
            TraversalCompilationResult.of(filtered.fold() as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("map") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val mapResult = registry.compile(lambda.body, lambdaContext, null)
            val mapped = if (mapResult.isConstant) {
                unfolded.map(AnonymousTraversal.constant(mapResult.constantValue))
            } else {
                unfolded.map(mapResult.traversal)
            }
            TraversalCompilationResult.of(mapped.fold() as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("exists") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded else unfolded.filter(AnonymousTraversal.constant(false))
            } else {
                unfolded.filter(predResult.traversal)
            }
            TraversalCompilationResult.of(filtered.count().`is`(P.gt(0L)) as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("all") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val notMatching = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded.filter(AnonymousTraversal.constant(false)) else unfolded
            } else {
                unfolded.not(predResult.traversal)
            }
            TraversalCompilationResult.of(notMatching.count().`is`(0L) as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("none") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded else unfolded.filter(AnonymousTraversal.constant(false))
            } else {
                unfolded.filter(predResult.traversal)
            }
            TraversalCompilationResult.of(filtered.count().`is`(0L) as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("one") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded else unfolded.filter(AnonymousTraversal.constant(false))
            } else {
                unfolded.filter(predResult.traversal)
            }
            TraversalCompilationResult.of(filtered.count().`is`(1L) as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("find") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded else unfolded.filter(AnonymousTraversal.constant(false))
            } else {
                unfolded.filter(predResult.traversal)
            }
            TraversalCompilationResult.of(filtered.limit(1) as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("reject") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val rejected = if (predResult.isConstant) {
                if (predResult.constantValue == true) unfolded.filter(AnonymousTraversal.constant(false)) else unfolded
            } else {
                unfolded.not(predResult.traversal)
            }
            TraversalCompilationResult.of(rejected.fold() as GraphTraversal<Any, Any>)
        }
        .build()
}
