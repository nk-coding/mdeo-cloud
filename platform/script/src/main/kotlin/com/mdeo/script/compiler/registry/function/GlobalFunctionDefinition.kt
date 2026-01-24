package com.mdeo.script.compiler.registry.function

/**
 * Implementation of FunctionDefinition that holds multiple overloads.
 *
 * This is used for both global functions and file-scope functions with overloads.
 */
class GlobalFunctionDefinitionImpl(
    override val name: String,
    override val ownerClass: String
) : FunctionDefinition {

    private val overloads: MutableMap<String, FunctionSignatureDefinition> = mutableMapOf()

    /**
     * Adds an overload to this function.
     *
     * @param overload The function signature definition for this overload.
     */
    fun addOverload(overload: FunctionSignatureDefinition) {
        overloads[overload.overloadKey] = overload
    }

    override fun getOverloads(): List<FunctionSignatureDefinition> = overloads.values.toList()

    override fun getOverload(overloadKey: String): FunctionSignatureDefinition? = overloads[overloadKey]
}
