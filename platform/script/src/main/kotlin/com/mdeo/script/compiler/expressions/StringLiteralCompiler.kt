package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedStringLiteralExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor

/**
 * Compiler for string literal expressions.
 * Generates bytecode to push a string constant onto the operand stack.
 */
class StringLiteralCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return true if the expression is a string literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.StringLiteral
    }

    /**
     * Compiles a string literal expression to bytecode.
     * Uses the LDC instruction to push the string constant onto the operand stack.
     *
     * @param expression The string literal expression to compile.
     * @param context The compilation context with type information and utilities.
     * @param mv The method visitor to emit bytecode to.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val stringExpr = expression as TypedStringLiteralExpression
        mv.visitLdcInsn(stringExpr.value)
    }
}
