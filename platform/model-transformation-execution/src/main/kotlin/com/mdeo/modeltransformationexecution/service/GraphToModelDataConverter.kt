package com.mdeo.modeltransformationexecution.service

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataLink
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
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
 */
class GraphToModelDataConverter {
    private val logger = LoggerFactory.getLogger(GraphToModelDataConverter::class.java)
    
    companion object {
        private const val PROPERTY_NAME = "name"
        private const val PROPERTY_CLASS_NAME = "className"
    }
    
    /**
     * Converts a graph to ModelData format.
     *
     * @param g The graph traversal source
     * @param metamodelUri The URI of the metamodel
     * @return The converted ModelData
     */
    fun convert(g: GraphTraversalSource, metamodelUri: String): ModelData {
        val instances = extractInstances(g)
        val links = extractLinks(g)
        
        return ModelData(
            metamodelUri = metamodelUri,
            instances = instances,
            links = links
        )
    }
    
    /**
     * Extracts all instances from the graph vertices.
     */
    private fun extractInstances(g: GraphTraversalSource): List<ModelDataInstance> {
        return g.V().toList().map { vertex ->
            convertVertexToInstance(vertex)
        }
    }
    
    /**
     * Converts a single vertex to a ModelDataInstance.
     */
    private fun convertVertexToInstance(vertex: Vertex): ModelDataInstance {
        val name = vertex.property<String>(PROPERTY_NAME).orElse(vertex.id().toString())
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
        return key == PROPERTY_NAME || key == PROPERTY_CLASS_NAME
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
    private fun extractLinks(g: GraphTraversalSource): List<ModelDataLink> {
        return g.E().toList().map { edge ->
            convertEdgeToLink(edge)
        }
    }
    
    /**
     * Converts a single edge to a ModelDataLink.
     * Parses the edge label using EdgeLabelUtils to extract property names.
     */
    private fun convertEdgeToLink(edge: Edge): ModelDataLink {
        val sourceVertex = edge.outVertex()
        val targetVertex = edge.inVertex()
        
        val sourceName = sourceVertex.property<String>(PROPERTY_NAME)
            .orElse(sourceVertex.id().toString())
        val targetName = targetVertex.property<String>(PROPERTY_NAME)
            .orElse(targetVertex.id().toString())
        
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
