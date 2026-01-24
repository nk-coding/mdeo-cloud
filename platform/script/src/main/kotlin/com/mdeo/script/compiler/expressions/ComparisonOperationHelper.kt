package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.expressions.TypedBinaryExpression
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.util.TypeConversionUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Helper for compiling comparison binary operations.
 * 
 * Handles the following relational operators:
 * - Less than (<)
 * - Greater than (>)
 * - Less than or equal (<=)
 * - Greater than or equal (>=)
 * 
 * Comparison operations follow Java's semantics:
 * - Both operands must be numeric types
 * - Operands are promoted to a common type before comparison
 * - The result is always a boolean (int 0 or 1 on the JVM stack)
 * 
 * For int comparisons, uses IF_ICMPxx instructions directly.
 * For long, float, and double, uses LCMP/FCMP/DCMP followed by IFxx.
 * 
 * Special handling for floating point comparisons:
 * - For < and <=: uses FCMPG/DCMPG to handle NaN (NaN comparison returns false)
 * - For > and >=: uses FCMPL/DCMPL to handle NaN (NaN comparison returns false)
 * 
 * @see TypeConversionUtil for type promotion logic
 */
object ComparisonOperationHelper {

    /**
     * Compiles a less-than (<) comparison.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     */
    fun compileLessThan(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType
    ) {
        compileComparison(expr, context, mv, leftType, rightType, Opcodes.IF_ICMPLT, Opcodes.IFLT)
    }

    /**
     * Compiles a greater-than (>) comparison.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     */
    fun compileGreaterThan(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType
    ) {
        compileComparison(expr, context, mv, leftType, rightType, Opcodes.IF_ICMPGT, Opcodes.IFGT)
    }

    /**
     * Compiles a less-than-or-equal (<=) comparison.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     */
    fun compileLessThanOrEqual(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType
    ) {
        compileComparison(expr, context, mv, leftType, rightType, Opcodes.IF_ICMPLE, Opcodes.IFLE)
    }

    /**
     * Compiles a greater-than-or-equal (>=) comparison.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     */
    fun compileGreaterThanOrEqual(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType
    ) {
        compileComparison(expr, context, mv, leftType, rightType, Opcodes.IF_ICMPGE, Opcodes.IFGE)
    }

    /**
     * Compiles a comparison operation.
     * 
     * For int comparisons, uses IF_ICMPxx instructions directly.
     * For long, float, and double, uses a two-step approach:
     * 1. Compare using LCMP/FCMP/DCMP to produce -1, 0, or 1
     * 2. Branch using IFxx based on the comparison result
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param intCompareOp The opcode for direct int comparison (IF_ICMPxx)
     * @param resultCompareOp The opcode for comparing the result of LCMP/FCMP/DCMP (IFxx)
     */
    private fun compileComparison(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        intCompareOp: Int,
        resultCompareOp: Int
    ) {
        val leftTypeName = TypeConversionUtil.getNumericTypeName(leftType)
            ?: throw IllegalArgumentException("Left operand must be numeric for comparison")
        
        val rightTypeName = TypeConversionUtil.getNumericTypeName(rightType)
            ?: throw IllegalArgumentException("Right operand must be numeric for comparison")
        
        val promotedType = TypeConversionUtil.getPromotedType(leftTypeName, rightTypeName)
        
        compileAndConvertOperands(expr, context, mv, leftTypeName, rightTypeName, promotedType)
        
        val trueLabel = Label()
        val endLabel = Label()
        
        emitComparisonBytecode(mv, promotedType, intCompareOp, resultCompareOp, trueLabel)
        
        emitBooleanResult(mv, trueLabel, endLabel)
    }

    /**
     * Compiles both operands and converts them to the promoted type.
     * 
     * @param expr The binary expression containing left and right operands
     * @param context The compilation context for compiling sub-expressions
     * @param mv The ASM method visitor
     * @param leftTypeName The type name of the left operand
     * @param rightTypeName The type name of the right operand
     * @param promotedType The common type both operands should be converted to
     */
    private fun compileAndConvertOperands(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftTypeName: String,
        rightTypeName: String,
        promotedType: String
    ) {
        context.compileExpression(expr.left, mv)
        TypeConversionUtil.emitConversion(leftTypeName, promotedType, mv)
        
        context.compileExpression(expr.right, mv)
        TypeConversionUtil.emitConversion(rightTypeName, promotedType, mv)
    }

    /**
     * Emits the comparison bytecode based on the promoted type.
     * 
     * For int types, uses direct IF_ICMPxx comparison. For long types, uses LCMP followed by IFxx.
     * For floating point types, selects the appropriate compare instruction to handle NaN correctly:
     * FCMPG/DCMPG for less-than comparisons, FCMPL/DCMPL for greater-than comparisons.
     * 
     * @param mv The ASM method visitor
     * @param promotedType The common type of both operands after promotion
     * @param intCompareOp The opcode for direct int comparison (IF_ICMPxx)
     * @param resultCompareOp The opcode for comparing the LCMP/FCMP/DCMP result (IFxx)
     * @param trueLabel The label to jump to when comparison is true
     */
    private fun emitComparisonBytecode(
        mv: MethodVisitor,
        promotedType: String,
        intCompareOp: Int,
        resultCompareOp: Int,
        trueLabel: Label
    ) {
        when (promotedType) {
            "builtin.int" -> {
                mv.visitJumpInsn(intCompareOp, trueLabel)
            }
            "builtin.long" -> {
                mv.visitInsn(Opcodes.LCMP)
                mv.visitJumpInsn(resultCompareOp, trueLabel)
            }
            "builtin.float" -> {
                val cmpOp = getFloatingPointCompareOp(resultCompareOp, isDouble = false)
                mv.visitInsn(cmpOp)
                mv.visitJumpInsn(resultCompareOp, trueLabel)
            }
            "builtin.double" -> {
                val cmpOp = getFloatingPointCompareOp(resultCompareOp, isDouble = true)
                mv.visitInsn(cmpOp)
                mv.visitJumpInsn(resultCompareOp, trueLabel)
            }
        }
    }

    /**
     * Gets the appropriate floating point comparison opcode for NaN handling.
     * 
     * For less-than and less-than-or-equal comparisons, uses FCMPG/DCMPG so NaN returns 1
     * (causing the comparison to fail). For greater-than and greater-than-or-equal comparisons,
     * uses FCMPL/DCMPL so NaN returns -1 (also causing the comparison to fail).
     * 
     * @param resultCompareOp The branch opcode (IFLT, IFLE, IFGT, or IFGE)
     * @param isDouble True for double comparisons, false for float comparisons
     * @return The appropriate FCMPG/FCMPL or DCMPG/DCMPL opcode
     */
    private fun getFloatingPointCompareOp(resultCompareOp: Int, isDouble: Boolean): Int {
        val isLessThanComparison = resultCompareOp == Opcodes.IFLT || resultCompareOp == Opcodes.IFLE
        return if (isDouble) {
            if (isLessThanComparison) {
                Opcodes.DCMPG
            } else {
                Opcodes.DCMPL
            }
        } else {
            if (isLessThanComparison) {
                Opcodes.FCMPG
            } else {
                Opcodes.FCMPL
            }
        }
    }

    /**
     * Emits the boolean result bytecode pattern.
     * 
     * Pushes 0 (false) for the fall-through case and jumps to end, then emits the true label
     * which pushes 1 (true), followed by the end label where execution converges.
     * 
     * @param mv The ASM method visitor
     * @param trueLabel The label marking the true branch
     * @param endLabel The label marking the end of the boolean result pattern
     */
    private fun emitBooleanResult(mv: MethodVisitor, trueLabel: Label, endLabel: Label) {
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        
        mv.visitLabel(endLabel)
    }
}
