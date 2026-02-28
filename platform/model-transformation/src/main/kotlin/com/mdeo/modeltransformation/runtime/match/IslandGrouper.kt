package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement

/**
 * A connected component of constraint instances and their links.
 *
 * An island represents a group of pattern instances (forbid or require) that are
 * structurally connected through links. All instances and links within an island
 * are evaluated together as a single composite constraint.
 *
 * Islands may also reference "anchor" nodes — matched instances from the main
 * pattern that the constraint nodes connect to. These anchor references appear
 * as link endpoints but are not included in [instances].
 *
 * @property instances The constraint instances (forbid or require) belonging to this island.
 * @property links The constraint links connecting instances within this island,
 *                 or connecting island instances to anchor (matched) nodes.
 */
data class Island(
    val instances: List<TypedPatternObjectInstanceElement>,
    val links: List<TypedPatternLinkElement>
)

/**
 * Groups constraint instances and links into isolated connected components (islands).
 *
 * Two constraint instances belong to the same island if they are transitively connected
 * through constraint links. A constraint instance with no links to other constraint
 * instances forms its own singleton island.
 *
 * Links that reference instances not present in the input list (i.e., anchor/matched
 * nodes) are assigned to the island of whichever constraint instance endpoint they
 * reference. If a link connects two constraint instances, both instances (and the link)
 * belong to the same island.
 *
 * The grouping algorithm uses a union-find data structure for efficient component
 * merging.
 */
object IslandGrouper {

    /**
     * Partitions the given constraint instances and links into a list of [Island]s.
     *
     * @param instances The constraint instances to group (e.g., all forbid instances
     *                  or all require instances from a pattern).
     * @param links The constraint links connecting instances (e.g., all forbid links
     *              or all require links from a pattern).
     * @return A list of islands, each containing its member instances and links.
     *         Returns an empty list when [instances] is empty.
     */
    fun groupIntoIslands(
        instances: List<TypedPatternObjectInstanceElement>,
        links: List<TypedPatternLinkElement>
    ): List<Island> {
        if (instances.isEmpty()) return emptyList()

        val nameToIndex = mutableMapOf<String, Int>()
        for ((index, instance) in instances.withIndex()) {
            nameToIndex[instance.objectInstance.name] = index
        }

        val parent = IntArray(instances.size) { it }
        val rank = IntArray(instances.size) { 0 }

        for (link in links) {
            val sourceIndex = nameToIndex[link.link.source.objectName]
            val targetIndex = nameToIndex[link.link.target.objectName]
            if (sourceIndex != null && targetIndex != null) {
                union(parent, rank, sourceIndex, targetIndex)
            }
        }

        val componentInstances = mutableMapOf<Int, MutableList<TypedPatternObjectInstanceElement>>()
        val componentLinks = mutableMapOf<Int, MutableList<TypedPatternLinkElement>>()

        for ((index, instance) in instances.withIndex()) {
            val root = find(parent, index)
            componentInstances.getOrPut(root) { mutableListOf() }.add(instance)
        }

        for (link in links) {
            val sourceIndex = nameToIndex[link.link.source.objectName]
            val targetIndex = nameToIndex[link.link.target.objectName]
            val representativeIndex = sourceIndex ?: targetIndex ?: continue
            val root = find(parent, representativeIndex)
            componentLinks.getOrPut(root) { mutableListOf() }.add(link)
        }

        return componentInstances.keys.map { root ->
            Island(
                instances = componentInstances[root]!!,
                links = componentLinks[root] ?: emptyList()
            )
        }
    }

    private fun find(parent: IntArray, i: Int): Int {
        var node = i
        while (parent[node] != node) {
            parent[node] = parent[parent[node]]
            node = parent[node]
        }
        return node
    }

    private fun union(parent: IntArray, rank: IntArray, a: Int, b: Int) {
        val rootA = find(parent, a)
        val rootB = find(parent, b)
        if (rootA == rootB) return
        when {
            rank[rootA] < rank[rootB] -> parent[rootA] = rootB
            rank[rootA] > rank[rootB] -> parent[rootB] = rootA
            else -> {
                parent[rootB] = rootA
                rank[rootA]++
            }
        }
    }
}
