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
 * Registries can be chained via the [parent] parameter. When a type is not found
 * in this registry, the lookup will fall back to the parent registry.
 *
 * The [GLOBAL] static registry contains all stdlib entries and is typically used
 * as the parent for dynamic registries.
 *
 * @param parent Optional parent registry for fallback lookups. When set, [getType]
 *               will search the parent if a type is not found locally.
 */
class TypeRegistry(private val parent: TypeRegistry? = null) {

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
     *
     * If the type is not found locally and a parent registry is set,
     * the lookup will fall back to the parent.
     *
     * @param typeName The type name.
     * @return The type definition, or null if not found in this registry or any parent.
     */
    fun getType(typeName: String): TypeDefinition? {
        return types[typeName] ?: parent?.getType(typeName)
    }

    /**
     * Looks up a method definition by type, method name, and overload key.
     *
     * Searches the type and its parent types (via extends) for the method.
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

        val type = getType(typeName) ?: return null

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

        val type = getType(typeName) ?: return emptyList()
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
     * Gets all type names registered in this registry (not including global).
     */
    val typeNames: Set<String>
        get() = types.keys

    /**
     * Checks if sourceType is a subtype of targetType.
     *
     * This traverses the type hierarchy via `extends` relationships.
     *
     * @param sourceTypeName The source type name.
     * @param targetTypeName The target type name.
     * @return true if sourceType is a subtype of targetType (or the same type), false otherwise.
     */
    fun isSubtype(sourceTypeName: String, targetTypeName: String): Boolean {
        if (sourceTypeName == targetTypeName) return true

        return isSubtypeInHierarchy(sourceTypeName, targetTypeName, mutableSetOf())
    }

    /**
     * Recursive helper to check subtype relationship in the type hierarchy.
     */
    private fun isSubtypeInHierarchy(
        sourceTypeName: String,
        targetTypeName: String,
        visited: MutableSet<String>
    ): Boolean {
        if (sourceTypeName in visited) return false
        visited.add(sourceTypeName)

        if (sourceTypeName == targetTypeName) return true

        val type = getType(sourceTypeName) ?: return false

        for (parentName in type.extends) {
            if (isSubtypeInHierarchy(parentName, targetTypeName, visited)) {
                return true
            }
        }

        return false
    }

    /**
     * Gets the JVM class name for instanceof/checkcast operations.
     * Returns the wrapper class for nullable primitives, and the reference class for others.
     *
     * @param typeName The type name.
     * @param isNullable Whether the type is nullable.
     * @return The JVM internal class name, or null if not found.
     */
    fun getJvmClassName(typeName: String, isNullable: Boolean = false): String? {
        val type = getType(typeName) ?: return null

        return if (isNullable && type.wrapperClassName != null) {
            type.wrapperClassName
        } else {
            type.jvmClassName ?: type.wrapperClassName
        }
    }

    /**
     * Gets the JVM wrapper class name for a primitive type.
     *
     * @param typeName The primitive type name.
     * @return The JVM wrapper class name, or null if not a primitive or not found.
     */
    fun getWrapperClassName(typeName: String): String? {
        return getType(typeName)?.wrapperClassName
    }
}
