package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedMemberAccessExpression] nodes.
 *
 * Compiles member access expressions (e.g., `object.property`) into [TraversalCompilationResult]
 * containing GraphTraversals that navigate to and retrieve property values or traverse edges.
 *
 * ## Property and Association Access
 * Member access expressions are compiled using the [GremlinTypeRegistry]:
 * 1. First, the receiver type is looked up from the expression's evalType
 * 2. Then, the property/association is looked up on that type in the registry
 * 3. The property definition's compile() method is called with the receiver traversal
 *
 * This approach supports:
 * - **Properties**: Compiled to `.values(propertyName)` steps
 * - **Associations**: Compiled to edge traversals (`.out()` or `.in()`)
 *
 * For example:
 * - `person.name` → `.values("name")` (simple property)
 * - `person.address` → `.out("address")` (outgoing association)
 * - `address.residents` → `.in("residents")` (incoming association)
 *
 * ## Error Handling
 * The compiler throws an [IllegalStateException] if:
 * - The receiver type cannot be determined (not a ClassTypeRef)
 * - The property/association is not found in the type registry
 *
 * This ensures that all member access expressions are properly typed and registered.
 *
 * ## Initial Traversal Propagation
 * The [initialTraversal] is passed ONLY to the target object (left) expression.
 * This ensures proper traversal construction when starting from a match-bound variable.
 *
 * ## Null-Safe Chaining
 * When [TypedMemberAccessExpression.isNullChaining] is true, the compiler handles
 * null receivers gracefully by using coalesce patterns. However, for pure traversal
 * mode, null values typically just result in no output from the traversal.
 *
 * ## Pure Gremlin
 * This compiler uses only pure Gremlin steps (no lambdas) for maximum portability.
 *
 * @param registry The traversal compiler registry for compiling sub-expressions
 * @see TypedMemberAccessExpression
 */
class MemberAccessCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedMemberAccessExpression
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val memberAccess = expression as TypedMemberAccessExpression
        val objectResult = compileObjectExpression(memberAccess, context, initialTraversal)
        
        // Get the receiver's type name - throw if not found
        val receiverTypeName = getTypeName(memberAccess.expression, context)
            ?: throw IllegalStateException(
                "Cannot determine type for member access expression: ${memberAccess.member}. " +
                "Type at index ${memberAccess.expression.evalType} is not a ClassTypeRef."
            )
        
        return compilePropertyWithRegistry(objectResult, receiverTypeName, memberAccess.member, context)
    }

    /**
     * Compiles the target object expression.
     *
     * Passes the initialTraversal to the object expression since it's the leftmost
     * part of the member access chain.
     */
    private fun compileObjectExpression(
        memberAccess: TypedMemberAccessExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        return registry.compile(memberAccess.expression, context, initialTraversal)
    }

    /**
     * Gets the type name for an expression from the compilation context.
     */
    private fun getTypeName(expression: TypedExpression, context: TraversalCompilationContext): String? {
        val evalType = expression.evalType
        val valueType = context.resolveTypeOrNull(evalType)
        return when (valueType) {
            is ClassTypeRef -> valueType.type
            else -> null
        }
    }

    /**
     * Compiles property access using the GremlinTypeRegistry.
     *
     * Looks up the property in the type registry and uses the property definition's
     * compile method. Throws an error if the property is not found.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compilePropertyWithRegistry(
        objectResult: TraversalCompilationResult<*, *>,
        typeName: String,
        propertyName: String,
        context: TraversalCompilationContext
    ): TraversalCompilationResult<*, *> {
        val propertyDef = context.typeRegistry.lookupProperty(typeName, propertyName)
            ?: throw IllegalStateException(
                "Property '$propertyName' not found on type '$typeName'. " +
                "Ensure the type registry is configured with all metamodel types."
            )
        
        // Use the property definition to compile the access
        return propertyDef.compile(objectResult.traversal)
    }
}
