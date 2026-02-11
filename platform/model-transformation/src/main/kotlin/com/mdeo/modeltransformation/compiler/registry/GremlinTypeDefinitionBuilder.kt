package com.mdeo.modeltransformation.compiler.registry

import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Default implementation of [GremlinTypeDefinition].
 *
 * This class provides a simple implementation that stores properties and methods
 * in maps for efficient lookup.
 *
 * @param typeName The unique name of this type.
 * @param extends The names of types this type extends.
 * @param cardinality The cardinality for graph property storage (null for single).
 * @param properties A map of property name to property definition.
 * @param methods A map of method name to list of method definitions (overloads).
 */
class SimpleGremlinTypeDefinition(
    override val typeName: String,
    override val extends: List<String> = emptyList(),
    override val cardinality: VertexProperty.Cardinality? = null,
    private val properties: Map<String, GremlinPropertyDefinition> = emptyMap(),
    private val methods: Map<String, List<GremlinMethodDefinition>> = emptyMap()
) : GremlinTypeDefinition {

    override fun getProperty(name: String): GremlinPropertyDefinition? {
        return properties[name]
    }

    override fun getMethod(name: String, overloadKey: String): GremlinMethodDefinition? {
        return methods[name]?.find { it.overloadKey == overloadKey }
    }

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
 * val stringType = GremlinTypeDefinitionBuilder("builtin.string")
 *     .extends("builtin.any")
 *     .property("length") { receiver ->
 *         GremlinCompilationResult.ValueResult((receiver as? String)?.length ?: 0)
 *     }
 *     .build()
 * ```
 *
 * @param typeName The unique name of the type being built.
 */
class GremlinTypeDefinitionBuilder(
    private val typeName: String
) {
    private val extendsList = mutableListOf<String>()
    private var cardinality: VertexProperty.Cardinality? = null
    private val properties = mutableMapOf<String, GremlinPropertyDefinition>()
    private val methods = mutableMapOf<String, MutableList<GremlinMethodDefinition>>()

    /**
     * Adds a parent type that this type extends.
     *
     * @param parentType The name of the parent type.
     * @return This builder, for method chaining.
     */
    fun extends(parentType: String): GremlinTypeDefinitionBuilder {
        extendsList.add(parentType)
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
        compiler: (GraphTraversal<*, *>) -> GremlinCompilationResult
    ): GremlinTypeDefinitionBuilder {
        properties[name] = SimpleGremlinPropertyDefinition(name, compiler)
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
     * Graph properties use `.values(propertyName)` to retrieve values from graph elements.
     * This is the standard way to access properties on vertices/edges in Gremlin.
     *
     * @param name The property name in the graph.
     * @return This builder, for method chaining.
     */
    @Suppress("UNCHECKED_CAST")
    fun graphProperty(name: String): GremlinTypeDefinitionBuilder {
        properties[name] = SimpleGremlinPropertyDefinition(name) { receiver ->
            val traversal = (receiver as GraphTraversal<Any, Any>).values<Any>(name)
            GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
        }
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
        compiler: (GraphTraversal<*, *>, List<GremlinCompilationResult>) -> GremlinCompilationResult
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
 * @param name The property name.
 * @param compiler A function that compiles the property access in traversal mode.
 */
class SimpleGremlinPropertyDefinition(
    override val name: String,
    private val compiler: (GraphTraversal<*, *>) -> GremlinCompilationResult
) : GremlinPropertyDefinition {

    override fun compile(receiver: GraphTraversal<*, *>): GremlinCompilationResult {
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

    @Suppress("UNCHECKED_CAST")
    override fun compile(receiver: GraphTraversal<*, *>): GremlinCompilationResult {
        val typed = receiver as GraphTraversal<Any, Any>
        
        val traversal: GraphTraversal<Any, out Any> = if (isOutgoing) {
            typed.out(edgeLabel)
        } else {
            typed.`in`(edgeLabel)
        }
        
        return GremlinCompilationResult.of(traversal as GraphTraversal<Any, Any>)
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
    private val compiler: (GraphTraversal<*, *>, List<GremlinCompilationResult>) -> GremlinCompilationResult
) : GremlinMethodDefinition {

    override fun compile(
        receiver: GraphTraversal<*, *>,
        arguments: List<GremlinCompilationResult>
    ): GremlinCompilationResult {
        return compiler(receiver, arguments)
    }
}



/**
 * Creates a type definition builder for the given type name.
 *
 * @param typeName The unique name of the type.
 * @return A new [GremlinTypeDefinitionBuilder].
 */
fun gremlinType(typeName: String): GremlinTypeDefinitionBuilder {
    return GremlinTypeDefinitionBuilder(typeName)
}
