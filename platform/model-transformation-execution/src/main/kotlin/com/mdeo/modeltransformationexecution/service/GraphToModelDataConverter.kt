package com.mdeo.modeltransformationexecution.service

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataLink
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
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
 */
class GraphToModelDataConverter {
    private val logger = LoggerFactory.getLogger(GraphToModelDataConverter::class.java)
    
    companion object {
        private const val PROPERTY_CLASS_NAME = "className"
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
     */
    private fun convertVertexToInstance(
        vertex: Vertex,
        nameRegistry: InstanceNameRegistry
    ): ModelDataInstance {
        val name = nameRegistry.getName(vertex.id()) ?: vertex.id().toString()
        val className = vertex.property<String>(PROPERTY_CLASS_NAME).orElse(vertex.label())
        val properties = extractProperties(vertex)
        
        return ModelDataInstance(
            name = name,
            className = className,
            properties = properties
        )
    }
    
    /**
     * Extracts properties from a vertex, excluding internal properties.
     */
    private fun extractProperties(vertex: Vertex): Map<String, ModelDataPropertyValue> {
        return vertex.properties<Any>()
            .asSequence()
            .filter { !isInternalProperty(it.key()) }
            .associate { prop ->
                prop.key() to convertToPropertyValue(prop.value())
            }
    }
    
    /**
     * Checks if a property key is an internal property that should be excluded.
     */
    private fun isInternalProperty(key: String): Boolean {
        return key == PROPERTY_CLASS_NAME
    }
    
    /**
     * Converts a Gremlin property value to ModelDataPropertyValue.
     */
    private fun convertToPropertyValue(value: Any?): ModelDataPropertyValue {
        return when (value) {
            null -> ModelDataPropertyValue.NullValue
            is String -> ModelDataPropertyValue.StringValue(value)
            is Number -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Boolean -> ModelDataPropertyValue.BooleanValue(value)
            is List<*> -> ModelDataPropertyValue.ListValue(
                value.map { convertToPropertyValue(it) }
            )
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
        
        val (sourceProperty, targetProperty) = parseEdgeLabel(edge.label())
        
        return ModelDataLink(
            sourceName = sourceName,
            sourceProperty = sourceProperty,
            targetName = targetName,
            targetProperty = targetProperty
        )
    }
    
    /**
     * Parses an edge label to extract source and target property names.
     * Uses EdgeLabelUtils.parseEdgeLabel for consistent parsing.
     */
    private fun parseEdgeLabel(edgeLabel: String): Pair<String?, String?> {
        return EdgeLabelUtils.parseEdgeLabel(edgeLabel, isOutgoing = true)
    }
}
