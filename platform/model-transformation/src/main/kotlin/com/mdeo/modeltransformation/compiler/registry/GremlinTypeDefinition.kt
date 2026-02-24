package com.mdeo.modeltransformation.compiler.registry

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Represents a type definition for Gremlin compilation.
 *
 * Type definitions contain information about properties and methods
 * available on a type, and how to compile accesses to them into
 * Gremlin traversal steps.
 *
 * Implementations should handle a specific type (e.g., "builtin.string",
 * "builtin.List") and provide Gremlin compilation for its members.
 *
 * @see GremlinPropertyDefinition
 * @see GremlinMethodDefinition
 * @see GremlinTypeRegistry
 */
interface GremlinTypeDefinition {

    /**
     * The package part of this type's fully qualified name (e.g., "builtin", "class/path/to/file").
     */
    val typePackage: String

    /**
     * The simple name of this type (e.g., "int", "string").
     */
    val typeName: String

    /**
     * The types this type extends, expressed as [ClassTypeRef].
     *
     * Members from parent types are inherited and can be looked up.
     * For example, all types typically extend the "any" type in the "builtin" package.
     */
    val extends: List<ClassTypeRef>
    
    /**
     * The cardinality of this type for graph property storage.
     * 
     * Determines how values of this type are stored in vertex properties:
     * - single: Single value (default for primitives, strings, etc.)
     * - list: Ordered collection allowing duplicates
     * - set: Unordered collection without duplicates
     * 
     * Null indicates default single cardinality.
     */
    val cardinality: VertexProperty.Cardinality?

    /**
     * Gets a property definition by name.
     *
     * Does NOT include inherited properties. Use [GremlinTypeRegistry.lookupProperty]
     * to include inherited properties.
     *
     * @param name The property name.
     * @return The property definition, or null if not found.
     */
    fun getProperty(name: String): GremlinPropertyDefinition?

    /**
     * Gets a method definition by name and overload key.
     *
     * Does NOT include inherited methods. Use [GremlinTypeRegistry.lookupMethod]
     * to include inherited methods.
     *
     * @param name The method name.
     * @param overloadKey The overload key that identifies a specific overload.
     *                    Empty string for non-overloaded methods.
     * @return The method definition, or null if not found.
     */
    fun getMethod(name: String, overloadKey: String): GremlinMethodDefinition?

    /**
     * Gets all method definitions for a given method name.
     *
     * Returns all overloads of the method. Does NOT include inherited methods.
     *
     * @param name The method name.
     * @return List of method definitions with this name, empty if not found.
     */
    fun getMethods(name: String): List<GremlinMethodDefinition>

    /**
     * Gets all property names defined on this type.
     *
     * Does NOT include inherited properties.
     */
    val propertyNames: Set<String>

    /**
     * Gets all method names defined on this type.
     *
     * Does NOT include inherited methods.
     */
    val methodNames: Set<String>
}

/**
 * Represents a property definition for Gremlin compilation.
 *
 * Properties are accessed via member access expressions (e.g., `collection.size`).
 * The property definition specifies how to compile the access into Gremlin steps.
 *
 * The compile method works in traversal mode, building upon a receiver traversal
 * to produce a result traversal. This unified approach replaces the previous
 * separate value/filter mode compilation.
 *
 * @see GremlinTypeDefinition
 */
interface GremlinPropertyDefinition {

    /**
     * The property name.
     */
    val name: String

    /**
     * Compiles an access to this property using traversal mode.
     *
     * This method builds upon the receiver traversal to produce a result
     * traversal that evaluates this property access. The receiver represents
     * the object on which the property is accessed.
     *
     * @param receiver The receiver traversal (the object the property is accessed on).
     * @return The compilation result containing the property value as a traversal.
     */
    fun compile(receiver: GraphTraversal<*, *>): GremlinCompilationResult
}

/**
 * Represents a method definition for Gremlin compilation.
 *
 * Methods are called via member call expressions (e.g., `collection.size()`).
 * The method definition specifies how to compile the call into Gremlin steps.
 *
 * The compile method works in traversal mode, building upon a receiver traversal
 * and argument compilation results to produce a result traversal. This unified
 * approach replaces the previous separate value/filter mode compilation.
 *
 * @see GremlinTypeDefinition
 */
interface GremlinMethodDefinition {

    /**
     * The method name.
     */
    val name: String

    /**
     * The overload key that uniquely identifies this method signature.
     *
     * For non-overloaded methods, this is an empty string "".
     * For overloaded methods, this is typically the distinguishing type name.
     */
    val overloadKey: String

    /**
     * The number of parameters this method accepts.
     */
    val parameterCount: Int

    /**
     * Compiles a call to this method using traversal mode.
     *
     * This method builds upon the receiver traversal and compiled argument
     * traversals to produce a result traversal that evaluates this method call.
     *
     * @param receiver The receiver traversal (the object the method is called on).
     * @param arguments The compiled argument traversals.
     * @return The compilation result containing the method's return value as a traversal.
     */
    fun compile(
        receiver: GraphTraversal<*, *>,
        arguments: List<GremlinCompilationResult>
    ): GremlinCompilationResult
}
