package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for boolean literal expressions.
 * Generates bytecode to push a boolean constant (0 or 1) onto the operand stack.
 */
class BooleanLiteralCompiler : ExpressionCompiler() {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return true if the expression is a boolean literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.BooleanLiteral
    }
    
    /**
     * Compiles a boolean literal expression to bytecode.
     * Pushes ICONST_1 for true or ICONST_0 for false onto the operand stack.
     *
     * @param expression The boolean literal expression to compile.
     * @param context The compilation context with type information and utilities.
     * @param mv The method visitor to emit bytecode to.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val boolExpr = expression as TypedBooleanLiteralExpression
        
        if (boolExpr.value) {
            mv.visitInsn(Opcodes.ICONST_1)
        } else {
            mv.visitInsn(Opcodes.ICONST_0)
        }
    }
}
