package com.mdeo.modeltransformation.stdlib.types

import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import com.mdeo.modeltransformation.stdlib.lambdaMethod
import com.mdeo.modeltransformation.stdlib.withLambdaParam
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Creates the Collection type definition.
 *
 * This is the base collection type with all methods implemented as pure
 * Gremlin traversals. Lambda methods are defined inline using the lambdaMethod builder.
 */
@Suppress("UNCHECKED_CAST")
fun createCollectionType(): GremlinTypeDefinition {
    return gremlinType("builtin.Collection")
        .extends("builtin.any")
        .cardinality(VertexProperty.Cardinality.list)
        // size()
        .method("size", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().count()
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // isEmpty()
        .method("isEmpty", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().count().`is`(0L)
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // notEmpty()
        .method("notEmpty", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().count().`is`(P.gt(0L))
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // sum()
        .method("sum", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().sum<Number>()
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // first()
        .method("first", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().limit(1)
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // last()
        .method("last", "", 0) { receiver, _ ->
            val traversal = (receiver as GraphTraversal<Any, Any>).unfold<Any>().tail(1)
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        // Lambda methods - defined inline
        // Note: All predicate-based methods use .is(true) to convert boolean results to 
        // filter semantics. Gremlin filter() passes elements when traversal produces ANY
        // value (not just true), so we must explicitly check for true.
        // 
        // Collection methods return multiple values (stream semantics) rather than folded lists.
        // This allows natural chaining and easier testing.
        .lambdaMethod("filter") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = unfolded.filter((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            GremlinCompilationResult.of(filtered as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("map") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val mapResult = registry.compile(lambda.body, lambdaContext, null)
            val mapped = unfolded.map(mapResult.traversal)
            GremlinCompilationResult.of(mapped as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("exists") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = unfolded.filter((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            // Use choose() to return true/false instead of filtering
            val result = (filtered.count() as GraphTraversal<Any, Long>).choose(
                AnonymousTraversal.`is`(P.gt(0L)),
                AnonymousTraversal.constant<Any>(true),
                AnonymousTraversal.constant<Any>(false)
            )
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        // "any" is an alias for "exists"
        .lambdaMethod("any") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = unfolded.filter((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            val result = (filtered.count() as GraphTraversal<Any, Long>).choose(
                AnonymousTraversal.`is`(P.gt(0L)),
                AnonymousTraversal.constant<Any>(true),
                AnonymousTraversal.constant<Any>(false)
            )
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("all") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            // not() keeps elements where predicate is NOT true (i.e., false)
            val notMatching = unfolded.not((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            // All pass if count of non-matching is 0
            val result = (notMatching.count() as GraphTraversal<Any, Long>).choose(
                AnonymousTraversal.`is`(P.eq(0L)),
                AnonymousTraversal.constant<Any>(true),
                AnonymousTraversal.constant<Any>(false)
            )
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("none") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = unfolded.filter((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            // None pass if count of matching is 0
            val result = (filtered.count() as GraphTraversal<Any, Long>).choose(
                AnonymousTraversal.`is`(P.eq(0L)),
                AnonymousTraversal.constant<Any>(true),
                AnonymousTraversal.constant<Any>(false)
            )
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("one") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = unfolded.filter((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            // Exactly one if count of matching is 1
            val result = (filtered.count() as GraphTraversal<Any, Long>).choose(
                AnonymousTraversal.`is`(P.eq(1L)),
                AnonymousTraversal.constant<Any>(true),
                AnonymousTraversal.constant<Any>(false)
            )
            GremlinCompilationResult.of(result as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("find") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            val filtered = unfolded.filter((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            GremlinCompilationResult.of(filtered.limit(1) as GraphTraversal<Any, Any>)
        }
        .lambdaMethod("reject") { receiver, lambda, context, registry ->
            val paramName = lambda.parameters.firstOrNull() ?: "it"
            val lambdaContext = context.withLambdaParam(paramName)
            val unfolded = (receiver as GraphTraversal<Any, Any>).unfold<Any>().`as`(paramName)
            val predResult = registry.compile(lambda.body, lambdaContext, null)
            // not() keeps elements where predicate is NOT true (i.e., false)
            val rejected = unfolded.not((predResult.traversal as GraphTraversal<Any, Any>).`is`(true))
            GremlinCompilationResult.of(rejected as GraphTraversal<Any, Any>)
        }
        .build()
}
