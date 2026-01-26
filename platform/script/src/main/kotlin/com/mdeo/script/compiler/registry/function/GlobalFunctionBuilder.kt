package com.mdeo.script.compiler.registry.function

import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.script.compiler.registry.property.GlobalPropertyDefinition
import com.mdeo.script.compiler.registry.property.StaticGlobalPropertyDefinition

/**
 * DSL for building global function definitions.
 *
 * Example usage:
 * ```kotlin
 * globalFunction("println") {
 *     staticOverload("") {
 *         descriptor = "(Ljava/lang/String;)V"
 *         owner = "com/mdeo/script/stdlib/globals/GlobalFunctions"
 *         jvmMethod = "println"
 *         parameterTypes = listOf(BuiltinTypes.STRING)
 *         returnType = BuiltinTypes.VOID
 *     }
 * }
 * ```
 */
fun globalFunction(name: String, block: GlobalFunctionBuilder.() -> Unit): FunctionDefinition {
    val builder = GlobalFunctionBuilder(name)
    builder.block()
    return builder.build()
}

/**
 * DSL for building global property definitions.
 *
 * Example usage:
 * ```kotlin
 * globalProperty("PI") {
 *     descriptor = "D"
 *     owner = "com/mdeo/script/stdlib/globals/GlobalFunctions"
 *     getter = "PI"
 * }
 * ```
 */
fun globalProperty(name: String, block: GlobalPropertyBuilder.() -> Unit): GlobalPropertyDefinition {
    val builder = GlobalPropertyBuilder(name)
    builder.block()
    return builder.build()
}

/**
 * Builder for GlobalFunctionDefinition.
 *
 * @param name The function name being defined.
 */
class GlobalFunctionBuilder(private val name: String) {

    private val overloads = mutableListOf<FunctionSignatureDefinition>()

    /**
     * Adds a static overload to this function.
     *
     * @param overloadKey The unique key identifying this overload. Use empty string "" for non-overloaded.
     * @param block Builder for configuring the overload.
     */
    fun staticOverload(overloadKey: String = "", block: StaticOverloadBuilder.() -> Unit) {
        val builder = StaticOverloadBuilder(overloadKey)
        builder.block()
        overloads.add(builder.build())
    }

    /**
     * Adds a static varargs overload to this function.
     *
     * @param overloadKey The unique key identifying this overload. Use empty string "" for non-overloaded.
     * @param block Builder for configuring the overload.
     */
    fun varArgsOverload(overloadKey: String = "", block: StaticOverloadBuilder.() -> Unit) {
        val builder = StaticOverloadBuilder(overloadKey, isVarArgs = true)
        builder.block()
        overloads.add(builder.build())
    }

    /**
     * Builds the global function definition.
     *
     * @return A FunctionDefinition with all configured overloads.
     */
    fun build(): FunctionDefinition {
        require(overloads.isNotEmpty()) { "At least one overload must be defined for function $name" }
        val ownerClass = overloads.first().ownerClass
        val function = GlobalFunctionDefinitionImpl(name, ownerClass)
        overloads.forEach { function.addOverload(it) }
        return function
    }
}

/**
 * Builder for static method overloads.
 */
class StaticOverloadBuilder(
    private val overloadKey: String,
    private val isVarArgs: Boolean = false
) {
    /**
     * The JVM method descriptor.
     */
    lateinit var descriptor: String

    /**
     * The JVM internal name of the owner class.
     */
    lateinit var owner: String

    /**
     * The JVM method name to invoke.
     */
    lateinit var jvmMethod: String

    /**
     * The parameter types for coercion (required).
     */
    lateinit var parameterTypes: List<ValueType>

    /**
     * The return type for coercion (required).
     */
    lateinit var returnType: ReturnType

    /**
     * Builds the function signature definition.
     */
    fun build(): FunctionSignatureDefinition {
        return StaticFunctionSignatureDefinition(
            overloadKey = overloadKey,
            descriptor = descriptor,
            ownerClass = owner,
            jvmMethodName = jvmMethod,
            isVarArgs = isVarArgs,
            parameterTypes = parameterTypes,
            returnType = returnType
        )
    }
}

/**
 * Builder for global property definitions.
 */
class GlobalPropertyBuilder(private val name: String) {
    /**
     * The JVM type descriptor of the property value.
     */
    lateinit var descriptor: String

    /**
     * The JVM internal name of the owner class.
     */
    lateinit var owner: String

    /**
     * The JVM field or getter name.
     */
    lateinit var getter: String

    /**
     * Builds the property definition.
     */
    fun build(): GlobalPropertyDefinition {
        return StaticGlobalPropertyDefinition(
            name = name,
            descriptor = descriptor,
            ownerClass = owner,
            getterName = getter
        )
    }
}
