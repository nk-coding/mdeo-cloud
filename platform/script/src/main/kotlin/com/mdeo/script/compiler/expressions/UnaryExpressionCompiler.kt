package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedUnaryExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.TypeConversionUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for unary expressions.
 * 
 * Handles unary operators:
 * - Minus (-): Negation for all numeric types (int, long, float, double)
 * - Not (!): Logical negation for boolean
 * 
 * The result type is the same as the operand type for numeric negation.
 */
class UnaryExpressionCompiler : ExpressionCompiler {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression the expression to check
     * @return true if the expression is a unary expression, false otherwise
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.Unary
    }
    
    /**
     * Compiles a unary expression to bytecode.
     *
     * Dispatches to the appropriate handler based on the unary operator:
     * negation (-) or logical NOT (!).
     *
     * @param expression the typed unary expression to compile
     * @param context the compilation context providing type information and expression compilation
     * @param mv the method visitor to emit bytecode to
     * @throws IllegalArgumentException if the unary operator is not supported
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val unaryExpr = expression as TypedUnaryExpression
        val operandType = context.getType(unaryExpr.expression.evalType)
        
        when (unaryExpr.operator) {
            "-" -> compileNegation(unaryExpr, context, mv, operandType)
            "!" -> compileLogicalNot(unaryExpr, context, mv)
            else -> throw IllegalArgumentException("Unsupported unary operator: ${unaryExpr.operator}")
        }
    }
    
    /**
     * Compiles numeric negation to bytecode.
     *
     * Uses the appropriate JVM negation opcode (INEG, LNEG, FNEG, or DNEG) 
     * based on the operand type.
     *
     * @param expr the unary expression containing the operand to negate
     * @param context the compilation context for compiling sub-expressions
     * @param mv the method visitor to emit bytecode to
     * @param operandType the type of the operand being negated
     * @throws IllegalArgumentException if the operand is not a numeric type
     */
    private fun compileNegation(
        expr: TypedUnaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        operandType: com.mdeo.script.ast.types.ReturnType
    ) {
        val typeName = TypeConversionUtil.getNumericTypeName(operandType)
            ?: throw IllegalArgumentException("Operand must be numeric for negation")
        
        context.compileExpression(expr.expression, mv)
        
        val negOpcode = TypeConversionUtil.getNegOpcode(typeName)
        mv.visitInsn(negOpcode)
    }
    
    /**
     * Compiles logical NOT to bytecode.
     *
     * Converts true to false and false to true using conditional branching.
     * If the operand is zero (false), pushes 1 (true); otherwise pushes 0 (false).
     *
     * @param expr the unary expression containing the boolean operand
     * @param context the compilation context for compiling sub-expressions
     * @param mv the method visitor to emit bytecode to
     */
    private fun compileLogicalNot(
        expr: TypedUnaryExpression,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        context.compileExpression(expr.expression, mv)
        
        val trueLabel = Label()
        val endLabel = Label()
        
        mv.visitJumpInsn(Opcodes.IFEQ, trueLabel)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLabel(endLabel)
    }
}
