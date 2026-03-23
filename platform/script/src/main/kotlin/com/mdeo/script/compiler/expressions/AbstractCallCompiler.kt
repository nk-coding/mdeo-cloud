package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedCallArgument
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Abstract base class for call expression compilers.
 *
 * This class provides shared functionality for:
 * - Parameter type coercion (boxing primitives, type widening) based on resolved parameter types
 *   provided by each [TypedCallArgument]
 * - Return type coercion (unboxing when needed)
 * - Varargs argument packaging with proper coercion
 * - Common compile patterns
 *
 * Subclasses implement specific invocation logic for:
 * - Global function calls ([FunctionCallCompiler])
 * - Member method calls ([MemberCallCompiler])
 * - Expression (lambda) calls ([ExpressionCallCompiler])
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
        val isReturnAny = actualReturnType.`package` == "builtin" && actualReturnType.type == "Any"

        if ((isReturnAny || isReturnNullable) &&
            !expectedType.isNullable && CoercionUtil.isPrimitiveType(expectedType)) {
            CoercionUtil.emitUnboxing(expectedType, mv)
        }
    }

    /**
     * Compiles call arguments with type coercion, handling both regular and varargs parameters.
     *
     * Each [TypedCallArgument] carries its own expected parameter type (resolved during
     * type checking, including generic substitution). This method uses that type to apply
     * correct coercion for each argument.
     *
     * Coercion is applied in two steps:
     * 1. Coerce from the argument's actual type to the resolved parameter type from
     *    [TypedCallArgument.parameterType] (e.g., int → double for generic resolution).
     * 2. Coerce from the resolved parameter type to the JVM method's expected type from
     *    [signatureParameterTypes] (e.g., boxing an int to Object when the JVM method
     *    takes Object). This step is only needed when the JVM type differs from the
     *    resolved type.
     *
     * For varargs, arguments starting at [varArgsStartIndex] are packaged into an Object[]
     * array. Each varargs element is first coerced to its expected type, then boxed if it is
     * a primitive (since Object[] can only hold reference types). Arguments before
     * [varArgsStartIndex] are compiled as regular parameters with coercion.
     *
     * @param arguments The list of call arguments with their expected parameter types.
     * @param context The compilation context.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     * @param signatureParameterTypes The JVM-level parameter types from the function/method
     *                                 signature. Used for the second coercion step (boxing,
     *                                 widening to match JVM descriptor). Empty list if not
     *                                 available (e.g., for expression calls where JVM types
     *                                 are derived differently).
     * @param varArgsStartIndex The index at which varargs begin. Arguments before this index
     *                          are compiled as regular parameters. Null means no varargs
     */
    protected fun compileArgumentsWithCoercion(
        arguments: List<TypedCallArgument>,
        context: CompilationContext,
        mv: MethodVisitor,
        signatureParameterTypes: List<ValueType> = emptyList(),
        varArgsStartIndex: Int? = null
    ) {
        if (varArgsStartIndex == null) {
            for ((i, arg) in arguments.withIndex()) {
                val resolvedType = context.getType(arg.parameterType)
                context.compileExpression(arg.value, mv, resolvedType)
                if (i < signatureParameterTypes.size) {
                    CoercionUtil.emitCoercion(resolvedType, signatureParameterTypes[i], mv, context)
                }
            }
            return
        }

        for (i in 0 until varArgsStartIndex.coerceAtMost(arguments.size)) {
            val arg = arguments[i]
            val resolvedType = context.getType(arg.parameterType)
            context.compileExpression(arg.value, mv, resolvedType)
            if (i < signatureParameterTypes.size) {
                CoercionUtil.emitCoercion(resolvedType, signatureParameterTypes[i], mv, context)
            }
        }

        val varArgs = arguments.subList(varArgsStartIndex.coerceAtMost(arguments.size), arguments.size)
        mv.visitLdcInsn(varArgs.size)
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")

        for ((index, arg) in varArgs.withIndex()) {
            mv.visitInsn(Opcodes.DUP)
            mv.visitLdcInsn(index)

            val resolvedType = context.getType(arg.parameterType)
            context.compileExpression(arg.value, mv, resolvedType)

            if (resolvedType is ClassTypeRef && !resolvedType.isNullable && CoercionUtil.isPrimitiveType(resolvedType)) {
                CoercionUtil.emitBoxing(resolvedType, mv)
            }

            mv.visitInsn(Opcodes.AASTORE)
        }
    }

}
