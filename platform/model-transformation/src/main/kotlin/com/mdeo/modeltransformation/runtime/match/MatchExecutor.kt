package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.compiler.SequentialLabelIdGenerator
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.graph.VertexRef
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationEngine
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * Unified executor that combines pattern matching and modifications in a single Gremlin traversal.
 *
 * Delegates to specialised helpers:
 * - [MatchPlanBuilder] — base traversal construction and match()-clause generation.
 * - [IslandConstraintApplier] — late forbid/require island constraints.
 * - [PostMatchFilterApplier] — injective, property-equality and where-clause filters.
 * - [GraphModificationApplier] — create-vertex, property-update, create-edge and delete steps.
 *
 * A single [SequentialLabelIdGenerator] is shared across all helpers via [ExpressionSupport]
 * to prevent step-label collisions.
 */
class MatchExecutor {

    companion object {
        /** Default limit for single-match operations. */
        const val DEFAULT_LIMIT = 1L
        /** Signals unlimited results (for foreach operations). */
        const val UNLIMITED = -1L
    }

    private val labelIdGenerator = SequentialLabelIdGenerator()

    /** Returns the first match, or [MatchResult.NoMatch] when none is found. */
    fun executeMatch(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): MatchResult = executeMatchWithLimit(pattern, context, engine, DEFAULT_LIMIT).firstOrNull()
        ?: MatchResult.NoMatch("No match found")

    /** Returns all matches for the pattern. */
    fun executeMatchAll(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): List<MatchResult.Matched> = executeMatchWithLimit(pattern, context, engine, UNLIMITED)

    /**
     * Executes the pattern match returning up to [limit] results (-1 for unlimited).
     *
     * When [TransformationEngine.deterministic] is false, the graph's vertex iteration order is
     * reset before each match so that independent match steps within the same transformation each
     * explore a fresh, independently-shuffled order.
     */
    fun executeMatchWithLimit(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        if (!engine.deterministic) engine.modelGraph.resetNondeterminism()

        val elements = PatternCategories.from(pattern)
        for (name in elements.allInstanceNames) {
            if (context.variableScope.getVariable(name) == null) {
                context.variableScope.setBinding(name, VariableBinding.InstanceBinding(vertexRef = null))
            }
        }
        return executeUnifiedTraversal(elements, context, engine, limit)
    }

    // -------------------------------------------------------------------------
    // Internal orchestration
    // -------------------------------------------------------------------------

    private fun executeUnifiedTraversal(
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        val expressionSupport = ExpressionSupport(engine, context, labelIdGenerator)
        val compilationContext = expressionSupport.newCompilationContext()

        for (varElement in elements.variables) {
            val varLabel = VariableBinding.variableLabel(varElement.variable.name)
            context.variableScope.setBinding(varElement.variable.name, VariableBinding.LabelBinding(varLabel))
        }

        val analyzer = MatchAnalyzer(context.variableScope)
        elements.variables.forEach { analyzer.analyzeVariable(it) }
        elements.whereClauses.forEach { analyzer.analyzeWhereClause(it) }
        (elements.matchableInstances + elements.createInstances + elements.deleteInstances)
            .forEach { analyzer.analyzeObjectInstance(it) }
        (elements.matchableLinks + elements.createLinks + elements.deleteLinks +
            elements.forbidLinks + elements.requireLinks)
            .forEach { analyzer.analyzeLink(it) }
        val referencedInstances = analyzer.getReferencedInstances()

        val matchPlan = MatchPlanBuilder(context, engine, compilationContext, expressionSupport)
            .build(elements, referencedInstances)

        val allMatchable = elements.matchableInstances + elements.deleteInstances
        val matchableNames = allMatchable.map { it.objectInstance.name }.toSet()

        val islandApplier = IslandConstraintApplier(matchableNames, expressionSupport)
        val postFilters = PostMatchFilterApplier(expressionSupport)

        val traversal = buildUnifiedTraversal(
            elements, matchPlan, allMatchable, islandApplier, postFilters,
            expressionSupport, compilationContext, limit
        )

        val matchedInstanceNames = allMatchable.map { it.objectInstance.name }
        return executeTraversalAndExtract(traversal, elements, context, engine, matchedInstanceNames)
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildUnifiedTraversal(
        elements: PatternCategories,
        plan: MatchPlan,
        allMatchable: List<com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement>,
        islandApplier: IslandConstraintApplier,
        postFilters: PostMatchFilterApplier,
        expressionSupport: ExpressionSupport,
        compilationContext: com.mdeo.modeltransformation.compiler.CompilationContext,
        limit: Long
    ): GraphTraversal<Vertex, Any> {
        var t: GraphTraversal<Vertex, Map<String, Any>> = if (plan.clauses.isEmpty()) {
            plan.baseTraversal as GraphTraversal<Vertex, Map<String, Any>>
        } else {
            buildMatchStep(plan.baseTraversal, plan.clauses)
        }

        t = islandApplier.apply(t, elements, plan.earlyConstraintPlan)
        t = postFilters.applyPropertyWhereConstraints(t, allMatchable)
        t = postFilters.applyWhereClauseConstraints(t, elements.whereClauses)
        t = postFilters.applyInjectiveConstraints(t, allMatchable)
        t = applyLimit(t, limit)

        val matchedNames = elements.matchableInstances.map { it.objectInstance.name }
        t = GraphModificationApplier(elements, expressionSupport, compilationContext).apply(t, matchedNames)

        val variableLabels = elements.variables.map { VariableBinding.variableLabel(it.variable.name) }
        return addSelectStep(t, elements.allInstanceNames, variableLabels)
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildMatchStep(
        base: GraphTraversal<Vertex, Vertex>,
        clauses: Array<GraphTraversal<Any, Any>>
    ): GraphTraversal<Vertex, Map<String, Any>> = when {
        clauses.size == 1 -> base.match<Any>(clauses[0])
        else -> base.match<Any>(clauses[0], *clauses.drop(1).toTypedArray())
    } as GraphTraversal<Vertex, Map<String, Any>>

    private fun applyLimit(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        limit: Long
    ): GraphTraversal<Vertex, Map<String, Any>> =
        if (limit <= 0) traversal
        else traversal.limit(limit) as GraphTraversal<Vertex, Map<String, Any>>

    @Suppress("UNCHECKED_CAST")
    private fun addSelectStep(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceNames: List<String>,
        variableLabels: List<String>
    ): GraphTraversal<Vertex, Any> {
        val stepLabels = instanceNames.map { VariableBinding.stepLabel(it) }
        val all = stepLabels + variableLabels
        return when (all.size) {
            0 -> traversal as GraphTraversal<Vertex, Any>
            1 -> traversal.select<Any>(all[0]) as GraphTraversal<Vertex, Any>
            else -> traversal.select<Any>(all[0], all[1], *all.drop(2).toTypedArray())
                as GraphTraversal<Vertex, Any>
        }
    }

    // -------------------------------------------------------------------------
    // Result extraction
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun executeTraversalAndExtract(
        traversal: GraphTraversal<Vertex, Any>,
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): List<MatchResult.Matched> {
        val instanceNames = elements.allInstanceNames
        val variableLabels = elements.variables.map { VariableBinding.variableLabel(it.variable.name) }
        val allLabels = instanceNames + variableLabels

        return traversal.toList().map { rawResult ->
            val result: Map<String, Any> = when {
                allLabels.isEmpty() -> rawResult as Map<String, Any>
                allLabels.size == 1 -> mapOf(allLabels[0] to rawResult)
                else -> {
                    val rawMap = rawResult as Map<String, Any>
                    instanceNames.associateWith { rawMap[VariableBinding.stepLabel(it)]!! } +
                        variableLabels.associateWith { rawMap[it]!! }
                }
            }
            extractMatchResult(result, elements, context, engine, matchedInstanceNames)
        }
    }

    private fun extractMatchResult(
        result: Map<String, Any>,
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): MatchResult.Matched {
        val bindings = mutableMapOf<String, Any?>()
        for (varElement in elements.variables) {
            val varName = varElement.variable.name
            val value = result[VariableBinding.variableLabel(varName)]
            bindings[varName] = value
            context.variableScope.setBinding(varName, VariableBinding.ValueBinding(value))
        }

        val instanceMappings = mutableMapOf<String, VertexRef>()
        val matchedNodeIds = mutableSetOf<Any>()
        val createdNodeIds = mutableSetOf<Any>()
        val deletedNodeIds = mutableSetOf<Any>()
        val createNames = elements.createInstances.map { it.objectInstance.name }.toSet()
        val deleteNames = elements.deleteInstances.map { it.objectInstance.name }.toSet()

        for (name in elements.allInstanceNames) {
            classifyNode(
                name, result[name], createNames, deleteNames,
                instanceMappings, matchedNodeIds, createdNodeIds, deletedNodeIds, engine
            )
        }
        return MatchResult.Matched(bindings, instanceMappings, matchedNodeIds, createdNodeIds, deletedNodeIds)
    }

    private fun classifyNode(
        name: String,
        value: Any?,
        createNames: Set<String>,
        deleteNames: Set<String>,
        instanceMappings: MutableMap<String, VertexRef>,
        matchedNodeIds: MutableSet<Any>,
        createdNodeIds: MutableSet<Any>,
        deletedNodeIds: MutableSet<Any>,
        engine: TransformationEngine
    ) {
        if (value !is Vertex) return
        val rawId = value.id()
        instanceMappings[name] = engine.modelGraph.createVertexRef(rawId)
        when {
            name in createNames -> {
                createdNodeIds.add(rawId)
                engine.instanceNameRegistry.registerWithUniqueName(rawId, name)
            }
            name in deleteNames -> {
                matchedNodeIds.add(rawId)
                deletedNodeIds.add(rawId)
            }
            else -> matchedNodeIds.add(rawId)
        }
    }
}
