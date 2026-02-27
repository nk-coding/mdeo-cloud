package com.mdeo.optimizer.graph

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph

/**
 * In-process TinkerGraph implementation of [GraphBackend].
 *
 * This is the default backend for local/single-node optimization runs.
 * Future backends (e.g. remote JanusGraph) would implement the same interface.
 */
class TinkerGraphBackend private constructor(
    private val graph: TinkerGraph
) : GraphBackend {

    constructor() : this(TinkerGraph.open())

    override fun traversal(): GraphTraversalSource = graph.traversal()

    override fun deepCopy(): GraphBackend {
        val copy = TinkerGraph.open()
        val g = graph.traversal()
        val gCopy = copy.traversal()

        // Map old vertex IDs to new vertices
        val vertexIdMap = mutableMapOf<Any, org.apache.tinkerpop.gremlin.structure.Vertex>()

        // Copy all vertices
        g.V().toList().forEach { vertex ->
            var traversal = gCopy.addV(vertex.label())

            // Copy all properties (including multi-valued / list cardinality)
            vertex.properties<Any>().forEachRemaining { vp ->
                traversal = traversal.property(
                    VertexProperty.Cardinality.list,
                    vp.key(),
                    vp.value()
                )
            }

            val newVertex = traversal.next()
            vertexIdMap[vertex.id()] = newVertex
        }

        // Copy all edges using the Structure API directly to avoid anonymous traversal issues
        g.E().toList().forEach { edge ->
            val fromVertex = vertexIdMap[edge.outVertex().id()]
                ?: error("Source vertex ${edge.outVertex().id()} not found in copy")
            val toVertex = vertexIdMap[edge.inVertex().id()]
                ?: error("Target vertex ${edge.inVertex().id()} not found in copy")

            val newEdge = fromVertex.addEdge(edge.label(), toVertex)

            // Copy edge properties
            edge.properties<Any>().forEachRemaining { ep ->
                newEdge.property(ep.key(), ep.value())
            }
        }

        return TinkerGraphBackend(copy)
    }

    override fun close() {
        graph.close()
    }
}

/**
 * Factory that creates [TinkerGraphBackend] instances.
 */
class TinkerGraphBackendFactory : GraphBackendFactory {
    override fun create(): GraphBackend = TinkerGraphBackend()
}
