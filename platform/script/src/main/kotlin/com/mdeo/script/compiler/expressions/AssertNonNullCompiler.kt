package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedAssertNonNullExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.util.CoercionUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for the assert non-null expression (!! postfix operator).
 *
 * Evaluates the inner expression and throws a NullPointerException if the result is null.
 * This follows Kotlin semantics for the non-null assertion operator.
 *
 * Example:
 * ```
 * val x: int? = null
 * val y = x!!  // throws NullPointerException
 * ```
 *
 * The result type is the non-nullable version of the input type.
 */
class AssertNonNullCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return true if the expression is an assert non-null expression.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "assertNonNull"
    }

    /**
     * Compiles the assert non-null expression to bytecode.
     *
     * Generates code that:
     * 1. Evaluates the inner expression (nullable type)
     * 2. Duplicates the value on the stack
     * 3. Checks if it's null
     * 4. If null, throws NullPointerException
     * 5. If non-null, unboxes if the result type is primitive
     *
     * @param expression The assert non-null expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor to emit bytecode to.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val assertExpr = expression as TypedAssertNonNullExpression
        val innerType = context.getType(assertExpr.expression.evalType)
        val resultType = context.getType(assertExpr.evalType)
        
        context.compileExpression(assertExpr.expression, mv, innerType)
        
        val notNullLabel = Label()
        
        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNullLabel)
        
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException")
        mv.visitInsn(Opcodes.DUP)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/NullPointerException",
            "<init>",
            "()V",
            false
        )
        mv.visitInsn(Opcodes.ATHROW)
        
        mv.visitLabel(notNullLabel)
        
        if (CoercionUtil.isPrimitiveType(resultType) && (resultType as? ClassTypeRef)?.isNullable == false) {
            CoercionUtil.emitUnboxing(resultType, mv)
        }
    }
}
