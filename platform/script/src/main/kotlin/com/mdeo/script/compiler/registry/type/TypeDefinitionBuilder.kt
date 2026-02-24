package com.mdeo.script.compiler.registry.type

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.VoidType

/**
 * DSL for building type definitions.
 *
 * Overload keys must match what TypeScript sends:
 * - Non-overloaded methods: use "" (empty string)
 * - Overloaded methods (like max/min): use the type identifier (e.g., "int", "long")
 *
 * Example usage:
 * ```kotlin
 * typeDefinition("builtin", "int") {
 *     extends("builtin", "any")
 *
 *     // Non-overloaded method: use empty string as overload key
 *     staticMethod("abs") {
 *         overload("", "(I)I", INT_HELPER, parameterTypes = emptyList(), returnType = BuiltinTypes.INT)
 *     }
 *
 *     // Overloaded method: use type identifier as overload key
 *     staticMethod("max") {
 *         overload("int", "(II)I", INT_HELPER, "max", parameterTypes = listOf(BuiltinTypes.INT), returnType = BuiltinTypes.INT)
 *     }
 * }
 * ```
 */
fun typeDefinition(typePackage: String, typeName: String, block: TypeDefinitionBuilder.() -> Unit): TypeDefinition {
    val builder = TypeDefinitionBuilder(typePackage, typeName)
    builder.block()
    return builder.build()
}

/**
 * Builder for TypeDefinition.
 *
 * @param typePackage The package part of the type's fully qualified name (e.g., "builtin", "class/path/to/file").
 * @param typeName The simple name of the type being defined (e.g., "int", "House").
 */
class TypeDefinitionBuilder(private val typePackage: String, private val typeName: String) {

    private val extends = mutableListOf<ClassTypeRef>()
    private val methods = mutableListOf<MethodDefinition>()
    private val properties = mutableListOf<PropertyDefinition>()
    private var jvmClassName: String? = null
    private var primitiveDescriptor: String? = null
    private var wrapperClassName: String? = null

    /**
     * Specifies that this type extends another type.
     *
     * @param parentType The parent type as a [ClassTypeRef].
     */
    fun extends(parentType: ClassTypeRef) {
        extends.add(parentType)
    }

    /**
     * Specifies that this type extends another type using package and name.
     *
     * @param pkg The package part of the parent type (e.g., "builtin", "class/path/to/file").
     * @param name The simple name of the parent type (e.g., "any", "House").
     * @param isNullable Whether the parent type reference is nullable.
     */
    fun extends(pkg: String, name: String, isNullable: Boolean = false) {
        extends.add(ClassTypeRef(`package` = pkg, type = name, isNullable = isNullable))
    }

    /**
     * Sets the JVM class name for this type.
     *
     * @param className The JVM internal class name (e.g., "java/lang/String").
     */
    fun jvmClass(className: String) {
        jvmClassName = className
    }

    /**
     * Sets the JVM primitive descriptor for this type.
     *
     * @param descriptor The JVM primitive descriptor (e.g., "I", "J", "D").
     */
    fun primitiveDesc(descriptor: String) {
        primitiveDescriptor = descriptor
    }

    /**
     * Sets the JVM wrapper class name for primitive types.
     *
     * @param className The JVM wrapper class name (e.g., "java/lang/Integer").
     */
    fun wrapperClass(className: String) {
        wrapperClassName = className
    }

    /**
     * Defines a static method (implemented as a static helper).
     *
     * @param name The method name.
     * @param block Builder for the method definition.
     */
    fun staticMethod(name: String, block: StaticMethodBuilder.() -> Unit) {
        val builder = StaticMethodBuilder(name)
        builder.block()
        methods.addAll(builder.build())
    }

    /**
     * Defines an instance method.
     *
     * @param name The method name.
     * @param block Builder for the method definition.
     */
    fun instanceMethod(name: String, block: InstanceMethodBuilder.() -> Unit) {
        val builder = InstanceMethodBuilder(name)
        builder.block()
        methods.addAll(builder.build())
    }

    /**
     * Defines a static property (implemented as a static helper).
     *
     * @param name The property name.
     * @param block Builder for the property definition.
     */
    fun staticProperty(name: String, block: StaticPropertyBuilder.() -> Unit) {
        val builder = StaticPropertyBuilder(name)
        builder.block()
        properties.add(builder.build())
    }

    /**
     * Defines an instance property.
     *
     * @param name The property name.
     * @param block Builder for the property definition.
     */
    fun instanceProperty(name: String, block: InstancePropertyBuilder.() -> Unit) {
        val builder = InstancePropertyBuilder(name)
        builder.block()
        properties.add(builder.build())
    }

    /**
     * Builds the type definition.
     */
    fun build(): TypeDefinition {
        val typeDef = TypeDefinitionImpl(typePackage, typeName, extends, jvmClassName, primitiveDescriptor, wrapperClassName)
        methods.forEach { typeDef.addMethod(it) }
        properties.forEach { typeDef.addProperty(it) }
        return typeDef
    }
}

/**
 * Builder for static method definitions.
 *
 * Supports multiple overloads with different keys.
 * Use empty string "" as overloadKey for non-overloaded methods.
 */
class StaticMethodBuilder(private val name: String) {

    private val overloads = mutableListOf<OverloadInfo>()

    /**
     * Holds information about a single overload.
     */
    private data class OverloadInfo(
        val overloadKey: String,
        val jvmMethodName: String,
        val descriptor: String,
        val ownerClass: String,
        val isVarArgs: Boolean = false,
        val parameterTypes: List<ValueType>,
        val returnType: ReturnType
    )

    /**
     * Adds an overload with explicit descriptor and owner.
     *
     * @param overloadKey The overload key for matching (empty string for non-overloaded).
     * @param descriptor The JVM method descriptor.
     * @param ownerClass The JVM internal class name.
     * @param jvmMethodName Optional custom JVM method name (defaults to [name]).
     * @param isVarArgs Whether this method uses varargs.
     * @param parameterTypes The parameter types for coercion (required).
     * @param returnType The return type for coercion (required).
     */
    fun overload(
        overloadKey: String = "",
        descriptor: String,
        ownerClass: String,
        jvmMethodName: String = name,
        isVarArgs: Boolean = false,
        parameterTypes: List<ValueType>,
        returnType: ReturnType
    ) {
        overloads.add(OverloadInfo(overloadKey, jvmMethodName, descriptor, ownerClass, isVarArgs, parameterTypes, returnType))
    }

    /**
     * Builds the method definitions.
     */
    fun build(): List<MethodDefinition> {
        return overloads.map { info ->
            StaticMethodDefinition(
                name = name,
                overloadKey = info.overloadKey,
                descriptor = info.descriptor,
                ownerClass = info.ownerClass,
                jvmMethodName = info.jvmMethodName,
                isVarArgs = info.isVarArgs,
                parameterTypes = info.parameterTypes,
                returnType = info.returnType
            )
        }
    }
}

/**
 * Builder for instance method definitions.
 */
class InstanceMethodBuilder(private val name: String) {

    private val overloads = mutableListOf<OverloadInfo>()

    private data class OverloadInfo(
        val overloadKey: String,
        val jvmMethodName: String,
        val descriptor: String,
        val ownerClass: String,
        val isInterface: Boolean,
        val isVarArgs: Boolean = false,
        val parameterTypes: List<ValueType>,
        val returnType: ReturnType
    )

    /**
     * Adds an overload with explicit descriptor.
     *
     * @param overloadKey The overload key for matching (empty string for non-overloaded).
     * @param descriptor The JVM method descriptor.
     * @param ownerClass The JVM internal class name.
     * @param isInterface Whether to use INVOKEINTERFACE.
     * @param jvmMethodName Optional custom JVM method name (defaults to [name]).
     * @param isVarArgs Whether this method uses varargs.
     * @param parameterTypes The parameter types for coercion (required).
     * @param returnType The return type for coercion (required).
     */
    fun overload(
        overloadKey: String = "",
        descriptor: String,
        ownerClass: String,
        isInterface: Boolean = false,
        jvmMethodName: String = name,
        isVarArgs: Boolean = false,
        parameterTypes: List<ValueType>,
        returnType: ReturnType
    ) {
        overloads.add(OverloadInfo(overloadKey, jvmMethodName, descriptor, ownerClass, isInterface, isVarArgs, parameterTypes, returnType))
    }

    /**
     * Builds the method definitions.
     */
    fun build(): List<MethodDefinition> {
        return overloads.map { info ->
            InstanceMethodDefinition(
                name = name,
                overloadKey = info.overloadKey,
                descriptor = info.descriptor,
                ownerClass = info.ownerClass,
                isInterface = info.isInterface,
                jvmMethodName = info.jvmMethodName,
                isVarArgs = info.isVarArgs,
                parameterTypes = info.parameterTypes,
                returnType = info.returnType
            )
        }
    }
}

/**
 * Builder for static property definitions.
 */
class StaticPropertyBuilder(private val name: String) {

    private var descriptor: String = "Ljava/lang/Object;"
    private var ownerClass: String = ""
    private var getterName: String = name
    private var receiverDescriptor: String = "Ljava/lang/Object;"

    /**
     * Sets the property return type descriptor.
     *
     * @param desc The JVM type descriptor.
     */
    fun returns(desc: String) {
        descriptor = desc
    }

    /**
     * Sets the owner class.
     *
     * @param className The JVM internal class name.
     */
    fun owner(className: String) {
        ownerClass = className
    }

    /**
     * Sets the getter method name.
     *
     * @param methodName The getter method name.
     */
    fun getter(methodName: String) {
        getterName = methodName
    }

    /**
     * Sets the receiver type descriptor.
     *
     * @param desc The JVM type descriptor for the receiver.
     */
    fun receiver(desc: String) {
        receiverDescriptor = desc
    }

    /**
     * Builds the property definition.
     */
    fun build(): PropertyDefinition {
        require(ownerClass.isNotEmpty()) { "Owner class must be specified" }
        return StaticPropertyDefinition(
            name = name,
            descriptor = descriptor,
            ownerClass = ownerClass,
            getterName = getterName,
            receiverDescriptor = receiverDescriptor
        )
    }
}

/**
 * Builder for instance property definitions.
 */
class InstancePropertyBuilder(private val name: String) {

    private var descriptor: String = "Ljava/lang/Object;"
    private var ownerClass: String = ""
    private var isInterface: Boolean = false
    private var getterName: String = "get${name.replaceFirstChar { it.uppercaseChar() }}"

    /**
     * Sets the property return type descriptor.
     *
     * @param desc The JVM type descriptor.
     */
    fun returns(desc: String) {
        descriptor = desc
    }

    /**
     * Sets the owner class.
     *
     * @param className The JVM internal class name.
     */
    fun owner(className: String) {
        ownerClass = className
    }

    /**
     * Marks the owner as an interface.
     */
    fun isInterface() {
        isInterface = true
    }

    /**
     * Sets the getter method name.
     *
     * @param methodName The getter method name.
     */
    fun getter(methodName: String) {
        getterName = methodName
    }

    /**
     * Builds the property definition.
     */
    fun build(): PropertyDefinition {
        require(ownerClass.isNotEmpty()) { "Owner class must be specified" }
        return InstancePropertyDefinition(
            name = name,
            descriptor = descriptor,
            ownerClass = ownerClass,
            isInterface = isInterface,
            getterName = getterName
        )
    }
}

/**
 * Gets the JVM type descriptor for a Java class.
 *
 * @param clazz The Java class.
 * @return The JVM type descriptor.
 */
fun getTypeDescriptor(clazz: Class<*>): String {
    return when {
        clazz == Void.TYPE -> "V"
        clazz == Boolean::class.javaPrimitiveType -> "Z"
        clazz == Byte::class.javaPrimitiveType -> "B"
        clazz == Char::class.javaPrimitiveType -> "C"
        clazz == Short::class.javaPrimitiveType -> "S"
        clazz == Int::class.javaPrimitiveType -> "I"
        clazz == Long::class.javaPrimitiveType -> "J"
        clazz == Float::class.javaPrimitiveType -> "F"
        clazz == Double::class.javaPrimitiveType -> "D"
        clazz.isArray -> "[" + getTypeDescriptor(clazz.componentType)
        else -> "L${clazz.name.replace('.', '/')};"
    }
}
