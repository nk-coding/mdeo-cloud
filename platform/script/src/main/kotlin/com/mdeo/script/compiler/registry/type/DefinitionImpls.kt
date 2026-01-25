package com.mdeo.script.compiler.registry.type

import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.ValueType
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Implementation of a type definition with mutable collections
 * for building through the DSL.
 *
 * @param typeName The unique name of this type.
 * @param extends The names of types this type extends.
 */
class TypeDefinitionImpl(
    override val typeName: String,
    override val extends: List<String> = emptyList()
) : TypeDefinition {

    private val methods: MutableMap<String, MutableList<MethodDefinition>> = mutableMapOf()
    private val properties: MutableMap<String, PropertyDefinition> = mutableMapOf()

    override fun getMethods(name: String): List<MethodDefinition> {
        return methods[name] ?: emptyList()
    }

    override fun getMethod(name: String, overloadKey: String): MethodDefinition? {
        return methods[name]?.find { it.overloadKey == overloadKey }
    }

    override fun getProperty(name: String): PropertyDefinition? {
        return properties[name]
    }

    override val methodNames: Set<String>
        get() = methods.keys

    override val propertyNames: Set<String>
        get() = properties.keys

    /**
     * Adds a method definition to this type.
     *
     * @param method The method definition to add.
     */
    fun addMethod(method: MethodDefinition) {
        methods.getOrPut(method.name) { mutableListOf() }.add(method)
    }

    /**
     * Adds a property definition to this type.
     *
     * @param property The property definition to add.
     */
    fun addProperty(property: PropertyDefinition) {
        properties[property.name] = property
    }
}

/**
 * Method definition for static helper methods.
 *
 * The receiver value is passed as the first argument to the static method.
 *
 * @param name The method name in the script language.
 * @param overloadKey The overload key for matching (empty string for non-overloaded).
 * @param descriptor The JVM method descriptor (including receiver as first param).
 * @param ownerClass The helper class containing the static method.
 * @param jvmMethodName The actual JVM method name (may differ from [name]).
 * @param isVarArgs Whether this method uses varargs.
 * @param parameterTypes The parameter types for coercion (excluding receiver).
 * @param returnType The return type for coercion.
 */
class StaticMethodDefinition(
    override val name: String,
    override val overloadKey: String,
    override val descriptor: String,
    override val ownerClass: String,
    override val jvmMethodName: String = name,
    override val isVarArgs: Boolean = false,
    override val parameterTypes: List<ValueType>,
    override val returnType: ReturnType
) : MethodDefinition {

    override val isStatic: Boolean = true
    override val isInterface: Boolean = false

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

/**
 * Method definition for instance methods.
 *
 * Uses INVOKEVIRTUAL or INVOKEINTERFACE based on [isInterface].
 *
 * @param name The method name in the script language.
 * @param overloadKey The overload key for matching (empty string for non-overloaded).
 * @param descriptor The JVM method descriptor (without receiver).
 * @param ownerClass The class/interface containing the method.
 * @param isInterface Whether to use INVOKEINTERFACE.
 * @param jvmMethodName The actual JVM method name (may differ from [name]).
 * @param isVarArgs Whether this method uses varargs.
 * @param parameterTypes The parameter types for coercion.
 * @param returnType The return type for coercion.
 */
class InstanceMethodDefinition(
    override val name: String,
    override val overloadKey: String,
    override val descriptor: String,
    override val ownerClass: String,
    override val isInterface: Boolean = false,
    override val jvmMethodName: String = name,
    override val isVarArgs: Boolean = false,
    override val parameterTypes: List<ValueType>,
    override val returnType: ReturnType
) : MethodDefinition {

    override val isStatic: Boolean = false

    override fun emitInvocation(mv: MethodVisitor) {
        mv.visitMethodInsn(
            if (isInterface) Opcodes.INVOKEINTERFACE else Opcodes.INVOKEVIRTUAL,
            ownerClass,
            jvmMethodName,
            descriptor,
            isInterface
        )
    }
}

/**
 * Property definition for static helper access.
 *
 * The receiver value is passed as an argument to a static method.
 *
 * @param name The property name in the script language.
 * @param descriptor The JVM type descriptor of the property value.
 * @param ownerClass The helper class containing the static method.
 * @param getterName The static method name to call.
 * @param receiverDescriptor The JVM descriptor for the receiver type.
 */
class StaticPropertyDefinition(
    override val name: String,
    override val descriptor: String,
    override val ownerClass: String,
    override val getterName: String = name,
    private val receiverDescriptor: String
) : PropertyDefinition {

    override val isStatic: Boolean = true
    override val isInterface: Boolean = false

    override fun emitAccess(mv: MethodVisitor) {
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            ownerClass,
            getterName,
            "($receiverDescriptor)$descriptor",
            false
        )
    }

    override fun emitSet(mv: MethodVisitor, valueDescriptor: String) {
        throw UnsupportedOperationException("Property '$name' is read-only")
    }
}

/**
 * Property definition for instance getter access.
 *
 * Uses INVOKEVIRTUAL or INVOKEINTERFACE based on [isInterface].
 *
 * @param name The property name in the script language.
 * @param descriptor The JVM type descriptor of the property value.
 * @param ownerClass The class/interface containing the getter.
 * @param isInterface Whether to use INVOKEINTERFACE.
 * @param getterName The getter method name.
 */
class InstancePropertyDefinition(
    override val name: String,
    override val descriptor: String,
    override val ownerClass: String,
    override val isInterface: Boolean = false,
    override val getterName: String = "get${name.replaceFirstChar { it.uppercaseChar() }}"
) : PropertyDefinition {

    override val isStatic: Boolean = false

    override fun emitAccess(mv: MethodVisitor) {
        mv.visitMethodInsn(
            if (isInterface) Opcodes.INVOKEINTERFACE else Opcodes.INVOKEVIRTUAL,
            ownerClass,
            getterName,
            "()$descriptor",
            isInterface
        )
    }

    override fun emitSet(mv: MethodVisitor, valueDescriptor: String) {
        throw UnsupportedOperationException("Property '$name' is read-only")
    }
}
