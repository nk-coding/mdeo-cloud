package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedBinaryExpression] nodes.
 *
 * Compiles binary expressions into [GremlinCompilationResult] containing GraphTraversals
 * that implement arithmetic, comparison, equality, and logical operators using pure Gremlin.
 *
 * ## Supported Operators
 *
 * ### Arithmetic Operators
 * - `+` - Addition (uses math step) OR String concatenation (for string operands)
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
 * ## String Concatenation
 * The `+` operator has special handling for string operands. When both operands
 * are constant strings, compile-time string concatenation is performed. String
 * concatenation currently requires constant operands for both sides.
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
        /**
         * Addition operator 
         */
        const val OPERATOR_ADD = "+"

        /**
         * Subtraction operator 
         */
        const val OPERATOR_SUBTRACT = "-"

        /**
         * Multiplication operator 
         */
        const val OPERATOR_MULTIPLY = "*"

        /**
         * Division operator 
         */
        const val OPERATOR_DIVIDE = "/"

        /**
         * Modulo operator 
         */
        const val OPERATOR_MODULO = "%"

        /**
         * Less than operator 
         */
        const val OPERATOR_LESS_THAN = "<"

        /**
         * Greater than operator 
         */
        const val OPERATOR_GREATER_THAN = ">"

        /**
         * Less than or equal operator 
         */
        const val OPERATOR_LESS_THAN_OR_EQUAL = "<="

        /**
         * Greater than or equal operator 
         */
        const val OPERATOR_GREATER_THAN_OR_EQUAL = ">="

        /**
         * Structural equality operator 
         */
        const val OPERATOR_EQUALS = "=="

        /**
         * Structural inequality operator 
         */
        const val OPERATOR_NOT_EQUALS = "!="

        /**
         * Logical AND operator 
         */
        const val OPERATOR_LOGICAL_AND = "&&"

        /**
         * Logical OR operator 
         */
        const val OPERATOR_LOGICAL_OR = "||"

        /**
         * Arithmetic operators that use the math step 
         */
        private val ARITHMETIC_OPERATORS = setOf(
            OPERATOR_ADD,
            OPERATOR_SUBTRACT,
            OPERATOR_MULTIPLY,
            OPERATOR_DIVIDE,
            OPERATOR_MODULO
        )

        /**
         * Comparison operators that produce boolean results 
         */
        private val COMPARISON_OPERATORS = setOf(
            OPERATOR_LESS_THAN,
            OPERATOR_GREATER_THAN,
            OPERATOR_LESS_THAN_OR_EQUAL,
            OPERATOR_GREATER_THAN_OR_EQUAL
        )

        /**
         * Equality operators 
         */
        private val EQUALITY_OPERATORS = setOf(
            OPERATOR_EQUALS,
            OPERATOR_NOT_EQUALS
        )

        /**
         * Logical operators 
         */
        private val LOGICAL_OPERATORS = setOf(
            OPERATOR_LOGICAL_AND,
            OPERATOR_LOGICAL_OR
        )

        /**
         * All supported operators 
         */
        private val ALL_OPERATORS = ARITHMETIC_OPERATORS +
            COMPARISON_OPERATORS +
            EQUALITY_OPERATORS +
            LOGICAL_OPERATORS

        /**
         * Math step operator symbols 
         */
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
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
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
     * 
     * Special case: For the + operator, checks if operands are strings and performs
     * string concatenation instead of numeric addition.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileArithmetic(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        // Special case: Handle string concatenation with + operator
        // Must check BEFORE compiling operands to avoid treating strings as numbers
        if (expr.operator == OPERATOR_ADD && isStringConcatenation(expr, context)) {
            val leftResult = registry.compile(expr.left, context, initialTraversal)
            val rightResult = registry.compile(expr.right, context, null)
            return compileRuntimeStringConcatenation(leftResult, rightResult)
        }

        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        val mathSymbol = MATH_OPERATOR_SYMBOLS[expr.operator]
            ?: throw CompilationException.unsupportedOperator(expr.operator, expr)

        val traversal = (leftResult.traversal as GraphTraversal<Any, Any>)
            .`as`("_left")
            .map(rightResult.traversal as GraphTraversal<Any, Any>)
            .`as`("_right")
            .math("_left $mathSymbol _right")

        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Checks if an expression has a string type using static type information.
     * 
     * @param expression The expression to check
     * @param context The compilation context containing type information
     * @return true if the expression's static type is a string type
     * @throws CompilationException if the type cannot be resolved (indicates a type checker bug)
     */
    private fun isStringType(expression: TypedExpression, context: CompilationContext): Boolean {
        val type = context.resolveTypeOrNull(expression.evalType)
            ?: throw CompilationException(
                "Cannot resolve type for expression at index ${expression.evalType}. " +
                "This indicates a bug in the type checker - all expressions must have valid evalType indices.",
                expression
            )
        return type is com.mdeo.expression.ast.types.ClassTypeRef && 
            (type.type == "builtin.string" || type.type == "string")
    }

    /**
     * Checks if this binary expression represents string concatenation.
     * 
     * Uses static type information only - relies on the type checker having correctly
     * determined the types of all operands. Returns true if the + operator is being
     * used and either operand has a string type.
     * 
     * @param expr The binary expression to check
     * @param context The compilation context containing type information
     * @return true if this is a string concatenation operation
     */
    private fun isStringConcatenation(
        expr: TypedBinaryExpression,
        context: CompilationContext
    ): Boolean {
        if (expr.operator != OPERATOR_ADD) {
            return false
        }
        
        // Check operand types using static type information
        // If either operand is a string, this is string concatenation
        return isStringType(expr.left, context) || isStringType(expr.right, context)
    }

    /**
     * Compiles runtime string concatenation using a Gremlin lambda.
     * 
     * This is necessary because Gremlin doesn't have a built-in string concatenation step
     * that works without lambdas. While we try to avoid lambdas for portability, string
     * concatenation is common enough to warrant lambda support.
     * 
     * The pattern used:
     * 1. Store left value with as("_left")
     * 2. Store right value with as("_right") 
     * 3. Use path objects to get values and concatenate them
     * 
     * Note: We access path objects by index to avoid label conflicts in nested concatenations.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileRuntimeStringConcatenation(
        leftResult: GremlinCompilationResult,
        rightResult: GremlinCompilationResult
    ): GremlinCompilationResult {
        // Use the general pattern: store both values and concatenate
        val traversal = (leftResult.traversal as GraphTraversal<Any, Any>)
            .`as`("_left")
            .flatMap<Any>(rightResult.traversal as GraphTraversal<Any, Any>)
            .`as`("_right")
            .map<String> { traverser ->
                // Get the penultimate and last objects from the path
                // The penultimate is the left value, the last is the right value (current)
                val pathObjects = traverser.path().objects()
                val leftVal = if (pathObjects.size >= 2) {
                    pathObjects[pathObjects.size - 2].toString()
                } else {
                    pathObjects.firstOrNull()?.toString() ?: ""
                }
                val rightVal = traverser.get().toString()
                leftVal + rightVal
            }
        
        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Compiles a comparison operator using Gremlin predicates and choose.
     *
     * Comparisons produce boolean results by using choose() with P predicates.
     * Uses labeled variables with where pattern for dynamic comparisons.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileComparison(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        // Use dynamic comparison pattern with math step
        val traversal = buildDynamicComparisonTraversal(
            expr.operator,
            leftResult.traversal as GraphTraversal<Any, Any>,
            rightResult.traversal as GraphTraversal<Any, Any>
        )

        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Compiles an equality operator using Gremlin predicates and choose.
     *
     * Uses labeled variables with where pattern for dynamic comparisons.
     * For equality, we use where(eq()) instead of math subtraction to support all types.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        val leftType = context.resolveTypeOrNull(expr.left.evalType)
            ?: throw CompilationException("Cannot resolve type for left operand of equality expression")
        val rightType = context.resolveTypeOrNull(expr.right.evalType)
            ?: throw CompilationException("Cannot resolve type for right operand of equality expression")

        val traversal = EqualityCompilerUtil.buildEqualityTraversal(
            expr.operator,
            leftResult.traversal as GraphTraversal<Any, Any>,
            rightResult.traversal as GraphTraversal<Any, Any>,
            leftType,
            rightType,
            context.typeRegistry
        )

        return GremlinCompilationResult.of(traversal)
    }

    /**
     * Compiles a logical operator using Gremlin and/or steps with choose.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileLogical(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): GremlinCompilationResult {
        val leftResult = registry.compile(expr.left, context, initialTraversal)
        val rightResult = registry.compile(expr.right, context, null)

        val traversal = buildLogicalTraversal(
            expr.operator,
            leftResult.traversal as GraphTraversal<Any, Any>,
            rightResult.traversal as GraphTraversal<Any, Any>
        )

        return GremlinCompilationResult.of(traversal)
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
