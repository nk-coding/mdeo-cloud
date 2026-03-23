package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedExpressionCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.LambdaInterfaceRegistry
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.util.MethodDescriptorUtil
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
class ExpressionCallCompiler : AbstractCallCompiler() {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if this is an expression call expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "call"
    }
    
    /**
     * Compiles an expression call expression to bytecode.
     *
     * This method generates bytecode that:
     * 1. Loads the lambda object onto the stack
     * 2. Compiles all arguments with type conversions as needed (with boxing for generic interfaces)
     * 3. Invokes the `call` method using INVOKEINTERFACE
     *
     * @param expression The expression call expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor for emitting bytecode instructions.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val exprCall = expression as TypedExpressionCallExpression
        val calleeType = context.getType(exprCall.expression.evalType)
        
        if (calleeType !is LambdaType) {
            throw IllegalStateException(
                "Expression call requires a lambda type, but got: $calleeType"
            )
        }
        
        context.compileExpression(exprCall.expression, mv, calleeType)
        
        val registry = context.getLambdaInterfaceRegistry()
        
        val lookupResult = registry.getInterfaceForLambdaType(calleeType)
        val functionalInterface = lookupResult.interfaceName
        val isPredefined = functionalInterface.startsWith("com/mdeo/script/runtime")
        
        compileArguments(exprCall, calleeType, isPredefined, context, mv)
        
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
     * Uses the resolved parameter types from each [TypedCallArgument] for coercion.
     * For predefined generic interfaces (Func0-3, Action0-3), all JVM parameters are Object,
     * so primitives must be boxed. For generated interfaces, the JVM parameters match the
     * lambda's actual parameter types.
     *
     * @param exprCall The expression call containing the arguments.
     * @param lambdaType The lambda type of the callee.
     * @param isPredefined Whether the functional interface is a predefined generic one.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun compileArguments(
        exprCall: TypedExpressionCallExpression,
        lambdaType: LambdaType,
        isPredefined: Boolean,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val jvmParamTypes: List<ValueType> = if (isPredefined) {
            // Predefined interfaces use Object for all parameters
            lambdaType.parameters.map { ClassTypeRef("builtin", "Any", true) }
        } else {
            // Generated interfaces use the actual types from the lambda
            lambdaType.parameters.map { it.type }
        }
        compileArgumentsWithCoercion(exprCall.arguments, context, mv, signatureParameterTypes = jvmParamTypes)
    }
    
    /**
     * Builds the method descriptor for the call method.
     *
     * This must match the descriptor of the generated interface's call method.
     * Generated interfaces use the actual types (with boxed wrappers for nullables),
     * while predefined interfaces (Func0-3, Action0-3) use erased Object types.
     *
     * @param lambdaType The lambda type (normalized key).
     * @param context The compilation context.
     * @return The method descriptor.
     */
    private fun buildMethodDescriptor(lambdaType: LambdaType, context: CompilationContext): String {
        val registry = context.getLambdaInterfaceRegistry()
        val lookupResult = registry.getInterfaceForLambdaType(lambdaType)
        
        val isPredefined = lookupResult.interfaceName.startsWith("com/mdeo/script/runtime")
        
        return if (isPredefined) {
            buildErasedMethodDescriptor(lambdaType)
        } else {
            val parameterTypes = lambdaType.parameters.map { it.type }
            MethodDescriptorUtil.buildDescriptor(parameterTypes, lambdaType.returnType)
        }
    }

    /**
     * Builds the erased method descriptor for generic interfaces.
     * 
     * All type parameters become Object, void stays void.
     * 
     * @param lambdaType The lambda type.
     * @return The erased method descriptor (e.g., "(Ljava/lang/Object;)Ljava/lang/Object;").
     */
    private fun buildErasedMethodDescriptor(lambdaType: LambdaType): String {
        val params = lambdaType.parameters.joinToString("") { "Ljava/lang/Object;" }
        val returnDesc = if (lambdaType.returnType is VoidType) {
            "V"
        } else {
            "Ljava/lang/Object;"
        }
        return "($params)$returnDesc"
    }
}
