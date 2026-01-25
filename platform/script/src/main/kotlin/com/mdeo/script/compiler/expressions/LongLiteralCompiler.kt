package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedLongLiteralExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for long literal expressions.
 * Generates bytecode to push a long constant onto the operand stack.
 */
class LongLiteralCompiler : ExpressionCompiler() {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is a long literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.LongLiteral
    }
    
    /**
     * Compiles a long literal expression to bytecode.
     *
     * Uses optimized opcodes for common values (0L and 1L use LCONST_0 and LCONST_1),
     * while other values are loaded using LDC instruction.
     *
     * @param expression The typed long literal expression to compile.
     * @param context The compilation context containing compiler state.
     * @param mv The method visitor used to generate bytecode instructions.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val longExpr = expression as TypedLongLiteralExpression
        val value = longExpr.value.toLong()
        
        when (value) {
            0L -> {
                mv.visitInsn(Opcodes.LCONST_0)
            }
            1L -> {
                mv.visitInsn(Opcodes.LCONST_1)
            }
            else -> {
                mv.visitLdcInsn(value)
            }
        }
    }
}
