package com.mdeo.modeltransformation.stdlib

import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.registry.GremlinMethodDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/** Scope index for lambda parameters */
const val LAMBDA_SCOPE_INDEX = 100

/**
 * Interface for lambda methods that support compilation with TypedLambdaExpression.
 * 
 * Lambda methods handle expressions like `collection.filter(it => pred)` where a lambda
 * is passed as an argument. These are compiled specially by MemberCallCompiler.
 */
interface LambdaMethodDefinition : GremlinMethodDefinition {
    fun compileWithLambda(
        receiver: GraphTraversal<*, *>,
        lambda: TypedLambdaExpression,
        context: TraversalCompilationContext,
        compilerRegistry: ExpressionCompilerRegistry
    ): TraversalCompilationResult<*, *>
}

/**
 * Builder extension for defining lambda methods inline.
 */
fun GremlinTypeDefinitionBuilder.lambdaMethod(
    name: String,
    compiler: (
        receiver: GraphTraversal<*, *>,
        lambda: TypedLambdaExpression,
        context: TraversalCompilationContext,
        compilerRegistry: ExpressionCompilerRegistry
    ) -> TraversalCompilationResult<*, *>
): GremlinTypeDefinitionBuilder {
    return method(object : LambdaMethodDefinition {
        override val name = name
        override val overloadKey = "lambda"
        override val parameterCount = 1

        override fun compileWithLambda(
            receiver: GraphTraversal<*, *>,
            lambda: TypedLambdaExpression,
            context: TraversalCompilationContext,
            compilerRegistry: ExpressionCompilerRegistry
        ): TraversalCompilationResult<*, *> = compiler(receiver, lambda, context, compilerRegistry)

        override fun compile(
            receiver: GraphTraversal<*, *>,
            arguments: List<TraversalCompilationResult<*, *>>
        ): TraversalCompilationResult<*, *> {
            throw UnsupportedOperationException("Lambda method '$name' must be compiled via compileWithLambda()")
        }
    })
}

/**
 * Helper to create a lambda context with the parameter bound.
 */
fun TraversalCompilationContext.withLambdaParam(paramName: String): TraversalCompilationContext {
    val lambdaScope = VariableScope.of(paramName to VariableBinding.TraversalBinding(paramName))
    return this.withScope(LAMBDA_SCOPE_INDEX, lambdaScope)
}
