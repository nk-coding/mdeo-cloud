package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for null literal expressions.
 * Generates bytecode to push null (ACONST_NULL) onto the operand stack.
 */
class NullLiteralCompiler : ExpressionCompiler() {

    /**
     * Determines if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is a null literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.NullLiteral
    }

    /**
     * Compiles a null literal expression by pushing null onto the operand stack.
     *
     * @param expression The null literal expression to compile.
     * @param context The compilation context containing compiler state.
     * @param mv The method visitor used to generate bytecode instructions.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        mv.visitInsn(Opcodes.ACONST_NULL)
    }
}
