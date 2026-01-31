package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedBinaryExpression] nodes.
 *
 * Compiles binary expressions into [TraversalCompilationResult] containing GraphTraversals
 * that implement arithmetic, comparison, equality, and logical operators using pure Gremlin.
 *
 * ## Supported Operators
 *
 * ### Arithmetic Operators
 * - `+` - Addition (uses math step)
 * - `-` - Subtraction (uses math step)
 * - `*` - Multiplication (uses math step)
 * - `/` - Division (uses math step)
 * - `%` - Modulo (uses math step)
 *
 * ### Comparison Operators
 * - `<` - Less than (uses P.lt)
 * - `>` - Greater than (uses P.gt)
 * - `<=` - Less than or equal (uses P.lte)
 * - `>=` - Greater than or equal (uses P.gte)
 *
 * ### Equality Operators
 * - `==` - Structural equality (uses P.eq)
 * - `!=` - Structural inequality (uses P.neq)
 *
 * ### Logical Operators
 * - `&&` - Logical AND (uses and step with choose)
 * - `||` - Logical OR (uses or step with choose)
 *
 * ## Initial Traversal Propagation
 * The [initialTraversal] is passed ONLY to the leftmost sub-expression. The right
 * sub-expression always starts fresh (null initialTraversal). This ensures proper
 * traversal construction in match contexts.
 *
 * ## Portability
 * All operators are implemented using pure Gremlin (no lambdas) for maximum
 * portability across different graph databases.
 *
 * @param registry The traversal compiler registry for compiling sub-expressions
 */
class BinaryOperatorCompiler(
    private val registry: ExpressionCompilerRegistry
) : ExpressionCompiler {

    companion object {
        /** Addition operator */
        const val OPERATOR_ADD = "+"

        /** Subtraction operator */
        const val OPERATOR_SUBTRACT = "-"

        /** Multiplication operator */
        const val OPERATOR_MULTIPLY = "*"

        /** Division operator */
        const val OPERATOR_DIVIDE = "/"

        /** Modulo operator */
        const val OPERATOR_MODULO = "%"

        /** Less than operator */
        const val OPERATOR_LESS_THAN = "<"

        /** Greater than operator */
        const val OPERATOR_GREATER_THAN = ">"

        /** Less than or equal operator */
        const val OPERATOR_LESS_THAN_OR_EQUAL = "<="

        /** Greater than or equal operator */
        const val OPERATOR_GREATER_THAN_OR_EQUAL = ">="

        /** Structural equality operator */
        const val OPERATOR_EQUALS = "=="

        /** Structural inequality operator */
        const val OPERATOR_NOT_EQUALS = "!="

        /** Logical AND operator */
        const val OPERATOR_LOGICAL_AND = "&&"

        /** Logical OR operator */
        const val OPERATOR_LOGICAL_OR = "||"

        /** Arithmetic operators that use the math step */
        private val ARITHMETIC_OPERATORS = setOf(
            OPERATOR_ADD,
            OPERATOR_SUBTRACT,
            OPERATOR_MULTIPLY,
            OPERATOR_DIVIDE,
            OPERATOR_MODULO
        )

        /** Comparison operators that produce boolean results */
        private val COMPARISON_OPERATORS = setOf(
            OPERATOR_LESS_THAN,
            OPERATOR_GREATER_THAN,
            OPERATOR_LESS_THAN_OR_EQUAL,
            OPERATOR_GREATER_THAN_OR_EQUAL
        )

        /** Equality operators */
        private val EQUALITY_OPERATORS = setOf(
            OPERATOR_EQUALS,
            OPERATOR_NOT_EQUALS
        )

        /** Logical operators */
        private val LOGICAL_OPERATORS = setOf(
            OPERATOR_LOGICAL_AND,
            OPERATOR_LOGICAL_OR
        )

        /** All supported operators */
        private val ALL_OPERATORS = ARITHMETIC_OPERATORS +
            COMPARISON_OPERATORS +
            EQUALITY_OPERATORS +
            LOGICAL_OPERATORS

        /** Math step operator symbols */
        private val MATH_OPERATOR_SYMBOLS = mapOf(
            OPERATOR_ADD to "+",
            OPERATOR_SUBTRACT to "-",
            OPERATOR_MULTIPLY to "*",
            OPERATOR_DIVIDE to "/",
            OPERATOR_MODULO to "%"
        )
    }

    override fun canCompile(expression: TypedExpression): Boolean {
        if (expression !is TypedBinaryExpression) return false
        return expression.operator in ALL_OPERATORS
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val binaryExpr = expression as TypedBinaryExpression
        val operator = binaryExpr.operator

        if (operator !in ALL_OPERATORS) {
            throw CompilationException.unsupportedOperator(operator, expression)
        }

        return when (operator) {
            in ARITHMETIC_OPERATORS -> compileArithmetic(binaryExpr, context, initialTraversal)
            in COMPARISON_OPERATORS -> compileComparison(binaryExpr, context, initialTraversal)
            in EQUALITY_OPERATORS -> compileEquality(binaryExpr, context, initialTraversal)
            in LOGICAL_OPERATORS -> compileLogical(binaryExpr, context, initialTraversal)
            else -> throw CompilationException.unsupportedOperator(operator, expression)
        }
    }

    /**
     * Compiles an arithmetic operator using Gremlin's math step.
     *
     * The math step expects labeled values, so we use `as("_left")` and `as("_right")`
     * to capture both operands, then apply the math operation.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileArithmetic(
        expr: TypedBinaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        if (leftResult.isConstant && rightResult.isConstant) {
            return compileConstantArithmetic(expr.operator, leftResult, rightResult, initialTraversal)
        }

        val mathSymbol = MATH_OPERATOR_SYMBOLS[expr.operator]
            ?: throw CompilationException.unsupportedOperator(expr.operator, expr)

        val traversal = (leftResult.traversal as GraphTraversal<Any, Any>)
            .`as`("_left")
            .map(rightResult.traversal as GraphTraversal<Any, Any>)
            .`as`("_right")
            .math("_left $mathSymbol _right")

        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles constant arithmetic at compile time.
     */
    private fun compileConstantArithmetic(
        operator: String,
        leftResult: TraversalCompilationResult<*, *>,
        rightResult: TraversalCompilationResult<*, *>,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val left = (leftResult.constantValue as Number).toDouble()
        val right = (rightResult.constantValue as Number).toDouble()

        val result: Double = when (operator) {
            OPERATOR_ADD -> left + right
            OPERATOR_SUBTRACT -> left - right
            OPERATOR_MULTIPLY -> left * right
            OPERATOR_DIVIDE -> left / right
            OPERATOR_MODULO -> left % right
            else -> throw IllegalStateException("Unexpected arithmetic operator: $operator")
        }

        return TraversalCompilationResult.constant(result, initialTraversal)
    }

    /**
     * Compiles a comparison operator using Gremlin predicates and choose.
     *
     * Comparisons produce boolean results by using choose() with P predicates.
     * For constant right operands, uses simple P.lt/gt/etc predicates.
     * For dynamic right operands, uses labeled variables with where pattern.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileComparison(
        expr: TypedBinaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        if (leftResult.isConstant && rightResult.isConstant) {
            return compileConstantComparison(expr.operator, leftResult, rightResult, initialTraversal)
        }

        // If right operand is constant, use simple predicate approach
        if (rightResult.isConstant) {
            val predicate = createComparisonPredicateFromConstant(expr.operator, rightResult.constantValue)
            val traversal = buildPredicateChoose(leftResult.traversal, predicate)
            return TraversalCompilationResult.of(traversal)
        }

        // For dynamic right operand, use labeled variable pattern with where
        val traversal = buildDynamicComparisonTraversal(
            expr.operator,
            leftResult.traversal as GraphTraversal<Any, Any>,
            rightResult.traversal as GraphTraversal<Any, Any>
        )

        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles constant comparison at compile time.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileConstantComparison(
        operator: String,
        leftResult: TraversalCompilationResult<*, *>,
        rightResult: TraversalCompilationResult<*, *>,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val left = leftResult.constantValue as Comparable<Any>
        val right = rightResult.constantValue as Comparable<Any>

        val result: Boolean = when (operator) {
            OPERATOR_LESS_THAN -> left < right
            OPERATOR_GREATER_THAN -> left > right
            OPERATOR_LESS_THAN_OR_EQUAL -> left <= right
            OPERATOR_GREATER_THAN_OR_EQUAL -> left >= right
            else -> throw IllegalStateException("Unexpected comparison operator: $operator")
        }

        return TraversalCompilationResult.constant(result, initialTraversal)
    }

    /**
     * Compiles an equality operator using Gremlin predicates and choose.
     *
     * For constant right operands, uses simple P.eq/neq predicates.
     * For dynamic right operands, uses labeled variables with where pattern.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileEquality(
        expr: TypedBinaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        if (leftResult.isConstant && rightResult.isConstant) {
            return compileConstantEquality(expr.operator, leftResult, rightResult, initialTraversal)
        }

        // If right operand is constant, use simple predicate approach
        if (rightResult.isConstant) {
            val predicate = createEqualityPredicateFromConstant(expr.operator, rightResult.constantValue)
            val traversal = buildPredicateChoose(leftResult.traversal, predicate)
            return TraversalCompilationResult.of(traversal)
        }

        // For dynamic right operand, use labeled variable pattern with where
        val traversal = buildDynamicComparisonTraversal(
            expr.operator,
            leftResult.traversal as GraphTraversal<Any, Any>,
            rightResult.traversal as GraphTraversal<Any, Any>
        )

        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles constant equality at compile time.
     */
    private fun compileConstantEquality(
        operator: String,
        leftResult: TraversalCompilationResult<*, *>,
        rightResult: TraversalCompilationResult<*, *>,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val left = leftResult.constantValue
        val right = rightResult.constantValue

        val result: Boolean = when (operator) {
            OPERATOR_EQUALS -> left == right
            OPERATOR_NOT_EQUALS -> left != right
            else -> throw IllegalStateException("Unexpected equality operator: $operator")
        }

        return TraversalCompilationResult.constant(result, initialTraversal)
    }

    /**
     * Compiles a logical operator using Gremlin and/or steps with choose.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileLogical(
        expr: TypedBinaryExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        if (leftResult.isConstant && rightResult.isConstant) {
            return compileConstantLogical(expr.operator, leftResult, rightResult, initialTraversal)
        }

        val traversal = buildLogicalTraversal(
            expr.operator,
            leftResult.traversal as GraphTraversal<Any, Any>,
            rightResult.traversal as GraphTraversal<Any, Any>
        )

        return TraversalCompilationResult.of(traversal)
    }

    /**
     * Compiles constant logical at compile time with short-circuit semantics.
     */
    private fun compileConstantLogical(
        operator: String,
        leftResult: TraversalCompilationResult<*, *>,
        rightResult: TraversalCompilationResult<*, *>,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val left = leftResult.constantValue as Boolean
        val right = rightResult.constantValue as Boolean

        val result: Boolean = when (operator) {
            OPERATOR_LOGICAL_AND -> left && right
            OPERATOR_LOGICAL_OR -> left || right
            else -> throw IllegalStateException("Unexpected logical operator: $operator")
        }

        return TraversalCompilationResult.constant(result, initialTraversal)
    }

    /**
     * Creates a Gremlin predicate for comparison operators from a constant value.
     */
    private fun createComparisonPredicateFromConstant(
        operator: String,
        rightValue: Any?
    ): P<*> {
        return when (operator) {
            OPERATOR_LESS_THAN -> P.lt(rightValue)
            OPERATOR_GREATER_THAN -> P.gt(rightValue)
            OPERATOR_LESS_THAN_OR_EQUAL -> P.lte(rightValue)
            OPERATOR_GREATER_THAN_OR_EQUAL -> P.gte(rightValue)
            else -> throw IllegalStateException("Unexpected comparison operator: $operator")
        }
    }

    /**
     * Creates a Gremlin predicate for equality operators from a constant value.
     */
    private fun createEqualityPredicateFromConstant(
        operator: String,
        rightValue: Any?
    ): P<*> {
        return when (operator) {
            OPERATOR_EQUALS -> P.eq(rightValue)
            OPERATOR_NOT_EQUALS -> P.neq(rightValue)
            else -> throw IllegalStateException("Unexpected equality operator: $operator")
        }
    }

    /**
     * Builds a traversal for dynamic comparison where the right operand is not constant.
     *
     * Uses the same pattern as arithmetic: store left with as("_left"), compute right with
     * map() and store with as("_right"), then use math step to compute difference
     * and check the result.
     *
     * Pattern:
     * ```
     * leftTraversal.as("_left")
     *     .map(rightTraversal).as("_right")
     *     .math("_left - _right")
     *     .choose(__.is(P.op(0)), __.constant(true), __.constant(false))
     * ```
     *
     * For inequality (!=), we invert the logic.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildDynamicComparisonTraversal(
        operator: String,
        leftTraversal: GraphTraversal<Any, Any>,
        rightTraversal: GraphTraversal<Any, Any>
    ): GraphTraversal<Any, Boolean> {
        // For != we use eq internally and invert the result
        val (effectiveOperator, invertResult) = if (operator == OPERATOR_NOT_EQUALS) {
            OPERATOR_EQUALS to true
        } else {
            operator to false
        }

        // Build the comparison predicate based on the operator
        val predicate: P<Double> = when (effectiveOperator) {
            OPERATOR_EQUALS -> P.eq(0.0)
            OPERATOR_LESS_THAN -> P.lt(0.0)
            OPERATOR_GREATER_THAN -> P.gt(0.0)
            OPERATOR_LESS_THAN_OR_EQUAL -> P.lte(0.0)
            OPERATOR_GREATER_THAN_OR_EQUAL -> P.gte(0.0)
            else -> throw IllegalStateException("Unexpected comparison operator: $effectiveOperator")
        }

        // Use the same pattern as arithmetic: as("_left").map(right).as("_right").math(...)
        val baseTraversal = leftTraversal
            .`as`("_left")
            .map(rightTraversal)
            .`as`("_right")
            .math("_left - _right")
            .choose(
                AnonymousTraversal.`is`(predicate),
                AnonymousTraversal.constant<Any>(true),
                AnonymousTraversal.constant<Any>(false)
            )

        // For != we need to invert the result
        return if (invertResult) {
            baseTraversal.choose(
                AnonymousTraversal.`is`(P.eq(true)),
                AnonymousTraversal.constant(false),
                AnonymousTraversal.constant(true)
            ) as GraphTraversal<Any, Boolean>
        } else {
            baseTraversal as GraphTraversal<Any, Boolean>
        }
    }

    /**
     * Builds a choose traversal that evaluates a predicate and returns true/false.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildPredicateChoose(
        leftTraversal: GraphTraversal<*, *>,
        predicate: P<*>
    ): GraphTraversal<Any, Boolean> {
        return (leftTraversal as GraphTraversal<Any, Any>)
            .choose(
                AnonymousTraversal.`is`(predicate as P<Any>),
                AnonymousTraversal.constant(true),
                AnonymousTraversal.constant(false)
            )
    }

    /**
     * Builds a logical AND/OR traversal using choose.
     */
    private fun buildLogicalTraversal(
        operator: String,
        leftTraversal: GraphTraversal<Any, Any>,
        rightTraversal: GraphTraversal<Any, Any>
    ): GraphTraversal<Any, Boolean> {
        val leftBoolCheck = AnonymousTraversal.`is`(P.eq(true))

        return when (operator) {
            OPERATOR_LOGICAL_AND -> {
                leftTraversal.choose(
                    leftBoolCheck,
                    rightTraversal.choose(
                        AnonymousTraversal.`is`(P.eq(true)),
                        AnonymousTraversal.constant(true),
                        AnonymousTraversal.constant(false)
                    ),
                    AnonymousTraversal.constant(false)
                )
            }
            OPERATOR_LOGICAL_OR -> {
                leftTraversal.choose(
                    leftBoolCheck,
                    AnonymousTraversal.constant(true),
                    rightTraversal.choose(
                        AnonymousTraversal.`is`(P.eq(true)),
                        AnonymousTraversal.constant(true),
                        AnonymousTraversal.constant(false)
                    )
                )
            }
            else -> throw IllegalStateException("Unexpected logical operator: $operator")
        }
    }
}
