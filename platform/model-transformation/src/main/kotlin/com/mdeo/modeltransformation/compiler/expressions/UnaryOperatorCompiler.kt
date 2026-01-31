package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedUnaryExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedUnaryExpression] nodes.
 *
 * Compiles unary expressions into [TraversalCompilationResult] containing GraphTraversals
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
        /** Logical NOT operator */
        const val OPERATOR_NOT = "!"

        /** Negation operator */
        const val OPERATOR_MINUS = "-"

        /** Unary plus operator */
        const val OPERATOR_PLUS = "+"

        /** All supported operators */
        private val SUPPORTED_OPERATORS = setOf(OPERATOR_NOT, OPERATOR_MINUS, OPERATOR_PLUS)
    }

    override fun canCompile(expression: TypedExpression): Boolean {
        if (expression !is TypedUnaryExpression) return false
        return expression.operator in SUPPORTED_OPERATORS
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
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
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileNegation(
        expr: TypedUnaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val innerResult = registry.compile(expr.expression, context, initialTraversal)

        if (innerResult.isConstant) {
            return compileConstantNegation(innerResult, initialTraversal)
        }

        val traversal = (innerResult.traversal as GraphTraversal<Any, Any>)
            .math("0 - _")

        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles constant negation at compile time.
     */
    private fun compileConstantNegation(
        innerResult: TraversalCompilationResult<*, *>,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val value = innerResult.constantValue as Number
        val negated = negateNumber(value)
        return TraversalCompilationResult.constant(negated, initialTraversal)
    }

    /**
     * Negates a number preserving its type.
     */
    private fun negateNumber(value: Number): Number {
        return when (value) {
            is Int -> -value
            is Long -> -value
            is Float -> -value
            is Double -> -value
            is Short -> (-value).toShort()
            is Byte -> (-value).toByte()
            else -> -value.toDouble()
        }
    }

    /**
     * Compiles unary plus (identity) operation.
     *
     * Unary plus is a no-op but validates that the operand is numeric.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileUnaryPlus(
        expr: TypedUnaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val innerResult = registry.compile(expr.expression, context, initialTraversal)

        if (innerResult.isConstant) {
            return TraversalCompilationResult.constant(innerResult.constantValue, initialTraversal)
        }

        return innerResult
    }

    /**
     * Compiles logical not using Gremlin's choose step.
     *
     * Uses choose with P.eq(true) to invert boolean values:
     * - If input is true, return false
     * - If input is false (or not true), return true
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileLogicalNot(
        expr: TypedUnaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val innerResult = registry.compile(expr.expression, context, initialTraversal)

        if (innerResult.isConstant) {
            return compileConstantLogicalNot(innerResult, initialTraversal)
        }

        val traversal = buildNotTraversal(innerResult.traversal as GraphTraversal<Any, Any>)
        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles constant logical not at compile time.
     */
    private fun compileConstantLogicalNot(
        innerResult: TraversalCompilationResult<*, *>,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val value = innerResult.constantValue as Boolean
        return TraversalCompilationResult.constant(!value, initialTraversal)
    }

    /**
     * Builds a traversal that inverts a boolean value using choose.
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
