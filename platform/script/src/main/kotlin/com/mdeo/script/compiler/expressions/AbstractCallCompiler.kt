package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.VoidType
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
abstract class AbstractCallCompiler : ExpressionCompiler() {

    /**
     * Emits return type coercion if needed.
     *
     * Handles unboxing when the return type from the function signature is a
     * generic (Any?) but the expected type at the call site is a primitive.
     *
     * @param expectedType The expected return type at the call site.
     * @param actualReturnType The actual return type from the method signature.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    protected fun emitReturnTypeCoercion(
        expectedType: ReturnType,
        actualReturnType: ReturnType,
        mv: MethodVisitor
    ) {
        if (expectedType !is ClassTypeRef || actualReturnType !is ClassTypeRef) {
            return
        }

        val isReturnNullable = actualReturnType.isNullable
        val returnBaseName = actualReturnType.type

        if ((returnBaseName == "builtin.any" || isReturnNullable) &&
            !expectedType.isNullable && CoercionUtil.isPrimitiveType(expectedType)) {
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

            val argType = context.getType(arg.evalType)
            context.compileExpression(arg, mv, argType)

            if (argType is ClassTypeRef && !argType.isNullable && CoercionUtil.isPrimitiveType(argType)) {
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
        parameterTypes: List<ValueType>,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        for ((index, arg) in arguments.withIndex()) {
            val argType = context.getType(arg.evalType)
            val expectedType = if (index < parameterTypes.size) parameterTypes[index] else argType
            context.compileExpression(arg, mv, expectedType)
        }
    }

}
