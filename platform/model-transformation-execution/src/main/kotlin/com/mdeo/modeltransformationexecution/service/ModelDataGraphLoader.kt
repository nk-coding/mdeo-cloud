package com.mdeo.modeltransformationexecution.service

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.slf4j.LoggerFactory

/**
 * Loads ModelData into a TinkerGraph.
 * 
 * Creates vertices for instances and edges for links, preserving
 * all property values and using the edge label format from EdgeLabelUtils.
 * 
 * Instance names are registered in the InstanceNameRegistry instead of being
 * stored as properties in the graph, avoiding conflicts with metamodel properties.
 * 
 * List properties are stored using VertexProperty.Cardinality.list, with each
 * element added as a separate property value. This ensures portability to
 * remote graph databases.
 */
class ModelDataGraphLoader {
    private val logger = LoggerFactory.getLogger(ModelDataGraphLoader::class.java)
    
    /**
     * Loads ModelData into the graph.
     *
     * @param g The graph traversal source
     * @param modelData The model data to load
     * @param nameRegistry Registry for tracking vertex ID to name mappings
     * @return Map of instance names to their vertices
     */
    fun load(
        g: GraphTraversalSource,
        modelData: ModelData,
        nameRegistry: InstanceNameRegistry
    ): Map<String, Vertex> {
        val vertexMap = createVertices(g, modelData.instances, nameRegistry)
        createEdges(g, modelData, vertexMap)
        return vertexMap
    }
    
    /**
     * Creates vertices for all instances.
     */
    private fun createVertices(
        g: GraphTraversalSource,
        instances: List<ModelDataInstance>,
        nameRegistry: InstanceNameRegistry
    ): Map<String, Vertex> {
        return instances.associate { instance ->
            instance.name to createVertex(g, instance, nameRegistry)
        }
    }
    
    /**
     * Creates a vertex for a single instance.
     * Registers the vertex ID with its name in the registry instead of storing it as a property.
     * 
     * List properties are stored using Cardinality.list, with each element added separately.
     * The className is used only as the vertex label, not as a property.
     */
    private fun createVertex(
        g: GraphTraversalSource,
        instance: ModelDataInstance,
        nameRegistry: InstanceNameRegistry
    ): Vertex {
        var traversal = g.addV(instance.className)
        
        for ((propertyName, propertyValue) in instance.properties) {
            traversal = addPropertyValue(traversal, propertyName, propertyValue)
        }
        
        val vertex = traversal.next()
        
        nameRegistry.register(vertex.id(), instance.name)
        
        return vertex
    }
    
    /**
     * Adds property value(s) to a vertex traversal.
     * For list properties, adds each element separately with Cardinality.list.
     * For scalar properties, adds a single value.
     */
    private fun addPropertyValue(
        traversal: org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal<Vertex, Vertex>,
        propertyName: String,
        propertyValue: ModelDataPropertyValue
    ): org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal<Vertex, Vertex> {
        return when (propertyValue) {
            is ModelDataPropertyValue.ListValue -> {
                var result = traversal
                for (element in propertyValue.values) {
                    val value = convertScalarPropertyValue(element)
                    if (value != null) {
                        result = result.property(VertexProperty.Cardinality.list, propertyName, value)
                    }
                }
                result
            }
            else -> {
                val value = convertScalarPropertyValue(propertyValue)
                if (value != null) {
                    traversal.property(propertyName, value)
                } else {
                    traversal
                }
            }
        }
    }
    
    /**
     * Converts ModelDataPropertyValue to a Gremlin-compatible scalar value.
     * Does not handle ListValue - that is handled separately in addPropertyValue.
     */
    private fun convertScalarPropertyValue(value: ModelDataPropertyValue): Any? {
        return when (value) {
            is ModelDataPropertyValue.NullValue -> null
            is ModelDataPropertyValue.StringValue -> value.value
            is ModelDataPropertyValue.NumberValue -> value.value
            is ModelDataPropertyValue.BooleanValue -> value.value
            is ModelDataPropertyValue.EnumValue -> value.enumEntry
            is ModelDataPropertyValue.ListValue -> throw IllegalArgumentException("Lists should be handled by addPropertyValue, not convertScalarPropertyValue")
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
            link.targetProperty
        )
        
        g.V(sourceVertex).addE(edgeLabel).to(targetVertex).next()
    }
}
