package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationEngine
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * The output of [MatchPlanBuilder.build]: the base traversal that enumerates component start
 * vertices and the (possibly empty) array of match clauses to pass to `match()`.
 *
 * When all components have a viable start and there are no referenced instances or variables,
 * [clauses] is empty and the match is executed entirely within the base traversal.
 *
 * The [earlyConstraintPlan] records which island/orphan-link constraints were already inlined
 * during base-traversal construction so that [IslandConstraintApplier] can skip them.
 */
internal data class MatchPlan(
    val baseTraversal: GraphTraversal<Vertex, Vertex>,
    val clauses: Array<GraphTraversal<Any, Any>>,
    val earlyConstraintPlan: EarlyConstraintPlan
)

/**
 * Builds the declarative [MatchPlan] for a single match execution.
 *
 * ## Base traversal strategy
 * For every connected component with a viable start vertex, the start scan (`V(id)` or
 * `V().hasLabel(...)`) is emitted *outside* `match()`. BFS then introduces all remaining
 * vertices in the component so that match clauses only encode edge walks, avoiding expensive
 * repeated `V()` scans inside `match()`.
 *
 * Components without a viable start fall back to classic anchor-based vertex clauses for
 * correctness.
 *
 * ## Early constraint inlining
 * Before building the base traversal, [EarlyConstraintInliner] analyses forbid/require
 * islands and orphan links. Eligible constraints are applied as inline filters immediately
 * after the anchor node is bound, dramatically pruning traversers before any mid-traversal
 * vertex scan.
 *
 * ## Component ordering
 * Components are sorted so that those with early-applicable constraints are processed first,
 * ensuring filtering happens before subsequent V() scans in other components.
 */
internal class MatchPlanBuilder(
    private val context: TransformationExecutionContext,
    private val engine: TransformationEngine,
    private val compilationContext: CompilationContext,
    private val expressionSupport: ExpressionSupport
) {
    private val anchorLabel = MATCH_ANCHOR_LABEL

    fun build(
        elements: PatternCategories,
        referencedInstances: Set<String>
    ): MatchPlan {
        val allMatchable = elements.matchableInstances + elements.deleteInstances
        val allMatchableLinks = elements.matchableLinks + elements.deleteLinks
        val instanceMap = allMatchable.associateBy { it.objectInstance.name }
        val matchableNames = allMatchable.map { it.objectInstance.name }.toSet()

        val components = buildComponents(allMatchable, allMatchableLinks)

        val forbidIslands = IslandGrouper.groupIntoIslands(elements.forbidInstances, elements.forbidLinks)
        val requireIslands = IslandGrouper.groupIntoIslands(elements.requireInstances, elements.requireLinks)
        val inliner = EarlyConstraintInliner(matchableNames, expressionSupport)
        val plan = inliner.compute(
            forbidIslands, requireIslands,
            elements.forbidInstances, elements.forbidLinks,
            elements.requireInstances, elements.requireLinks
        )

        val sortedComponents = components.sortedByDescending { component ->
            component.instances.count { it in plan.earlyConstraints }
        }

        @Suppress("UNCHECKED_CAST")
        var baseTraversal = engine.traversalSource
            .inject(emptyMap<String, Any>()).`as`(anchorLabel) as GraphTraversal<Vertex, Vertex>
        val clauses = mutableListOf<GraphTraversal<Any, Any>>()
        val coveredInstances = mutableSetOf<String>()
        val coveredLinks = mutableSetOf<TypedPatternLinkElement>()
        var allComponentsStarted = true

        for (component in sortedComponents) {
            val startName = chooseComponentStart(component.instances, instanceMap)
            if (startName == null) {
                allComponentsStarted = false
                continue
            }

            val startInstance = instanceMap[startName]!!
            baseTraversal = addComponentStart(baseTraversal, startInstance)
            coveredInstances.add(startName)

            baseTraversal = inliner.applyForInstance(baseTraversal, startName, plan)
            baseTraversal = inliner.applyOrphanLinks(baseTraversal, coveredInstances, plan, isNegative = true)
            baseTraversal = inliner.applyOrphanLinks(baseTraversal, coveredInstances, plan, isNegative = false)

            if (component.links.isEmpty()) continue

            val orderedLinks = IslandTraversalUtils.orderLinksByBFS(component.links, startName)
            var currentNode: String = startName
            for ((link, isReversed) in orderedLinks) {
                coveredLinks.add(link)
                val toName = if (isReversed) link.link.source.objectName else link.link.target.objectName
                val fromName = if (isReversed) link.link.target.objectName else link.link.source.objectName
                baseTraversal = addConnectedEdge(baseTraversal, currentNode, fromName, link, isReversed, instanceMap[toName])
                coveredInstances.add(toName)
                currentNode = toName

                baseTraversal = inliner.applyForInstance(baseTraversal, toName, plan)
                baseTraversal = inliner.applyOrphanLinks(baseTraversal, coveredInstances, plan, isNegative = true)
                baseTraversal = inliner.applyOrphanLinks(baseTraversal, coveredInstances, plan, isNegative = false)
            }
        }

        if (allComponentsStarted && referencedInstances.isEmpty() && elements.variables.isEmpty()) {
            return MatchPlan(baseTraversal, emptyArray(), plan)
        }

        // Fall-back: instances not covered by the base traversal become match() clauses
        for (instance in allMatchable) {
            if (instance.objectInstance.name in coveredInstances) continue
            val vertexId = (context.variableScope.getVariable(instance.objectInstance.name)
                as? VariableBinding.InstanceBinding)?.vertexId
            if (vertexId != null) {
                clauses.add(buildIdVertexMatchClause(vertexId, instance.objectInstance.name))
            } else if (instance.objectInstance.className != null) {
                clauses.add(buildVertexMatchClause(instance))
            }
        }

        for (refName in referencedInstances) {
            if (refName in instanceMap || refName in coveredInstances) continue
            val vertexId = (context.variableScope.getVariable(refName)
                as? VariableBinding.InstanceBinding)?.vertexId
            if (vertexId != null) {
                clauses.add(buildIdVertexMatchClause(vertexId, refName))
            }
        }

        for (link in allMatchableLinks) {
            if (link !in coveredLinks) clauses.add(buildEdgeMatchClause(link))
        }

        for (varElement in elements.variables) {
            val varName = varElement.variable.name
            val varLabel = VariableBinding.variableLabel(varName)
            val (varClause, _) = buildVariableMatchClause(varElement, varLabel)
            clauses.add(varClause)
        }

        return MatchPlan(baseTraversal, clauses.toTypedArray(), plan)
    }

    // -------------------------------------------------------------------------
    // Connected-component helpers
    // -------------------------------------------------------------------------

    private data class MatchComponent(
        val instances: List<String>,
        val links: List<TypedPatternLinkElement>
    )

    private fun buildComponents(
        instances: List<TypedPatternObjectInstanceElement>,
        links: List<TypedPatternLinkElement>
    ): List<MatchComponent> {
        if (instances.isEmpty()) return emptyList()

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
            if (!visited.add(name)) return@mapNotNull null
            val queue = ArrayDeque<String>()
            queue.add(name)
            val compInstances = mutableListOf<String>()
            val compLinks = linkedSetOf<TypedPatternLinkElement>()
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                compInstances.add(cur)
                for (link in adjacency.getValue(cur)) {
                    compLinks.add(link)
                    val next = if (link.link.source.objectName == cur)
                        link.link.target.objectName
                    else
                        link.link.source.objectName
                    if (visited.add(next)) queue.add(next)
                }
            }
            MatchComponent(compInstances, compLinks.toList())
        }
    }

    /**
     * Chooses the cheapest start vertex for a component.
     *
     * Prefers pre-bound instances (V(id)), then the first typed instance.
     * Returns `null` when no viable start exists (falls back to match() clauses).
     */
    private fun chooseComponentStart(
        componentInstances: List<String>,
        instanceMap: Map<String, TypedPatternObjectInstanceElement>
    ): String? {
        for (name in componentInstances) {
            val binding = context.variableScope.getVariable(name)
            if ((binding as? VariableBinding.InstanceBinding)?.vertexId != null) return name
        }
        return componentInstances.firstOrNull { instanceMap[it]?.objectInstance?.className != null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addComponentStart(
        traversal: GraphTraversal<Vertex, Vertex>,
        instance: TypedPatternObjectInstanceElement
    ): GraphTraversal<Vertex, Vertex> {
        val name = instance.objectInstance.name
        val binding = context.variableScope.getVariable(name)
        val vertexId = (binding as? VariableBinding.InstanceBinding)?.vertexId
        val sub: GraphTraversal<*, *> = if (vertexId != null)
            traversal.V(vertexId)
        else
            traversal.V().hasLabel(instance.objectInstance.className!!)
        return sub.`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Vertex, Vertex>
    }

    @Suppress("UNCHECKED_CAST")
    private fun addConnectedEdge(
        traversal: GraphTraversal<Vertex, Vertex>,
        currentNode: String,
        fromName: String,
        link: TypedPatternLinkElement,
        isReversed: Boolean,
        targetInstance: TypedPatternObjectInstanceElement?
    ): GraphTraversal<Vertex, Vertex> {
        val toName = if (isReversed) link.link.source.objectName else link.link.target.objectName
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
            link.link.source.propertyName, link.link.target.propertyName
        )
        var result: GraphTraversal<*, *> = traversal
        if (fromName != currentNode) {
            result = result.select<Any>(VariableBinding.stepLabel(fromName))
        }
        result = if (isReversed) result.`in`(edgeLabel) else result.out(edgeLabel)

        if (targetInstance != null) {
            val vertexId = (context.variableScope.getVariable(toName)
                as? VariableBinding.InstanceBinding)?.vertexId
            result = if (vertexId != null) result.hasId(vertexId)
                     else if (targetInstance.objectInstance.className != null)
                         result.hasLabel(targetInstance.objectInstance.className)
                     else result
        }
        return result.`as`(VariableBinding.stepLabel(toName)) as GraphTraversal<Vertex, Vertex>
    }

    // -------------------------------------------------------------------------
    // Match clause builders
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun buildVertexMatchClause(
        instance: TypedPatternObjectInstanceElement
    ): GraphTraversal<Any, Any> {
        val name = instance.objectInstance.name
        val className = instance.objectInstance.className
        return if (className == null) {
            AnonymousTraversal.`as`<Any>(anchorLabel)
                .select<Any>(VariableBinding.stepLabel(name))
                .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>
        } else {
            AnonymousTraversal.`as`<Any>(anchorLabel)
                .V().hasLabel(className)
                .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildIdVertexMatchClause(vertexId: Any, name: String): GraphTraversal<Any, Any> =
        AnonymousTraversal.`as`<Any>(anchorLabel)
            .V(vertexId)
            .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>

    @Suppress("UNCHECKED_CAST")
    private fun buildEdgeMatchClause(link: TypedPatternLinkElement): GraphTraversal<Any, Any> {
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
            link.link.source.propertyName, link.link.target.propertyName
        )
        return AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(link.link.source.objectName))
            .out(edgeLabel)
            .`as`(VariableBinding.stepLabel(link.link.target.objectName)) as GraphTraversal<Any, Any>
    }

    @Suppress("UNCHECKED_CAST")
    internal fun buildVariableMatchClause(
        variableElement: com.mdeo.modeltransformation.ast.patterns.TypedPatternVariableElement,
        variableLabel: String
    ): Pair<GraphTraversal<Any, Any>, com.mdeo.modeltransformation.compiler.CompilationResult> {
        val anchorTraversal = AnonymousTraversal.`as`<Any>(anchorLabel)
        val result = engine.expressionCompilerRegistry.compile(
            variableElement.variable.value,
            compilationContext,
            anchorTraversal
        )
        return Pair(result.traversal.`as`(variableLabel) as GraphTraversal<Any, Any>, result)
    }
}
