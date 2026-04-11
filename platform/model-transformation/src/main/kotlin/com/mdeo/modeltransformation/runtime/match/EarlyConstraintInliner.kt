package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.compiler.VariableBinding
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * A pre-computed inline filter that can be applied directly in the base traversal right
 * after its anchor node is bound, pruning traversers early.
 *
 * @property islandIndex Index of the owning island in the forbid/require island list.
 * @property inlineTraversal Filter chain starting from the current traverser (= anchor vertex).
 * @property isNegative `true` for forbid (wrapped in `not()`), `false` for require.
 */
internal data class EarlyInlineConstraint(
    val islandIndex: Int,
    val inlineTraversal: GraphTraversal<Any, Any>,
    val isNegative: Boolean
)

/**
 * A forbid/require link whose both endpoints are matched instances rather than island instances.
 *
 * Because both endpoints will be bound during base-traversal construction, this constraint
 * can be inlined immediately once the second endpoint is covered.
 */
internal data class OrphanLinkInfo(
    val linkIndex: Int,
    val sourceName: String,
    val targetName: String,
    val edgeLabel: String
)

/**
 * Accumulated state from [EarlyConstraintInliner.compute] plus the mutable sets that track
 * which constraints have already been applied during base-traversal construction.
 *
 * The `applied*` sets are mutated by [EarlyConstraintInliner.applyForInstance] and
 * [EarlyConstraintInliner.applyOrphanLinks] so that [IslandConstraintApplier] can skip them.
 */
internal class EarlyConstraintPlan(
    /** Map from anchor instance name to the list of early-inline constraints anchored on it. */
    val earlyConstraints: Map<String, List<EarlyInlineConstraint>>,
    val forbidOrphanLinks: List<OrphanLinkInfo>,
    val requireOrphanLinks: List<OrphanLinkInfo>
) {
    val appliedForbidIslands: MutableSet<Int> = mutableSetOf()
    val appliedRequireIslands: MutableSet<Int> = mutableSetOf()
    val appliedForbidOrphanLinks: MutableSet<Int> = mutableSetOf()
    val appliedRequireOrphanLinks: MutableSet<Int> = mutableSetOf()
}

/**
 * Computes which island/orphan constraints can be inlined into the base traversal and
 * applies them immediately after their anchor node is bound.
 *
 * An island is eligible for early inlining when:
 * - It has links (non-disconnected).
 * - Exactly one of its link endpoints belongs to the main match pattern (one anchor).
 * - Its graph forms a simple path from the anchor (no back-tracking needed).
 *
 * Inlining these constraints prunes traversers before expensive mid-traversal V() scans,
 * yielding the large speedups measured for `createSprint` and `addItemToSprint`.
 */
internal class EarlyConstraintInliner(
    private val matchableNames: Set<String>,
    private val expressionSupport: ExpressionSupport
) {
    /**
     * Analyses forbid/require islands and orphan links and returns a [EarlyConstraintPlan]
     * that can be consumed iteratively as instances are covered during base-traversal building.
     */
    fun compute(
        forbidIslands: List<Island>,
        requireIslands: List<Island>,
        forbidInstances: List<TypedPatternObjectInstanceElement>,
        forbidLinks: List<TypedPatternLinkElement>,
        requireInstances: List<TypedPatternObjectInstanceElement>,
        requireLinks: List<TypedPatternLinkElement>
    ): EarlyConstraintPlan {
        val earlyConstraints = mutableMapOf<String, MutableList<EarlyInlineConstraint>>()

        for ((index, island) in forbidIslands.withIndex()) {
            computeInlineConstraint(island, index, isNegative = true)
                ?.let { (anchor, constraint) ->
                    earlyConstraints.getOrPut(anchor) { mutableListOf() }.add(constraint)
                }
        }
        for ((index, island) in requireIslands.withIndex()) {
            computeInlineConstraint(island, index, isNegative = false)
                ?.let { (anchor, constraint) ->
                    earlyConstraints.getOrPut(anchor) { mutableListOf() }.add(constraint)
                }
        }

        return EarlyConstraintPlan(
            earlyConstraints = earlyConstraints,
            forbidOrphanLinks = identifyOrphanLinks(forbidInstances, forbidLinks),
            requireOrphanLinks = identifyOrphanLinks(requireInstances, requireLinks)
        )
    }

    /**
     * Applies all early inline constraints whose anchor is [instanceName], adding inline
     * `.not(...)` or `.where(...)` filters to [traversal] and updating [plan]'s applied sets.
     */
    @Suppress("UNCHECKED_CAST")
    fun applyForInstance(
        traversal: GraphTraversal<Vertex, Vertex>,
        instanceName: String,
        plan: EarlyConstraintPlan
    ): GraphTraversal<Vertex, Vertex> {
        val constraints = plan.earlyConstraints[instanceName] ?: return traversal
        var result = traversal
        for (constraint in constraints) {
            result = if (constraint.isNegative) {
                (result.not(constraint.inlineTraversal) as GraphTraversal<Vertex, Vertex>).also {
                    plan.appliedForbidIslands.add(constraint.islandIndex)
                }
            } else {
                (result.where(constraint.inlineTraversal) as GraphTraversal<Vertex, Vertex>).also {
                    plan.appliedRequireIslands.add(constraint.islandIndex)
                }
            }
        }
        return result
    }

    /**
     * Applies orphan-link constraints for which both endpoints are now in [coveredInstances],
     * updating [plan]'s applied sets.
     *
     * @param isNegative `true` to apply forbid orphan links, `false` for require.
     */
    @Suppress("UNCHECKED_CAST")
    fun applyOrphanLinks(
        traversal: GraphTraversal<Vertex, Vertex>,
        coveredInstances: Set<String>,
        plan: EarlyConstraintPlan,
        isNegative: Boolean
    ): GraphTraversal<Vertex, Vertex> {
        val orphanLinks = if (isNegative) plan.forbidOrphanLinks else plan.requireOrphanLinks
        val appliedIndices = if (isNegative) plan.appliedForbidOrphanLinks else plan.appliedRequireOrphanLinks

        var result = traversal
        for (info in orphanLinks) {
            if (info.linkIndex in appliedIndices) continue
            if (info.sourceName !in coveredInstances || info.targetName !in coveredInstances) continue

            val inner = AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(info.sourceName))
                .out(info.edgeLabel)
                .`as`(VariableBinding.stepLabel(info.targetName)) as GraphTraversal<Any, Any>

            result = if (isNegative) {
                result.where(AnonymousTraversal.not<Any>(inner)) as GraphTraversal<Vertex, Vertex>
            } else {
                result.where(inner) as GraphTraversal<Vertex, Vertex>
            }
            appliedIndices.add(info.linkIndex)
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Tries to build an inline constraint for [island].
     *
     * Returns `null` if the island has no links, has more than one anchor, or requires
     * back-tracking (which would need `select()` and cannot be simply inlined).
     */
    @Suppress("UNCHECKED_CAST")
    private fun computeInlineConstraint(
        island: Island,
        islandIndex: Int,
        isNegative: Boolean
    ): Pair<String, EarlyInlineConstraint>? {
        if (island.links.isEmpty()) return null

        val islandNames = island.instances.map { it.objectInstance.name }.toSet()
        val anchorNames = IslandTraversalUtils.findAnchorNames(island.links, islandNames, matchableNames)
        if (anchorNames.size != 1) return null

        val anchor = anchorNames.first()
        val orderedLinks = IslandTraversalUtils.orderLinksByBFS(island.links, anchor)

        // Reject if any link's fromNode differs from the current traversal head
        var current = anchor
        for ((link, isReversed) in orderedLinks) {
            val from = if (isReversed) link.link.target.objectName else link.link.source.objectName
            if (from != current) return null
            current = if (isReversed) link.link.source.objectName else link.link.target.objectName
        }

        val chain = buildInlineChain(anchor, orderedLinks, island)
        return anchor to EarlyInlineConstraint(islandIndex, chain, isNegative)
    }

    /** Builds the inline filter chain starting from `identity()` (current traverser = anchor). */
    @Suppress("UNCHECKED_CAST")
    private fun buildInlineChain(
        anchor: String,
        orderedLinks: List<Pair<TypedPatternLinkElement, Boolean>>,
        island: Island
    ): GraphTraversal<Any, Any> {
        val islandClassMap = island.instances.mapNotNull { inst ->
            inst.objectInstance.className?.let { inst.objectInstance.name to it }
        }.toMap()
        val islandInstanceMap = island.instances.associateBy { it.objectInstance.name }

        var chain: GraphTraversal<Any, Any> =
            AnonymousTraversal.identity<Any>() as GraphTraversal<Any, Any>
        var current = anchor

        for ((link, isReversed) in orderedLinks) {
            val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                link.link.source.propertyName, link.link.target.propertyName
            )
            val toNode = if (isReversed) link.link.source.objectName else link.link.target.objectName

            chain = if (isReversed) chain.`in`(edgeLabel) as GraphTraversal<Any, Any>
                    else chain.out(edgeLabel) as GraphTraversal<Any, Any>

            islandClassMap[toNode]?.let { cls ->
                chain = chain.hasLabel(cls) as GraphTraversal<Any, Any>
            }
            islandInstanceMap[toNode]?.let { inst ->
                chain = expressionSupport.applyPropertyEqualityConstraints(
                    chain, inst.objectInstance.className, inst.objectInstance.properties
                )
            }
            current = toNode
        }
        return chain
    }

    /** Finds links whose both endpoints are matched instances (not island instances). */
    private fun identifyOrphanLinks(
        conditionInstances: List<TypedPatternObjectInstanceElement>,
        conditionLinks: List<TypedPatternLinkElement>
    ): List<OrphanLinkInfo> {
        val conditionNames = conditionInstances.map { it.objectInstance.name }.toSet()
        return conditionLinks.mapIndexedNotNull { index, link ->
            val src = link.link.source.objectName
            val tgt = link.link.target.objectName
            if (src !in conditionNames && src in matchableNames &&
                tgt !in conditionNames && tgt in matchableNames) {
                OrphanLinkInfo(
                    linkIndex = index,
                    sourceName = src,
                    targetName = tgt,
                    edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                        link.link.source.propertyName, link.link.target.propertyName
                    )
                )
            } else null
        }
    }
}
