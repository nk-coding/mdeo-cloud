package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Traversal-based compiler for [TypedMemberAccessExpression] nodes.
 *
 * Compiles member access expressions (e.g., `object.property`) into [GremlinCompilationResult]
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
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val memberAccess = expression as TypedMemberAccessExpression
        val objectResult = compileObjectExpression(memberAccess, context, initialTraversal)
        
        val receiverType = getTypeName(memberAccess.expression, context)
            ?: throw IllegalStateException(
                "Cannot determine type for member access expression: ${memberAccess.member}. " +
                "Type at index ${memberAccess.expression.evalType} is not a ClassTypeRef."
            )
        
        return compilePropertyWithRegistry(objectResult, receiverType, memberAccess.member, context)
    }

    /**
     * Compiles the target object expression for member access.
     *
     * Passes the initialTraversal to the object expression since it's the leftmost
     * part of the member access chain. This ensures proper traversal construction when
     * the member access is used in match contexts.
     *
     * @param memberAccess The member access expression containing the target object
     * @param context The compilation context with type information and registry
     * @param initialTraversal Optional traversal to build upon (passed to the object expression)
     * @return The compiled traversal result for the object expression
     */
    private fun compileObjectExpression(
        memberAccess: TypedMemberAccessExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        return registry.compile(memberAccess.expression, context, initialTraversal)
    }

    /**
     * Gets the type name for an expression from the compilation context.
     *
     * Resolves the expression's evalType index to a ValueType and extracts the
     * type name if it's a ClassTypeRef. Returns null for non-class types.
     *
     * @param expression The expression to get the type name from
     * @param context The compilation context containing type resolution information
     * @return The type name string if the expression is a ClassTypeRef, null otherwise
     */
    private fun getTypeName(expression: TypedExpression, context: CompilationContext): ClassTypeRef? {
        val evalType = expression.evalType
        val valueType = context.resolveTypeOrNull(evalType)
        return when (valueType) {
            is ClassTypeRef -> valueType
            else -> null
        }
    }

    /**
     * Compiles property access using the GremlinTypeRegistry.
     *
     * Looks up the property in the type registry and uses the property definition's
     * compile method to generate the appropriate Gremlin traversal for accessing
     * the property or navigating the association.
     *
     * @param objectResult The compiled traversal result for the object expression
     * @param typeName The name of the type containing the property
     * @param propertyName The name of the property or association to access
     * @param context The compilation context with type registry
     * @return The compiled traversal result for the property access
     * @throws IllegalStateException if the property is not found in the type registry
     */
    @Suppress("UNCHECKED_CAST")
    private fun compilePropertyWithRegistry(
        objectResult: GremlinCompilationResult,
        typeRef: ClassTypeRef,
        propertyName: String,
        context: CompilationContext
    ): GremlinCompilationResult {
        val propertyDef = context.typeRegistry.lookupProperty(typeRef, propertyName)
            ?: throw IllegalStateException(
                "Property '$propertyName' not found on type '${typeRef.`package`}.${typeRef.type}'. " +
                "Ensure the type registry is configured with all metamodel types."
            )
        
        return propertyDef.compile(objectResult.traversal)
    }
}
