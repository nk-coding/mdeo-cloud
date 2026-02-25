package com.mdeo.modeltransformation.stdlib

import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
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
    /**
     * Compiles the method call with a lambda argument.
     *
     * This method handles the special compilation logic for lambda-based operations,
     * allowing the lambda body to be directly integrated into the Gremlin traversal.
     *
     * @param receiver The receiver traversal that this method is called on
     * @param lambda The typed lambda expression to be compiled
     * @param context The compilation context containing variable bindings and scope information
     * @param compilerRegistry The registry used to compile the lambda body expression
     * @return The compiled Gremlin traversal result
     */
    fun compileWithLambda(
        receiver: GraphTraversal<*, *>,
        lambda: TypedLambdaExpression,
        context: CompilationContext,
        compilerRegistry: ExpressionCompilerRegistry
    ): CompilationResult
}

/**
 * Builder extension for defining lambda methods inline.
 *
 * This extension function allows lambda methods to be defined directly in the type definition
 * builder using a concise syntax. The compiler function receives the receiver traversal,
 * lambda expression, compilation context, and compiler registry.
 *
 * @param name The name of the lambda method
 * @param compiler The compilation function that implements the method behavior
 * @return The builder for method chaining
 */
fun GremlinTypeDefinitionBuilder.lambdaMethod(
    name: String,
    compiler: (
        receiver: GraphTraversal<*, *>,
        lambda: TypedLambdaExpression,
        context: CompilationContext,
        compilerRegistry: ExpressionCompilerRegistry
    ) -> CompilationResult
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
        ): CompilationResult = compiler(receiver, lambda, context, compilerRegistry)

        override fun compile(
            receiver: GraphTraversal<*, *>,
            arguments: List<CompilationResult>
        ): CompilationResult {
            throw UnsupportedOperationException("Lambda method '$name' must be compiled via compileWithLambda()")
        }
    })
}

/**
 * Helper to create a lambda context with the parameter bound.
 * 
 * Creates a child scope at the next scope level (parent scope index + 1) with the
 * specified parameter name bound to a step label. This is used when compiling lambda
 * bodies to make the lambda parameter accessible within the lambda's scope.
 * 
 * @param paramName The name of the lambda parameter to bind
 * @param stepLabel The unique step label to use for the parameter binding in the traversal
 * @return A new compilation context with the child scope configured
 */
fun CompilationContext.withLambdaParam(paramName: String, stepLabel: String): CompilationContext {
    val childScopeIndex = this.currentScope.scopeIndex + 1
    return this.withChildScope(childScopeIndex, mapOf(paramName to VariableBinding.LabelBinding(stepLabel)))
}
