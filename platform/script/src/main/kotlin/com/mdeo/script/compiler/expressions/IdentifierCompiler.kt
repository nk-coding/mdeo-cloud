package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.CompilationException
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.RefTypeUtil
import com.mdeo.script.compiler.VariableInfo
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles identifier expressions to bytecode.
 * 
 * An identifier expression loads the value of a local variable onto the stack.
 * If the variable is wrapped in a Ref type (because it's written by a lambda),
 * this compiler loads the Ref and then reads its .value field.
 * 
 * For global scope identifiers (scope level 0), this compiler first checks
 * the global function registry for global properties, then falls back to
 * local variable lookup.
 * 
 * This compiler uses polymorphic scope lookup to handle both regular function
 * scopes and lambda body scopes uniformly. The LambdaBodyScope class returns
 * VariableInfo with the correct slot indices and isWrittenByLambda flags for:
 * - Lambda parameters (loaded from local slots)
 * - Captured variables (loaded from parameter slots, with isWrittenByLambda=true if Ref-wrapped)
 * - Local variables declared in the lambda body
 * 
 * This compiler overrides [compile] to provide correct coercion from the variable's
 * actual type (from scope lookup) to the expected type, rather than relying on
 * the AST's evalType which may differ (e.g., for lambda parameters with generic types).
 */
class IdentifierCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a [TypedIdentifierExpression], false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedIdentifierExpression
    }

    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val identifier = expression as TypedIdentifierExpression

        if (identifier.scope == 0) {
            val globalProperty = context.globalPropertyRegistry.getProperty(identifier.name)
                ?: throw CompilationException("Variable not found: ${identifier.name} at scope level ${identifier.scope}")
            globalProperty.emitAccess(mv)
            return
        }

        if (identifier.scope == 1) {
            val fileScopeProperty = context.fileScopePropertyRegistry.getProperty(identifier.name)
                ?: throw CompilationException("Variable not found: ${identifier.name} at scope level ${identifier.scope}")
            fileScopeProperty.emitAccess(mv)
            return
        }

        val variable = context.currentScope?.lookupVariable(identifier.name, identifier.scope)
            ?: throw IllegalStateException("Variable not found: ${identifier.name} at scope level ${identifier.scope}")

        compileVariableLoad(variable, variable.type, mv)
    }

    /**
     * Compiles a variable load, handling Ref-wrapped variables if needed.
     *
     * For variables with isWrittenByLambda=true (Ref-wrapped), loads the Ref object
     * first, then reads its `.value` field. Otherwise, loads directly from the slot.
     *
     * @param variable The variable info containing slot and Ref-wrapping information.
     * @param type The type of the variable.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun compileVariableLoad(
        variable: VariableInfo,
        type: ReturnType,
        mv: MethodVisitor
    ) {
        if (variable.isWrittenByLambda && variable.slotIndex >= 0) {
            compileRefWrappedVariableLoad(variable.slotIndex, type, mv)
        } else {
            val loadOpcode = ASMUtil.getLoadOpcode(type)
            mv.visitVarInsn(loadOpcode, variable.slotIndex)
        }
    }

    /**
     * Compiles a load of a variable wrapped in a Ref type.
     *
     * First loads the Ref object from the local variable slot, then reads its `.value` field.
     *
     * @param slotIndex The local variable slot containing the Ref object.
     * @param type The underlying type of the variable (used to determine the Ref class).
     * @param mv The method visitor for emitting bytecode.
     */
    private fun compileRefWrappedVariableLoad(
        slotIndex: Int,
        type: ReturnType,
        mv: MethodVisitor
    ) {
        mv.visitVarInsn(Opcodes.ALOAD, slotIndex)

        val refClassName = RefTypeUtil.getRefClassName(type)
        val valueDescriptor = RefTypeUtil.getRefValueDescriptor(type)
        mv.visitFieldInsn(Opcodes.GETFIELD, refClassName, "value", valueDescriptor)
    }
}
