package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionCallExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.compiler.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.TypeConversionUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles expression call expressions to bytecode.
 * 
 * An expression call is when a lambda or function reference stored in a variable
 * is invoked directly, rather than through member access. For example:
 * 
 * ```
 * val f = (x: Int) => x + 1
 * val result = f(10)  // This is an expression call
 * ```
 * 
 * The compiler generates:
 * 1. Load the lambda object from the variable
 * 2. Push all arguments onto the stack (no boxing needed with custom interfaces)
 * 3. Invoke the `call` method on the custom functional interface using INVOKEINTERFACE
 */
class ExpressionCallCompiler : ExpressionCompiler {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if this is an expression call expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.ExpressionCall
    }
    
    /**
     * Compiles an expression call expression to bytecode.
     *
     * This method generates bytecode that:
     * 1. Loads the lambda object onto the stack
     * 2. Compiles all arguments with type conversions as needed (no boxing)
     * 3. Invokes the `call` method using INVOKEINTERFACE
     *
     * @param expression The expression call expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor for emitting bytecode instructions.
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val exprCall = expression as TypedExpressionCallExpression
        val calleeType = context.getType(exprCall.expression.evalType)
        
        if (calleeType !is LambdaType) {
            throw IllegalStateException(
                "Expression call requires a lambda type, but got: $calleeType"
            )
        }
        
        context.compileExpression(exprCall.expression, mv)
        
        compileArguments(exprCall, calleeType, context, mv)
        
        val functionalInterface = getFunctionalInterface(calleeType)
        val methodDescriptor = buildMethodDescriptor(calleeType, context)
        
        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            functionalInterface,
            "call",
            methodDescriptor,
            true
        )
    }
    
    /**
     * Compiles all arguments for the expression call.
     *
     * With custom interfaces using primitive types directly, no boxing is needed.
     *
     * @param exprCall The expression call containing the arguments.
     * @param calleeType The lambda type being called.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun compileArguments(
        exprCall: TypedExpressionCallExpression,
        calleeType: LambdaType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        for ((index, arg) in exprCall.arguments.withIndex()) {
            context.compileExpression(arg, mv)
            
            if (index < calleeType.parameters.size) {
                val argType = context.getType(arg.evalType)
                val paramType = calleeType.parameters[index].type
                
                if (argType is ClassTypeRef && paramType is ClassTypeRef) {
                    TypeConversionUtil.emitConversionIfNeeded(argType, paramType, mv)
                }
            }
        }
    }
    
    /**
     * Builds the method descriptor for the call method.
     *
     * @param lambdaType The lambda type.
     * @param context The compilation context.
     * @return The method descriptor (e.g., "(I)I" for (int) -> int).
     */
    private fun buildMethodDescriptor(lambdaType: LambdaType, context: CompilationContext): String {
        val params = lambdaType.parameters.joinToString("") { param ->
            context.getTypeDescriptor(param.type)
        }
        val returnDesc = context.getTypeDescriptor(lambdaType.returnType)
        return "($params)$returnDesc"
    }
    
    /**
     * Determines the appropriate functional interface for a lambda type.
     *
     * Uses the centralized CoercionUtil.getFunctionalInterfaceName to generate
     * interface names following the pattern: Lambda$ReturnType$ParamTypes
     *
     * @param lambdaType The lambda type to map to a functional interface.
     * @return The fully qualified internal name of the functional interface.
     */
    private fun getFunctionalInterface(lambdaType: LambdaType): String {
        val parameterTypes = lambdaType.parameters.map { it.type }
        return CoercionUtil.getFunctionalInterfaceName(lambdaType.returnType, parameterTypes)
    }
}
