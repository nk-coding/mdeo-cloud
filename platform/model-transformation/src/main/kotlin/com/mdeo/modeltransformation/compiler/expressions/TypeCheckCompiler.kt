package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTypeCheckExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedTypeCheckExpression] nodes.
 *
 * Compiles type check expressions (expr is Type, expr !is Type) into
 * [GremlinCompilationResult] containing GraphTraversals that check the type
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
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val checkExpr = expression as TypedTypeCheckExpression
        return compileTypeCheck(checkExpr, context, initialTraversal)
    }

    /**
     * Compiles a type check expression.
     *
     * Resolves the target type from the type check expression and delegates to the
     * appropriate compilation strategy based on whether it's a vertex type (ClassTypeRef)
     * or other type. Vertex types use hasLabel() checking, while other types use
     * static type analysis.
     *
     * @param expr The type check expression to compile (is or !is)
     * @param context The compilation context for type resolution
     * @param initialTraversal Optional initial traversal to build upon (passed to inner expression)
     * @return The compiled traversal result producing a boolean type check result
     */
    private fun compileTypeCheck(
        expr: TypedTypeCheckExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
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
     * Uses choose() with hasLabel() to determine if the graph element has the expected
     * label. The hasLabel() predicate is used in an anonymous traversal within choose()
     * to produce a boolean result. For negated checks (!is), the true/false values
     * are swapped.
     *
     * @param innerResult The compiled traversal result for the expression being type-checked
     * @param checkType The class type being checked against
     * @param isNegated true if this is a negated type check (!is), false for normal check (is)
     * @return The compiled traversal result producing a boolean type check result
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileVertexTypeCheck(
        innerResult: GremlinCompilationResult,
        checkType: ClassTypeRef,
        isNegated: Boolean
    ): GremlinCompilationResult {
        // checkType.type is already the simple type name (e.g., "Person", not "class/path.Person")
        val typeName = checkType.type
        val (trueValue, falseValue) = if (isNegated) Pair(false, true) else Pair(true, false)

        val traversal = (innerResult.traversal as GraphTraversal<Any, Any>).choose(
            AnonymousTraversal.hasLabel<Any>(typeName),
            AnonymousTraversal.constant(trueValue),
            AnonymousTraversal.constant(falseValue)
        )

        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Compiles a type check for non-vertex types.
     *
     * For primitive and other types (non-ClassTypeRef), performs compile-time type
     * analysis where possible. Currently returns a constant result (true for is,
     * false for !is) since these types don't have runtime type checking in Gremlin.
     * This could be enhanced for more precise compile-time type analysis.
     *
     * @param innerResult The compiled traversal result for the expression being type-checked
     * @param checkType The value type being checked against (non-ClassTypeRef)
     * @param isNegated true if this is a negated type check (!is), false for normal check (is)
     * @return The compiled traversal result producing a constant boolean result
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileGenericTypeCheck(
        innerResult: GremlinCompilationResult,
        checkType: ValueType,
        isNegated: Boolean
    ): GremlinCompilationResult {
        val result = if (isNegated) false else true
        val baseTraversal = innerResult.traversal as GraphTraversal<Any, Any>?
        return GremlinCompilationResult.constant<Any, Boolean>(result, baseTraversal)
    }
}
