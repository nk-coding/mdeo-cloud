package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedUnaryExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedUnaryExpression] nodes.
 *
 * Compiles unary expressions into [CompilationResult] containing GraphTraversals
 * that implement negation and logical not operators using pure Gremlin.
 *
 * ## Supported Operators
 *
 * ### Negation Operator
 * - `-` - Numeric negation (uses math step with "0 - _")
 *
 * ### Unary Plus Operator
 * - `+` - Unary plus (identity, no-op for numeric values)
 *
 * ### Logical Not Operator
 * - `!` - Logical not (uses choose with P.eq(true) to invert boolean)
 *
 * ## Initial Traversal Propagation
 * The [initialTraversal] is passed to the inner expression since there is only
 * one sub-expression in a unary operation.
 *
 * ## Portability
 * All operators are implemented using pure Gremlin (no lambdas) for maximum
 * portability across different graph databases.
 *
 * @param registry The traversal compiler registry for compiling the inner expression
 */
class UnaryOperatorCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    companion object {
        /**
         * Logical NOT operator 
         */
        const val OPERATOR_NOT = "!"

        /**
         * Negation operator 
         */
        const val OPERATOR_MINUS = "-"

        /**
         * Unary plus operator 
         */
        const val OPERATOR_PLUS = "+"

        /**
         * All supported operators 
         */
        private val SUPPORTED_OPERATORS = setOf(OPERATOR_NOT, OPERATOR_MINUS, OPERATOR_PLUS)
    }

    /**
     * Determines whether this compiler can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if the expression is a [TypedUnaryExpression] with a supported operator
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        if (expression !is TypedUnaryExpression) return false
        return expression.operator in SUPPORTED_OPERATORS
    }

    /**
     * Compiles a unary expression into a Gremlin traversal.
     *
     * This method dispatches to the appropriate compilation method based on the operator:
     * - `-`: Numeric negation using math("0 - _")
     * - `+`: Unary plus (identity/no-op)
     * - `!`: Logical not using choose()
     *
     * @param expression The unary expression to compile
     * @param context The compilation context
     * @param initialTraversal Optional initial traversal to build upon
     * @return A [CompilationResult] containing the compiled unary operation
     * @throws CompilationException if the operator is not supported
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val unaryExpr = expression as TypedUnaryExpression
        val operator = unaryExpr.operator

        if (operator !in SUPPORTED_OPERATORS) {
            throw CompilationException.unsupportedOperator(operator, expression)
        }

        return when (operator) {
            OPERATOR_MINUS -> compileNegation(unaryExpr, context, initialTraversal)
            OPERATOR_PLUS -> compileUnaryPlus(unaryExpr, context, initialTraversal)
            OPERATOR_NOT -> compileLogicalNot(unaryExpr, context, initialTraversal)
            else -> throw CompilationException.unsupportedOperator(operator, expression)
        }
    }

    /**
     * Compiles a numeric negation using Gremlin's math step.
     *
     * Uses the formula "0 - _" to negate the value where "_" refers to the current value.
     * This provides a pure Gremlin implementation without requiring lambdas.
     *
     * @param expr The unary expression containing the value to negate
     * @param context The compilation context
     * @param initialTraversal Optional initial traversal to build upon
     * @return A [CompilationResult] with the negation traversal
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileNegation(
        expr: TypedUnaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val innerResult = registry.compile(expr.expression, context, initialTraversal)

        val traversal = (innerResult.traversal as GraphTraversal<Any, Any>)
            .math("0 - _")

        return CompilationResult.of(traversal)
    }

    /**
     * Compiles unary plus (identity) operation.
     *
     * Unary plus is a no-op but validates that the operand is numeric.
     * The inner expression is simply compiled and returned as-is.
     *
     * @param expr The unary expression with unary plus operator
     * @param context The compilation context
     * @param initialTraversal Optional initial traversal to build upon
     * @return A [CompilationResult] with the inner expression unchanged
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileUnaryPlus(
        expr: TypedUnaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        return registry.compile(expr.expression, context, initialTraversal)
    }

    /**
     * Compiles logical not using Gremlin's choose step.
     *
     * Uses choose with P.eq(true) to invert boolean values:
     * - If input is true, return false
     * - If input is false (or not true), return true
     *
     * @param expr The unary expression with logical not operator
     * @param context The compilation context
     * @param initialTraversal Optional initial traversal to build upon
     * @return A [CompilationResult] with the boolean inversion traversal
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileLogicalNot(
        expr: TypedUnaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): CompilationResult {
        val innerResult = registry.compile(expr.expression, context, initialTraversal)

        val traversal = buildNotTraversal(innerResult.traversal as GraphTraversal<Any, Any>)
        return CompilationResult.of(traversal)
    }

    /**
     * Builds a traversal that inverts a boolean value using choose.
     *
     * Creates a choose step that checks if the value equals true:
     * - If true: returns constant(false)
     * - Otherwise: returns constant(true)
     *
     * @param innerTraversal The traversal producing the boolean value to invert
     * @return A [GraphTraversal] that inverts the boolean value
     */
    private fun buildNotTraversal(
        innerTraversal: GraphTraversal<Any, Any>
    ): GraphTraversal<Any, Boolean> {
        return innerTraversal.choose(
            AnonymousTraversal.`is`(P.eq(true)),
            AnonymousTraversal.constant(false),
            AnonymousTraversal.constant(true)
        )
    }
}
