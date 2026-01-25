package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedFloatLiteralExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for float literal expressions.
 * Generates bytecode to push a float constant onto the operand stack.
 */
class FloatLiteralCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is a float literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.FloatLiteral
    }

    /**
     * Compiles a float literal expression to bytecode.
     *
     * Uses optimized bytecode instructions (FCONST_0, FCONST_1, FCONST_2) for common
     * float values, and LDC for other values. For zero comparison, bit comparison is
     * used via [java.lang.Float.floatToRawIntBits] to correctly handle negative zero,
     * since (-0.0f == 0.0f) is true but their bit representations differ.
     *
     * @param expression The float literal expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor to emit bytecode instructions.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val floatExpr = expression as TypedFloatLiteralExpression
        val value = floatExpr.value.toFloat()

        when {
            isPositiveZero(value) -> mv.visitInsn(Opcodes.FCONST_0)
            value == 1.0f -> mv.visitInsn(Opcodes.FCONST_1)
            value == 2.0f -> mv.visitInsn(Opcodes.FCONST_2)
            else -> mv.visitLdcInsn(value)
        }
    }

    /**
     * Checks if the given float value is positive zero using bit comparison.
     *
     * This is necessary because (-0.0f == 0.0f) evaluates to true, but FCONST_0
     * should only be used for positive zero.
     *
     * @param value The float value to check.
     * @return True if the value is positive zero, false otherwise.
     */
    private fun isPositiveZero(value: Float): Boolean {
        return java.lang.Float.floatToRawIntBits(value) == java.lang.Float.floatToRawIntBits(0.0f)
    }
}
