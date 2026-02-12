package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.GenericTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import com.mdeo.modeltransformation.stdlib.LambdaMethodDefinition
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal compiler for member call expressions.
 *
 * Member call expressions (`receiver.methodName(args)`) invoke methods on objects.
 * This compiler resolves methods from the type registry and generates appropriate
 * Gremlin traversals.
 *
 * ## Lambda Method Support
 * When the first argument is a [TypedLambdaExpression] and the method is a
 * [LambdaMethodDefinition], the lambda body is compiled as a pure
 * Gremlin traversal with the lambda parameter bound to the current traverser.
 *
 * ## Standard Method Support
 * For non-lambda methods, arguments are compiled and passed to the method's
 * compile function from the type registry.
 *
 * @param registry The traversal compiler registry for compiling sub-expressions.
 * @see TypedMemberCallExpression
 * @see LambdaMethodDefinition
 */
class MemberCallCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedMemberCallExpression
    }

    @Suppress("UNCHECKED_CAST")
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val memberCall = expression as TypedMemberCallExpression
        
        // Compile the receiver expression
        val receiverResult = registry.compile(memberCall.expression, context, initialTraversal)
        
        // Get the receiver type name for method lookup
        val receiverTypeName = getTypeName(memberCall.expression, context)
        
        // Get the overload key from arguments
        val overloadKey = getOverloadKey(memberCall.arguments, context)
        
        // Look up the method in the type registry
        val methodDef = context.typeRegistry.lookupMethod(receiverTypeName, memberCall.member, overloadKey)
            ?: throw IllegalArgumentException(
                "Method '${memberCall.member}' not found on type '$receiverTypeName' " +
                "with overload key '$overloadKey'"
            )
        
        // Check if this is a lambda method with lambda argument
        val firstArg = memberCall.arguments.firstOrNull()
        if (firstArg is TypedLambdaExpression && methodDef is LambdaMethodDefinition) {
            return methodDef.compileWithLambda(
                receiverResult.traversal as GraphTraversal<Any, Any>,
                firstArg,
                context,
                registry
            )
        }
        
        // Standard method: compile all arguments and call method compile
        val argResults = memberCall.arguments.map { arg ->
            registry.compile(arg, context, null)
        }
        
        return methodDef.compile(
            receiverResult.traversal as GraphTraversal<Any, Any>,
            argResults
        )
    }

    /**
     * Gets the type name from an expression for method lookup.
     *
     * Resolves the expression's evalType to determine the type name for looking up
     * methods in the type registry. Returns "builtin.any" for generic types, lambdas,
     * or unresolved types as a fallback.
     *
     * @param expression The expression to get the type from
     * @param context The compilation context containing type resolution information
     * @return The type name string for method lookup ("builtin.any" for unknown types)
     */
    private fun getTypeName(expression: TypedExpression, context: CompilationContext): String {
        val type = context.resolveTypeOrNull(expression.evalType)
        return when (type) {
            is ClassTypeRef -> type.type
            is GenericTypeRef -> "builtin.any"
            is LambdaType -> "builtin.any"
            null -> "builtin.any"
            else -> "builtin.any"
        }
    }

    /**
     * Gets the overload key from method arguments for method lookup.
     *
     * The overload key is used to distinguish between different overloads of the same
     * method name. For lambda methods (first argument is a lambda), returns an empty
     * string. For other methods, returns the type name of the first argument if it's
     * a ClassTypeRef, otherwise returns an empty string.
     *
     * @param arguments The list of method argument expressions
     * @param context The compilation context containing type resolution information
     * @return The overload key string (empty string for lambdas or untyped arguments)
     */
    private fun getOverloadKey(
        arguments: List<TypedExpression>,
        context: CompilationContext
    ): String {
        if (arguments.isEmpty()) return ""
        
        val firstArg = arguments.first()
        
        // Lambda methods have empty string as overload key
        if (firstArg is TypedLambdaExpression) {
            return ""
        }
        
        // For other methods, use the first argument's type
        val argType = context.resolveTypeOrNull(firstArg.evalType)
        return when (argType) {
            is ClassTypeRef -> argType.type
            is GenericTypeRef -> ""
            is LambdaType -> ""
            null -> ""
            else -> ""
        }
    }
}
