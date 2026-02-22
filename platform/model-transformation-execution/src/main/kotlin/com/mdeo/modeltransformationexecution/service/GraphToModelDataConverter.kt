package com.mdeo.modeltransformationexecution.service

import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.ReturnType
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
    private val metamodelData: MetamodelData,
    private val types: List<ReturnType>,
    private val typeRegistry: GremlinTypeRegistry
) {
    private val logger = LoggerFactory.getLogger(GraphToModelDataConverter::class.java)
    
    private val classMap: Map<String, ClassData> = metamodelData.classes.associateBy { 
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
     * Looks up a PropertyData for the given class and property name, traversing the
     * class hierarchy to find inherited properties.
     *
     * @param className The class to start the search from
     * @param propertyName The property name to find
     * @return The PropertyData if found, or null if not found in the hierarchy
     */
    private fun findPropertyInHierarchy(className: String, propertyName: String): com.mdeo.expression.ast.types.PropertyData? {
        val classData = classMap[className] ?: return null
        val property = classData.properties.find { it.name == propertyName }
        if (property != null) return property
        for (parentName in classData.extends) {
            val parentProperty = findPropertyInHierarchy(parentName, propertyName)
            if (parentProperty != null) return parentProperty
        }
        return null
    }

    /**
     * Extracts properties from a vertex, handling list properties correctly.
     * 
     * Uses metamodel type information to determine which properties are collection types.
     * Properties stored with Cardinality.list will have multiple VertexProperty instances
     * with the same key. These are collected into a ListValue based on their metamodel multiplicity.
     * 
     * Enum properties are stored in the graph as `EnumName`.`entryName` strings and are
     * parsed back to EnumValue using the metamodel property type information.
     */
    private fun extractProperties(vertex: Vertex, className: String): Map<String, ModelDataPropertyValue> {
        val classData = classMap[className]
        if (classData == null) {
            throw IllegalStateException("No metamodel class found for $className")
        }
        
        val vertexPropertyMap = mutableMapOf<String, MutableList<Any?>>()
        vertex.properties<Any>().forEachRemaining { vertexProperty ->
            val key = vertexProperty.key()
            if (!vertexPropertyMap.containsKey(key)) {
                vertexPropertyMap[key] = mutableListOf()
            }
            vertexPropertyMap[key]!!.add(vertexProperty.value())
        }
        
        return vertexPropertyMap.mapNotNull { (propertyName, values) ->
            val propData = findPropertyInHierarchy(className, propertyName)
            if (propData == null) {
                throw IllegalStateException("No type information found for property $propertyName of class $className in metamodel")
            }
            
            val isCollectionType = propData.multiplicity.isMultiple()
            
            val propertyValue = if (isCollectionType) {
                ModelDataPropertyValue.ListValue(
                    values.map { convertToScalarPropertyValue(it, propData.enumType) }
                )
            } else {
                if (values.size > 1) {
                    throw IllegalStateException("Multiple values found for non-collection property $propertyName of class $className in vertex ${vertex.id()}")
                }
                convertToScalarPropertyValue(values.firstOrNull(), propData.enumType)
            }
            
            propertyName to propertyValue
        }.toMap()
    }
    
    /**
     * Extracts properties without type information, falling back to heuristic approach.
     */
    private fun extractPropertiesWithoutTypeInfo(vertex: Vertex): Map<String, ModelDataPropertyValue> {
        val vertexPropertyMap = mutableMapOf<String, MutableList<Any?>>()
        
        vertex.properties<Any>().forEachRemaining { vertexProperty ->
            val key = vertexProperty.key()
            if (!vertexPropertyMap.containsKey(key)) {
                vertexPropertyMap[key] = mutableListOf()
            }
            vertexPropertyMap[key]!!.add(vertexProperty.value())
        }
        
        return vertexPropertyMap.mapValues { (_, values) ->
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
     * Converts a single scalar value to ModelDataPropertyValue.
     *
     * When [enumTypeName] is non-null, the value is expected to be a backtick-formatted
     * enum string (e.g., "`Status`.`ACTIVE`") and is parsed into an [ModelDataPropertyValue.EnumValue]
     * containing only the entry name (e.g., "ACTIVE").
     *
     * @param value The raw graph property value.
     * @param enumTypeName The enum type name if this property is an enum type, or null otherwise.
     */
    private fun convertToScalarPropertyValue(
        value: Any?,
        enumTypeName: String? = null
    ): ModelDataPropertyValue {
        return when (value) {
            null -> ModelDataPropertyValue.NullValue
            is String -> {
                if (enumTypeName != null) {
                    ModelDataPropertyValue.EnumValue(parseEnumEntryName(value))
                } else {
                    ModelDataPropertyValue.StringValue(value)
                }
            }
            is Number -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Boolean -> ModelDataPropertyValue.BooleanValue(value)
            else -> ModelDataPropertyValue.StringValue(value.toString())
        }
    }

    /**
     * Parses an enum entry name from the backtick-formatted enum string used in the graph.
     *
     * Expected format: `` `EnumName`.`entryName` ``
     *
     * @param value The backtick-formatted enum string.
     * @return The entry name, or the original value if parsing fails.
     */
    private fun parseEnumEntryName(value: String): String {
        val regex = Regex("`[^`]+`\\.`([^`]+)`")
        val match = regex.matchEntire(value)
        return match?.groupValues?.get(1) ?: value
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
