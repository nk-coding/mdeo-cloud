package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionCallExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.ast.types.ReturnType
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
        return expression.kind == TypedExpressionKind.ExpressionCall
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
        val normalizedKey = registry.createKey(calleeType)
        
        val lookupResult = registry.getInterfaceForLambdaType(calleeType)
        val functionalInterface = lookupResult.interfaceName
        val isPredefined = functionalInterface.startsWith("com/mdeo/script/runtime")
        
        compileArguments(exprCall, calleeType, context, mv, isPredefined)
        
        // Use calleeType for generated interfaces (actual types), normalizedKey for predefined (erased types)
        val methodDescriptor = buildMethodDescriptor(if (isPredefined) normalizedKey else calleeType, context)
        
        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            functionalInterface,
            "call",
            methodDescriptor,
            true
        )
        
        // Unbox return value only if the interface returns Object (uses predefined generic interface)
        if (isPredefined) {
            emitUnboxingIfNeeded(calleeType.returnType, mv)
        }
    }
    
    /**
     * Compiles all arguments for the expression call.
     *
     * For predefined generic interfaces (Func0-3, Action0-3, Predicate1),
     * arguments need to be boxed to Object. For generated interfaces,
     * arguments use the actual types directly.
     *
     * @param exprCall The expression call containing the arguments.
     * @param calleeType The actual lambda type being called.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param isPredefinedInterface Whether the interface is a predefined runtime interface.
     */
    private fun compileArguments(
        exprCall: TypedExpressionCallExpression,
        calleeType: LambdaType,
        context: CompilationContext,
        mv: MethodVisitor,
        isPredefinedInterface: Boolean
    ) {
        val parameterTypes = calleeType.parameters.map { it.type }
        for ((index, arg) in exprCall.arguments.withIndex()) {
            val argType = context.getType(arg.evalType)
            val targetType = if (index < parameterTypes.size) parameterTypes[index] else argType
            context.compileExpression(arg, mv, targetType)
            
            // Only box primitives if using a predefined interface that expects Object
            if (isPredefinedInterface) {
                emitBoxingIfNeeded(targetType, mv)
            }
        }
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
        
        // Check if this is a predefined interface (in the runtime package)
        val isPredefined = lookupResult.interfaceName.startsWith("com/mdeo/script/runtime")
        
        return if (isPredefined) {
            // Predefined interfaces use erased Object types
            buildErasedMethodDescriptor(lambdaType)
        } else {
            // Generated interfaces use actual types with boxed wrappers
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
        val returnDesc = if (lambdaType.returnType is com.mdeo.script.ast.types.VoidType) {
            "V"
        } else {
            "Ljava/lang/Object;"
        }
        return "($params)$returnDesc"
    }

    /**
     * Compiles arguments with boxing when needed for predefined interfaces.
     *
     * @param arguments The argument expressions to compile.
     * @param parameterTypes The actual parameter types from the lambda.
     * @param normalizedParamTypes The normalized parameter types (from the interface).
     * @param context The compilation context.
     * @param mv The method visitor.
     * @param needsBoxing Whether ANY arguments need to be boxed (determines descriptor type).
     */
    private fun compileArgumentsWithBoxing(
        arguments: List<TypedExpression>,
        parameterTypes: List<ReturnType>,
        normalizedParamTypes: List<ReturnType>,
        context: CompilationContext,
        mv: MethodVisitor,
        needsBoxing: Boolean
    ) {
        for ((index, arg) in arguments.withIndex()) {
            val argType = context.getType(arg.evalType)
            val targetType = if (index < parameterTypes.size) parameterTypes[index] else argType
            context.compileExpression(arg, mv, targetType)
            if (index < normalizedParamTypes.size && needsBoxing) {
                // Only box if the NORMALIZED parameter type needs boxing
                if (LambdaInterfaceRegistry.needsBoxing(normalizedParamTypes[index])) {
                    emitBoxingIfNeeded(targetType, mv)
                }
            }
        }
    }

    /**
     * Emits boxing instructions for primitive types when calling generic interfaces.
     *
     * Only boxes non-nullable primitive types. Nullable types are already boxed
     * by the coercion step.
     *
     * @param type The type to box if primitive (must be non-nullable).
     * @param mv The method visitor.
     */
    private fun emitBoxingIfNeeded(type: ReturnType, mv: MethodVisitor) {
        if (type !is ClassTypeRef) return
        if (type.isNullable) return
        
        when (type.type) {
            "builtin.int" -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false
            )
            "builtin.long" -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false
            )
            "builtin.float" -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false
            )
            "builtin.double" -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false
            )
            "builtin.boolean" -> mv.visitMethodInsn(
                Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false
            )
        }
    }

    /**
     * Emits unboxing instructions for the return value of generic interfaces.
     *
     * Only unboxes to non-nullable primitive types. For nullable types, only does
     * a CHECKCAST to the expected boxed type.
     *
     * @param returnType The return type to unbox if non-nullable primitive.
     * @param mv The method visitor.
     */
    private fun emitUnboxingIfNeeded(returnType: ReturnType, mv: MethodVisitor) {
        if (returnType is com.mdeo.script.ast.types.VoidType) return
        if (returnType !is ClassTypeRef) return
        
        if (returnType.isNullable) {
            emitNullableCheckCast(returnType, mv)
            return
        }
        
        when (returnType.type) {
            "builtin.int" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false
                )
            }
            "builtin.long" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long")
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false
                )
            }
            "builtin.float" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float")
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false
                )
            }
            "builtin.double" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double")
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false
                )
            }
            "builtin.boolean" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean")
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false
                )
            }
            "builtin.string" -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
            }
        }
    }

    /**
     * Emits a CHECKCAST for nullable types.
     * 
     * @param returnType The nullable return type.
     * @param mv The method visitor.
     */
    private fun emitNullableCheckCast(returnType: ClassTypeRef, mv: MethodVisitor) {
        when (returnType.type) {
            "builtin.int" -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
            "builtin.long" -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long")
            "builtin.float" -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float")
            "builtin.double" -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double")
            "builtin.boolean" -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean")
            "builtin.string" -> mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
        }
    }
}
