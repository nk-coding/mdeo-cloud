package com.mdeo.modeltransformation.graph

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.Model
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.graph.mdeo.MdeoGraph
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import java.lang.ref.WeakReference

/**
 * Thin [ModelGraph] wrapper over [MdeoGraph].
 *
 * Graph construction, deep-copy mechanics, and model ownership are implemented
 * directly by [MdeoGraph]. This wrapper owns the [InstanceNameRegistry] so match
 * execution, export, and deep-copy behavior can control logical instance names
 * independently from the backing graph's internal placeholder instance names.
 */
class MdeoModelGraph private constructor(
    private var graph: MdeoGraph,
    private val instanceNameRegistry: InstanceNameRegistry
) : ModelGraph {

    /**
     * Weak references to all [VertexRef] instances created by this graph.
     *
     * Using weak references prevents this list from keeping [VertexRef] objects
     * alive after their owning scope has been garbage-collected.
     * Dead references are pruned during [resetNondeterminism].
     */
    private val trackedRefs = mutableListOf<WeakReference<VertexRef>>()

    override val metamodel: Metamodel
        get() = graph.metamodel

    override val nameRegistry: InstanceNameRegistry
        get() = instanceNameRegistry

    override fun createVertexRef(rawId: Any): VertexRef {
        val ref = VertexRef(rawId)
        trackedRefs.add(WeakReference(ref))
        return ref
    }

    override fun traversal(): GraphTraversalSource = graph.traversal()

    override fun deepCopy(): MdeoModelGraph {
        return MdeoModelGraph(graph.deepCopy(), instanceNameRegistry.copy())
    }

    /**
     * Shuffles the vertex iteration list to provide nondeterministic traversal order.
     *
     * Since vertex IDs remain stable (no graph rebuild), this always returns an empty map.
     * This method also prunes dead [VertexRef] weak references.
     */
    override fun resetNondeterminism(): Map<Any, Any> {
        graph.shuffleVertices()

        val iter = trackedRefs.iterator()
        while (iter.hasNext()) {
            if (iter.next().get() == null) {
                iter.remove()
            }
        }

        return emptyMap()
    }

    override fun toModelData(): ModelData {
        return toModel().toModelData()
    }

    override fun toModel(): Model = graph.toModel(instanceNameRegistry)

    override fun close() {
        graph.close()
    }

    companion object {
        /**
         * Creates an [MdeoModelGraph] by loading [ModelData] into a fresh [MdeoGraph].
         */
        fun create(modelData: ModelData, metamodel: Metamodel): MdeoModelGraph {
            val model = metamodel.loadModel(modelData)
            val registry = InstanceNameRegistry()
            val graph = MdeoGraph.open(metamodel, model, registry)
            return MdeoModelGraph(graph, registry)
        }
    }
}
