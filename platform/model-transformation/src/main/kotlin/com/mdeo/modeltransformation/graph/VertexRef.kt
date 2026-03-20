package com.mdeo.modeltransformation.graph

/**
 * A stable, mutable reference to a vertex in a [ModelGraph].
 *
 * When [ModelGraph.resetNondeterminism] is called, the underlying graph is rebuilt
 * with new auto-generated vertex IDs. [VertexRef] wraps the raw vertex ID and is
 * automatically updated by [TinkerModelGraph] when this happens, so any
 * [com.mdeo.modeltransformation.compiler.VariableBinding.InstanceBinding] holding
 * a [VertexRef] always sees the current, valid vertex ID without needing to be
 * explicitly updated.
 *
 * Create instances via [ModelGraph.createVertexRef] to ensure they are registered
 * with the graph and receive automatic updates.
 */
class VertexRef internal constructor(rawId: Any) {
    /**
     * The current raw vertex ID.
     *
     * Updated automatically by [TinkerModelGraph] when the graph's nondeterminism
     * is reset and vertex IDs change. Exposed as an internal setter so only the
     * owning [TinkerModelGraph] can modify it.
     */
    var rawId: Any = rawId
        internal set
}
