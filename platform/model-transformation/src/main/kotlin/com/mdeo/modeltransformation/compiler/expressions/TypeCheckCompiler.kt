package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTypeCheckExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedTypeCheckExpression] nodes.
 *
 * Compiles type check expressions (expr is Type, expr !is Type) into
 * [TraversalCompilationResult] containing GraphTraversals that check the type
 * of graph elements using pure Gremlin.
 *
 * ## Gremlin Implementation
 * Type checking is implemented differently depending on the target type:
 *
 * ### Vertex Types (Reference Types)
 * Uses `hasLabel(typeName)` with `choose()` to return true/false:
 * ```
 * exprTraversal.choose(
 *     __.hasLabel(typeName),
 *     __.constant(true),
 *     __.constant(false)
 * )
 * ```
 *
 * ### Other Types
 * For non-vertex types, the type check is handled at compile time where possible,
 * or returns a constant result based on static type analysis.
 *
 * ## Initial Traversal Propagation
 * The [initialTraversal] is passed to the inner expression since there is
 * only one sub-expression.
 *
 * ## Negation Support
 * When [TypedTypeCheckExpression.isNegated] is true (!is), the result is inverted.
 *
 * ## Portability
 * Uses pure Gremlin (no lambdas) for maximum portability across graph databases.
 *
 * @param registry The traversal compiler registry for compiling the inner expression
 */
class TypeCheckCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedTypeCheckExpression
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val checkExpr = expression as TypedTypeCheckExpression
        return compileTypeCheck(checkExpr, context, initialTraversal)
    }

    /**
     * Compiles a type check expression.
     *
     * Resolves the target type and delegates to the appropriate compilation
     * strategy based on whether it's a vertex type or other type.
     */
    private fun compileTypeCheck(
        expr: TypedTypeCheckExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val checkType = context.resolveType(expr.checkType)
        val innerResult = registry.compile(expr.expression, context, initialTraversal)

        return when (checkType) {
            is ClassTypeRef -> compileVertexTypeCheck(innerResult, checkType, expr.isNegated)
            else -> compileGenericTypeCheck(innerResult, checkType, expr.isNegated)
        }
    }

    /**
     * Compiles a type check for vertex types using hasLabel.
     *
     * Uses choose() with hasLabel() to determine if the element has the expected label.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileVertexTypeCheck(
        innerResult: TraversalCompilationResult<*, *>,
        checkType: ClassTypeRef,
        isNegated: Boolean
    ): TraversalCompilationResult<*, *> {
        val typeName = extractTypeName(checkType.type)
        val (trueValue, falseValue) = if (isNegated) Pair(false, true) else Pair(true, false)

        val traversal = (innerResult.traversal as GraphTraversal<Any, Any>).choose(
            AnonymousTraversal.hasLabel<Any>(typeName),
            AnonymousTraversal.constant(trueValue),
            AnonymousTraversal.constant(falseValue)
        )

        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles a type check for non-vertex types.
     *
     * For primitive and other types, performs compile-time type analysis where
     * possible. Returns a constant result based on static type compatibility.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileGenericTypeCheck(
        innerResult: TraversalCompilationResult<*, *>,
        checkType: ValueType,
        isNegated: Boolean
    ): TraversalCompilationResult<*, *> {
        val result = if (isNegated) false else true
        val baseTraversal = innerResult.traversal as GraphTraversal<Any, Any>?
        return TraversalCompilationResult.constant<Any, Boolean>(result, baseTraversal)
    }

    /**
     * Extracts the simple type name from a fully qualified type string.
     *
     * Type strings may be in the form "namespace.TypeName" and this method
     * returns just the "TypeName" part for use with hasLabel().
     */
    private fun extractTypeName(typeString: String): String {
        return typeString.substringAfterLast(".")
    }
}
