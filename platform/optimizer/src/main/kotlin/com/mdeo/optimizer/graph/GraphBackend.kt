package com.mdeo.optimizer.graph

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Abstraction over graph database backends used during optimization.
 *
 * Current implementation: TinkerGraph (in-process).
 * Future implementations may include remote JanusGraph or other Gremlin-compatible stores.
 *
 * Each instance represents a single graph database connection/session.
 * Implementations must be closeable to release resources.
 */
interface GraphBackend : AutoCloseable {
    /**
     * Returns a Gremlin [GraphTraversalSource] for this backend.
     * The returned source is valid until [close] is called.
     */
    fun traversal(): GraphTraversalSource

    /**
     * Creates a deep copy of the current graph state into a new, independent backend instance.
     * The caller is responsible for closing the returned backend.
     *
     * This is used to snapshot a candidate model before applying a transformation,
     * ensuring that failed transformations do not corrupt the original.
     */
    fun deepCopy(): GraphBackend
}
