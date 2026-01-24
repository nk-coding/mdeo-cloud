package com.mdeo.script.compiler.registry.type

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.MethodDefinition
import com.mdeo.script.compiler.registry.type.PropertyDefinition

/**
 * Registry for type definitions used during compilation.
 *
 * The registry provides lookup for types, methods, and properties,
 * including inheritance resolution for types that extend other types.
 *
 * There are two registries:
 * - A global static registry containing all stdlib entries
 * - A dynamic registry that can be extended during compilation
 *
 * The dynamic registry defers to the global registry for types not found locally.
 */
class TypeRegistry {

    private val types: MutableMap<String, TypeDefinition> = mutableMapOf()

    companion object {
        /**
         * The global static registry containing all stdlib type definitions.
         * This is initialized lazily and shared across all compilation contexts.
         */
        val GLOBAL: TypeRegistry by lazy { createStdlibRegistry() }

        /**
         * Creates the global stdlib registry with all built-in types.
         */
        private fun createStdlibRegistry(): TypeRegistry {
            val registry = TypeRegistry()
            com.mdeo.script.stdlib.registrar.types.StdlibRegistrar.registerAll(registry)
            return registry
        }
    }

    /**
     * Registers a type definition in this registry.
     *
     * @param type The type definition to register.
     */
    fun register(type: TypeDefinition) {
        types[type.typeName] = type
    }

    /**
     * Gets a type definition by name from this registry.
     * Does not fall back to the global registry.
     *
     * @param typeName The type name.
     * @return The type definition, or null if not found.
     */
    fun getType(typeName: String): TypeDefinition? {
        return types[typeName]
    }

    /**
     * Gets a type definition by name, falling back to the global registry.
     *
     * @param typeName The type name.
     * @return The type definition, or null if not found.
     */
    fun getTypeOrGlobal(typeName: String): TypeDefinition? {
        return types[typeName] ?: (if (this !== GLOBAL) GLOBAL.getType(typeName) else null)
    }

    /**
     * Looks up a method definition by type, method name, and overload key.
     *
     * Searches the type and its parent types (via extends) for the method.
     * Falls back to the global registry if not found locally.
     *
     * @param typeName The type name.
     * @param methodName The method name.
     * @param overloadKey The overload key.
     * @return The method definition, or null if not found.
     */
    fun lookupMethod(typeName: String, methodName: String, overloadKey: String): MethodDefinition? {
        return lookupMethodInHierarchy(typeName, methodName, overloadKey, mutableSetOf())
    }

    /**
     * Recursive helper to look up a method in the type hierarchy.
     */
    private fun lookupMethodInHierarchy(
        typeName: String,
        methodName: String,
        overloadKey: String,
        visited: MutableSet<String>
    ): MethodDefinition? {
        if (typeName in visited) return null
        visited.add(typeName)

        val type = getTypeOrGlobal(typeName) ?: return null

        val method = type.getMethod(methodName, overloadKey)
        if (method != null) {
            return method
        }

        for (parentName in type.extends) {
            val parentMethod = lookupMethodInHierarchy(parentName, methodName, overloadKey, visited)
            if (parentMethod != null) {
                return parentMethod
            }
        }

        return null
    }

    /**
     * Looks up all method overloads by type and method name.
     *
     * Searches the type and its parent types for all methods with the given name.
     *
     * @param typeName The type name.
     * @param methodName The method name.
     * @return List of all method definitions with this name in the type hierarchy.
     */
    fun lookupMethods(typeName: String, methodName: String): List<MethodDefinition> {
        return lookupMethodsInHierarchy(typeName, methodName, mutableSetOf())
    }

    /**
     * Recursive helper to look up all methods in the type hierarchy.
     */
    private fun lookupMethodsInHierarchy(
        typeName: String,
        methodName: String,
        visited: MutableSet<String>
    ): List<MethodDefinition> {
        if (typeName in visited) return emptyList()
        visited.add(typeName)

        val type = getTypeOrGlobal(typeName) ?: return emptyList()
        val result = mutableListOf<MethodDefinition>()

        result.addAll(type.getMethods(methodName))

        for (parentName in type.extends) {
            result.addAll(lookupMethodsInHierarchy(parentName, methodName, visited))
        }

        return result
    }

    /**
     * Looks up a property definition by type and property name.
     *
     * Searches the type and its parent types for the property.
     *
     * @param typeName The type name.
     * @param propertyName The property name.
     * @return The property definition, or null if not found.
     */
    fun lookupProperty(typeName: String, propertyName: String): PropertyDefinition? {
        return lookupPropertyInHierarchy(typeName, propertyName, mutableSetOf())
    }

    /**
     * Recursive helper to look up a property in the type hierarchy.
     */
    private fun lookupPropertyInHierarchy(
        typeName: String,
        propertyName: String,
        visited: MutableSet<String>
    ): PropertyDefinition? {
        if (typeName in visited) return null
        visited.add(typeName)

        val type = getTypeOrGlobal(typeName) ?: return null

        val property = type.getProperty(propertyName)
        if (property != null) return property

        for (parentName in type.extends) {
            val parentProperty = lookupPropertyInHierarchy(parentName, propertyName, visited)
            if (parentProperty != null) return parentProperty
        }

        return null
    }

    /**
     * Gets all type names registered in this registry (not including global).
     */
    val typeNames: Set<String>
        get() = types.keys
}
