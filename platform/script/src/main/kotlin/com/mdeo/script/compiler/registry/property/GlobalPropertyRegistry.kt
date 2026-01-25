package com.mdeo.script.compiler.registry.property

/**
 * Registry for global properties.
 *
 * Global properties are accessible at scope level 0 without requiring a receiver object.
 * This includes stdlib constants like PI, E, etc.
 *
 * There are two registries:
 * - A global static registry containing all stdlib properties
 * - A dynamic registry that can be extended during compilation
 *
 * The dynamic registry defers to the global registry for properties not found locally.
 */
class GlobalPropertyRegistry {

    private val properties: MutableMap<String, GlobalPropertyDefinition> = mutableMapOf()

    companion object {
        /**
         * The global static registry containing all stdlib global property definitions.
         * This is initialized lazily and shared across all compilation contexts.
         */
        val GLOBAL: GlobalPropertyRegistry by lazy { createStdlibRegistry() }

        /**
         * Creates the global stdlib registry with all built-in global properties.
         */
        private fun createStdlibRegistry(): GlobalPropertyRegistry {
            val registry = GlobalPropertyRegistry()
            return registry
        }
    }

    /**
     * Registers a global property definition in this registry.
     *
     * @param property The global property definition to register.
     */
    fun registerProperty(property: GlobalPropertyDefinition) {
        properties[property.name] = property
    }

    /**
     * Gets a global property definition by name from this registry.
     * Does not fall back to the global registry.
     *
     * @param name The property name.
     * @return The property definition, or null if not found.
     */
    fun getProperty(name: String): GlobalPropertyDefinition? {
        return properties[name]
    }

    /**
     * Gets a global property definition by name, falling back to the global registry.
     *
     * @param name The property name.
     * @return The property definition, or null if not found.
     */
    fun getPropertyOrGlobal(name: String): GlobalPropertyDefinition? {
        return properties[name] ?: (if (this !== GLOBAL) GLOBAL.getProperty(name) else null)
    }

    /**
     * Gets all property names registered in this registry (not including global).
     */
    val propertyNames: Set<String>
        get() = properties.keys
}
