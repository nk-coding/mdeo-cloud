package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedTernaryExpression
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.CoercionUtil
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
class TernaryExpressionCompiler : ExpressionCompiler {
    
    /**
     * Determines if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a ternary expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.Ternary
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
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val ternaryExpr = expression as TypedTernaryExpression
        val resultType = context.getType(ternaryExpr.evalType)
        
        val falseLabel = Label()
        val endLabel = Label()
        
        context.compileExpression(ternaryExpr.condition, mv)
        mv.visitJumpInsn(Opcodes.IFEQ, falseLabel)
        
        compileBranchWithCoercion(ternaryExpr.trueExpression, resultType, context, mv)
        
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(falseLabel)
        
        compileBranchWithCoercion(ternaryExpr.falseExpression, resultType, context, mv)
        
        mv.visitLabel(endLabel)
    }
    
    /**
     * Compiles a branch expression and coerces its result to the target type.
     *
     * @param branchExpression The expression representing either the true or false branch.
     * @param targetType The type to coerce the branch result to.
     * @param context The compilation context.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    private fun compileBranchWithCoercion(
        branchExpression: TypedExpression,
        targetType: ReturnType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        context.compileExpression(branchExpression, mv)
        val branchType = context.getType(branchExpression.evalType)
        CoercionUtil.emitCoercion(branchType, targetType, branchExpression, mv)
    }
}
