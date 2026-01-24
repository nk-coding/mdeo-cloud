package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.util.TypeConversionUtil
import com.mdeo.script.compiler.util.MethodDescriptorUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Abstract base class for call expression compilers.
 *
 * This class provides shared functionality for:
 * - Parameter type coercion (boxing primitives, type widening)
 * - Return type coercion (unboxing when needed)
 * - Varargs argument packaging
 * - Common compile patterns
 *
 * Subclasses implement specific invocation logic for:
 * - Global function calls ([FunctionCallCompiler])
 * - Member method calls ([MemberCallCompiler])
 */
abstract class AbstractCallCompiler : ExpressionCompiler {

    /**
     * Emits argument coercion bytecode if needed.
     *
     * Handles boxing primitives for generic parameters (Any?) and type widening.
     * For nullable parameters, if the argument is a primitive of a different type,
     * the primitive is first widened to the target type and then boxed to the
     * target wrapper type. For same-type or Any? parameters, the source type
     * is simply boxed.
     *
     * @param argType The actual type of the argument expression.
     * @param paramType The expected parameter type from the function signature.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun emitArgumentCoercion(
        argType: ReturnType,
        paramType: String,
        mv: MethodVisitor
    ) {
        if (argType !is ClassTypeRef) {
            return
        }

        val isParamNullable = paramType.endsWith("?")
        val paramBaseName = if (isParamNullable) paramType.dropLast(1) else paramType

        if (paramBaseName == "builtin.any" || isParamNullable) {
            if (!argType.isNullable && CoercionUtil.isPrimitiveType(argType.type)) {
                emitPrimitiveToBoxedCoercion(argType.type, paramBaseName, mv)
            }
        } else {
            val targetType = ClassTypeRef(paramBaseName, isParamNullable)
            CoercionUtil.emitCoercion(argType, targetType, mv)
        }
    }

    /**
     * Emits argument coercion bytecode with expression context.
     *
     * This variant provides additional context from the source expression
     * for more precise coercion decisions.
     *
     * @param argType The actual type of the argument expression.
     * @param paramType The expected parameter type from the function signature.
     * @param arg The source argument expression.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun emitArgumentCoercion(
        argType: ReturnType,
        paramType: String,
        arg: TypedExpression,
        mv: MethodVisitor
    ) {
        if (argType !is ClassTypeRef) {
            return
        }

        val isParamNullable = paramType.endsWith("?")
        val paramBaseName = if (isParamNullable) paramType.dropLast(1) else paramType

        if (paramBaseName == "builtin.any" || isParamNullable) {
            if (!argType.isNullable && CoercionUtil.isPrimitiveType(argType.type)) {
                emitPrimitiveToBoxedCoercion(argType.type, paramBaseName, mv)
            }
        } else {
            val targetType = ClassTypeRef(paramBaseName, isParamNullable)
            CoercionUtil.emitCoercion(argType, targetType, arg, mv)
        }
    }

    /**
     * Emits bytecode to widen a primitive type and box it to the target wrapper type.
     *
     * If the source and target are different primitive types, the source is first
     * widened (e.g., int -> long) and then boxed. If they are the same type or
     * the target is Any, the source is simply boxed.
     *
     * @param sourceType The source primitive type name (e.g., "builtin.int").
     * @param targetType The target type name (e.g., "builtin.long" or "builtin.any").
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun emitPrimitiveToBoxedCoercion(sourceType: String, targetType: String, mv: MethodVisitor) {
        if (CoercionUtil.isPrimitiveType(targetType) && sourceType != targetType) {
            TypeConversionUtil.emitConversion(sourceType, targetType, mv)
            CoercionUtil.emitBoxing(targetType, mv)
        } else {
            CoercionUtil.emitBoxing(sourceType, mv)
        }
    }

    /**
     * Emits return type coercion if needed.
     *
     * Handles unboxing when the return type from the function signature is a
     * generic (Any?) but the expected type at the call site is a primitive.
     *
     * @param expectedType The expected return type at the call site.
     * @param actualReturnType The actual return type from the method signature (may be null).
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun emitReturnTypeCoercion(
        expectedType: ReturnType,
        actualReturnType: String?,
        mv: MethodVisitor
    ) {
        if (expectedType !is ClassTypeRef || actualReturnType == null) {
            return
        }

        val isReturnNullable = actualReturnType.endsWith("?")
        val returnBaseName = if (isReturnNullable) actualReturnType.dropLast(1) else actualReturnType

        if ((returnBaseName == "builtin.any" || isReturnNullable) &&
            !expectedType.isNullable && CoercionUtil.isPrimitiveType(expectedType.type)) {
            CoercionUtil.emitUnboxing(expectedType.type, mv)
        }
    }

    /**
     * Compiles varargs arguments into an Object array.
     *
     * For varargs calls, all arguments are packaged into a single Object[]
     * before the method invocation. Primitive values are boxed automatically.
     *
     * @param arguments The list of argument expressions.
     * @param context The compilation context.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun compileVarArgsArray(
        arguments: List<TypedExpression>,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val argCount = arguments.size

        mv.visitLdcInsn(argCount)
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")

        for ((index, arg) in arguments.withIndex()) {
            mv.visitInsn(Opcodes.DUP)
            mv.visitLdcInsn(index)

            context.compileExpression(arg, mv)

            val argType = context.getType(arg.evalType)
            if (argType is ClassTypeRef && !argType.isNullable && CoercionUtil.isPrimitiveType(argType.type)) {
                CoercionUtil.emitBoxing(argType.type, mv)
            }

            mv.visitInsn(Opcodes.AASTORE)
        }
    }

    /**
     * Compiles arguments with type coercion based on parameter types.
     *
     * Each argument is compiled and then coerced to match the expected
     * parameter type from the method signature.
     *
     * @param arguments The list of argument expressions.
     * @param parameterTypes The list of expected parameter types.
     * @param context The compilation context.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun compileArgumentsWithCoercion(
        arguments: List<TypedExpression>,
        parameterTypes: List<String>,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        for ((index, arg) in arguments.withIndex()) {
            context.compileExpression(arg, mv)

            if (index < parameterTypes.size) {
                val argType = context.getType(arg.evalType)
                emitArgumentCoercion(argType, parameterTypes[index], arg, mv)
            }
        }
    }

}
