package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for double literal expressions.
 * Generates bytecode to push a double constant onto the operand stack.
 */
class DoubleLiteralCompiler : ExpressionCompiler() {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a double literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "doubleLiteral"
    }
    
    /**
     * Compiles a double literal expression to bytecode.
     *
     * Uses bit comparison to correctly handle negative zero, since (-0.0 == 0.0) is true
     * but their bit representations differ. Optimizes by using DCONST_0 for zero and
     * DCONST_1 for one, falling back to LDC for other values.
     *
     * @param expression The double literal expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor to emit bytecode to.
     * @return Unit.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val doubleExpr = expression as TypedDoubleLiteralExpression
        val value = doubleExpr.value.toDouble()
        
        when {
            isPositiveZero(value) -> {
                mv.visitInsn(Opcodes.DCONST_0)
            }
            value == 1.0 -> {
                mv.visitInsn(Opcodes.DCONST_1)
            }
            else -> {
                mv.visitLdcInsn(value)
            }
        }
    }
    
    /**
     * Checks if a double value is positive zero using bit comparison.
     *
     * This is necessary because (-0.0 == 0.0) returns true in standard comparison,
     * but we need to distinguish them for correct bytecode generation.
     *
     * @param value The double value to check.
     * @return True if the value is positive zero (0.0), false otherwise.
     */
    private fun isPositiveZero(value: Double): Boolean {
        return java.lang.Double.doubleToRawLongBits(value) == java.lang.Double.doubleToRawLongBits(0.0)
    }
}
