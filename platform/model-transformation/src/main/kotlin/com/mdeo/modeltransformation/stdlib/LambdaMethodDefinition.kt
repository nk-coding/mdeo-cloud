package com.mdeo.modeltransformation.stdlib

import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.registry.GremlinMethodDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinitionBuilder
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

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
        context: CompilationContext,
        compilerRegistry: ExpressionCompilerRegistry
    ): GremlinCompilationResult
}

/**
 * Builder extension for defining lambda methods inline.
 */
fun GremlinTypeDefinitionBuilder.lambdaMethod(
    name: String,
    compiler: (
        receiver: GraphTraversal<*, *>,
        lambda: TypedLambdaExpression,
        context: CompilationContext,
        compilerRegistry: ExpressionCompilerRegistry
    ) -> GremlinCompilationResult
): GremlinTypeDefinitionBuilder {
    return method(object : LambdaMethodDefinition {
        override val name = name
        override val overloadKey = ""
        override val parameterCount = 1

        override fun compileWithLambda(
            receiver: GraphTraversal<*, *>,
            lambda: TypedLambdaExpression,
            context: CompilationContext,
            compilerRegistry: ExpressionCompilerRegistry
        ): GremlinCompilationResult = compiler(receiver, lambda, context, compilerRegistry)

        override fun compile(
            receiver: GraphTraversal<*, *>,
            arguments: List<GremlinCompilationResult>
        ): GremlinCompilationResult {
            throw UnsupportedOperationException("Lambda method '$name' must be compiled via compileWithLambda()")
        }
    })
}

/**
 * Helper to create a lambda context with the parameter bound.
 * Creates a child scope at the next scope level (parent scope index + 1).
 */
fun CompilationContext.withLambdaParam(paramName: String): CompilationContext {
    val childScopeIndex = this.currentScope.scopeIndex + 1
    return this.withChildScope(childScopeIndex, mapOf(paramName to VariableBinding.InstanceBinding(vertexId = null)))
}
