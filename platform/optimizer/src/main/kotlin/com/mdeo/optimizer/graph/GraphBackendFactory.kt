package com.mdeo.optimizer.graph

/**
 * Factory for creating [GraphBackend] instances.
 *
 * Abstracting the factory allows switching the backing implementation
 * (e.g. TinkerGraph vs remote JanusGraph) without changing optimizer core logic.
 */
interface GraphBackendFactory {
    /**
     * Creates a new, empty graph backend.
     */
    fun create(): GraphBackend
}
