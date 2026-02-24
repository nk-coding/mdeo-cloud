package com.mdeo.script.compiler.registry.type

import com.mdeo.expression.ast.types.ClassTypeRef

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

    // Double map: package -> name -> TypeDefinition
    private val types: MutableMap<String, MutableMap<String, TypeDefinition>> = mutableMapOf()

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
        types.getOrPut(type.typePackage) { mutableMapOf() }[type.typeName] = type
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
    fun getType(typeRef: ClassTypeRef): TypeDefinition? {
        return types[typeRef.`package`]?.get(typeRef.type) ?: parent?.getType(typeRef)
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
    fun lookupMethod(typeRef: ClassTypeRef, methodName: String, overloadKey: String): MethodDefinition? {
        return lookupMethodInHierarchy(typeRef, methodName, overloadKey, mutableSetOf())
    }

    /**
     * Recursive helper to look up a method in the type hierarchy.
     */
    private fun lookupMethodInHierarchy(
        typeRef: ClassTypeRef,
        methodName: String,
        overloadKey: String,
        visited: MutableSet<ClassTypeRef>
    ): MethodDefinition? {
        if (typeRef in visited) return null
        visited.add(typeRef)

        val type = getType(typeRef) ?: return null

        val method = type.getMethod(methodName, overloadKey)
        if (method != null) {
            return method
        }

        for (parentRef in type.extends) {
            val parentMethod = lookupMethodInHierarchy(parentRef, methodName, overloadKey, visited)
            if (parentMethod != null) {
                return parentMethod
            }
        }

        return null
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
    fun lookupMethods(typeRef: ClassTypeRef, methodName: String): List<MethodDefinition> {
        return lookupMethodsInHierarchy(typeRef, methodName, mutableSetOf())
    }

    /**
     * Recursive helper to look up all methods in the type hierarchy.
     */
    private fun lookupMethodsInHierarchy(
        typeRef: ClassTypeRef,
        methodName: String,
        visited: MutableSet<ClassTypeRef>
    ): List<MethodDefinition> {
        if (typeRef in visited) return emptyList()
        visited.add(typeRef)

        val type = getType(typeRef) ?: return emptyList()
        val result = mutableListOf<MethodDefinition>()

        result.addAll(type.getMethods(methodName))

        for (parentRef in type.extends) {
            result.addAll(lookupMethodsInHierarchy(parentRef, methodName, visited))
        }

        return result
    }

    /**
     * Looks up a property definition by type ref and property name.
     *
     * Searches the type and its parent types for the property.
     *
     * @param typeRef The type reference ([ClassTypeRef]: package + simple name).
     * @param propertyName The property name.
     * @return The property definition, or null if not found.
     */
    fun lookupProperty(typeRef: ClassTypeRef, propertyName: String): PropertyDefinition? {
        return lookupPropertyInHierarchy(typeRef, propertyName, mutableSetOf())
    }

    /**
     * Recursive helper to look up a property in the type hierarchy.
     */
    private fun lookupPropertyInHierarchy(
        typeRef: ClassTypeRef,
        propertyName: String,
        visited: MutableSet<ClassTypeRef>
    ): PropertyDefinition? {
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
     * Gets all type refs registered in this registry (not including global).
     */
    val allTypeRefs: Set<ClassTypeRef>
        get() = types.flatMap { (pkg, nameMap) ->
            nameMap.keys.map { name -> ClassTypeRef(`package` = pkg, type = name, isNullable = false) }
        }.toSet()

    /**
     * Checks if sourceType is a subtype of targetType.
     *
     * This traverses the type hierarchy via `extends` relationships.
     *
     * @param sourceRef The source type reference.
     * @param targetRef The target type reference.
     * @return true if sourceType is a subtype of targetType (or the same type), false otherwise.
     */
    fun isSubtype(sourceRef: ClassTypeRef, targetRef: ClassTypeRef): Boolean {
        if (sourceRef == targetRef) return true

        return isSubtypeInHierarchy(sourceRef, targetRef, mutableSetOf())
    }

    /**
     * Recursive helper to check subtype relationship in the type hierarchy.
     */
    private fun isSubtypeInHierarchy(
        sourceRef: ClassTypeRef,
        targetRef: ClassTypeRef,
        visited: MutableSet<ClassTypeRef>
    ): Boolean {
        if (sourceRef in visited) return false
        visited.add(sourceRef)

        if (sourceRef == targetRef) return true

        val type = getType(sourceRef) ?: return false

        for (parentRef in type.extends) {
            if (isSubtypeInHierarchy(parentRef, targetRef, visited)) {
                return true
            }
        }

        return false
    }

    /**
     * Gets the JVM class name for instanceof/checkcast operations.
     * Returns the wrapper class for nullable primitives, and the reference class for others.
     *
     * @param typeRef The type reference.
     * @param isNullable Whether the type is nullable.
     * @return The JVM internal class name, or null if not found.
     */
    fun getJvmClassName(typeRef: ClassTypeRef, isNullable: Boolean = false): String? {
        val type = getType(typeRef) ?: return null

        return if (isNullable && type.wrapperClassName != null) {
            type.wrapperClassName
        } else {
            type.jvmClassName ?: type.wrapperClassName
        }
    }

    /**
     * Gets the JVM wrapper class name for a primitive type.
     *
     * @param typeRef The type reference.
     * @return The JVM wrapper class name, or null if not a primitive or not found.
     */
    fun getWrapperClassName(typeRef: ClassTypeRef): String? {
        return getType(typeRef)?.wrapperClassName
    }
}
