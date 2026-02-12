package com.mdeo.modeltransformation.compiler.registry

import com.mdeo.modeltransformation.stdlib.StdlibRegistrar

/**
 * Registry for Gremlin type definitions used during compilation.
 *
 * The registry provides lookup for types, methods, and properties,
 * including inheritance resolution for types that extend other types.
 *
 * Registries can be chained via the [parent] parameter. When a type is not found
 * in this registry, the lookup will fall back to the parent registry.
 *
 * The [GLOBAL] static registry contains all stdlib entries and is typically used
 * as the parent for dynamic registries.
 *
 * Example usage:
 * ```kotlin
 * // Create a local registry with GLOBAL as parent
 * val registry = GremlinTypeRegistry(parent = GremlinTypeRegistry.GLOBAL)
 * registry.register(MyCustomTypeDefinition())
 *
 * // Type lookup will first check local, then fall back to GLOBAL
 * val property = registry.lookupProperty("builtin.string", "length")
 * ```
 *
 * @param parent Optional parent registry for fallback lookups. When set, [getType]
 *               will search the parent if a type is not found locally.
 * @see GremlinTypeDefinition
 * @see GremlinPropertyDefinition
 * @see GremlinMethodDefinition
 */
class GremlinTypeRegistry(private val parent: GremlinTypeRegistry? = null) {

    private val types: MutableMap<String, GremlinTypeDefinition> = mutableMapOf()

    companion object {
        /**
         * The global static registry containing all stdlib type definitions.
         * This is initialized lazily and shared across all compilation contexts.
         */
        val GLOBAL: GremlinTypeRegistry by lazy { createStdlibRegistry() }

        /**
         * Creates the global stdlib registry with all built-in types.
         */
        private fun createStdlibRegistry(): GremlinTypeRegistry {
            val registry = GremlinTypeRegistry()
            StdlibRegistrar.registerAll(registry)
            return registry
        }

        /**
         * Creates a registry with the given types pre-registered.
         *
         * @param types The types to register.
         * @return A new registry with the types registered.
         */
        fun of(vararg types: GremlinTypeDefinition): GremlinTypeRegistry {
            return GremlinTypeRegistry().registerAll(*types)
        }
    }

    /**
     * Registers a type definition in this registry.
     *
     * @param type The type definition to register.
     * @return This registry, for method chaining.
     */
    fun register(type: GremlinTypeDefinition): GremlinTypeRegistry {
        types[type.typeName] = type
        return this
    }

    /**
     * Registers multiple type definitions in this registry.
     *
     * @param typesToRegister The type definitions to register.
     * @return This registry, for method chaining.
     */
    fun registerAll(vararg typesToRegister: GremlinTypeDefinition): GremlinTypeRegistry {
        typesToRegister.forEach { types[it.typeName] = it }
        return this
    }

    /**
     * Gets a type definition by name from this registry.
     *
     * If the type is not found locally and a parent registry is set,
     * the lookup will fall back to the parent.
     *
     * @param typeName The type name.
     * @return The type definition, or null if not found in this registry or any parent.
     */
    fun getType(typeName: String): GremlinTypeDefinition? {
        return types[typeName] ?: parent?.getType(typeName)
    }

    /**
     * Looks up a property definition by type name and property name.
     *
     * Searches the type and its parent types (via extends) for the property.
     *
     * @param typeName The type name.
     * @param propertyName The property name.
     * @return The property definition, or null if not found.
     */
    fun lookupProperty(typeName: String, propertyName: String): GremlinPropertyDefinition? {
        return lookupPropertyInHierarchy(typeName, propertyName, mutableSetOf())
    }

    /**
     * Looks up a method definition by type name, method name, and overload key.
     *
     * Searches the type and its parent types (via extends) for the method.
     *
     * @param typeName The type name.
     * @param methodName The method name.
     * @param overloadKey The overload key.
     * @return The method definition, or null if not found.
     */
    fun lookupMethod(typeName: String, methodName: String, overloadKey: String): GremlinMethodDefinition? {
        return lookupMethodInHierarchy(typeName, methodName, overloadKey, mutableSetOf())
    }

    /**
     * Looks up all method overloads by type name and method name.
     *
     * Searches the type and its parent types for all methods with the given name.
     *
     * @param typeName The type name.
     * @param methodName The method name.
     * @return List of all method definitions with this name in the type hierarchy.
     */
    fun lookupMethods(typeName: String, methodName: String): List<GremlinMethodDefinition> {
        return lookupMethodsInHierarchy(typeName, methodName, mutableSetOf())
    }

    /**
     * Returns the number of registered types.
     *
     * @return The number of registered types.
     */
    fun size(): Int = types.size

    /**
     * Clears all registered types.
     */
    fun clear() {
        types.clear()
    }

    /**
     * Checks if a type is registered.
     *
     * @param typeName The type name to check.
     * @return True if the type is registered, false otherwise.
     */
    fun hasType(typeName: String): Boolean {
        return types.containsKey(typeName)
    }
    
    /**
     * Checks if a type is a subtype of another type (including direct match).
     * 
     * The check includes both direct equality and inheritance through the extends chain.
     * 
     * @param typeName The type to check
     * @param parentTypeName The potential parent type
     * @return True if typeName is the same as or inherits from parentTypeName
     */
    fun isSubtypeOf(typeName: String, parentTypeName: String): Boolean {
        if (typeName == parentTypeName) return true
        
        val type = getType(typeName) ?: return false
        
        for (extendedType in type.extends) {
            if (isSubtypeOf(extendedType, parentTypeName)) {
                return true
            }
        }
        
        return false
    }

    /**
     * Internal method to look up a property in the type hierarchy.
     *
     * Recursively searches the type and its parent types for the property.
     * Uses a visited set to prevent infinite loops in circular inheritance.
     *
     * @param typeName The type name to search
     * @param propertyName The property name to find
     * @param visited Set of already visited types to prevent circular lookups
     * @return The property definition, or null if not found
     */
    private fun lookupPropertyInHierarchy(
        typeName: String,
        propertyName: String,
        visited: MutableSet<String>
    ): GremlinPropertyDefinition? {
        if (typeName in visited) return null
        visited.add(typeName)

        val type = getType(typeName) ?: return null
        val property = type.getProperty(propertyName)
        if (property != null) return property

        for (parentName in type.extends) {
            val parentProperty = lookupPropertyInHierarchy(parentName, propertyName, visited)
            if (parentProperty != null) return parentProperty
        }

        return null
    }

    /**
     * Internal method to look up a method in the type hierarchy.
     *
     * Recursively searches the type and its parent types for the method with
     * the specified overload key. Uses a visited set to prevent infinite loops
     * in circular inheritance.
     *
     * @param typeName The type name to search
     * @param methodName The method name to find
     * @param overloadKey The overload key to match
     * @param visited Set of already visited types to prevent circular lookups
     * @return The method definition, or null if not found
     */
    private fun lookupMethodInHierarchy(
        typeName: String,
        methodName: String,
        overloadKey: String,
        visited: MutableSet<String>
    ): GremlinMethodDefinition? {
        if (typeName in visited) return null
        visited.add(typeName)

        val type = getType(typeName) ?: return null
        val method = type.getMethod(methodName, overloadKey)
        if (method != null) return method

        for (parentName in type.extends) {
            val parentMethod = lookupMethodInHierarchy(parentName, methodName, overloadKey, visited)
            if (parentMethod != null) return parentMethod
        }

        return null
    }

    /**
     * Internal method to look up all method overloads in the type hierarchy.
     *
     * Recursively collects all methods with the specified name from the type
     * and its parent types. Uses a visited set to prevent infinite loops in
     * circular inheritance.
     *
     * @param typeName The type name to search
     * @param methodName The method name to find
     * @param visited Set of already visited types to prevent circular lookups
     * @return List of all method definitions with this name in the hierarchy
     */
    private fun lookupMethodsInHierarchy(
        typeName: String,
        methodName: String,
        visited: MutableSet<String>
    ): List<GremlinMethodDefinition> {
        if (typeName in visited) return emptyList()
        visited.add(typeName)

        val type = getType(typeName) ?: return emptyList()
        val result = mutableListOf<GremlinMethodDefinition>()

        result.addAll(type.getMethods(methodName))

        for (parentName in type.extends) {
            result.addAll(lookupMethodsInHierarchy(parentName, methodName, visited))
        }

        return result
    }
}
