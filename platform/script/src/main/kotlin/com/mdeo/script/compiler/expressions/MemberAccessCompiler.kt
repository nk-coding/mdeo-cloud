package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedMemberAccessExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.registry.type.TypeRegistry
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles member access expressions to bytecode.
 *
 * Member access expressions access properties/fields on objects.
 * Supports null-safe chaining (?.) where if the expression is null,
 * the result is null instead of throwing a NullPointerException.
 *
 * The compiler looks up property definitions from the [TypeRegistry] using:
 * - The target type name (e.g., "builtin.string")
 * - The property name
 *
 * If a property is not found in the registry, the compiler falls back to
 * file-scope behavior for custom types.
 */
class MemberAccessCompiler : ExpressionCompiler {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is a member access expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.MemberAccess
    }

    /**
     * Compiles a member access expression to bytecode.
     *
     * @param expression The typed member access expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val memberAccess = expression as TypedMemberAccessExpression
        val targetType = context.getType(memberAccess.expression.evalType)
        val resultType = context.getType(memberAccess.evalType)

        if (memberAccess.isNullChaining) {
            compileNullSafeMemberAccess(memberAccess, context, mv, targetType, resultType)
        } else {
            compileMemberAccess(memberAccess, context, mv, targetType, resultType)
        }
    }

    /**
     * Compiles a null-safe member access (?.).
     * If the expression is null, the result is null instead of throwing a NullPointerException.
     */
    private fun compileNullSafeMemberAccess(
        memberAccess: TypedMemberAccessExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ReturnType,
        resultType: ReturnType
    ) {
        val nullLabel = Label()
        val endLabel = Label()

        context.compileExpression(memberAccess.expression, mv)

        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNULL, nullLabel)

        emitMemberAccess(context, memberAccess.member, targetType, resultType, mv)

        if (resultType is ClassTypeRef && !resultType.isNullable &&
            CoercionUtil.getPrimitiveTypeName(resultType) != null
        ) {
            CoercionUtil.emitBoxing(resultType.type, mv)
        }

        mv.visitJumpInsn(Opcodes.GOTO, endLabel)

        mv.visitLabel(nullLabel)
        mv.visitInsn(Opcodes.POP)
        mv.visitInsn(Opcodes.ACONST_NULL)

        mv.visitLabel(endLabel)
    }

    /**
     * Compiles a regular (non-null-safe) member access.
     */
    private fun compileMemberAccess(
        memberAccess: TypedMemberAccessExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ReturnType,
        resultType: ReturnType
    ) {
        context.compileExpression(memberAccess.expression, mv)

        emitMemberAccess(context, memberAccess.member, targetType, resultType, mv)
    }

    /**
     * Emits the bytecode for accessing a member on the object on the stack.
     *
     * First tries to look up the property in the registry. If found, uses the
     * registry definition to emit the access. Otherwise falls back to file-scope behavior.
     */
    private fun emitMemberAccess(
        context: CompilationContext,
        memberName: String,
        targetType: ReturnType,
        resultType: ReturnType,
        mv: MethodVisitor
    ) {
        val lookupType = if (targetType is LambdaType) {
            "builtin.any"
        } else if (targetType is ClassTypeRef) {
            targetType.type
        } else {
            throw UnsupportedOperationException("Cannot access member on non-class/lambda type")
        }

        val propertyDef = context.typeRegistry.lookupProperty(lookupType, memberName)
            ?: throw UnsupportedOperationException("Property '$memberName' not found in type registry for type '$lookupType'")

        propertyDef.emitAccess(mv)
    }

}
