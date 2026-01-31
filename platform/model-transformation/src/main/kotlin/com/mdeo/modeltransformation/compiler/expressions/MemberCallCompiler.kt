package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.GenericTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
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
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
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
     * @param expression The expression to get the type from.
     * @param context The compilation context.
     * @return The type name string.
     */
    private fun getTypeName(expression: TypedExpression, context: TraversalCompilationContext): String {
        val type = context.resolveTypeOrNull(expression.evalType)
        return when (type) {
            is ClassTypeRef -> mapTypeName(type.type)
            is GenericTypeRef -> "builtin.any"
            is LambdaType -> "builtin.any"
            null -> "builtin.any"
            else -> "builtin.any"
        }
    }

    /**
     * Maps AST type names to registry type names.
     *
     * The AST uses type names like "builtin.List" while the registry
     * uses names like "collection.list".
     */
    private fun mapTypeName(typeName: String): String {
        return when {
            typeName.startsWith("builtin.List") -> "collection.list"
            typeName.startsWith("builtin.Set") -> "collection.set"
            typeName.startsWith("builtin.OrderedSet") -> "collection.ordered-set"
            typeName.startsWith("builtin.Bag") -> "collection.bag"
            typeName.startsWith("builtin.Collection") -> "collection.collection"
            typeName.startsWith("builtin.ReadonlyCollection") -> "collection.readonly"
            typeName.startsWith("builtin.ReadonlyOrderedCollection") -> "collection.readonly-ordered"
            else -> typeName
        }
    }

    /**
     * Gets the overload key from method arguments.
     *
     * For lambda methods, the overload key is "lambda".
     * For other methods, it's based on argument types.
     *
     * @param arguments The method arguments.
     * @param context The compilation context.
     * @return The overload key string.
     */
    private fun getOverloadKey(
        arguments: List<TypedExpression>,
        context: TraversalCompilationContext
    ): String {
        if (arguments.isEmpty()) return ""
        
        val firstArg = arguments.first()
        
        // Lambda methods have overload key "lambda"
        if (firstArg is TypedLambdaExpression) {
            return "lambda"
        }
        
        // For other methods, use the first argument's type
        val argType = context.resolveTypeOrNull(firstArg.evalType)
        return when (argType) {
            is ClassTypeRef -> mapTypeName(argType.type)
            is GenericTypeRef -> ""
            is LambdaType -> "lambda"
            null -> ""
            else -> ""
        }
    }
}
