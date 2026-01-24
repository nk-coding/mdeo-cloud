package com.mdeo.script.compiler.registry.function

/**
 * Interface for function registries that support hierarchical lookup.
 *
 * The registry system is designed to support:
 * - Global scope functions (stdlib functions like println, listOf)
 * - File-scope functions (functions defined in the current file)
 * - Imported functions (functions from other files)
 *
 * The lookup follows a hierarchy:
 * 1. Local functions in the current file
 * 2. Imported functions from other files
 * 3. Global functions (stdlib)
 */
interface FunctionRegistry {

    /**
     * Looks up a function definition by name.
     *
     * The implementation should search through its local definitions first,
     * then fall back to the parent registry if available.
     *
     * @param name The function name to look up.
     * @return The function definition, or null if not found.
     */
    fun lookupFunction(name: String): FunctionDefinition?

    /**
     * Gets the parent registry in the hierarchy.
     *
     * @return The parent registry, or null if this is the root (global) registry.
     */
    fun getParent(): FunctionRegistry?
}
