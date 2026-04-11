package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.compiler.VariableBinding
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * Applies late island constraints (forbid/require) to a traversal after the `match()` step.
 *
 * Islands that were already inlined by [EarlyConstraintInliner] are skipped via the
 * `applied*` tracking sets on [EarlyConstraintPlan].
 *
 * ## Why constraints remain here
 * Some islands cannot be inlined because they have multiple anchors, require back-tracking,
 * or are fully disconnected (no links). These must be expressed as post-match `where()` /
 * `not(where())` filters at this stage.
 */
internal class IslandConstraintApplier(
    private val matchableNames: Set<String>,
    private val expressionSupport: ExpressionSupport
) {
    /**
     * Appends all late-applied island and orphan-link constraints to [traversal].
     *
     * Constraints whose indices appear in [plan]'s `applied*` sets are skipped.
     */
    @Suppress("UNCHECKED_CAST")
    fun apply(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories,
        plan: EarlyConstraintPlan
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal

        val forbidIslands = IslandGrouper.groupIntoIslands(elements.forbidInstances, elements.forbidLinks)
        for ((index, island) in forbidIslands.withIndex()) {
            if (index in plan.appliedForbidIslands) continue
            result = addSingleIslandConstraint(result, island, isNegative = true)
        }
        result = addOrphanLinkConstraints(
            result, elements.forbidInstances, elements.forbidLinks,
            isNegative = true, plan.appliedForbidOrphanLinks
        )

        val requireIslands = IslandGrouper.groupIntoIslands(elements.requireInstances, elements.requireLinks)
        for ((index, island) in requireIslands.withIndex()) {
            if (index in plan.appliedRequireIslands) continue
            result = addSingleIslandConstraint(result, island, isNegative = false)
        }
        result = addOrphanLinkConstraints(
            result, elements.requireInstances, elements.requireLinks,
            isNegative = false, plan.appliedRequireOrphanLinks
        )

        return result
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun addOrphanLinkConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        conditionInstances: List<TypedPatternObjectInstanceElement>,
        conditionLinks: List<TypedPatternLinkElement>,
        isNegative: Boolean,
        earlyAppliedIndices: Set<Int>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val conditionNames = conditionInstances.map { it.objectInstance.name }.toSet()
        var result = traversal
        for ((index, link) in conditionLinks.withIndex()) {
            if (index in earlyAppliedIndices) continue
            val src = link.link.source.objectName
            val tgt = link.link.target.objectName
            if (src !in conditionNames && src in matchableNames &&
                tgt !in conditionNames && tgt in matchableNames) {
                val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                    link.link.source.propertyName, link.link.target.propertyName
                )
                val inner = AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(src))
                    .out(edgeLabel)
                    .`as`(VariableBinding.stepLabel(tgt)) as GraphTraversal<Any, Any>
                result = if (isNegative)
                    result.where(AnonymousTraversal.not<Any>(inner)) as GraphTraversal<Vertex, Map<String, Any>>
                else
                    result.where(inner) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun addSingleIslandConstraint(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        island: Island,
        isNegative: Boolean
    ): GraphTraversal<Vertex, Map<String, Any>> {
        if (island.links.isEmpty()) {
            return addDisconnectedIslandConstraint(traversal, island, isNegative)
        }

        val islandNames = island.instances.map { it.objectInstance.name }.toSet()
        val islandClassMap = island.instances.mapNotNull { inst ->
            inst.objectInstance.className?.let { inst.objectInstance.name to it }
        }.toMap()

        val inner = buildIslandChainTraversal(island, islandNames, islandClassMap)
            ?: return traversal

        return if (isNegative)
            traversal.where(AnonymousTraversal.not<Any>(inner)) as GraphTraversal<Vertex, Map<String, Any>>
        else
            traversal.where(inner) as GraphTraversal<Vertex, Map<String, Any>>
    }

    /**
     * Builds a Gremlin chain traversal that walks the island's connectivity graph using BFS.
     *
     * When the chain needs to backtrack to a previously visited node to explore a different
     * branch, `.select(nodeName)` is used. Every node reached gets labelled unconditionally
     * via `.as(stepLabel)` to support this backtracking in tree-shaped island patterns.
     *
     * @return The chain traversal, or `null` if no anchor is found in the main pattern.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildIslandChainTraversal(
        island: Island,
        islandNames: Set<String>,
        islandClassMap: Map<String, String>
    ): GraphTraversal<Any, Any>? {
        val islandInstanceMap = island.instances.associateBy { it.objectInstance.name }
        val anchorNames = IslandTraversalUtils.findAnchorNames(island.links, islandNames, matchableNames)
        val startAnchor = anchorNames.firstOrNull() ?: return null

        val orderedLinks = IslandTraversalUtils.orderLinksByBFS(island.links, startAnchor)
        val needBacktrackLabel = IslandTraversalUtils.findNodesNeedingBacktrackLabel(orderedLinks, startAnchor)

        var chain: GraphTraversal<Any, Any> =
            AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(startAnchor)) as GraphTraversal<Any, Any>
        var current = startAnchor

        for ((link, isReversed) in orderedLinks) {
            val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                link.link.source.propertyName, link.link.target.propertyName
            )
            val from = if (isReversed) link.link.target.objectName else link.link.source.objectName
            val to = if (isReversed) link.link.source.objectName else link.link.target.objectName

            if (from != current) {
                chain = chain.select<Any>(VariableBinding.stepLabel(from)) as GraphTraversal<Any, Any>
            }

            chain = if (isReversed) chain.`in`(edgeLabel) as GraphTraversal<Any, Any>
                    else chain.out(edgeLabel) as GraphTraversal<Any, Any>

            islandClassMap[to]?.let { cls ->
                chain = chain.hasLabel(cls) as GraphTraversal<Any, Any>
            }
            islandInstanceMap[to]?.let { inst ->
                chain = expressionSupport.applyPropertyEqualityConstraints(
                    chain, inst.objectInstance.className, inst.objectInstance.properties
                )
            }

            if (to in matchableNames || to in needBacktrackLabel) {
                chain = chain.`as`(VariableBinding.stepLabel(to)) as GraphTraversal<Any, Any>
            }
            current = to
        }
        return chain
    }

    /**
     * Adds a constraint for an island with no links (fully disconnected).
     *
     * - Forbid: `where(V().hasLabel(cls).count().is(eq(0)))` — no such vertex may exist.
     * - Require: `where(V().hasLabel(cls).count().is(gt(0)))` — at least one must exist.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addDisconnectedIslandConstraint(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        island: Island,
        isNegative: Boolean
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (instance in island.instances) {
            val className = instance.objectInstance.className ?: continue
            val predicate = if (isNegative) P.eq(0L) else P.gt(0L)
            var clause: GraphTraversal<Any, Any> =
                AnonymousTraversal.V<Any>().hasLabel(className) as GraphTraversal<Any, Any>
            clause = expressionSupport.applyPropertyEqualityConstraints(
                clause, instance.objectInstance.className, instance.objectInstance.properties
            )
            result = result.where(clause.count().`is`(predicate)) as GraphTraversal<Vertex, Map<String, Any>>
        }
        return result
    }
}
