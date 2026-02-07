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
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
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
class MatchExecutor {
    
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
        
        /**
         * Maximum scope depth for registering matched variable bindings.
         * Covers global (0), ModelTransformation (1), and several levels of nesting (2+).
         * The typed AST scope level depends on the nesting depth of the match pattern.
         */
        private const val MAX_SCOPE_DEPTH = 5
    }
    
    /**
     * Executes a pattern match with modifications, returning the first match.
     */
    fun executeMatch(
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
    fun executeMatchAll(
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
    fun executeMatchWithLimit(
        pattern: TypedPattern,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        val elements = PatternCategories.from(pattern)
        
        if (!hasMatchableElements(elements)) {
            return handleEmptyPattern(elements, context, engine)
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
            return handleEmptyPattern(elements, context, engine)
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
        
        // Matched instances (including delete instances) are available for where clause expressions
        val matchedInstanceNamesForWhere = (elements.matchableInstances + elements.deleteInstances).map { it.objectInstance.name }
        traversal = addWhereClauseConstraints(traversal, elements.whereClauses, engine, matchedInstanceNamesForWhere)
        traversal = applyLimit(traversal, limit)
        
        val matchedInstanceNames = elements.matchableInstances.map { it.objectInstance.name }
        traversal = addCreateVertexSteps(traversal, elements.createInstances, engine, matchedInstanceNames)
        
        // Reconstruct the map with all instances (matched + created) BEFORE setting properties
        traversal = addSelectStep(traversal, elements.allInstanceNames)
        
        // Now set deferred properties that can reference any instance
        traversal = addPropertyUpdateSteps(traversal, elements, engine, matchedInstanceNames)
        traversal = addCreateEdgeSteps(traversal, elements.createLinks)
        traversal = addDeleteSteps(traversal, elements)
        
        return traversal
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
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in createInstances) {
            val className = extractSimpleClassName(instance.objectInstance.className)
            val name = instance.objectInstance.name
            
            result = result.addV(className).`as`(name) as GraphTraversal<Vertex, Map<String, Any>>
            result = addSimplePropertySteps(result, instance.objectInstance.properties, engine, matchedInstanceNames)
        }
        
        return result
    }
    
    /**
     * Adds simple (non-list) property steps to a vertex creation.
     * Only handles constant-value properties. Non-constant properties (like property access)
     * are deferred to addDeferredPropertySteps.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addSimplePropertySteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        properties: List<TypedPatternPropertyAssignment>,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (property in properties) {
            if (property.operator == "=") {
                val value = compilePropertyValue(property.value, engine, matchedInstanceNames)
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
     * Uses sideEffect pattern for list cardinality properties and for deferred
     * property assignments that reference matched variables.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addPropertyUpdateSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in elements.matchableInstances) {
            if (instance.objectInstance.modifier == "delete") continue
            result = addInstancePropertyUpdates(result, instance, engine, matchedInstanceNames)
        }
        
        for (instance in elements.createInstances) {
            result = addListPropertySteps(result, instance, engine, matchedInstanceNames)
            result = addDeferredPropertySteps(result, instance, engine, matchedInstanceNames)
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
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        
        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            
            val value = compilePropertyValue(property.value, engine, matchedInstanceNames)
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
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        
        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            
            val value = compilePropertyValue(property.value, engine, matchedInstanceNames)
            if (value is List<*>) {
                result = addListPropertySideEffect(result, name, property.propertyName, value)
            }
        }
        
        return result
    }
    
    /**
     * Adds deferred property assignments for created instances.
     * 
     * This handles properties whose values depend on matched variables (e.g., house.address + "test").
     * These properties couldn't be set during vertex creation and must be set using sideEffect.
     * 
     * The pattern used is:
     * ```kotlin
     * .sideEffect(
     *     __.select("targetInstance")
     *       .property("propName", <compiled value traversal>)
     * )
     * ```
     * 
     * The value traversal is compiled with matched variables in scope, allowing it to reference
     * matched instances using select() steps (e.g., select("house").values("address")).
     * 
     * This pattern works because:
     * - The sideEffect traversal has access to the match context (labeled steps)
     * - The value traversal can reference those labels via select()
     * - The property() step accepts a traversal as its value argument
     * - The value traversal is evaluated for each matched result
     */
    @Suppress("UNCHECKED_CAST")
    private fun addDeferredPropertySteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instance: TypedPatternObjectInstanceElement,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        
        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            
            // Compile with matched variable context - will throw on failure
            val compilationResult = compilePropertyValueWithContext(property.value, engine, matchedInstanceNames)
            
            // If the result is a traversal (not a constant), set the property using sideEffect
            if (compilationResult != null && !compilationResult.isConstant) {
                @Suppress("UNCHECKED_CAST")
                val valueTraversal = compilationResult.traversal as GraphTraversal<Any, Any>
                
                // Pattern: select the target vertex and set its property to the computed value
                // The valueTraversal can reference match context variables (e.g., select("house"))
                val propertyTraversal = AnonymousTraversal.select<Any, Any>(name)
                    .property(property.propertyName, valueTraversal)
                
                result = result.sideEffect(propertyTraversal) as GraphTraversal<Vertex, Map<String, Any>>
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
                val value = compilePropertyValue(property.value, engine, emptyList())
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
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal

        for ((counter, whereClause) in whereClauses.withIndex()) {
            result = applyTraversalWhereClause(result, whereClause, engine, counter, matchedInstanceNames)
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
        counter: Int,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val compiledTraversal = compileExpressionToTraversal(
            whereClause.whereClause.expression, engine, matchedInstanceNames
        )

        if (compiledTraversal == null) {
            throw IllegalStateException("Failed to compile where clause expression: ${whereClause.whereClause.expression}")
        }
        
        return applyTraversalAsFilter(traversal, compiledTraversal)
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
     *
     * @param matchedInstanceNames Names of instances matched in the pattern, used to
     *        populate variable scopes so identifiers can be resolved via select() calls.
     */
    private fun compileExpressionToTraversal(
        expression: TypedExpression,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<*, *>? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            return null
        }
        
        // Build variable scopes with TraversalBinding for each matched instance
        // These instances are available as step labels from the match() clause
        val bindings = matchedInstanceNames.associateWith { name ->
            VariableBinding.TraversalBinding(name)
        }
        val scope = VariableScope(bindings)
        val scopeMap = (0..MAX_SCOPE_DEPTH).associateWith { scope }
        
        val context = TraversalCompilationContext(
            types = engine.types,
            traversalSource = engine.traversalSource,
            variableScopes = scopeMap,
            typeRegistry = engine.typeRegistry
        )
        
        return engine.expressionCompilerRegistry.compile(expression, context).traversal
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
        val nodeClassification = classifyNodes(result, elements, engine)
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
        elements: PatternCategories,
        engine: TransformationEngine
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
                instanceMappings, matchedNodeIds, createdNodeIds, deletedNodeIds, engine
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
        deletedNodeIds: MutableSet<Any>,
        engine: TransformationEngine
    ) {
        if (value is Vertex) {
            val vertexId = value.id()
            instanceMappings[name] = vertexId
            when {
                createInstanceNames.contains(name) -> {
                    createdNodeIds.add(vertexId)
                    // Register the created node with its instance name
                    engine.instanceNameRegistry.registerWithUniqueName(vertexId, name)
                }
                deleteInstanceNames.contains(name) -> {
                    matchedNodeIds.add(vertexId)
                    deletedNodeIds.add(vertexId)
                }
                else -> matchedNodeIds.add(vertexId)
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
            bindings[variable.name] = compilePropertyValue(variable.value, engine, emptyList())
        }
        return bindings
    }
    
    /**
     * Handles patterns with no matchable instances.
     * This includes patterns with only create elements, only variables, etc.
     * 
     * @param elements The categorized pattern elements.
     * @param context The current execution context containing previously bound instances.
     * @param engine The transformation engine.
     */
    private fun handleEmptyPattern(
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): List<MatchResult.Matched> {
        if (!checkForbidConstraints(elements.forbidInstances, engine)) {
            return emptyList()
        }
        
        if (elements.createInstances.isEmpty() && elements.createLinks.isEmpty()) {
            val bindings = evaluateVariables(elements.variables, engine)
            return listOf(MatchResult.Matched(bindings = bindings))
        }
        
        return executeCreateOnlyPattern(elements, context, engine)
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
     * 
     * @param elements The categorized pattern elements.
     * @param context The current execution context containing previously bound instances.
     * @param engine The transformation engine.
     */
    private fun executeCreateOnlyPattern(
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): List<MatchResult.Matched> {
        val g = engine.traversalSource
        // Initialize instance mappings with instances from the context
        // This allows links to reference instances bound in previous matches
        val instanceMappings = context.getAllInstances().toMutableMap()
        val createdNodeIds = mutableSetOf<Any>()
        val createdEdgeIds = mutableSetOf<Any>()
        
        // Build list of instance names available for property value compilation
        // This includes both instances from context and names of instances being created
        val availableInstanceNames = instanceMappings.keys.toMutableList()
        elements.createInstances.forEach { availableInstanceNames.add(it.objectInstance.name) }
        
        for (instance in elements.createInstances) {
            val className = extractSimpleClassName(instance.objectInstance.className)
            val name = instance.objectInstance.name
            
            var traversal = g.addV(className)
            for (property in instance.objectInstance.properties) {
                if (property.operator == "=") {
                    val value = compilePropertyValueWithContext(
                        property.value, context, engine, availableInstanceNames
                    )
                    if (value != null && value !is List<*>) {
                        traversal = traversal.property(property.propertyName, value)
                    }
                }
            }
            
            val vertex = traversal.next()
            val vertexId = vertex.id()
            instanceMappings[name] = vertexId
            createdNodeIds.add(vertexId)
            
            // Register the vertex with its instance name in the registry
            engine.instanceNameRegistry.registerWithUniqueName(vertexId, name)
            
            handleListPropertiesForCreate(instance, vertexId, context, engine, availableInstanceNames)
        }
        
        createEdgesForPattern(elements.createLinks, instanceMappings, engine, createdEdgeIds)
        
        val bindings = evaluateVariables(elements.variables, engine)
        
        // Only include newly created instances in the result mappings
        // (don't duplicate instances already in the context)
        val newInstanceMappings = instanceMappings.filter { (name, _) -> 
            !context.hasInstance(name)
        }
        
        return listOf(MatchResult.Matched(
            bindings = bindings,
            instanceMappings = newInstanceMappings,
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
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        availableInstanceNames: List<String>
    ) {
        val g = engine.traversalSource
        for (property in instance.objectInstance.properties) {
            if (property.operator == "=") {
                val value = compilePropertyValueWithContext(
                    property.value, context, engine, availableInstanceNames
                )
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
     * Compiles a property value expression, returning its constant value if possible.
     * 
     * Attempts to compile the expression using the traversal compiler registry with a context
     * that includes matched variables. If successful and the result is a constant,
     * returns the constant value.
     * 
     * @param expression The expression to compile
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The constant value if compilation succeeds, null otherwise
     */
    private fun compilePropertyValue(
        expression: TypedExpression,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): Any? {
        return compilePropertyValueWithTraversal(expression, engine, matchedInstanceNames)
    }
    
    /**
     * Compiles a property value expression and returns the full compilation result.
     * 
     * This variant returns the TraversalCompilationResult, allowing the caller to
     * distinguish between constant and non-constant (traversal) results.
     * 
     * @param expression The expression to compile
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The compilation result, or null if compilation fails
     */
    private fun compilePropertyValueWithContext(
        expression: TypedExpression,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): TraversalCompilationResult<*, *>? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            throw IllegalStateException(
                "Expression compiler not found for expression of type '${expression::class.simpleName}'. " +
                "Ensure a compiler is registered for this expression kind."
            )
        }
        
        return try {
            val context = buildCompilationContext(engine, matchedInstanceNames)
            engine.expressionCompilerRegistry.compile(expression, context)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to compile expression '${expression::class.simpleName}': ${e.message}. " +
                "This may indicate a missing type registration for metamodel types.",
                e
            )
        }
    }
    
    /**
     * Compiles a property value expression with access to the transformation execution context.
     * 
     * This variant is used for create-only patterns where instances from previous matches
     * need to be accessible. The transformation context allows resolving instance IDs
     * for variables that were bound in earlier match statements.
     * 
     * @param expression The expression to compile
     * @param txContext The transformation execution context with bound instances
     * @param engine The transformation engine
     * @param availableInstanceNames Names of instances available for reference
     * @return The constant or computed value, or null if compilation fails
     */
    private fun compilePropertyValueWithContext(
        expression: TypedExpression,
        txContext: TransformationExecutionContext,
        engine: TransformationEngine,
        availableInstanceNames: List<String>
    ): Any? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            throw IllegalStateException(
                "Expression compiler not found for expression of type '${expression::class.simpleName}'. " +
                "Ensure a compiler is registered for this expression kind."
            )
        }
        
        return try {
            val context = buildCompilationContextWithTransformation(engine, txContext, availableInstanceNames)
            val result = engine.expressionCompilerRegistry.compile(expression, context)
            
            if (result.isConstant) {
                return result.constantValue
            }
            
            // For non-constant results, use inject().flatMap() to execute the traversal.
            // This is necessary because the traversal might be:
            // 1. An anonymous traversal (e.g., string concatenation of literals)
            // 2. A graph-bound traversal (e.g., g.V(id).values("prop"))
            // 
            // The inject().flatMap() pattern works for both cases:
            // - For anonymous traversals, inject() provides a starting point
            // - For graph-bound traversals, flatMap() properly executes the inner traversal
            @Suppress("UNCHECKED_CAST")
            val traversal = result.traversal as GraphTraversal<Any, Any>
            val injectedTraversal = engine.traversalSource.inject(null as Any?).flatMap(traversal)
            
            return if (injectedTraversal.hasNext()) {
                injectedTraversal.next()
            } else {
                null
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to compile expression '${expression::class.simpleName}': ${e.message}. " +
                "This may indicate a missing type registration for metamodel types.",
                e
            )
        }
    }
    
    /**
     * Attempts to compile property value using the traversal compiler registry.
     *
     * Returns the constant value if the compiled traversal is a constant.
     * If it's not a constant, executes the traversal to get the value.
     * 
     * @param expression The expression to compile
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The constant or computed value, or null if the traversal produces no result
     */
    @Suppress("UNCHECKED_CAST")
    private fun compilePropertyValueWithTraversal(
        expression: TypedExpression,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): Any? {
        val result = compilePropertyValueWithContext(expression, engine, matchedInstanceNames)
        if (result == null) return null
        
        if (result.isConstant) {
            return result.constantValue
        }
        
        val traversal = result.traversal as GraphTraversal<Any, Any>
        val injectedTraversal = engine.traversalSource.inject(null as Any?).flatMap(traversal)
        return if (injectedTraversal.hasNext()) {
            injectedTraversal.next()
        } else {
            null
        }
    }
    
    /**
     * Builds a compilation context with matched instance names available as TraversalBindings.
     * 
     * This allows property expressions to reference matched variables like `house.address`
     * using `.select("house").values("address")` in the compiled Gremlin traversal.
     * 
     * The bindings are registered at multiple scope levels to handle identifiers from
     * different nesting depths in the typed AST:
     * - Scope 0: Direct/test usage
     * - Scope 1: Top-level match patterns (ModelTransformation scope, MT_SCOPE_INDEX)
     * - Scope 2+: Nested match patterns (if-match, while-match, for-match, etc.)
     * 
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances to make available
     * @return A TraversalCompilationContext with matched variables in scope
     */
    private fun buildCompilationContext(
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): TraversalCompilationContext {
        val bindings = matchedInstanceNames.associateWith { name ->
            VariableBinding.TraversalBinding(name)
        }
        val scope = VariableScope(bindings)
        
        // Register bindings at multiple scope levels to handle identifiers from different
        // nesting depths. The typed AST assigns scope levels based on the scope hierarchy:
        // - Level 0: Global scope (built-ins)
        // - Level 1: ModelTransformation scope (top-level match patterns) 
        // - Level 2+: Nested scopes (if-match, while-match, for-match conditions)
        // We register at all common levels so identifiers resolve regardless of nesting depth.
        val scopeMap = (0..MAX_SCOPE_DEPTH).associateWith { scope }
        
        return TraversalCompilationContext(
            types = engine.types,
            traversalSource = engine.traversalSource,
            variableScopes = scopeMap,
            matchDefinedVariables = matchedInstanceNames.toSet(),
            typeRegistry = engine.typeRegistry
        )
    }
    
    /**
     * Builds a compilation context with access to the transformation execution context.
     * 
     * This is used for create-only patterns where instances from previous matches
     * need to be accessible. Unlike [buildCompilationContext], this version includes
     * the transformation context so that the IdentifierCompiler can resolve instances
     * via [TransformationExecutionContext.lookupInstance].
     * 
     * For instances from the transformation context (not in the current match pattern),
     * we use InstanceBinding which resolves the vertex ID directly rather than using
     * select() which only works within match() traversals.
     * 
     * @param engine The transformation engine
     * @param txContext The transformation execution context with bound instances
     * @param availableInstanceNames Names of instances available for reference
     * @return A TraversalCompilationContext with transformation context for resolution
     */
    private fun buildCompilationContextWithTransformation(
        engine: TransformationEngine,
        txContext: TransformationExecutionContext,
        availableInstanceNames: List<String>
    ): TraversalCompilationContext {
        // For instances from the transformation context, we resolve directly to their vertex IDs
        // using InstanceBinding rather than TraversalBinding (which uses select())
        // 
        // IMPORTANT: We track which instances don't have IDs yet (being created in this pattern)
        // Only THOSE should be in matchDefinedVariables. Instances that already have IDs should
        // use InstanceBinding and NOT be in matchDefinedVariables (since select() only works
        // inside a match() traversal, and create-only patterns don't have one).
        val instancesBeingCreated = mutableSetOf<String>()
        val bindings = availableInstanceNames.associateWith { name ->
            val instanceId = txContext.lookupInstance(name)
            if (instanceId != null) {
                VariableBinding.InstanceBinding(instanceId, name)
            } else {
                // Instance not yet created - this happens when referencing instances
                // being created in the same pattern. Track it for matchDefinedVariables.
                instancesBeingCreated.add(name)
                VariableBinding.TraversalBinding(name)
            }
        }
        val scope = VariableScope(bindings)
        val scopeMap = (0..MAX_SCOPE_DEPTH).associateWith { scope }
        
        return TraversalCompilationContext(
            types = engine.types,
            traversalSource = engine.traversalSource,
            variableScopes = scopeMap,
            // Only instances being created (without IDs) should use select() / TraversalBinding
            // Instances from context should use g.V(id) / InstanceBinding
            matchDefinedVariables = instancesBeingCreated,
            typeRegistry = engine.typeRegistry,
            transformationContext = txContext
        )
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
