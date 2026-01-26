package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for integer literal expressions.
 * Generates bytecode to push an integer constant onto the operand stack.
 *
 * Uses the most efficient bytecode instruction based on the integer value:
 * - ICONST_n for values -1 to 5
 * - BIPUSH for values in byte range (-128 to 127)
 * - SIPUSH for values in short range (-32768 to 32767)
 * - LDC for all other integer values
 */
class IntLiteralCompiler : ExpressionCompiler() {

    /**
     * Determines if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is an integer literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "intLiteral"
    }

    /**
     * Compiles an integer literal expression to bytecode.
     *
     * Selects the most efficient bytecode instruction based on the integer value:
     * - ICONST_n for values -1 to 5 (single-byte instruction)
     * - BIPUSH for values in byte range (two-byte instruction)
     * - SIPUSH for values in short range (three-byte instruction)
     * - LDC for all other integer values (loads from constant pool)
     *
     * @param expression The integer literal expression to compile.
     * @param context The compilation context containing compiler state.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val intExpr = expression as TypedIntLiteralExpression
        val value = intExpr.value.toInt()

        when {
            value >= -1 && value <= 5 -> {
                mv.visitInsn(Opcodes.ICONST_0 + value)
            }
            value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE -> {
                mv.visitIntInsn(Opcodes.BIPUSH, value)
            }
            value >= Short.MIN_VALUE && value <= Short.MAX_VALUE -> {
                mv.visitIntInsn(Opcodes.SIPUSH, value)
            }
            else -> {
                mv.visitLdcInsn(value)
            }
        }
    }
}
