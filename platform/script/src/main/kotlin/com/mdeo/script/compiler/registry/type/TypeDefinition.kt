package com.mdeo.script.compiler.registry.type

import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.ValueType
import org.objectweb.asm.MethodVisitor

/**
 * Represents a type definition in the registry.
 *
 * A type definition contains the type name and its members (methods and properties).
 * Types can extend other types to inherit their members.
 */
interface TypeDefinition {
    /**
     * The unique name of this type (e.g., "builtin.int", "builtin.List").
     */
    val typeName: String

    /**
     * The names of types this type extends.
     * Members from parent types are inherited and can be looked up.
     */
    val extends: List<String>

    /**
     * Gets all method definitions for a given method name.
     * Does NOT include inherited methods.
     *
     * @param name The method name.
     * @return List of method definitions with this name (different overloads), empty if not found.
     */
    fun getMethods(name: String): List<MethodDefinition>

    /**
     * Gets a specific method overload by name and overload key.
     * Does NOT include inherited methods.
     *
     * @param name The method name.
     * @param overloadKey The overload key that identifies this specific overload.
     * @return The method definition, or null if not found.
     */
    fun getMethod(name: String, overloadKey: String): MethodDefinition?

    /**
     * Gets a property definition by name.
     * Does NOT include inherited properties.
     *
     * @param name The property name.
     * @return The property definition, or null if not found.
     */
    fun getProperty(name: String): PropertyDefinition?

    /**
     * Gets all method names defined on this type (not inherited).
     */
    val methodNames: Set<String>

    /**
     * Gets all property names defined on this type (not inherited).
     */
    val propertyNames: Set<String>
}

/**
 * Represents a method definition in the type registry.
 *
 * Methods can be either instance-based (called on an object) or static-based
 * (implemented as static helper methods).
 */
interface MethodDefinition {
    /**
     * The method name.
     */
    val name: String

    /**
     * The overload key that uniquely identifies this method signature.
     *
     * For non-overloaded methods, this is an empty string "".
     * For overloaded methods (like `max` with different parameter types),
     * this is typically the distinguishing type name like "builtin.int", "builtin.long".
     *
     * This is used to match against the overload field in TypedMemberCallExpression.
     */
    val overloadKey: String

    /**
     * The JVM method descriptor.
     * For instance methods: "(arg1Desc arg2Desc...)returnDesc"
     * For static helpers: "(receiverDesc arg1Desc arg2Desc...)returnDesc"
     */
    val descriptor: String

    /**
     * Whether this method is implemented as a static helper.
     * If true, the receiver is passed as the first argument.
     * If false, uses INVOKEVIRTUAL or INVOKEINTERFACE.
     */
    val isStatic: Boolean

    /**
     * The JVM internal name of the owner class.
     * For instance methods: the class/interface containing the method.
     * For static helpers: the helper class (e.g., "com/mdeo/script/stdlib/primitives/IntHelper").
     */
    val ownerClass: String

    /**
     * Whether to use INVOKEINTERFACE instead of INVOKEVIRTUAL for instance methods.
     */
    val isInterface: Boolean

    /**
     * The JVM method name to invoke.
     * May differ from [name] if the Kotlin implementation uses a different name.
     */
    val jvmMethodName: String

    /**
     * Whether this method uses varargs.
     */
    val isVarArgs: Boolean
        get() = false

    /**
     * The parameter types for type coercion (excluding receiver for static methods).
     *
     * Each element is a ValueType representing the expected type of the corresponding parameter.
     */
    val parameterTypes: List<ValueType>

    /**
     * The return type for type coercion.
     *
     * Represents the expected return type of the method.
     * Use VoidType for methods that don't return a value.
     */
    val returnType: ReturnType

    /**
     * Emits the method invocation bytecode.
     *
     * The receiver (for instance methods) or receiver value (for static helpers)
     * and all arguments should already be on the stack when this is called.
     *
     * @param mv The method visitor to emit bytecode to.
     */
    fun emitInvocation(mv: MethodVisitor)
}

/**
 * Represents a property definition in the type registry.
 *
 * Properties can be either instance-based (accessed via getter methods)
 * or static-based (implemented as static helper methods).
 */
interface PropertyDefinition {
    /**
     * The property name.
     */
    val name: String

    /**
     * The JVM type descriptor of the property value.
     */
    val descriptor: String

    /**
     * Whether this property is implemented as a static helper.
     * If true, the receiver is passed as an argument.
     * If false, uses a getter method or field access.
     */
    val isStatic: Boolean

    /**
     * The JVM internal name of the owner class.
     */
    val ownerClass: String

    /**
     * Whether to use INVOKEINTERFACE instead of INVOKEVIRTUAL for instance getters.
     */
    val isInterface: Boolean

    /**
     * The JVM getter method name (e.g., "getLength", "size").
     * May be the property name itself for some getters.
     */
    val getterName: String

    /**
     * Emits the property access bytecode.
     *
     * The receiver object should already be on the stack when this is called.
     *
     * @param mv The method visitor to emit bytecode to.
     */
    fun emitAccess(mv: MethodVisitor)

    /**
     * Emits the property set bytecode.
     *
     * The receiver object and the value to set should already be on the stack when this is called.
     * The stack should have: [receiver, value]
     *
     * This method can handle different types of setting operations:
     * - Calling a setter method
     * - Setting a field directly
     * - Other custom logic
     *
     * @param mv The method visitor to emit bytecode to.
     * @param valueDescriptor The JVM type descriptor of the value being set.
     */
    fun emitSet(mv: MethodVisitor, valueDescriptor: String)
}
