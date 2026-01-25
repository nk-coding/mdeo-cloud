package com.mdeo.script.compiler.registry.function

/**
 * Registry for global scope functions.
 *
 * Global functions are accessible at scope level 0 without requiring a receiver object.
 * This includes stdlib functions like println, listOf, setOf, etc.
 *
 * There are two registries:
 * - A global static registry containing all stdlib functions
 * - A dynamic registry that can be extended during compilation
 *
 * The dynamic registry defers to the global registry for functions not found locally.
 *
 * This class implements [FunctionRegistry] to participate in the hierarchical
 * function lookup system. Global functions now fully support the unified [FunctionDefinition]
 * interface with overloads, making them interoperable with file-scope functions.
 */
class GlobalFunctionRegistry : FunctionRegistry {

    private val functions: MutableMap<String, FunctionDefinition> = mutableMapOf()

    companion object {
        /**
         * The global static registry containing all stdlib global function definitions.
         * This is initialized lazily and shared across all compilation contexts.
         */
        val GLOBAL: GlobalFunctionRegistry by lazy { createStdlibRegistry() }

        /**
         * Creates the global stdlib registry with all built-in global functions.
         */
        private fun createStdlibRegistry(): GlobalFunctionRegistry {
            val registry = GlobalFunctionRegistry()
            com.mdeo.script.stdlib.registrar.global.GlobalFunctionRegistrar.registerAll(registry)
            return registry
        }
    }

    /**
     * Registers a global function definition in this registry.
     *
     * @param function The global function definition to register.
     */
    fun registerFunction(function: FunctionDefinition) {
        functions[function.name] = function
    }

    /**
     * Gets a global function definition by name from this registry.
     * Does not fall back to the global registry.
     *
     * @param name The function name.
     * @return The function definition, or null if not found.
     */
    fun getFunction(name: String): FunctionDefinition? {
        return functions[name]
    }

    /**
     * Gets a global function definition by name, falling back to the global registry.
     *
     * @param name The function name.
     * @return The function definition, or null if not found.
     */
    fun getFunctionOrGlobal(name: String): FunctionDefinition? {
        return functions[name] ?: (if (this !== GLOBAL) GLOBAL.getFunction(name) else null)
    }

    /**
     * Looks up a method overload by function name and overload key.
     *
     * @param name The function name.
     * @param overloadKey The overload key that identifies this specific overload.
     * @return The function signature definition, or null if not found.
     */
    fun lookupMethod(name: String, overloadKey: String): FunctionSignatureDefinition? {
        val function = getFunctionOrGlobal(name) ?: return null
        return function.getOverload(overloadKey)
    }

    /**
     * Gets all function names registered in this registry (not including global).
     */
    val functionNames: Set<String>
        get() = functions.keys

    /**
     * Looks up a function by name.
     *
     * @param name The function name to look up.
     * @return The function definition, or null if not found.
     */
    override fun lookupFunction(name: String): FunctionDefinition? {
        return getFunctionOrGlobal(name)
    }

    /**
     * Gets the parent registry.
     *
     * @return Always null, as GlobalFunctionRegistry is the root of the hierarchy.
     */
    override fun getParent(): FunctionRegistry? {
        return null
    }
}
