package com.mdeo.modeltransformation.compiler.registry

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.CompilationResult
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Default implementation of [GremlinTypeDefinition].
 *
 * This class provides a simple implementation that stores properties and methods
 * in maps for efficient lookup.
 *
 * @param typePackage The package part of this type's fully qualified name (e.g., "builtin", "class/path/to/file").
 * @param typeName The simple name of this type (e.g., "int", "string").
 * @param extends The types this type extends, as [ClassTypeRef].
 * @param cardinality The cardinality for graph property storage (null for single).
 * @param properties A map of property name to property definition.
 * @param methods A map of method name to list of method definitions (overloads).
 */
class SimpleGremlinTypeDefinition(
    override val typePackage: String,
    override val typeName: String,
    override val extends: List<ClassTypeRef> = emptyList(),
    override val cardinality: VertexProperty.Cardinality? = null,
    private val properties: Map<String, GremlinPropertyDefinition> = emptyMap(),
    private val methods: Map<String, List<GremlinMethodDefinition>> = emptyMap()
) : GremlinTypeDefinition {

    /**
     * Gets a property definition by name from this type.
     *
     * @param name The property name
     * @return The property definition, or null if not found
     */
    override fun getProperty(name: String): GremlinPropertyDefinition? {
        return properties[name]
    }

    /**
     * Gets a method definition by name and overload key from this type.
     *
     * @param name The method name
     * @param overloadKey The overload key that identifies a specific overload
     * @return The method definition, or null if not found
     */
    override fun getMethod(name: String, overloadKey: String): GremlinMethodDefinition? {
        return methods[name]?.find { it.overloadKey == overloadKey }
    }

    /**
     * Gets all method definitions for a given method name from this type.
     *
     * @param name The method name
     * @return List of method definitions with this name, empty if not found
     */
    override fun getMethods(name: String): List<GremlinMethodDefinition> {
        return methods[name] ?: emptyList()
    }

    override val propertyNames: Set<String>
        get() = properties.keys

    override val methodNames: Set<String>
        get() = methods.keys
}

/**
 * Builder for creating [GremlinTypeDefinition] instances.
 *
 * Provides a fluent API for defining types with properties and methods.
 *
 * Example usage:
 * ```kotlin
 * val stringType = GremlinTypeDefinitionBuilder("builtin", "string")
 *     .extends("builtin", "Any")
 *     .property("length") { receiver ->
 *         CompilationResult.ValueResult((receiver as? String)?.length ?: 0)
 *     }
 *     .build()
 * ```
 *
 * @param typePackage The package part of the type's fully qualified name (e.g., "builtin", "class/path/to/file").
 * @param typeName The simple name of the type being built (e.g., "string", "House").
 */
class GremlinTypeDefinitionBuilder(
    private val typePackage: String,
    private val typeName: String
) {
    private val extendsList = mutableListOf<ClassTypeRef>()
    private var cardinality: VertexProperty.Cardinality? = null
    private val properties = mutableMapOf<String, GremlinPropertyDefinition>()
    private val methods = mutableMapOf<String, MutableList<GremlinMethodDefinition>>()

    /**
     * Adds a parent type that this type extends.
     *
     * @param parentType The parent type as a [ClassTypeRef].
     * @return This builder, for method chaining.
     */
    fun extends(parentType: ClassTypeRef): GremlinTypeDefinitionBuilder {
        extendsList.add(parentType)
        return this
    }

    /**
     * Adds a parent type that this type extends using package and name.
     *
     * @param pkg The package part of the parent type (e.g., "builtin").
     * @param name The simple name of the parent type (e.g., "Any").
     * @param isNullable Whether the parent type reference is nullable.
     * @return This builder, for method chaining.
     */
    fun extends(pkg: String, name: String, isNullable: Boolean = false): GremlinTypeDefinitionBuilder {
        extendsList.add(ClassTypeRef(`package` = pkg, type = name, isNullable = isNullable))
        return this
    }
    
    /**
     * Sets the cardinality for this type.
     *
     * @param cardinality The cardinality (single, list, or set).
     * @return This builder, for method chaining.
     */
    fun cardinality(cardinality: VertexProperty.Cardinality): GremlinTypeDefinitionBuilder {
        this.cardinality = cardinality
        return this
    }

    /**
     * Adds a property to this type using a traversal compiler.
     *
     * @param name The property name.
     * @param compiler A function that compiles the property access in traversal mode.
     * @return This builder, for method chaining.
     */
    fun property(
        name: String,
        compiler: (GraphTraversal<*, *>) -> CompilationResult
    ): GremlinTypeDefinitionBuilder {
        properties[name] = SimpleGremlinPropertyDefinition(name, compiler = compiler)
        return this
    }

    /**
     * Adds a property definition to this type.
     *
     * @param propertyDef The property definition to add.
     * @return This builder, for method chaining.
     */
    fun property(propertyDef: GremlinPropertyDefinition): GremlinTypeDefinitionBuilder {
        properties[propertyDef.name] = propertyDef
        return this
    }



    /**
     * Adds a graph property to this type.
     *
     * Graph properties use `.values(graphKey)` to retrieve values from graph elements.
     * The property is registered under [name] for lookup, but uses [graphKey] in the traversal.
     *
     * @param name The property name for lookup in the type registry.
     * @param graphKey The key used in the graph for storage/retrieval (defaults to [name]).
     * @return This builder, for method chaining.
     */
    @Suppress("UNCHECKED_CAST")
    fun graphProperty(name: String, graphKey: String = name): GremlinTypeDefinitionBuilder {
        properties[name] = SimpleGremlinPropertyDefinition(name, graphKey) { receiver ->
            val traversal = (receiver as GraphTraversal<Any, Any>).values<Any>(graphKey)
            CompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
        return this
    }

    /**
     * Adds an enum entry property to this type.
     *
     * Enum entries are constant values that don't result in graph traversals.
     * Instead, accessing an entry produces a constant string value in the format
     * `EnumName`.`entryName` (with backticks in the actual string).
     *
     * This is used for enum-container types where accessing `MyEnum.VALUE`
     * produces the string literal "`MyEnum`.`VALUE`".
     *
     * @param name The entry name.
     * @param value The constant string value this entry produces.
     * @return This builder, for method chaining.
     */
    fun enumEntry(name: String, value: String): GremlinTypeDefinitionBuilder {
        properties[name] = EnumEntryPropertyDefinition(name, value)
        return this
    }

    /**
     * Adds an association (edge traversal) to this type.
     *
     * Associations represent relations between model classes and are compiled
     * to edge traversals (`.out()` or `.in()`) in Gremlin.
     *
     * For nullable associations, the traversal is wrapped in `coalesce()` with
     * a null fallback to ensure a traverser with null is produced when no edge exists.
     *
     * @param propertyName The property name used to access this association.
     * @param edgeLabel The edge label in the graph.
     * @param isOutgoing True for outgoing edges (.out()), false for incoming (.in()).
     * @param isNullable True if the association can be null (uses coalesce pattern).
     * @return This builder, for method chaining.
     */
    fun association(
        propertyName: String,
        edgeLabel: String,
        isOutgoing: Boolean,
        isNullable: Boolean = false
    ): GremlinTypeDefinitionBuilder {
        properties[propertyName] = AssociationGremlinPropertyDefinition(
            name = propertyName,
            edgeLabel = edgeLabel,
            isOutgoing = isOutgoing,
            isNullable = isNullable
        )
        return this
    }

    /**
     * Adds a method to this type.
     *
     * @param name The method name.
     * @param overloadKey The overload key (empty string for non-overloaded methods).
     * @param parameterCount The number of parameters.
     * @param compiler A function that compiles the method call in traversal mode.
     * @return This builder, for method chaining.
     */
    fun method(
        name: String,
        overloadKey: String = "",
        parameterCount: Int = 0,
        compiler: (GraphTraversal<*, *>, List<CompilationResult>) -> CompilationResult
    ): GremlinTypeDefinitionBuilder {
        val methodDef = SimpleGremlinMethodDefinition(name, overloadKey, parameterCount, compiler)
        methods.getOrPut(name) { mutableListOf() }.add(methodDef)
        return this
    }



    /**
     * Adds a method definition to this type.
     *
     * @param methodDef The method definition to add.
     * @return This builder, for method chaining.
     */
    fun method(methodDef: GremlinMethodDefinition): GremlinTypeDefinitionBuilder {
        methods.getOrPut(methodDef.name) { mutableListOf() }.add(methodDef)
        return this
    }

    /**
     * Builds the type definition.
     *
     * @return A new [GremlinTypeDefinition] with the configured properties and methods.
     */
    fun build(): GremlinTypeDefinition {
        return SimpleGremlinTypeDefinition(
            typePackage = typePackage,
            typeName = typeName,
            extends = extendsList.toList(),
            cardinality = cardinality,
            properties = properties.toMap(),
            methods = methods.mapValues { it.value.toList() }
        )
    }
}

/**
 * Simple implementation of [GremlinPropertyDefinition] with a traversal compiler.
 *
 * @param name The property name for lookup.
 * @param graphKey The key used in the graph for this property (defaults to [name]).
 * @param compiler A function that compiles the property access in traversal mode.
 */
class SimpleGremlinPropertyDefinition(
    override val name: String,
    override val graphKey: String = name,
    private val compiler: (GraphTraversal<*, *>) -> CompilationResult
) : GremlinPropertyDefinition {

    /**
     * Compiles an access to this property using traversal mode.
     *
     * Delegates to the compiler function provided during construction.
     *
     * @param receiver The receiver traversal (the object the property is accessed on)
     * @return The compilation result containing the property value as a traversal
     */
    override fun compile(receiver: GraphTraversal<*, *>): CompilationResult {
        return compiler(receiver)
    }
}



/**
 * Association (edge traversal) implementation of [GremlinPropertyDefinition].
 *
 * Compiles to edge traversals (`.out()` or `.in()`) in Gremlin.
 * For nullable associations, wraps in `coalesce()` with a null fallback.
 *
 * @param name The property name.
 * @param edgeLabel The edge label in the graph.
 * @param isOutgoing True for outgoing edges (.out()), false for incoming (.in()).
 * @param isNullable True if the association can be null.
 */
class AssociationGremlinPropertyDefinition(
    override val name: String,
    val edgeLabel: String,
    val isOutgoing: Boolean,
    val isNullable: Boolean = false
) : GremlinPropertyDefinition {

    /**
     * Compiles an association access to edge traversals.
     *
     * Creates either an outgoing (.out()) or incoming (.in()) edge traversal
     * based on the association direction.
     *
     * @param receiver The receiver traversal (the vertex to traverse from)
     * @return The compilation result containing the edge traversal
     */
    @Suppress("UNCHECKED_CAST")
    override fun compile(receiver: GraphTraversal<*, *>): CompilationResult {
        val typed = receiver as GraphTraversal<Any, Any>
        
        val traversal: GraphTraversal<Any, out Any> = if (isOutgoing) {
            typed.out(edgeLabel)
        } else {
            typed.`in`(edgeLabel)
        }
        
        return CompilationResult.of(traversal as GraphTraversal<Any, Any>)
    }
}

/**
 * Enum entry property definition that produces a constant string value.
 *
 * When accessing an enum entry like `MyEnum.VALUE`, this property produces
 * the constant string "`MyEnum`.`VALUE`" without performing any graph traversal.
 *
 * @param name The entry name.
 * @param value The constant string value this entry produces.
 */
class EnumEntryPropertyDefinition(
    override val name: String,
    private val value: String
) : GremlinPropertyDefinition {

    /**
     * Compiles an enum entry access to a constant value traversal.
     *
     * This does NOT perform any graph traversal. Instead, it returns a
     * ValueResult containing the constant enum value string.
     *
     * @param receiver The receiver traversal (ignored for enum entries)
     * @return The compilation result containing the constant enum value string
     */
    override fun compile(receiver: GraphTraversal<*, *>): CompilationResult {
        return CompilationResult.constant<Any, String>(value, null)
    }
}

/**
 * Simple implementation of [GremlinMethodDefinition].
 *
 * @param name The method name.
 * @param overloadKey The overload key.
 * @param parameterCount The number of parameters.
 * @param compiler A function that compiles the method call in traversal mode.
 */
class SimpleGremlinMethodDefinition(
    override val name: String,
    override val overloadKey: String,
    override val parameterCount: Int,
    private val compiler: (GraphTraversal<*, *>, List<CompilationResult>) -> CompilationResult
) : GremlinMethodDefinition {

    /**
     * Compiles a call to this method using traversal mode.
     *
     * Delegates to the compiler function provided during construction.
     *
     * @param receiver The receiver traversal (the object the method is called on)
     * @param arguments The compiled argument traversals
     * @return The compilation result containing the method's return value as a traversal
     */
    override fun compile(
        receiver: GraphTraversal<*, *>,
        arguments: List<CompilationResult>
    ): CompilationResult {
        return compiler(receiver, arguments)
    }
}



/**
 * Creates a type definition builder for the given type package and name.
 *
 * @param typePackage The package part of the type's fully qualified name (e.g., "builtin").
 * @param typeName The simple name of the type (e.g., "string").
 * @return A new [GremlinTypeDefinitionBuilder].
 */
fun gremlinType(typePackage: String, typeName: String): GremlinTypeDefinitionBuilder {
    return GremlinTypeDefinitionBuilder(typePackage, typeName)
}
