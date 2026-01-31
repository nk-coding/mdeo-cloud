package com.mdeo.modeltransformationexecution.service

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.slf4j.LoggerFactory

/**
 * Loads ModelData into a TinkerGraph.
 * 
 * Creates vertices for instances and edges for links, preserving
 * all property values and using the edge label format from EdgeLabelUtils.
 */
class ModelDataGraphLoader {
    private val logger = LoggerFactory.getLogger(ModelDataGraphLoader::class.java)
    
    companion object {
        private const val PROPERTY_NAME = "name"
        private const val PROPERTY_CLASS_NAME = "className"
    }
    
    /**
     * Loads ModelData into the graph.
     *
     * @param g The graph traversal source
     * @param modelData The model data to load
     * @return Map of instance names to their vertices
     */
    fun load(g: GraphTraversalSource, modelData: ModelData): Map<String, Vertex> {
        val vertexMap = createVertices(g, modelData.instances)
        createEdges(g, modelData, vertexMap)
        return vertexMap
    }
    
    /**
     * Creates vertices for all instances.
     */
    private fun createVertices(
        g: GraphTraversalSource,
        instances: List<ModelDataInstance>
    ): Map<String, Vertex> {
        return instances.associate { instance ->
            instance.name to createVertex(g, instance)
        }
    }
    
    /**
     * Creates a vertex for a single instance.
     */
    private fun createVertex(g: GraphTraversalSource, instance: ModelDataInstance): Vertex {
        var traversal = g.addV(instance.className)
            .property(PROPERTY_NAME, instance.name)
            .property(PROPERTY_CLASS_NAME, instance.className)
        
        for ((propertyName, propertyValue) in instance.properties) {
            val value = convertPropertyValue(propertyValue)
            if (value != null) {
                traversal = traversal.property(propertyName, value)
            }
        }
        
        return traversal.next()
    }
    
    /**
     * Converts ModelDataPropertyValue to a Gremlin-compatible value.
     */
    private fun convertPropertyValue(value: ModelDataPropertyValue): Any? {
        return when (value) {
            is ModelDataPropertyValue.NullValue -> null
            is ModelDataPropertyValue.StringValue -> value.value
            is ModelDataPropertyValue.NumberValue -> value.value
            is ModelDataPropertyValue.BooleanValue -> value.value
            is ModelDataPropertyValue.EnumValue -> value.enumEntry
            is ModelDataPropertyValue.ListValue -> value.values.mapNotNull { convertPropertyValue(it) }
        }
    }
    
    /**
     * Creates edges for all links in the model.
     */
    private fun createEdges(
        g: GraphTraversalSource,
        modelData: ModelData,
        vertexMap: Map<String, Vertex>
    ) {
        for (link in modelData.links) {
            createEdge(g, link, vertexMap)
        }
    }
    
    /**
     * Creates an edge for a single link.
     */
    private fun createEdge(
        g: GraphTraversalSource,
        link: com.mdeo.modeltransformation.ast.model.ModelDataLink,
        vertexMap: Map<String, Vertex>
    ) {
        val sourceVertex = vertexMap[link.sourceName]
        val targetVertex = vertexMap[link.targetName]
        
        if (sourceVertex == null) {
            logger.warn("Source vertex not found: ${link.sourceName}")
            return
        }
        if (targetVertex == null) {
            logger.warn("Target vertex not found: ${link.targetName}")
            return
        }
        
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
            link.sourceProperty,
            link.targetProperty,
            isOutgoing = true
        )
        
        g.V(sourceVertex).addE(edgeLabel).to(targetVertex).next()
    }
}
