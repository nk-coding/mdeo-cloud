package com.mdeo.modeltransformation.runtime.match.plan

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.runtime.match.ExpressionNodeAnalyzer
import com.mdeo.modeltransformation.runtime.match.Island
import com.mdeo.modeltransformation.runtime.match.IslandGrouper
import com.mdeo.modeltransformation.runtime.match.IslandTraversalUtils
import com.mdeo.modeltransformation.runtime.match.PatternCategories

/**
 * Builds a fully imperative [MatchPlan] from categorised pattern elements.
 *
 * The builder places all work into [MatchPlan.baseSteps]:
 *
 * 1. Connected-component starts, edge walks, and reachable vertices form the core traversal.
 * 2. Property constraints whose values are constants or expressions referencing only
 *    already-bound nodes are inlined right after their owning instance.
 * 3. Single-anchor island constraints are inlined right after their anchor is bound.
 * 4. Orphan link constraints are inlined once both endpoints are covered.
 * 5. Uncovered instances, referenced instances, non-inlined islands, variables,
 *    deferred property constraints, and where clauses are appended as imperative steps.
 * 6. Only injective and cross-node where clauses go into [MatchPlan.postMatchFilters].
 *
 * No `match()` step is ever used — the plan is purely imperative.
 *
 * @param getVertexId Returns the pre-bound vertex ID for an instance name.
 * @param nodeAnalyzer Analyzes expression trees to find referenced match nodes.
 * @param isCollectionExpression Returns true when an expression evaluates to a collection type.
 */
internal class MatchPlanBuilder(
    private val getVertexId: (String) -> Any?,
    private val nodeAnalyzer: ExpressionNodeAnalyzer,
    private val isCollectionExpression: (TypedExpression) -> Boolean
) {

    /**
     * Builds a [MatchPlan] from the given [elements] and [referencedInstances].
     *
     * All construction logic is encapsulated in a fresh [PlanExecution] instance
     * to keep mutable state local to the call and each phase concise.
     *
     * @param elements The categorised pattern elements to plan.
     * @param referencedInstances Instance names referenced externally that must be bound
     *   even if they are not part of the matchable instance set.
     * @return The completed [MatchPlan].
     */
    fun build(elements: PatternCategories, referencedInstances: Set<String>): MatchPlan =
        PlanExecution(elements, referencedInstances).run()

    /**
     * Partitions [instances] into connected components given the [links] between them.
     *
     * Two instances belong to the same component when they are transitively connected
     * through one or more links. Instances with no links each form a singleton component.
     *
     * @param instances All matchable instances to partition.
     * @param links All matchable links that may connect instances.
     * @return A list of [MatchComponent]s, one per connected component.
     */
    private fun buildComponents(
        instances: List<TypedPatternObjectInstanceElement>,
        links: List<TypedPatternLinkElement>
    ): List<MatchComponent> {
        if (instances.isEmpty()) { return emptyList() }
        val names = instances.map { it.objectInstance.name }
        val adjacency = names.associateWith { mutableListOf<TypedPatternLinkElement>() }.toMutableMap()
        for (link in links) {
            val src = link.link.source.objectName
            val tgt = link.link.target.objectName
            if (src in adjacency && tgt in adjacency) {
                adjacency.getValue(src).add(link)
                adjacency.getValue(tgt).add(link)
            }
        }
        val visited = mutableSetOf<String>()
        return names.mapNotNull { name ->
            if (!visited.add(name)) { return@mapNotNull null }
            val queue = ArrayDeque<String>()
            queue.add(name)
            val compInstances = mutableListOf<String>()
            val compLinks = linkedSetOf<TypedPatternLinkElement>()
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                compInstances.add(cur)
                for (link in adjacency.getValue(cur)) {
                    compLinks.add(link)
                    val next = if (link.link.source.objectName == cur) link.link.target.objectName else link.link.source.objectName
                    if (visited.add(next)) { queue.add(next) }
                }
            }
            MatchComponent(compInstances, compLinks.toList())
        }
    }

    /**
     * Chooses the best starting instance for traversing a connected component.
     *
     * Prefers a pre-bound instance (one with a known vertex ID) so that the traversal
     * starts from a single vertex. Falls back to any instance with a class constraint,
     * allowing a labelled vertex scan.
     *
     * @param componentInstances The ordered list of instance names in the component.
     * @param instanceMap Map from instance name to its element.
     * @return The chosen starting instance name, or `null` if the component is empty.
     */
    private fun chooseComponentStart(
        componentInstances: List<String>,
        instanceMap: Map<String, TypedPatternObjectInstanceElement>
    ): String? {
        for (name in componentInstances) {
            if (getVertexId(name) != null) { return name }
        }
        return componentInstances.firstOrNull { instanceMap[it]?.objectInstance?.className != null }
    }

    /**
     * Computes a map from instance name to the list of combined island indices
     * (across [allIslands]) for which that instance is the sole anchor.
     *
     * An island is inlinable when exactly one matched instance acts as its anchor,
     * making it possible to attach the island constraint right after that anchor is bound.
     *
     * @param allIslands All forbid and require islands in (forbid ++ require) index order.
     * @param matchableNames Names of all matched instances in the main pattern.
     * @return Map from anchor instance name to a list of indices in [allIslands] it anchors.
     */
    private fun computeInlinableIslands(
        allIslands: List<Island>,
        matchableNames: Set<String>
    ): Map<String, List<Int>> {
        val result = mutableMapOf<String, MutableList<Int>>()
        for ((index, island) in allIslands.withIndex()) {
            if (island.links.isEmpty()) { continue }
            val islandNames = island.instances.map { it.objectInstance.name }.toSet()
            val anchors = IslandTraversalUtils.findAnchorNames(island.links, islandNames, matchableNames)
            if (anchors.size == 1) {
                result.getOrPut(anchors.first()) { mutableListOf() }.add(index)
            }
        }
        return result
    }

    /**
     * Builds the [BaseStep] for a deferred (non-inlined) island constraint.
     *
     * Islands without links become a [BaseStep.DisconnectedIslandFilter]. Connected
     * islands are converted to a [BaseStep.InlineIslandConstraint] with
     * `needsSelect = true` so the traversal navigates to the anchor before applying
     * the constraint chain.
     *
     * @param island The island to build the step for.
     * @param matchableNames Names of all matched instances (used to find anchors).
     * @param isNegative `true` for a forbid island, `false` for a require island.
     * @return The appropriate [BaseStep].
     */
    private fun buildDeferredIslandStep(
        island: Island,
        matchableNames: Set<String>,
        isNegative: Boolean
    ): BaseStep {
        if (island.links.isEmpty()) {
            return BaseStep.DisconnectedIslandFilter(island, isNegative)
        }
        val islandNames = island.instances.map { it.objectInstance.name }.toSet()
        val anchorNames = IslandTraversalUtils.findAnchorNames(island.links, islandNames, matchableNames)
        val anchor = anchorNames.firstOrNull()
            ?: return BaseStep.DisconnectedIslandFilter(island, isNegative)
        val orderedLinks = IslandTraversalUtils.orderLinksByBFS(island.links, anchor)
        val backtrackLabels = IslandTraversalUtils.findNodesNeedingBacktrackLabel(orderedLinks, anchor)
        return BaseStep.InlineIslandConstraint(island, anchor, orderedLinks, backtrackLabels, isNegative, needsSelect = true)
    }

    /**
     * Identifies orphan links — forbid or require links whose both endpoints are
     * main-pattern (matchable) instances rather than constraint-only instances.
     *
     * Orphan links connect two already-matchable nodes without introducing new
     * constraint nodes; they are tracked and handled separately from island links.
     *
     * @param conditionInstances All forbid or require instances for the block.
     * @param conditionLinks All forbid or require links for the block.
     * @param matchableNames Names of all matched (non-condition) instances.
     * @return List of [OrphanLinkInfo] objects describing each orphan link.
     */
    private fun identifyOrphanLinks(
        conditionInstances: List<TypedPatternObjectInstanceElement>,
        conditionLinks: List<TypedPatternLinkElement>,
        matchableNames: Set<String>
    ): List<OrphanLinkInfo> {
        val conditionNames = conditionInstances.map { it.objectInstance.name }.toSet()
        return conditionLinks.mapNotNull { link ->
            val src = link.link.source.objectName
            val tgt = link.link.target.objectName
            if (src !in conditionNames && src in matchableNames &&
                tgt !in conditionNames && tgt in matchableNames) {
                OrphanLinkInfo(src, tgt, EdgeLabelUtils.computeEdgeLabel(
                    link.link.source.propertyName, link.link.target.propertyName
                ))
            } else null
        }
    }

    /**
     * Merges instances that share the same name.
     *
     * When the same instance name appears multiple times (e.g., once with a className and once
     * without for a re-reference with additional property constraints), the className is taken
     * from the first occurrence that has one, and properties are combined from all occurrences.
     *
     * @param instances The (possibly duplicated) list of instances to merge.
     * @return A de-duplicated list of instances with properties merged per name.
     */
    private fun mergeInstancesByName(
        instances: List<TypedPatternObjectInstanceElement>
    ): List<TypedPatternObjectInstanceElement> {
        val grouped = instances.groupBy { it.objectInstance.name }
        return grouped.map { (_, elements) ->
            if (elements.size == 1) { return@map elements.first() }
            val className = elements.firstNotNullOfOrNull { it.objectInstance.className }
            val modifier = elements.firstNotNullOfOrNull { it.objectInstance.modifier }
            val allProperties = elements.flatMap { it.objectInstance.properties }
            TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = modifier,
                    name = elements.first().objectInstance.name,
                    className = className,
                    properties = allProperties
                )
            )
        }
    }

    private data class DeferredPropertyInfo(
        val instanceName: String,
        val className: String?,
        val property: com.mdeo.modeltransformation.ast.patterns.TypedPatternPropertyAssignment
    )

    private data class OrphanLinkInfo(
        val sourceName: String,
        val targetName: String,
        val edgeLabel: String
    )

    private data class MatchComponent(
        val instances: List<String>,
        val links: List<TypedPatternLinkElement>
    )

    /**
     * Encapsulates all mutable state for a single invocation of [build].
     *
     * Splitting the execution into an inner class keeps each phase concise and avoids
     * threading the entire mutable context through every private method signature.
     * The outer-class members ([getVertexId], [nodeAnalyzer], [isCollectionExpression],
     * and the utility methods) are accessible via the implicit outer reference.
     */
    private inner class PlanExecution(
        private val elements: PatternCategories,
        private val referencedInstances: Set<String>
    ) {
        private val allMatchable = mergeInstancesByName(elements.matchableInstances + elements.deleteInstances)
        private val allMatchableLinks = elements.matchableLinks + elements.deleteLinks
        private val instanceMap = allMatchable.associateBy { it.objectInstance.name }
        private val matchableNames = allMatchable.map { it.objectInstance.name }.toSet()
        private val variableNames = elements.variables.map { it.variable.name }.toSet()
        private val forbidIslands = IslandGrouper.groupIntoIslands(elements.forbidInstances, elements.forbidLinks)
        private val requireIslands = IslandGrouper.groupIntoIslands(elements.requireInstances, elements.requireLinks)
        private val forbidOrphanLinks = identifyOrphanLinks(elements.forbidInstances, elements.forbidLinks, matchableNames)
        private val requireOrphanLinks = identifyOrphanLinks(elements.requireInstances, elements.requireLinks, matchableNames)
        private val baseSteps = mutableListOf<BaseStep>()
        private val postMatchFilters = mutableListOf<PostMatchFilter>()
        private val coveredInstances = mutableSetOf<String>()
        private val coveredLinks = mutableSetOf<TypedPatternLinkElement>()
        private val inlinedIslandIndices = mutableSetOf<Int>()
        private val inlinedOrphanLinkIndices = mutableSetOf<Pair<Boolean, Int>>()
        private val deferredProperties = mutableListOf<DeferredPropertyInfo>()

        /**
         * Executes all plan construction phases and returns the completed [MatchPlan].
         *
         * @return The [MatchPlan] ready for compilation into a Gremlin traversal.
         */
        fun run(): MatchPlan {
            val inlinableIslands = computeInlinableIslands(forbidIslands + requireIslands, matchableNames)
            val components = buildComponents(allMatchable, allMatchableLinks)
            val sortedComponents = components.sortedWith(
                compareByDescending<MatchComponent> { comp -> comp.instances.any { getVertexId(it) != null } }
                    .thenByDescending { comp ->
                        comp.instances.count { name -> name in inlinableIslands }
                    }
            )
            processComponents(sortedComponents)
            addUncoveredInstances()
            addReferencedInstances()
            addUncoveredLinks()
            addDeferredIslands()
            addDeferredOrphanLinks()
            addVariableBindings()
            addDeferredPropertyConstraints()
            addWhereClauses()
            addInjectiveConstraints()
            return MatchPlan(baseSteps, postMatchFilters)
        }

        /**
         * Iterates over each connected component and delegates to [processComponent].
         *
         * @param sortedComponents Components in traversal-start priority order.
         */
        private fun processComponents(sortedComponents: List<MatchComponent>) {
            for (component in sortedComponents) {
                processComponent(component)
            }
        }

        /**
         * Emits all steps for a single connected component.
         *
         * Chooses the best starting vertex, emits a [BaseStep.VertexScan], applies inline
         * constraints for the start instance, then delegates link traversal to
         * [processComponentLinks].
         *
         * @param component The component to process.
         */
        private fun processComponent(component: MatchComponent) {
            val startName = chooseComponentStart(component.instances, instanceMap) ?: return
            val startInstance = instanceMap[startName]!!
            val className = startInstance.objectInstance.className
            val vertexId = getVertexId(startName)
            if (className == null && vertexId == null) {
                throw IllegalStateException(
                    "Instance '${startName}' has no class constraint and no pre-bound vertex. " +
                    "All matchable instances must be typed or pre-bound."
                )
            }
            baseSteps.add(BaseStep.VertexScan(startName, className, vertexId))
            coveredInstances.add(startName)
            applyInlineConstraintsAt(startName, startInstance)
            if (component.links.isNotEmpty()) {
                processComponentLinks(component.links, startName)
            }
        }

        /**
         * Emits [BaseStep.EdgeWalk] steps for each link in BFS order.
         *
         * After each newly covered instance is added to [coveredInstances], inline
         * constraints (property, island, orphan-link) are applied immediately.
         *
         * @param links The component's links to walk.
         * @param startName The name of the already-covered start vertex.
         */
        private fun processComponentLinks(links: List<TypedPatternLinkElement>, startName: String) {
            val orderedLinks = IslandTraversalUtils.orderLinksByBFS(links, startName)
            var currentNode = startName
            for ((link, isReversed) in orderedLinks) {
                coveredLinks.add(link)
                val fromName = if (isReversed) link.link.target.objectName else link.link.source.objectName
                val toName = if (isReversed) link.link.source.objectName else link.link.target.objectName
                val toInstance = instanceMap[toName]
                baseSteps.add(
                    BaseStep.EdgeWalk(
                        link = link,
                        isReversed = isReversed,
                        fromInstanceName = fromName,
                        toInstanceName = toName,
                        toClassName = toInstance?.objectInstance?.className,
                        toVertexId = getVertexId(toName),
                        needsSelect = fromName != currentNode
                    )
                )
                coveredInstances.add(toName)
                currentNode = toName
                applyInlineConstraintsAt(toName, toInstance)
            }
        }

        /**
         * Applies all inline constraints for [instanceName] immediately after it is covered.
         *
         * This includes inline property constraints for [instance] (when non-null), island
         * constraints anchored at [instanceName], and orphan-link constraints whose both
         * endpoints are now covered.
         *
         * @param instanceName The name of the newly covered instance.
         * @param instance The element for [instanceName], or `null` when not in the matchable set.
         */
        private fun applyInlineConstraintsAt(
            instanceName: String,
            instance: TypedPatternObjectInstanceElement?
        ) {
            if (instance != null) {
                addInlinePropertyConstraints(instance)
            }
            inlineIslandConstraints(instanceName)
            inlineOrphanLinks(forbidOrphanLinks, isNegative = true)
            inlineOrphanLinks(requireOrphanLinks, isNegative = false)
        }

        /**
         * Evaluates each property of [instance] and either emits an inline property
         * constraint or defers it to [deferredProperties].
         *
         * A property is inlined when its expression is a constant (no referenced nodes,
         * non-collection) or references only already-covered instances and no pattern
         * variables. All other properties are deferred.
         *
         * @param instance The instance whose properties are to be evaluated.
         */
        private fun addInlinePropertyConstraints(instance: TypedPatternObjectInstanceElement) {
            for (property in instance.objectInstance.properties) {
                if (property.operator != "==") { continue }
                val referencedNodes = nodeAnalyzer.findReferencedNodes(property.value)
                val referencedVars = referencedNodes.filter { it in variableNames }
                val isConstant = referencedNodes.isEmpty() && !isCollectionExpression(property.value)
                val canInline = referencedVars.isEmpty() && (isConstant || (referencedNodes.isNotEmpty()
                    && referencedNodes.all { it in coveredInstances }
                    && !isCollectionExpression(property.value)))
                if (canInline) {
                    baseSteps.add(
                        BaseStep.InlinePropertyConstraint(
                            instance.objectInstance.name,
                            instance.objectInstance.className,
                            property,
                            isConstant
                        )
                    )
                } else {
                    deferredProperties.add(
                        DeferredPropertyInfo(
                            instance.objectInstance.name,
                            instance.objectInstance.className,
                            property
                        )
                    )
                }
            }
        }

        /**
         * Attempts to inline all island constraints anchored at [instanceName].
         *
         * An island is inlined when it has exactly one anchor and that anchor equals
         * [instanceName]. Already-processed islands (in [inlinedIslandIndices]) are skipped.
         *
         * @param instanceName The name of the newly covered anchor candidate.
         */
        private fun inlineIslandConstraints(instanceName: String) {
            val allIslands = forbidIslands.map { it to true } + requireIslands.map { it to false }
            for ((index, pair) in allIslands.withIndex()) {
                if (index in inlinedIslandIndices) { continue }
                val (island, isNegative) = pair
                if (island.links.isEmpty()) { continue }
                val islandNames = island.instances.map { it.objectInstance.name }.toSet()
                val anchors = IslandTraversalUtils.findAnchorNames(island.links, islandNames, matchableNames)
                if (anchors.size != 1 || anchors.first() != instanceName) { continue }
                val orderedLinks = IslandTraversalUtils.orderLinksByBFS(island.links, instanceName)
                val backtrackLabels = IslandTraversalUtils.findNodesNeedingBacktrackLabel(orderedLinks, instanceName)
                baseSteps.add(
                    BaseStep.InlineIslandConstraint(
                        island = island,
                        anchorName = instanceName,
                        orderedLinks = orderedLinks,
                        nodesNeedingBacktrackLabel = backtrackLabels,
                        isNegative = isNegative
                    )
                )
                inlinedIslandIndices.add(index)
            }
        }

        /**
         * Attempts to inline any [orphanLinks] whose both endpoints are already covered.
         *
         * Links already processed (tracked via [inlinedOrphanLinkIndices]) are skipped.
         *
         * @param orphanLinks The list of orphan links to attempt inlining.
         * @param isNegative `true` for forbid links, `false` for require links.
         */
        private fun inlineOrphanLinks(orphanLinks: List<OrphanLinkInfo>, isNegative: Boolean) {
            for ((index, info) in orphanLinks.withIndex()) {
                val key = Pair(isNegative, index)
                if (key in inlinedOrphanLinkIndices) { continue }
                if (info.sourceName !in coveredInstances || info.targetName !in coveredInstances) { continue }
                baseSteps.add(
                    BaseStep.InlineOrphanLinkConstraint(info.sourceName, info.targetName, info.edgeLabel, isNegative)
                )
                inlinedOrphanLinkIndices.add(key)
            }
        }

        /**
         * Emits [BaseStep.VertexScan] steps for matchable instances not covered during
         * the connected-component traversal phase.
         */
        private fun addUncoveredInstances() {
            for (instance in allMatchable) {
                val name = instance.objectInstance.name
                if (name in coveredInstances) { continue }
                val vertexId = getVertexId(name)
                val className = instance.objectInstance.className
                when {
                    vertexId != null -> baseSteps.add(BaseStep.VertexScan(name, className, vertexId))
                    className != null -> baseSteps.add(BaseStep.VertexScan(name, className, null))
                    else -> throw IllegalStateException(
                        "Instance '${name}' has no class constraint and no pre-bound vertex. " +
                        "All matchable instances must be typed or pre-bound."
                    )
                }
                coveredInstances.add(name)
            }
        }

        /**
         * Emits [BaseStep.VertexScan] steps for externally referenced instances not yet
         * covered and not part of the matchable instance set.
         *
         * A referenced instance is silently skipped when no pre-bound vertex ID is available.
         */
        private fun addReferencedInstances() {
            for (refName in referencedInstances) {
                if (refName in coveredInstances || refName in instanceMap) continue
                val vertexId = getVertexId(refName) ?: continue
                baseSteps.add(BaseStep.VertexScan(refName, null, vertexId))
                coveredInstances.add(refName)
            }
        }

        /**
         * Emits steps for matchable links not covered during component traversal.
         *
         * - Both endpoints covered → [BaseStep.InlineOrphanLinkConstraint].
         * - Source covered only → forward [BaseStep.EdgeWalk] to reach the target.
         * - Target covered only → reversed [BaseStep.EdgeWalk] to reach the source.
         * - Neither covered → forward [BaseStep.EdgeWalk] from source to target.
         */
        private fun addUncoveredLinks() {
            for (link in allMatchableLinks) {
                if (link in coveredLinks) continue
                val srcName = link.link.source.objectName
                val tgtName = link.link.target.objectName
                val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                    link.link.source.propertyName, link.link.target.propertyName
                )
                when {
                    srcName in coveredInstances && tgtName in coveredInstances -> {
                        baseSteps.add(
                            BaseStep.InlineOrphanLinkConstraint(
                                sourceName = srcName,
                                targetName = tgtName,
                                edgeLabel = edgeLabel,
                                isNegative = false
                            )
                        )
                    }
                    srcName in coveredInstances -> {
                        val toInstance = instanceMap[tgtName]
                        baseSteps.add(
                            BaseStep.EdgeWalk(
                                link = link, isReversed = false,
                                fromInstanceName = srcName, toInstanceName = tgtName,
                                toClassName = toInstance?.objectInstance?.className,
                                toVertexId = getVertexId(tgtName),
                                needsSelect = true
                            )
                        )
                        coveredInstances.add(tgtName)
                    }
                    tgtName in coveredInstances -> {
                        val fromInstance = instanceMap[srcName]
                        baseSteps.add(
                            BaseStep.EdgeWalk(
                                link = link, isReversed = true,
                                fromInstanceName = tgtName, toInstanceName = srcName,
                                toClassName = fromInstance?.objectInstance?.className,
                                toVertexId = getVertexId(srcName),
                                needsSelect = true
                            )
                        )
                        coveredInstances.add(srcName)
                    }
                    else -> {
                        val toInstance = instanceMap[tgtName]
                        baseSteps.add(
                            BaseStep.EdgeWalk(
                                link = link, isReversed = false,
                                fromInstanceName = srcName, toInstanceName = tgtName,
                                toClassName = toInstance?.objectInstance?.className,
                                toVertexId = getVertexId(tgtName),
                                needsSelect = true
                            )
                        )
                    }
                }
            }
        }

        /**
         * Emits [BaseStep.InlineIslandConstraint] or [BaseStep.DisconnectedIslandFilter] steps
         * for all islands not inlined during component traversal.
         */
        private fun addDeferredIslands() {
            val allIslands = forbidIslands.map { it to true } + requireIslands.map { it to false }
            for ((index, pair) in allIslands.withIndex()) {
                if (index in inlinedIslandIndices) continue
                val (island, isNegative) = pair
                baseSteps.add(buildDeferredIslandStep(island, matchableNames, isNegative))
            }
        }

        /**
         * Emits [BaseStep.InlineOrphanLinkConstraint] steps for orphan links not inlined
         * during component traversal.
         */
        private fun addDeferredOrphanLinks() {
            for ((index, info) in forbidOrphanLinks.withIndex()) {
                if (Pair(true, index) in inlinedOrphanLinkIndices) continue
                baseSteps.add(
                    BaseStep.InlineOrphanLinkConstraint(info.sourceName, info.targetName, info.edgeLabel, isNegative = true)
                )
            }
            for ((index, info) in requireOrphanLinks.withIndex()) {
                if (Pair(false, index) in inlinedOrphanLinkIndices) continue
                baseSteps.add(
                    BaseStep.InlineOrphanLinkConstraint(info.sourceName, info.targetName, info.edgeLabel, isNegative = false)
                )
            }
        }

        /**
         * Emits [BaseStep.VariableBinding] steps for all pattern variable definitions.
         */
        private fun addVariableBindings() {
            for (varElement in elements.variables) {
                baseSteps.add(
                    BaseStep.VariableBinding(varElement, VariableBinding.variableLabel(varElement.variable.name))
                )
            }
        }

        /**
         * Emits [BaseStep.DeferredPropertyConstraint] steps for all properties that
         * could not be inlined during component traversal.
         */
        private fun addDeferredPropertyConstraints() {
            for (info in deferredProperties) {
                baseSteps.add(BaseStep.DeferredPropertyConstraint(info.instanceName, info.className, info.property))
            }
        }

        /**
         * Classifies each where-clause as either a [BaseStep.WhereFilter] (references at
         * most one matchable instance) or a [PostMatchFilter.CrossNodeWhereClause]
         * (references multiple matchable instances) and appends it accordingly.
         */
        private fun addWhereClauses() {
            for (clause in elements.whereClauses) {
                val referencedMatchable = nodeAnalyzer.findReferencedNodes(clause.whereClause.expression)
                    .filter { it in matchableNames }
                if (referencedMatchable.size > 1) {
                    postMatchFilters.add(PostMatchFilter.CrossNodeWhereClause(clause))
                } else {
                    baseSteps.add(BaseStep.WhereFilter(clause))
                }
            }
        }

        /**
         * Appends [PostMatchFilter.InjectiveConstraint]s for all pairs of matched instances
         * that share the same class name, ensuring they bind to distinct vertices.
         */
        private fun addInjectiveConstraints() {
            for (i in allMatchable.indices) {
                for (j in i + 1 until allMatchable.size) {
                    val a = allMatchable[i].objectInstance
                    val b = allMatchable[j].objectInstance
                    if (a.name != b.name && a.className != null && a.className == b.className) {
                        postMatchFilters.add(PostMatchFilter.InjectiveConstraint(a.name, b.name))
                    }
                }
            }
        }
    }
}
