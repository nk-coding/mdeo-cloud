package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternVariableElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternWhereClauseElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternPropertyAssignment
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationMode
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationEngine
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Unified executor that combines pattern matching and modifications in a single Gremlin query.
 *
 * This executor builds a single Gremlin traversal that:
 * 1. Matches the pattern using match() step
 * 2. Applies limit for single match or unlimited for foreach
 * 3. Creates new vertices using addV()
 * 4. Sets properties on vertices
 * 5. Creates edges using addE()
 * 6. Deletes edges and vertices using drop()
 *
 * The design ensures atomicity by executing all operations in one traversal,
 * avoiding separate match and modification phases.
 *
 * ## Traversal Expression Compilation
 * Where clauses and property values are compiled using the traversal compiler registry.
 * This ensures expressions are converted to pure Gremlin traversals without Java lambdas.
 */
class UnifiedMatchExecutor : MatchExecutor {
    
    companion object {
        /**
         * Default limit for single match operations. 
         */
        const val DEFAULT_LIMIT = 1L
        
        /**
         * Value indicating no limit (for foreach operations). 
         */
        const val UNLIMITED = -1L
        
        /**
         * Prefix for generated where clause variable names. 
         */
        private const val WHERE_VAR_PREFIX = "_where"
    }
    
    /**
     * Executes a pattern match with modifications, returning the first match.
     */
    override fun executeMatch(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): MatchResult {
        return executeMatchWithLimit(pattern, context, engine, DEFAULT_LIMIT).firstOrNull()
            ?: MatchResult.NoMatch("No match found")
    }
    
    /**
     * Executes a pattern match with modifications, returning all matches.
     */
    override fun executeMatchAll(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): List<MatchResult.Matched> {
        return executeMatchWithLimit(pattern, context, engine, UNLIMITED)
    }
    
    /**
     * Executes pattern match with a specified limit.
     *
     * @param pattern The pattern to match.
     * @param context The current execution context.
     * @param engine The transformation engine.
     * @param limit Maximum number of matches (-1 for unlimited).
     * @return List of match results with modifications applied.
     */
    override fun executeMatchWithLimit(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        val elements = PatternCategories.from(pattern)
        
        if (!hasMatchableElements(elements)) {
            return handleEmptyPattern(elements, engine)
        }
        
        return executeUnifiedTraversal(elements, context, engine, limit)
    }
    
    /**
     * Executes the unified traversal combining match and modifications.
     */
    private fun executeUnifiedTraversal(
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        val allMatchableInstances = elements.matchableInstances + elements.deleteInstances
        val allMatchableLinks = elements.matchableLinks + elements.deleteLinks
        
        val matchClauses = buildMatchClauses(allMatchableInstances, allMatchableLinks, context, engine)
        if (matchClauses.isEmpty()) {
            return handleEmptyPattern(elements, engine)
        }
        
        val traversal = buildUnifiedTraversal(elements, matchClauses, engine, limit)
        return executeTraversalAndExtract(traversal, elements, engine)
    }
    
    /**
     * Builds the unified Gremlin traversal.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildUnifiedTraversal(
        elements: PatternCategories,
        matchClauses: Array<GraphTraversal<Any, Any>>,
        engine: TransformationEngine,
        limit: Long
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val g = engine.traversalSource
        
        var traversal = buildMatchStep(g.V(), matchClauses)
        traversal = addForbidConstraints(traversal, elements)
        traversal = addWhereClauseConstraints(traversal, elements.whereClauses, engine)
        traversal = applyLimit(traversal, limit)
        traversal = addCreateVertexSteps(traversal, elements.createInstances, engine)
        traversal = addPropertyUpdateSteps(traversal, elements, engine)
        traversal = addCreateEdgeSteps(traversal, elements.createLinks)
        traversal = addDeleteSteps(traversal, elements)
        
        return addSelectStep(traversal, elements.allInstanceNames)
    }
    
    /**
     * Builds the match() step from the clauses.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildMatchStep(
        traversal: GraphTraversal<Vertex, Vertex>,
        matchClauses: Array<GraphTraversal<Any, Any>>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        return if (matchClauses.size == 1) {
            traversal.match<Any>(matchClauses[0]) as GraphTraversal<Vertex, Map<String, Any>>
        } else {
            traversal.match<Any>(matchClauses[0], *matchClauses.drop(1).toTypedArray()) 
                as GraphTraversal<Vertex, Map<String, Any>>
        }
    }
    
    /**
     * Applies limit to the traversal (unless unlimited).
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyLimit(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        limit: Long
    ): GraphTraversal<Vertex, Map<String, Any>> {
        return if (limit > 0) {
            traversal.limit(limit) as GraphTraversal<Vertex, Map<String, Any>>
        } else {
            traversal
        }
    }
    
    /**
     * Adds addV() steps for creating new vertices.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateVertexSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        createInstances: List<TypedPatternObjectInstanceElement>,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in createInstances) {
            val className = extractSimpleClassName(instance.objectInstance.className)
            val name = instance.objectInstance.name
            
            result = result.addV(className).`as`(name) as GraphTraversal<Vertex, Map<String, Any>>
            result = addSimplePropertySteps(result, instance.objectInstance.properties, engine)
        }
        
        return result
    }
    
    /**
     * Adds simple (non-list) property steps to a vertex creation.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addSimplePropertySteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        properties: List<TypedPatternPropertyAssignment>,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (property in properties) {
            if (property.operator == "=") {
                val value = compilePropertyValue(property.value, engine)
                if (value != null && value !is List<*>) {
                    result = result.property(property.propertyName, value) 
                        as GraphTraversal<Vertex, Map<String, Any>>
                }
            }
        }
        
        return result
    }
    
    /**
     * Adds property update steps for all instances (matched and created).
     * Uses sideEffect pattern for list cardinality properties.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addPropertyUpdateSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in elements.matchableInstances) {
            if (instance.objectInstance.modifier == "delete") continue
            result = addInstancePropertyUpdates(result, instance, engine)
        }
        
        for (instance in elements.createInstances) {
            result = addListPropertySteps(result, instance, engine)
        }
        
        return result
    }
    
    /**
     * Adds property updates for a matched instance.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addInstancePropertyUpdates(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instance: TypedPatternObjectInstanceElement,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        
        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            
            val value = compilePropertyValue(property.value, engine)
            if (value == null) continue
            
            result = if (value is List<*>) {
                addListPropertySideEffect(result, name, property.propertyName, value)
            } else {
                result.sideEffect(
                    AnonymousTraversal.select<Any, Any>(name)
                        .property(property.propertyName, value)
                ) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        
        return result
    }
    
    /**
     * Adds list property steps for a created instance using sideEffect.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addListPropertySteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instance: TypedPatternObjectInstanceElement,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        
        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            
            val value = compilePropertyValue(property.value, engine)
            if (value is List<*>) {
                result = addListPropertySideEffect(result, name, property.propertyName, value)
            }
        }
        
        return result
    }
    
    /**
     * Adds a sideEffect step for list cardinality properties.
     *
     * Uses the pattern from Prompt.md:
     * .sideEffect(
     *     __.unfold(listValues).as("v").select("target").property(list, "prop", select("v"))
     * )
     */
    @Suppress("UNCHECKED_CAST")
    private fun addListPropertySideEffect(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceName: String,
        propertyName: String,
        values: List<*>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val tempVarName = "${instanceName}_${propertyName}_val"
        
        val sideEffectTraversal = AnonymousTraversal.inject<Any>(*values.toTypedArray())
            .unfold<Any>()
            .`as`(tempVarName)
            .select<Any>(instanceName)
            .property(
                VertexProperty.Cardinality.list,
                propertyName,
                AnonymousTraversal.select<Any, Any>(tempVarName)
            )
        
        return traversal.sideEffect(sideEffectTraversal) as GraphTraversal<Vertex, Map<String, Any>>
    }
    
    /**
     * Adds addE() steps for creating new edges.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateEdgeSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        createLinks: List<TypedPatternLinkElement>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (link in createLinks) {
            val sourceName = link.link.source.objectName
            val targetName = link.link.target.objectName
            val sourceProperty = link.link.source.propertyName
            val targetProperty = link.link.target.propertyName
            val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty, link.link.isOutgoing)
            
            result = result.sideEffect(
                AnonymousTraversal.select<Any, Any>(sourceName)
                    .addE(edgeLabel)
                    .to(AnonymousTraversal.select<Any, Any>(targetName))
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        
        return result
    }
    
    /**
     * Adds drop() steps for deleting edges and vertices.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addDeleteSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (link in elements.deleteLinks) {
            result = addDeleteEdgeStep(result, link)
        }
        
        for (instance in elements.deleteInstances) {
            val name = instance.objectInstance.name
            result = result.sideEffect(
                AnonymousTraversal.select<Any, Any>(name).drop()
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        
        return result
    }
    
    /**
     * Adds a step to delete an edge between two instances.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addDeleteEdgeStep(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        link: TypedPatternLinkElement
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val sourceName = link.link.source.objectName
        val targetName = link.link.target.objectName
        val sourceProperty = link.link.source.propertyName
        val targetProperty = link.link.target.propertyName
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty, link.link.isOutgoing)
        
        return traversal.sideEffect(
            AnonymousTraversal.select<Any, Any>(sourceName)
                .outE(edgeLabel)
                .where(AnonymousTraversal.inV().`as`(targetName))
                .drop()
        ) as GraphTraversal<Vertex, Map<String, Any>>
    }
    
    /**
     * Builds match clauses for the Gremlin match() step.
     */
    private fun buildMatchClauses(
        instances: List<TypedPatternObjectInstanceElement>,
        links: List<TypedPatternLinkElement>,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): Array<GraphTraversal<Any, Any>> {
        val clauses = mutableListOf<GraphTraversal<Any, Any>>()
        
        for (instance in instances) {
            clauses.add(buildVertexMatchClause(instance, context, engine))
        }
        
        for (link in links) {
            buildEdgeMatchClause(link)?.let { clauses.add(it) }
        }
        
        @Suppress("UNCHECKED_CAST")
        return clauses.toTypedArray()
    }
    
    /**
     * Builds a vertex match clause for an object instance.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildVertexMatchClause(
        instance: TypedPatternObjectInstanceElement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): GraphTraversal<Any, Any> {
        val name = instance.objectInstance.name
        val className = extractSimpleClassName(instance.objectInstance.className)
        val preBoundId = context.lookupInstance(name)
        
        var clause = if (preBoundId != null) {
            AnonymousTraversal.`as`<Any>(name)
                .hasId(preBoundId)
                .hasLabel(className) as GraphTraversal<Any, Any>
        } else {
            AnonymousTraversal.`as`<Any>(name)
                .hasLabel(className) as GraphTraversal<Any, Any>
        }
        
        for (property in instance.objectInstance.properties) {
            if (property.operator == "==") {
                val value = compilePropertyValue(property.value, engine)
                if (value != null) {
                    clause = clause.has(property.propertyName, value) as GraphTraversal<Any, Any>
                }
            }
        }
        
        return clause
    }
    
    /**
     * Builds an edge match clause for a link.
     *
     * Computes the edge label from source and target property names using EdgeLabelUtils,
     * and uses link.isOutgoing to determine traversal direction (.out() vs .in()).
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildEdgeMatchClause(link: TypedPatternLinkElement): GraphTraversal<Any, Any>? {
        val sourceName = link.link.source.objectName
        val targetName = link.link.target.objectName
        val sourceProperty = link.link.source.propertyName
        val targetProperty = link.link.target.propertyName
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty, link.link.isOutgoing)
        
        return if (link.link.isOutgoing) {
            AnonymousTraversal.`as`<Any>(sourceName)
                .out(edgeLabel)
                .`as`(targetName) as GraphTraversal<Any, Any>
        } else {
            AnonymousTraversal.`as`<Any>(sourceName)
                .`in`(edgeLabel)
                .`as`(targetName) as GraphTraversal<Any, Any>
        }
    }
    
    /**
     * Adds forbid constraints as where(not(...)) clauses.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addForbidConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val matchableNames = elements.matchableInstances.map { it.objectInstance.name }.toSet()
        val forbidMap = elements.forbidInstances.associate { 
            it.objectInstance.name to extractSimpleClassName(it.objectInstance.className)
        }
        
        for (forbidLink in elements.forbidLinks) {
            result = addForbidLinkConstraint(result, forbidLink, matchableNames, forbidMap)
        }
        
        result = addDisconnectedForbidConstraints(result, elements)
        
        return result
    }
    
    /**
     * Adds a forbid constraint for a single link.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addForbidLinkConstraint(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        forbidLink: TypedPatternLinkElement,
        matchableNames: Set<String>,
        forbidMap: Map<String, String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val sourceName = forbidLink.link.source.objectName
        val targetName = forbidLink.link.target.objectName
        val sourceProperty = forbidLink.link.source.propertyName
        val targetProperty = forbidLink.link.target.propertyName
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty, forbidLink.link.isOutgoing)
        
        return when {
            matchableNames.contains(sourceName) && forbidMap.containsKey(targetName) -> {
                val forbidClassName = forbidMap[targetName]!!
                val notClause = AnonymousTraversal.`as`<Any>(sourceName)
                    .out(edgeLabel).hasLabel(forbidClassName) as GraphTraversal<Any, Any>
                traversal.where(AnonymousTraversal.not<Any>(notClause)) 
                    as GraphTraversal<Vertex, Map<String, Any>>
            }
            matchableNames.contains(targetName) && forbidMap.containsKey(sourceName) -> {
                val forbidClassName = forbidMap[sourceName]!!
                val notClause = AnonymousTraversal.`as`<Any>(targetName)
                    .`in`(edgeLabel).hasLabel(forbidClassName) as GraphTraversal<Any, Any>
                traversal.where(AnonymousTraversal.not<Any>(notClause))
                    as GraphTraversal<Vertex, Map<String, Any>>
            }
            matchableNames.contains(sourceName) && matchableNames.contains(targetName) -> {
                val notClause = AnonymousTraversal.`as`<Any>(sourceName)
                    .out(edgeLabel).`as`(targetName) as GraphTraversal<Any, Any>
                traversal.where(AnonymousTraversal.not<Any>(notClause))
                    as GraphTraversal<Vertex, Map<String, Any>>
            }
            else -> traversal
        }
    }
    
    /**
     * Adds constraints for forbid instances not connected via links.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addDisconnectedForbidConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val connectedForbidNames = mutableSetOf<String>()
        for (link in elements.forbidLinks) {
            connectedForbidNames.add(link.link.source.objectName)
            connectedForbidNames.add(link.link.target.objectName)
        }
        
        var result = traversal
        for (forbidInstance in elements.forbidInstances) {
            val name = forbidInstance.objectInstance.name
            if (!connectedForbidNames.contains(name)) {
                val className = extractSimpleClassName(forbidInstance.objectInstance.className)
                val notClause = AnonymousTraversal.V<Any>()
                    .hasLabel(className).count().`is`(P.eq(0L)) as GraphTraversal<Any, Any>
                result = result.where(notClause) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        
        return result
    }
    
    /**
     * Adds where clause constraints using traversal-based expression compilation.
     *
     * Where clauses are compiled to traversals and applied as where() filters.
     * For boolean expressions, the compiled traversal determines if the where
     * clause passes or fails.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addWhereClauseConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        whereClauses: List<TypedPatternWhereClauseElement>,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal

        for ((counter, whereClause) in whereClauses.withIndex()) {
            result = applyTraversalWhereClause(result, whereClause, engine, counter)
        }
        
        return result
    }
    
    /**
     * Applies a where clause using traversal-based expression compilation.
     *
     * Compiles the where clause expression to a traversal and applies it as a filter.
     * The traversal produces a boolean value which is compared against true.
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyTraversalWhereClause(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        whereClause: TypedPatternWhereClauseElement,
        engine: TransformationEngine,
        counter: Int
    ): GraphTraversal<Vertex, Map<String, Any>> {
        return try {
            val compiledTraversal = compileExpressionToTraversal(
                whereClause.whereClause.expression, engine
            )
            
            if (compiledTraversal != null) {
                applyTraversalAsFilter(traversal, compiledTraversal)
            } else {
                applyLegacyWhereClause(traversal, whereClause, engine)
            }
        } catch (e: Exception) {
            applyLegacyWhereClause(traversal, whereClause, engine)
        }
    }
    
    /**
     * Applies a compiled traversal as a where clause filter.
     *
     * The traversal is expected to produce a boolean value. The filter passes
     * if the result equals true.
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyTraversalAsFilter(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        booleanTraversal: GraphTraversal<*, *>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        return traversal.where(
            booleanTraversal.`is`(true)
        ) as GraphTraversal<Vertex, Map<String, Any>>
    }
    
    /**
     * Compiles an expression to a GraphTraversal using the traversal compiler registry.
     *
     * Returns null if compilation fails or is not supported.
     */
    private fun compileExpressionToTraversal(
        expression: TypedExpression,
        engine: TransformationEngine
    ): GraphTraversal<*, *>? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            return null
        }
        
        val context = TraversalCompilationContext(
            types = emptyList(),
            traversalSource = engine.traversalSource
        )
        
        return engine.expressionCompilerRegistry.compile(expression, context).traversal
    }
    
    /**
     * Applies a where clause using the legacy expression compiler (fallback).
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyLegacyWhereClause(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        whereClause: TypedPatternWhereClauseElement,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val valueContext = TraversalCompilationContext(
            types = emptyList(),
            traversalSource = engine.traversalSource
        )
        
        return try {
            val result = engine.expressionCompilerRegistry.compile(
                whereClause.whereClause.expression, valueContext
            )
            
            if (result.isConstant) {
                if (isFalsy(result.constantValue)) {
                    traversal.where(
                        AnonymousTraversal.not<Any>(AnonymousTraversal.identity<Any>())
                    ) as GraphTraversal<Vertex, Map<String, Any>>
                } else {
                    traversal
                }
            } else {
                // For non-constant results, use the traversal as a where filter
                traversal.where(result.traversal) as GraphTraversal<Vertex, Map<String, Any>>
            }
        } catch (e: Exception) {
            traversal
        }
    }
    
    /**
     * Checks if a value is "falsy".
     */
    private fun isFalsy(value: Any?): Boolean {
        return value == null || value == false || value == "" ||
            (value is Number && value.toDouble() == 0.0)
    }
    
    /**
     * Adds the select() step for extracting named bindings.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addSelectStep(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        return when (instanceNames.size) {
            0 -> traversal
            1 -> {
                val name = instanceNames[0]
                traversal.select<Any>(name)
                    .map { t -> mapOf(name to t.get()) } as GraphTraversal<Vertex, Map<String, Any>>
            }
            else -> traversal.select<Any>(
                instanceNames[0], instanceNames[1], *instanceNames.drop(2).toTypedArray()
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
    }
    
    /**
     * Executes the traversal and extracts match results.
     */
    private fun executeTraversalAndExtract(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories,
        engine: TransformationEngine
    ): List<MatchResult.Matched> {
        return try {
            traversal.toList().map { result ->
                extractMatchResult(result, elements, engine)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Extracts a MatchResult from a traversal result.
     */
    private fun extractMatchResult(
        result: Map<String, Any>,
        elements: PatternCategories,
        engine: TransformationEngine
    ): MatchResult.Matched {
        val nodeClassification = classifyNodes(result, elements)
        val edgeIds = collectEdgeIds(elements)
        val bindings = evaluateVariables(elements.variables, engine)
        
        return MatchResult.Matched(
            bindings = bindings,
            instanceMappings = nodeClassification.instanceMappings,
            matchedNodeIds = nodeClassification.matchedNodeIds,
            createdNodeIds = nodeClassification.createdNodeIds,
            deletedNodeIds = nodeClassification.deletedNodeIds,
            createdEdgeIds = edgeIds.createdEdgeIds,
            deletedEdgeIds = edgeIds.deletedEdgeIds
        )
    }
    
    /**
     * Classifies nodes from traversal result into matched, created, and deleted categories.
     */
    private fun classifyNodes(
        result: Map<String, Any>,
        elements: PatternCategories
    ): NodeClassification {
        val instanceMappings = mutableMapOf<String, Any>()
        val matchedNodeIds = mutableSetOf<Any>()
        val createdNodeIds = mutableSetOf<Any>()
        val deletedNodeIds = mutableSetOf<Any>()
        
        val createInstanceNames = elements.createInstances.map { it.objectInstance.name }.toSet()
        val deleteInstanceNames = elements.deleteInstances.map { it.objectInstance.name }.toSet()
        
        for (name in elements.allInstanceNames) {
            classifyNodeByName(
                name, result[name], createInstanceNames, deleteInstanceNames,
                instanceMappings, matchedNodeIds, createdNodeIds, deletedNodeIds
            )
        }
        
        return NodeClassification(instanceMappings, matchedNodeIds, createdNodeIds, deletedNodeIds)
    }
    
    /**
     * Classifies a single node into the appropriate category.
     */
    private fun classifyNodeByName(
        name: String,
        value: Any?,
        createInstanceNames: Set<String>,
        deleteInstanceNames: Set<String>,
        instanceMappings: MutableMap<String, Any>,
        matchedNodeIds: MutableSet<Any>,
        createdNodeIds: MutableSet<Any>,
        deletedNodeIds: MutableSet<Any>
    ) {
        if (value is Vertex) {
            instanceMappings[name] = value.id()
            when {
                createInstanceNames.contains(name) -> createdNodeIds.add(value.id())
                deleteInstanceNames.contains(name) -> {
                    matchedNodeIds.add(value.id())
                    deletedNodeIds.add(value.id())
                }
                else -> matchedNodeIds.add(value.id())
            }
        } else if (value != null) {
            instanceMappings[name] = value
        }
    }
    
    /**
     * Collects synthetic edge IDs for created and deleted edges.
     */
    private fun collectEdgeIds(elements: PatternCategories): EdgeIds {
        val createdEdgeIds = elements.createLinks.indices.map { "created_edge_$it" }.toSet()
        val deletedEdgeIds = elements.deleteLinks.indices.map { "deleted_edge_$it" }.toSet()
        return EdgeIds(createdEdgeIds, deletedEdgeIds)
    }
    
    private data class NodeClassification(
        val instanceMappings: Map<String, Any>,
        val matchedNodeIds: Set<Any>,
        val createdNodeIds: Set<Any>,
        val deletedNodeIds: Set<Any>
    )
    
    private data class EdgeIds(
        val createdEdgeIds: Set<Any>,
        val deletedEdgeIds: Set<Any>
    )
    
    /**
     * Evaluates pattern variables.
     */
    private fun evaluateVariables(
        variables: List<TypedPatternVariableElement>,
        engine: TransformationEngine
    ): Map<String, Any?> {
        val bindings = mutableMapOf<String, Any?>()
        for (variableElement in variables) {
            val variable = variableElement.variable
            bindings[variable.name] = compilePropertyValue(variable.value, engine)
        }
        return bindings
    }
    
    /**
     * Handles patterns with no matchable instances.
     * This includes patterns with only create elements, only variables, etc.
     */
    private fun handleEmptyPattern(
        elements: PatternCategories,
        engine: TransformationEngine
    ): List<MatchResult.Matched> {
        if (!checkForbidConstraints(elements.forbidInstances, engine)) {
            return emptyList()
        }
        
        if (elements.createInstances.isEmpty() && elements.createLinks.isEmpty()) {
            val bindings = evaluateVariables(elements.variables, engine)
            return listOf(MatchResult.Matched(bindings = bindings))
        }
        
        return executeCreateOnlyPattern(elements, engine)
    }
    
    /**
     * Checks that forbid constraints are satisfied (no forbidden instances exist).
     */
    private fun checkForbidConstraints(
        forbidInstances: List<TypedPatternObjectInstanceElement>,
        engine: TransformationEngine
    ): Boolean {
        if (forbidInstances.isEmpty()) return true
        
        val g = engine.traversalSource
        for (forbidInstance in forbidInstances) {
            val className = extractSimpleClassName(forbidInstance.objectInstance.className)
            val count = g.V().hasLabel(className).count().next()
            if (count > 0) return false
        }
        return true
    }
    
    /**
     * Executes a pattern that has only create elements (no matches needed).
     */
    private fun executeCreateOnlyPattern(
        elements: PatternCategories,
        engine: TransformationEngine
    ): List<MatchResult.Matched> {
        val g = engine.traversalSource
        val instanceMappings = mutableMapOf<String, Any>()
        val createdNodeIds = mutableSetOf<Any>()
        val createdEdgeIds = mutableSetOf<Any>()
        
        for (instance in elements.createInstances) {
            val className = extractSimpleClassName(instance.objectInstance.className)
            val name = instance.objectInstance.name
            
            var traversal = g.addV(className)
            for (property in instance.objectInstance.properties) {
                if (property.operator == "=") {
                    val value = compilePropertyValue(property.value, engine)
                    if (value != null && value !is List<*>) {
                        traversal = traversal.property(property.propertyName, value)
                    }
                }
            }
            
            val vertex = traversal.next()
            instanceMappings[name] = vertex.id()
            createdNodeIds.add(vertex.id())
            
            handleListPropertiesForCreate(instance, vertex.id(), engine)
        }
        
        createEdgesForPattern(elements.createLinks, instanceMappings, engine, createdEdgeIds)
        
        val bindings = evaluateVariables(elements.variables, engine)
        
        return listOf(MatchResult.Matched(
            bindings = bindings,
            instanceMappings = instanceMappings,
            createdNodeIds = createdNodeIds,
            createdEdgeIds = createdEdgeIds
        ))
    }
    
    /**
     * Handles list properties for a created vertex.
     */
    private fun handleListPropertiesForCreate(
        instance: TypedPatternObjectInstanceElement,
        vertexId: Any,
        engine: TransformationEngine
    ) {
        val g = engine.traversalSource
        for (property in instance.objectInstance.properties) {
            if (property.operator == "=") {
                val value = compilePropertyValue(property.value, engine)
                if (value is List<*>) {
                    for (item in value) {
                        if (item != null) {
                            g.V(vertexId)
                                .property(VertexProperty.Cardinality.list, property.propertyName, item)
                                .iterate()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Creates edges for create links and tracks their IDs.
     */
    private fun createEdgesForPattern(
        createLinks: List<TypedPatternLinkElement>,
        instanceMappings: Map<String, Any>,
        engine: TransformationEngine,
        createdEdgeIds: MutableSet<Any>
    ) {
        val g = engine.traversalSource
        for (link in createLinks) {
            val sourceName = link.link.source.objectName
            val targetName = link.link.target.objectName
            val sourceProperty = link.link.source.propertyName
            val targetProperty = link.link.target.propertyName
            val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty, link.link.isOutgoing)
            
            val sourceId = instanceMappings[sourceName]
            val targetId = instanceMappings[targetName]
            
            if (sourceId != null && targetId != null) {
                val edge = g.V(sourceId)
                    .addE(edgeLabel)
                    .to(AnonymousTraversal.V<Any>(targetId))
                    .next()
                createdEdgeIds.add(edge.id())
            }
        }
    }
    
    /**
     * Compiles a property value expression using traversal compilers.
     *
     * First attempts to compile using the traversal compiler registry.
     * If successful and the result is a constant, returns the constant value.
     * Falls back to the legacy expression compiler if traversal compilation fails.
     */
    private fun compilePropertyValue(
        expression: TypedExpression,
        engine: TransformationEngine
    ): Any? {
        return compilePropertyValueWithTraversal(expression, engine)
            ?: compilePropertyValueLegacy(expression, engine)
    }
    
    /**
     * Attempts to compile property value using the traversal compiler registry.
     *
     * Returns the constant value if the compiled traversal is a constant.
     * Returns null if compilation fails or the result is not a constant.
     */
    private fun compilePropertyValueWithTraversal(
        expression: TypedExpression,
        engine: TransformationEngine
    ): Any? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            return null
        }
        
        return try {
            val context = TraversalCompilationContext(
                types = emptyList(),
                traversalSource = engine.traversalSource
            )
            val result = engine.expressionCompilerRegistry.compile(expression, context)
            if (result.isConstant) result.constantValue else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Compiles property value using the expression compiler registry.
     */
    private fun compilePropertyValueLegacy(
        expression: TypedExpression,
        engine: TransformationEngine
    ): Any? {
        return try {
            val context = TraversalCompilationContext(types = emptyList(), traversalSource = engine.traversalSource)
            val result = engine.expressionCompilerRegistry.compile(expression, context)
            if (result.isConstant) {
                result.constantValue
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts simple class name from fully qualified name.
     */
    private fun extractSimpleClassName(fullClassName: String): String {
        return fullClassName.substringAfterLast(".")
    }
    
    /**
     * Checks if the pattern has any elements that need to be matched.
     */
    private fun hasMatchableElements(elements: PatternCategories): Boolean {
        return elements.matchableInstances.isNotEmpty() || 
               elements.deleteInstances.isNotEmpty() || 
               elements.deleteLinks.isNotEmpty()
    }
}

/**
 * Internal data class for holding categorized pattern elements for the unified executor.
 */
private data class PatternCategories(
    val matchableInstances: List<TypedPatternObjectInstanceElement>,
    val matchableLinks: List<TypedPatternLinkElement>,
    val createInstances: List<TypedPatternObjectInstanceElement>,
    val deleteInstances: List<TypedPatternObjectInstanceElement>,
    val createLinks: List<TypedPatternLinkElement>,
    val deleteLinks: List<TypedPatternLinkElement>,
    val forbidInstances: List<TypedPatternObjectInstanceElement>,
    val forbidLinks: List<TypedPatternLinkElement>,
    val variables: List<TypedPatternVariableElement>,
    val whereClauses: List<TypedPatternWhereClauseElement>
) {
    /**
     * All instance names for final select() step - includes matched, created, and deleted. 
     */
    val allInstanceNames: List<String>
        get() = matchableInstances.map { it.objectInstance.name } +
                createInstances.map { it.objectInstance.name } +
                deleteInstances.map { it.objectInstance.name }
    
    companion object {
        /**
         * Creates PatternCategories from a pattern.
         */
        fun from(pattern: TypedPattern): PatternCategories {
            val matchableInstances = mutableListOf<TypedPatternObjectInstanceElement>()
            val matchableLinks = mutableListOf<TypedPatternLinkElement>()
            val createInstances = mutableListOf<TypedPatternObjectInstanceElement>()
            val deleteInstances = mutableListOf<TypedPatternObjectInstanceElement>()
            val createLinks = mutableListOf<TypedPatternLinkElement>()
            val deleteLinks = mutableListOf<TypedPatternLinkElement>()
            val forbidInstances = mutableListOf<TypedPatternObjectInstanceElement>()
            val forbidLinks = mutableListOf<TypedPatternLinkElement>()
            val variables = mutableListOf<TypedPatternVariableElement>()
            val whereClauses = mutableListOf<TypedPatternWhereClauseElement>()
            
            for (element in pattern.elements) {
                when (element) {
                    is TypedPatternObjectInstanceElement -> categorizeInstance(
                        element, matchableInstances, createInstances, deleteInstances, forbidInstances
                    )
                    is TypedPatternLinkElement -> categorizeLink(
                        element, matchableLinks, createLinks, deleteLinks, forbidLinks
                    )
                    is TypedPatternVariableElement -> variables.add(element)
                    is TypedPatternWhereClauseElement -> whereClauses.add(element)
                }
            }
            
            return PatternCategories(
                matchableInstances, matchableLinks, createInstances, deleteInstances,
                createLinks, deleteLinks, forbidInstances, forbidLinks, variables, whereClauses
            )
        }
        
        private fun categorizeInstance(
            element: TypedPatternObjectInstanceElement,
            matchable: MutableList<TypedPatternObjectInstanceElement>,
            create: MutableList<TypedPatternObjectInstanceElement>,
            delete: MutableList<TypedPatternObjectInstanceElement>,
            forbid: MutableList<TypedPatternObjectInstanceElement>
        ) {
            when (element.objectInstance.modifier) {
                "create" -> create.add(element)
                "delete" -> delete.add(element)
                "forbid" -> forbid.add(element)
                else -> matchable.add(element)
            }
        }
        
        private fun categorizeLink(
            element: TypedPatternLinkElement,
            matchable: MutableList<TypedPatternLinkElement>,
            create: MutableList<TypedPatternLinkElement>,
            delete: MutableList<TypedPatternLinkElement>,
            forbid: MutableList<TypedPatternLinkElement>
        ) {
            when (element.link.modifier) {
                "create" -> create.add(element)
                "delete" -> delete.add(element)
                "forbid" -> forbid.add(element)
                else -> matchable.add(element)
            }
        }
    }
}
