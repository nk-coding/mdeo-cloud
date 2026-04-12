package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement

/**
 * Pure graph-algorithm utilities for walking island/constraint subgraphs.
 *
 * These functions depend only on the pattern AST, not on any engine or execution
 * context, making them easy to test and reuse across the match pipeline.
 */
internal object IslandTraversalUtils {

    /**
     * Finds the anchor node names for an island — main-pattern matched nodes that are
     * connected to island instances via island links but are not themselves island instances.
     */
    fun findAnchorNames(
        links: List<TypedPatternLinkElement>,
        islandInstanceNames: Set<String>,
        matchableNames: Set<String>
    ): Set<String> {
        val anchors = mutableSetOf<String>()
        for (link in links) {
            val source = link.link.source.objectName
            val target = link.link.target.objectName
            if (source !in islandInstanceNames && source in matchableNames) { anchors.add(source) }
            if (target !in islandInstanceNames && target in matchableNames) { anchors.add(target) }
        }
        return anchors
    }

    /**
     * Orders island links into a BFS traversal sequence starting from [startAnchor].
     *
     * Returns a list of `(link, isReversed)` pairs; when `isReversed` is true, the link
     * is traversed target→source (`.in(edgeLabel)` instead of `.out(edgeLabel)`).
     */
    fun orderLinksByBFS(
        links: List<TypedPatternLinkElement>,
        startAnchor: String
    ): List<Pair<TypedPatternLinkElement, Boolean>> {
        val visited = mutableSetOf(startAnchor)
        val ordered = mutableListOf<Pair<TypedPatternLinkElement, Boolean>>()
        val remaining = links.toMutableList()

        var changed = true
        while (remaining.isNotEmpty() && changed) {
            changed = false
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val link = iterator.next()
                val src = link.link.source.objectName
                val tgt = link.link.target.objectName
                when {
                    src in visited -> {
                        ordered.add(link to false)
                        visited.add(tgt)
                        iterator.remove()
                        changed = true
                    }
                    tgt in visited -> {
                        ordered.add(link to true)
                        visited.add(src)
                        iterator.remove()
                        changed = true
                    }
                }
            }
        }
        return ordered
    }

    /**
     * Determines which nodes in the BFS-ordered link sequence need a step label for
     * backtracking (i.e. the chain will `.select()` back to them for a different branch).
     */
    fun findNodesNeedingBacktrackLabel(
        orderedLinks: List<Pair<TypedPatternLinkElement, Boolean>>,
        startAnchor: String
    ): Set<String> {
        val needed = mutableSetOf<String>()
        var current = startAnchor
        for ((link, isReversed) in orderedLinks) {
            val from = if (isReversed) link.link.target.objectName else link.link.source.objectName
            val to = if (isReversed) link.link.source.objectName else link.link.target.objectName
            if (from != current) { needed.add(from) }
            current = to
        }
        return needed
    }
}
