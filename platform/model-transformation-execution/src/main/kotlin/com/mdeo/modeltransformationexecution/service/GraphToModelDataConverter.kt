package com.mdeo.modeltransformationexecution.service

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.TypedClass
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataLink
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.slf4j.LoggerFactory

/**
 * Converts a TinkerGraph back to ModelData format.
 * 
 * This converter extracts vertices (instances) and edges (links) from a graph
 * and converts them to the ModelData representation. Edge labels are parsed
 * using EdgeLabelUtils to extract property names.
 * 
 * Instance names are retrieved from the InstanceNameRegistry instead of graph properties,
 * avoiding conflicts with metamodel properties named "name".
 * 
 * Properties stored with Cardinality.list are reconstructed as list values.
 * Uses metamodel type information to determine which properties are collection types.
 */
class GraphToModelDataConverter(
    private val metamodelClasses: List<TypedClass>,
    private val types: List<ReturnType>,
    private val typeRegistry: GremlinTypeRegistry
) {
    private val logger = LoggerFactory.getLogger(GraphToModelDataConverter::class.java)
    
    // Cache for looking up TypedClass by fully qualified name
    private val classMap: Map<String, TypedClass> = metamodelClasses.associateBy { 
        it.name
    }
    
    /**
     * Converts a graph to ModelData format.
     *
     * @param g The graph traversal source
     * @param metamodelUri The URI of the metamodel
     * @param nameRegistry Registry for retrieving vertex ID to name mappings
     * @return The converted ModelData
     */
    fun convert(
        g: GraphTraversalSource,
        metamodelUri: String,
        nameRegistry: InstanceNameRegistry
    ): ModelData {
        val instances = extractInstances(g, nameRegistry)
        val links = extractLinks(g, nameRegistry)
        
        return ModelData(
            metamodelUri = metamodelUri,
            instances = instances,
            links = links
        )
    }
    
    /**
     * Extracts all instances from the graph vertices.
     */
    private fun extractInstances(
        g: GraphTraversalSource,
        nameRegistry: InstanceNameRegistry
    ): List<ModelDataInstance> {
        return g.V().toList().map { vertex ->
            convertVertexToInstance(vertex, nameRegistry)
        }
    }
    
    /**
     * Converts a single vertex to a ModelDataInstance.
     * Gets the instance name from the registry instead of the graph properties.
     * Gets the className from the vertex label.
     */
    private fun convertVertexToInstance(
        vertex: Vertex,
        nameRegistry: InstanceNameRegistry
    ): ModelDataInstance {
        val name = nameRegistry.getName(vertex.id()) ?: vertex.id().toString()
        val className = vertex.label()
        val properties = extractProperties(vertex, className)
        
        return ModelDataInstance(
            name = name,
            className = className,
            properties = properties
        )
    }
    
    /**
     * Extracts properties from a vertex, handling list properties correctly.
     * 
     * Uses metamodel type information to determine which properties are collection types.
     * Properties stored with Cardinality.list will have multiple VertexProperty instances
     * with the same key. These are collected into a ListValue based on their metamodel type.
     */
    private fun extractProperties(vertex: Vertex, className: String): Map<String, ModelDataPropertyValue> {
        val typedClass = classMap[className]
        if (typedClass == null) {
            throw IllegalStateException("No metamodel class found for $className")
        }
        
        val propertyTypes = typedClass.properties.associate { prop ->
            prop.name to prop.typeIndex
        }
        
        val propertyMap = mutableMapOf<String, MutableList<Any?>>()
        vertex.properties<Any>().forEachRemaining { vertexProperty ->
            val key = vertexProperty.key()
            if (!propertyMap.containsKey(key)) {
                propertyMap[key] = mutableListOf()
            }
            propertyMap[key]!!.add(vertexProperty.value())
        }
        
        return propertyMap.mapNotNull { (propertyName, values) ->
            val typeIndex = propertyTypes[propertyName]
            if (typeIndex == null) {
                throw IllegalStateException("No type information found for property $propertyName of class $className in metamodel")
            }
            
            val isCollectionType = isCollectionProperty(typeIndex)
            
            val propertyValue = if (isCollectionType) {
                ModelDataPropertyValue.ListValue(
                    values.map { convertToScalarPropertyValue(it) }
                )
            } else {
                if (values.size > 1) {
                    throw IllegalStateException("Multiple values found for non-collection property $propertyName of class $className in vertex ${vertex.id()}")
                }
                convertToScalarPropertyValue(values.firstOrNull())
            }
            
            propertyName to propertyValue
        }.toMap()
    }
    
    /**
     * Extracts properties without type information, falling back to heuristic approach.
     */
    private fun extractPropertiesWithoutTypeInfo(vertex: Vertex): Map<String, ModelDataPropertyValue> {
        val propertyMap = mutableMapOf<String, MutableList<Any?>>()
        
        vertex.properties<Any>().forEachRemaining { vertexProperty ->
            val key = vertexProperty.key()
            if (!propertyMap.containsKey(key)) {
                propertyMap[key] = mutableListOf()
            }
            propertyMap[key]!!.add(vertexProperty.value())
        }
        
        return propertyMap.mapValues { (_, values) ->
            if (values.size > 1) {
                ModelDataPropertyValue.ListValue(
                    values.map { convertToScalarPropertyValue(it) }
                )
            } else {
                convertToScalarPropertyValue(values.firstOrNull())
            }
        }
    }
    
    /**
     * Checks if a property type (by type index) is a collection type.
     */
    private fun isCollectionProperty(typeIndex: Int): Boolean {
        if (typeIndex < 0 || typeIndex >= types.size) return false
        
        val type = types[typeIndex] as? ValueType ?: return false
        if (type !is ClassTypeRef) return false
        
        val typeDefinition = typeRegistry.getType(type.type) ?: return false
        val cardinality = typeDefinition.cardinality
        
        return cardinality == VertexProperty.Cardinality.list || cardinality == VertexProperty.Cardinality.set
    }
    
    /**
     * Converts a single scalar value to ModelDataPropertyValue.
     */
    private fun convertToScalarPropertyValue(value: Any?): ModelDataPropertyValue {
        return when (value) {
            null -> ModelDataPropertyValue.NullValue
            is String -> ModelDataPropertyValue.StringValue(value)
            is Number -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Boolean -> ModelDataPropertyValue.BooleanValue(value)
            else -> ModelDataPropertyValue.StringValue(value.toString())
        }
    }
    
    /**
     * Extracts all links from the graph edges.
     */
    private fun extractLinks(
        g: GraphTraversalSource,
        nameRegistry: InstanceNameRegistry
    ): List<ModelDataLink> {
        return g.E().toList().map { edge ->
            convertEdgeToLink(edge, nameRegistry)
        }
    }
    
    /**
     * Converts a single edge to a ModelDataLink.
     * Parses the edge label using EdgeLabelUtils to extract property names.
     * Gets instance names from the registry instead of graph properties.
     */
    private fun convertEdgeToLink(
        edge: Edge,
        nameRegistry: InstanceNameRegistry
    ): ModelDataLink {
        val sourceVertex = edge.outVertex()
        val targetVertex = edge.inVertex()
        
        val sourceName = nameRegistry.getName(sourceVertex.id()) ?: sourceVertex.id().toString()
        val targetName = nameRegistry.getName(targetVertex.id()) ?: targetVertex.id().toString()
        
        val (sourceProperty, targetProperty) = EdgeLabelUtils.parseEdgeLabel(edge.label())
        
        return ModelDataLink(
            sourceName = sourceName,
            sourceProperty = sourceProperty,
            targetName = targetName,
            targetProperty = targetProperty
        )
    }
}
