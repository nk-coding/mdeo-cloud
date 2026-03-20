package com.mdeo.modeltransformation.graph

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.ModelData
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Graph-based representation of a model.
 *
 * A [ModelGraph] wraps a graph database (e.g. TinkerGraph) that stores model instances
 * as vertices and associations as edges. It owns the [Metamodel] used for graph key
 * resolution and the [InstanceNameRegistry] that maps vertex IDs to instance names.
 *
 * Implementations support deep copying for copy-before-transform mutation strategies,
 * and resetting nondeterministic behavior between match operations.
 *
 * @see TinkerModelGraph
 */
interface ModelGraph : AutoCloseable {

    /** The compiled metamodel this graph is based on. */
    val metamodel: Metamodel

    /** Registry mapping vertex IDs to instance names. */
    val nameRegistry: InstanceNameRegistry

    /**
     * Returns a Gremlin [GraphTraversalSource] for querying and modifying this graph.
     * The returned source is valid until [close] is called.
     */
    fun traversal(): GraphTraversalSource

    /**
     * Creates a deep copy of this model graph.
     *
     * The copy has independent graph state, name registry, and can be modified
     * without affecting the original. For implementations backed by ordered stores
     * (e.g. TinkerGraph), the copy may have shuffled vertex insertion order to
     * provide nondeterministic traversal behavior.
     *
     * @return A new, independent [ModelGraph] with the same model state.
     */
    fun deepCopy(): ModelGraph

    /**
     * Resets the nondeterministic behavior of the underlying graph.
     *
     * This ensures that subsequent operations (like match queries) may produce
     * different iteration orders. For stores where traversal order depends on
     * insertion order (e.g. TinkerGraph), this internally rebuilds the graph
     * with shuffled vertex order.
     *
     * The first call after construction or [deepCopy] is a no-op, since the
     * graph already has fresh nondeterministic state from its initial creation.
     * Subsequent calls perform the actual reset.
     *
     * @return A mapping from old vertex IDs to new vertex IDs. Empty if no change occurred.
     */
    fun resetNondeterminism(): Map<Any, Any>

    /**
     * Creates a [VertexRef] for the given raw vertex ID.
     *
     * The returned [VertexRef] is tracked by this graph instance and its
     * [VertexRef.rawId] is automatically updated whenever [resetNondeterminism]
     * rebuilds the graph with new vertex IDs. Use this factory instead of
     * constructing [VertexRef] directly so that updates propagate correctly.
     *
     * @param rawId The current raw vertex ID (e.g., from `Vertex.id()`).
     * @return A new [VertexRef] pointing to the given vertex.
     */
    fun createVertexRef(rawId: Any): VertexRef

    /**
     * Converts the current graph state back to [ModelData].
     *
     * @return The model data representing the current graph state.
     */
    fun toModelData(): ModelData

    /**
     * Returns the current graph state as a [com.mdeo.metamodel.Model].
     *
     * For [MdeoModelGraph], this returns the live model backed directly by the
     * underlying [ModelInstance] objects. For [TinkerModelGraph], this performs
     * an eager conversion via [toModelData] and [com.mdeo.metamodel.Metamodel.loadModel].
     *
     * @return A [com.mdeo.metamodel.Model] reflecting the current state of this graph.
     */
    fun toModel(): com.mdeo.metamodel.Model
}
