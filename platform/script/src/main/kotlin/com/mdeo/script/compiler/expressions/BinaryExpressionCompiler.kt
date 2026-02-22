package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor

/**
 * Compiler for binary expressions.
 * 
 * This compiler acts as a dispatcher, delegating to specialized helpers
 * based on the operator type:
 * 
 * - **Arithmetic operations** (+, -, *, /, %): Handled by [ArithmeticOperationHelper].
 *   Includes numeric arithmetic and string concatenation.
 * 
 * - **Comparison operations** (<, >, <=, >=): Handled by [ComparisonOperationHelper].
 *   Supports all numeric types with proper type promotion.
 * 
 * - **Structural equality operations** (==, !=): Handled by [EqualityOperationHelper].
 *   Uses .equals() for object comparison (Kotlin semantics).
 * 
 * - **Strict equality operations** (===, !==): Handled by [EqualityOperationHelper].
 *   Uses reference comparison for objects.
 * 
 * - **Null coalescing** (??): Handled by [NullCoalescingHelper].
 *   Returns left if non-null, otherwise right.
 * 
 * - **Logical operations** (&&, ||): Handled by [LogicalOperationHelper].
 *   Implements short-circuit evaluation.
 * 
 * Type promotion follows Java's binary numeric promotion rules:
 * 1. If either operand is double, both are converted to double
 * 2. Otherwise, if either operand is float, both are converted to float
 * 3. Otherwise, if either operand is long, both are converted to long
 * 4. Otherwise, both are converted to int
 * 
 * @see ArithmeticOperationHelper
 * @see ComparisonOperationHelper
 * @see EqualityOperationHelper
 * @see NullCoalescingHelper
 * @see LogicalOperationHelper
 */
class BinaryExpressionCompiler : ExpressionCompiler() {
    
    /**
     * Determines if this compiler can handle the given expression.
     *
     * @param expression The expression to check for compatibility.
     * @return `true` if the expression is a binary expression, `false` otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "binary"
    }
    
    /**
     * Compiles a binary expression to JVM bytecode.
     *
     * Dispatches to specialized helpers based on the operator type:
     * - Arithmetic (+, -, *, /, %) to [ArithmeticOperationHelper]
     * - Comparison (<, >, <=, >=) to [ComparisonOperationHelper]
     * - Structural equality (==, !=) to [EqualityOperationHelper.compileEquality]
     * - Strict equality (===, !==) to [EqualityOperationHelper.compileStrictEquality]
     * - Null coalescing (??) to [NullCoalescingHelper]
     * - Logical (&&, ||) to [LogicalOperationHelper]
     *
     * @param expression The binary expression to compile.
     * @param context The compilation context providing type resolution and other utilities.
     * @param mv The ASM method visitor for emitting bytecode instructions.
     * @throws IllegalArgumentException if the operator is not supported.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val binaryExpr = expression as TypedBinaryExpression
        val resultType = context.getType(binaryExpr.evalType)
        val leftType = context.getType(binaryExpr.left.evalType)
        val rightType = context.getType(binaryExpr.right.evalType)
        
        when (binaryExpr.operator) {
            "+" -> ArithmeticOperationHelper.compileAddition(binaryExpr, context, mv, leftType, rightType, resultType)
            "-" -> ArithmeticOperationHelper.compileSubtraction(binaryExpr, context, mv, leftType, rightType, resultType)
            "*" -> ArithmeticOperationHelper.compileMultiplication(binaryExpr, context, mv, leftType, rightType, resultType)
            "/" -> ArithmeticOperationHelper.compileDivision(binaryExpr, context, mv, leftType, rightType, resultType)
            "%" -> ArithmeticOperationHelper.compileModulo(binaryExpr, context, mv, leftType, rightType, resultType)
            "<" -> ComparisonOperationHelper.compileLessThan(binaryExpr, context, mv, leftType, rightType)
            ">" -> ComparisonOperationHelper.compileGreaterThan(binaryExpr, context, mv, leftType, rightType)
            "<=" -> ComparisonOperationHelper.compileLessThanOrEqual(binaryExpr, context, mv, leftType, rightType)
            ">=" -> ComparisonOperationHelper.compileGreaterThanOrEqual(binaryExpr, context, mv, leftType, rightType)
            "==" -> EqualityOperationHelper.compileEquality(binaryExpr, context, mv, leftType, rightType, true)
            "!=" -> EqualityOperationHelper.compileEquality(binaryExpr, context, mv, leftType, rightType, false)
            "===" -> EqualityOperationHelper.compileStrictEquality(binaryExpr, context, mv, leftType, rightType, true)
            "!==" -> EqualityOperationHelper.compileStrictEquality(binaryExpr, context, mv, leftType, rightType, false)
            "??" -> NullCoalescingHelper.compileNullCoalescing(binaryExpr, context, mv)
            "&&" -> LogicalOperationHelper.compileLogicalAnd(binaryExpr, context, mv)
            "||" -> LogicalOperationHelper.compileLogicalOr(binaryExpr, context, mv)
            
            else -> throw IllegalArgumentException("Unsupported binary operator: ${binaryExpr.operator}")
        }
    }
}
