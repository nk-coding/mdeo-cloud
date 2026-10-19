package com.mdeo.modeltransformation.graph

import com.mdeo.modeltransformation.graph.mdeo.MdeoGraph
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.LazyBarrierStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.PathRetractionStrategy
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The traversal strategies the model graphs register for their traversals.
 *
 * A match traversal is built once and run once, so every strategy is paid once per match; the
 * matcher's traversals are long (one step per pattern node, link check, condition and
 * modification) and select every pattern label at the end. `PathRetractionStrategy` drops path
 * labels no later step reads: here every label is read by the final `select`, so it can retract
 * almost nothing, while its analysis of every step's referenced labels grows with the square of
 * the traversal's length (most of the strategies' time on a realisation's repair statements).
 * Like `LazyBarrierStrategy`, it is not registered.
 */
class TraversalStrategiesTest {

    private fun strategies(graphClass: Class<*>) =
        TraversalStrategies.GlobalCache.getStrategies(graphClass).toList().map { it.javaClass.name }

    @Test
    fun `MdeoGraph traversals apply no path retraction and no lazy barriers`() {
        Class.forName(MdeoGraph::class.java.name) // registers its strategies
        val registered = strategies(MdeoGraph::class.java)
        assertFalse(PathRetractionStrategy::class.java.name in registered, "$registered")
        assertFalse(LazyBarrierStrategy::class.java.name in registered, "$registered")
        assertTrue(registered.isNotEmpty())
    }

    @Test
    fun `TinkerGraph traversals of the model graph apply no path retraction and no lazy barriers`() {
        Class.forName(com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph::class.java.name)
        val registered = strategies(TinkerGraph::class.java)
        assertFalse(PathRetractionStrategy::class.java.name in registered, "$registered")
        assertFalse(LazyBarrierStrategy::class.java.name in registered, "$registered")
    }
}
