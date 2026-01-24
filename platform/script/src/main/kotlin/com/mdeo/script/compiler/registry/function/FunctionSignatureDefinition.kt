package com.mdeo.script.compiler.registry.function

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Represents a specific function signature (overload) for a function.
 *
 * Contains all information needed to emit bytecode for calling this function signature.
 * This can represent both global stdlib functions and file-scope functions.
 */
interface FunctionSignatureDefinition {
    /**
     * The overload key that uniquely identifies this method signature.
     *
     * For non-overloaded functions, this is typically an empty string "".
     * For overloaded functions (like methods that accept different types),
     * this is typically the distinguishing type name like "builtin.int", "builtin.long".
     */
    val overloadKey: String

    /**
     * The JVM method descriptor.
     * Example: "([Ljava/lang/Object;)Lcom/mdeo/script/stdlib/collections/ScriptList;"
     */
    val descriptor: String

    /**
     * The JVM internal name of the owner class.
     * Example: "com/mdeo/script/stdlib/globals/GlobalFunctions"
     */
    val ownerClass: String

    /**
     * The JVM method name to invoke.
     */
    val jvmMethodName: String

    /**
     * Whether this method uses varargs.
     */
    val isVarArgs: Boolean

    /**
     * The parameter type names for type coercion.
     * Example: ["builtin.string", "builtin.int"]
     * Empty list for varargs methods (all args boxed to Object).
     */
    val parameterTypes: List<String>

    /**
     * The return type name for type coercion.
     * Example: "builtin.int", "void", "builtin.string?"
     * Null for void return type.
     */
    val returnType: String?

    /**
     * Emits the method invocation bytecode.
     *
     * All arguments should already be on the stack when this is called.
     *
     * @param mv The method visitor to emit bytecode to.
     */
    fun emitInvocation(mv: MethodVisitor)
}

/**
 * Implementation of FunctionSignatureDefinition for static method calls.
 *
 * This is the primary implementation used for both stdlib global functions
 * (like println, listOf, setOf) and file-scope functions.
 *
 * @param overloadKey The overload key (empty string for non-overloaded or file-scope functions).
 * @param descriptor The JVM method descriptor.
 * @param ownerClass The owner class internal name.
 * @param jvmMethodName The JVM method name to invoke.
 * @param isVarArgs Whether this is a varargs method.
 * @param parameterTypes The parameter type names for coercion.
 * @param returnType The return type name for coercion (null for void).
 */
class StaticFunctionSignatureDefinition(
    override val overloadKey: String,
    override val descriptor: String,
    override val ownerClass: String,
    override val jvmMethodName: String,
    override val isVarArgs: Boolean = false,
    override val parameterTypes: List<String> = emptyList(),
    override val returnType: String? = null
) : FunctionSignatureDefinition {

    override fun emitInvocation(mv: MethodVisitor) {
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            ownerClass,
            jvmMethodName,
            descriptor,
            false
        )
    }
}
