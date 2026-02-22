package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles ternary (conditional) expressions to bytecode.
 * 
 * A ternary expression has the form: condition ? trueExpr : falseExpr
 * 
 * Structure:
 *   [evaluate condition]
 *   IFEQ falseLabel       ; if condition is false, go to false branch
 *   [evaluate trueExpr]
 *   [coerce to result type if needed]
 *   GOTO endLabel
 * falseLabel:
 *   [evaluate falseExpr]
 *   [coerce to result type if needed]
 * endLabel:
 * 
 * The result of the ternary expression is the value of either trueExpr or falseExpr
 * left on the stack.
 */
class TernaryExpressionCompiler : ExpressionCompiler() {
    
    /**
     * Determines if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a ternary expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "ternary"
    }
    
    /**
     * Compiles a ternary expression to bytecode.
     *
     * Generates bytecode that evaluates the condition, then either the true or false branch.
     * Each branch is compiled and coerced to the result type before leaving the value on the stack.
     *
     * @param expression The ternary expression to compile.
     * @param context The compilation context providing access to type information and expression compilation.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val ternaryExpr = expression as TypedTernaryExpression
        val resultType = context.getType(ternaryExpr.evalType)
        
        val falseLabel = Label()
        val endLabel = Label()
        
        val conditionType = context.getType(ternaryExpr.condition.evalType)
        context.compileExpression(ternaryExpr.condition, mv, conditionType)
        mv.visitJumpInsn(Opcodes.IFEQ, falseLabel)
        
        context.compileExpression(ternaryExpr.trueExpression, mv, resultType)
        
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(falseLabel)
        
        context.compileExpression(ternaryExpr.falseExpression, mv, resultType)
        
        mv.visitLabel(endLabel)
    }
}
