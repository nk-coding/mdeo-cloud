package com.mdeo.modeltransformation.compiler.registry

import com.mdeo.expression.ast.types.ClassTypeRef
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
 * @param parent Optional parent registry for fallback lookups. When set, [getType]
 *               will search the parent if a type is not found locally.
 */
class TypeRegistry(private val parent: TypeRegistry? = null) {

    // Double map: package -> name -> GremlinTypeDefinition
    private val types: MutableMap<String, MutableMap<String, GremlinTypeDefinition>> = mutableMapOf()

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
            StdlibRegistrar.registerAll(registry)
            return registry
        }

        /**
         * Creates a registry with the given types pre-registered.
         *
         * @param types The types to register.
         * @return A new registry with the types registered.
         */
        fun of(vararg types: GremlinTypeDefinition): TypeRegistry {
            return TypeRegistry().registerAll(*types)
        }
    }

    /**
     * Registers a type definition in this registry.
     *
     * @param type The type definition to register.
     * @return This registry, for method chaining.
     */
    fun register(type: GremlinTypeDefinition): TypeRegistry {
        types.getOrPut(type.typePackage) { mutableMapOf() }[type.typeName] = type
        return this
    }

    /**
     * Registers multiple type definitions in this registry.
     *
     * @param typesToRegister The type definitions to register.
     * @return This registry, for method chaining.
     */
    fun registerAll(vararg typesToRegister: GremlinTypeDefinition): TypeRegistry {
        typesToRegister.forEach { types.getOrPut(it.typePackage) { mutableMapOf() }[it.typeName] = it }
        return this
    }

    /**
     * Gets a type definition by [ClassTypeRef] from this registry.
     *
     * If the type is not found locally and a parent registry is set,
     * the lookup will fall back to the parent.
     *
     * @param typeRef The type reference (package + simple name).
     * @return The type definition, or null if not found in this registry or any parent.
     */
    fun getType(typeRef: ClassTypeRef): GremlinTypeDefinition? {
        return types[typeRef.`package`]?.get(typeRef.type) ?: parent?.getType(typeRef)
    }

    /**
     * Looks up a property definition by type ref and property name.
     *
     * Searches the type and its parent types (via extends) for the property.
     *
     * @param typeRef The type reference ([ClassTypeRef]: package + simple name).
     * @param propertyName The property name.
     * @return The property definition, or null if not found.
     */
    fun lookupProperty(typeRef: ClassTypeRef, propertyName: String): GremlinPropertyDefinition? {
        return lookupPropertyInHierarchy(typeRef, propertyName, mutableSetOf())
    }

    /**
     * Looks up a method definition by type ref, method name, and overload key.
     *
     * Searches the type and its parent types (via extends) for the method.
     *
     * @param typeRef The type reference ([ClassTypeRef]: package + simple name).
     * @param methodName The method name.
     * @param overloadKey The overload key.
     * @return The method definition, or null if not found.
     */
    fun lookupMethod(typeRef: ClassTypeRef, methodName: String, overloadKey: String): GremlinMethodDefinition? {
        return lookupMethodInHierarchy(typeRef, methodName, overloadKey, mutableSetOf())
    }

    /**
     * Looks up all method overloads by type ref and method name.
     *
     * Searches the type and its parent types for all methods with the given name.
     *
     * @param typeRef The type reference ([ClassTypeRef]: package + simple name).
     * @param methodName The method name.
     * @return List of all method definitions with this name in the type hierarchy.
     */
    fun lookupMethods(typeRef: ClassTypeRef, methodName: String): List<GremlinMethodDefinition> {
        return lookupMethodsInHierarchy(typeRef, methodName, mutableSetOf())
    }

    /**
     * Returns the number of registered types.
     *
     * @return The number of registered types.
     */
    fun size(): Int = types.values.sumOf { it.size }

    /**
     * Clears all registered types.
     */
    fun clear() {
        types.clear()
    }

    /**
     * Checks if a type is registered.
     *
     * @param typeRef The type reference to check.
     * @return True if the type is registered, false otherwise.
     */
    fun hasType(typeRef: ClassTypeRef): Boolean {
        return types[typeRef.`package`]?.containsKey(typeRef.type) == true
    }
    
    /**
     * Checks if a type is a subtype of another type (including direct match).
     * 
     * The check includes both direct equality and inheritance through the extends chain.
     * 
     * @param typeRef The type to check.
     * @param parentRef The potential parent type.
     * @return True if typeRef is the same as or inherits from parentRef.
     */
    fun isSubtypeOf(typeRef: ClassTypeRef, parentRef: ClassTypeRef): Boolean {
        if (typeRef == parentRef) return true
        
        val type = getType(typeRef) ?: return false
        
        for (extendedRef in type.extends) {
            if (isSubtypeOf(extendedRef, parentRef)) {
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
     * @param typeRef The type reference to search.
     * @param propertyName The property name to find.
     * @param visited Set of already visited types to prevent circular lookups.
     * @return The property definition, or null if not found.
     */
    private fun lookupPropertyInHierarchy(
        typeRef: ClassTypeRef,
        propertyName: String,
        visited: MutableSet<ClassTypeRef>
    ): GremlinPropertyDefinition? {
        if (typeRef in visited) return null
        visited.add(typeRef)

        val type = getType(typeRef) ?: return null
        val property = type.getProperty(propertyName)
        if (property != null) return property

        for (parentRef in type.extends) {
            val parentProperty = lookupPropertyInHierarchy(parentRef, propertyName, visited)
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
     * @param typeRef The type reference to search.
     * @param methodName The method name to find.
     * @param overloadKey The overload key to match.
     * @param visited Set of already visited types to prevent circular lookups.
     * @return The method definition, or null if not found.
     */
    private fun lookupMethodInHierarchy(
        typeRef: ClassTypeRef,
        methodName: String,
        overloadKey: String,
        visited: MutableSet<ClassTypeRef>
    ): GremlinMethodDefinition? {
        if (typeRef in visited) return null
        visited.add(typeRef)

        val type = getType(typeRef) ?: return null
        val method = type.getMethod(methodName, overloadKey)
        if (method != null) return method

        for (parentRef in type.extends) {
            val parentMethod = lookupMethodInHierarchy(parentRef, methodName, overloadKey, visited)
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
     * @param typeRef The type reference to search.
     * @param methodName The method name to find.
     * @param visited Set of already visited types to prevent circular lookups.
     * @return List of all method definitions with this name in the hierarchy.
     */
    private fun lookupMethodsInHierarchy(
        typeRef: ClassTypeRef,
        methodName: String,
        visited: MutableSet<ClassTypeRef>
    ): List<GremlinMethodDefinition> {
        if (typeRef in visited) return emptyList()
        visited.add(typeRef)

        val type = getType(typeRef) ?: return emptyList()
        val result = mutableListOf<GremlinMethodDefinition>()

        result.addAll(type.getMethods(methodName))

        for (parentRef in type.extends) {
            result.addAll(lookupMethodsInHierarchy(parentRef, methodName, visited))
        }

        return result
    }
}
