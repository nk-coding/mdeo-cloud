package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternVariableElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternWhereClauseElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternPropertyAssignment
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
import com.mdeo.modeltransformation.compiler.VariableBinding
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
    
    private var idCounter = 0
    
    private fun getUniqueId(): String {
        return "id_${idCounter++}"
    }
    
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
         * Anchor label for match clauses to ensure all clauses are connected. 
         */
        private const val ANCHOR_LABEL = "_"
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
        
        val scope = context.variableScope
        for (name in elements.allInstanceNames) {
            if (scope.getVariable(name) == null) {
                scope.setBinding(name, VariableBinding.InstanceBinding(vertexId = null))
            }
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
        val analyzer = MatchAnalyzer(context.variableScope)
        for (element in elements.variables) {
            analyzer.analyzeVariable(element)
        }
        for (element in elements.whereClauses) {
            analyzer.analyzeWhereClause(element)
        }
        for (instance in elements.matchableInstances + elements.createInstances + elements.deleteInstances) {
            analyzer.analyzeObjectInstance(instance)
        }
        for (link in elements.matchableLinks + elements.createLinks + elements.deleteLinks + elements.forbidLinks) {
            analyzer.analyzeLink(link)
        }
        
        val referencedInstances = analyzer.getReferencedInstances()
        
        val allMatchableInstances = elements.matchableInstances + elements.deleteInstances
        val allMatchableLinks = elements.matchableLinks + elements.deleteLinks
        
        val matchClauses = buildMatchClauses(allMatchableInstances, allMatchableLinks, referencedInstances,  context, engine)
        
        val traversal = buildUnifiedTraversal(elements, matchClauses, context, engine, limit)
        val matchedInstanceNames = (elements.matchableInstances + elements.deleteInstances).map { it.objectInstance.name }
        return executeTraversalAndExtract(traversal, elements, context, engine, matchedInstanceNames)
    }
    
    /**
     * Builds the unified Gremlin traversal.
     * 
     * Uses g.inject(null).as("_") as the anchor point for the match() step.
     * This ensures all match clauses can connect through the anchor, solving
     * the "unsolvable match pattern" issue for disconnected patterns.
     * 
     * The final select step returns Any (either a single element or a map),
     * so this method returns GraphTraversal<Vertex, Any>.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildUnifiedTraversal(
        elements: PatternCategories,
        matchClauses: Array<GraphTraversal<Any, Any>>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): GraphTraversal<Vertex, Any> {
        val g = engine.traversalSource
        
        var traversal: GraphTraversal<Vertex, Map<String, Any>> = if (matchClauses.isEmpty()) {
            var traversal = g.inject(emptyMap<String, Any>()) as GraphTraversal<Vertex, Map<String, Any>>
            traversal = addForbidConstraints(traversal, elements)
            traversal = addWhereClauseConstraints(traversal, elements.whereClauses, context, engine)
            traversal
        } else {
            var traversal = buildMatchStep(
                g.inject(emptyMap<String, Any>()).`as`(ANCHOR_LABEL) as GraphTraversal<Vertex, Vertex>,
                matchClauses
            )
            traversal = addForbidConstraints(traversal, elements)
            traversal = addWhereClauseConstraints(traversal, elements.whereClauses, context, engine)
            traversal
        }
        
        traversal = applyLimit(traversal, limit)
        
        val matchedInstanceNames = elements.matchableInstances.map { it.objectInstance.name }
        traversal = addCreateVertexSteps(traversal, elements.createInstances, context, engine, matchedInstanceNames)
        
        traversal = addPropertyUpdateSteps(traversal, elements, context, engine, matchedInstanceNames)
        val currentPatternInstanceNames = elements.allInstanceNames.toSet()
        traversal = addCreateEdgeSteps(traversal, elements.createLinks, context, engine, currentPatternInstanceNames)
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
     * Adds addV() steps for creating new vertices with all their properties.
     * 
     * Properties are handled in two phases:
     * 1. Inline: Constant non-collection properties set directly with .property()
     * 2. Inline: Collection properties set with .property(Cardinality.list/set, ...)
     * 3. Deferred: Non-constant properties set via addDeferredProperties()
     * 
     * Uses step labels for consistency with matched vertices.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateVertexSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        createInstances: List<TypedPatternObjectInstanceElement>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in createInstances) {
            val className = instance.objectInstance.className
            val name = instance.objectInstance.name
            
            result = result.addV(className).`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Vertex, Map<String, Any>>
            result = addCreateVertexProperties(result, name, instance.objectInstance.properties, context, engine, matchedInstanceNames)
        }
        
        return result
    }
    
    /**
     * Adds all properties to a newly created vertex.
     * 
     * Separates properties into:
     * - Regular constant values: set inline with .property()
     * - List/collection values: set via sideEffect with Cardinality.list
     * - Non-constant expressions: set inline with .property(traversal)
     * 
     * @param traversal The traversal with the newly created vertex as current element
     * @param instanceName The name of the instance being created
     * @param properties The properties to set
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The extended traversal
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateVertexProperties(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceName: String,
        properties: List<TypedPatternPropertyAssignment>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal

        val (listProperties, simpleProperties) = properties.filter { it.operator == "=" }.partition { property ->
            val expressionType = resolveExpressionType(property.value, engine)!!
            isCollectionType(expressionType, engine)
        }

        for (property in simpleProperties) {
            val compilationResult = compilePropertyExpression(property.value, context, engine, matchedInstanceNames)!!
            if (compilationResult is GremlinCompilationResult.ValueResult) {
                val value = compilationResult.value
                if (value != null) {
                    result = result.property(property.propertyName, value) 
                        as GraphTraversal<Vertex, Map<String, Any>>
                }
            } else {
                result = result.property(property.propertyName, compilationResult.traversal) 
                    as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        
        for (property in listProperties) {
            val listResult = compilePropertyExpression(property.value, context, engine, matchedInstanceNames)!!
            val listTraversal = listResult.traversal as GraphTraversal<Any, Any>
            val valueLabel = getUniqueId()
            println("why does the fucking list traversal not work")
            
            result = result.sideEffect(
                listTraversal
                    .`as`(valueLabel)
                    .select<Any>(VariableBinding.stepLabel(instanceName))
                    .property(
                        VertexProperty.Cardinality.list,
                        property.propertyName,
                        AnonymousTraversal.select<Any, Any>(valueLabel)
                    )
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        
        return result
    }
    
    /**
     * Resolves the type of an expression from the types array in the engine.
     * 
     * @param expression The expression to get the type for
     * @param engine The transformation engine containing type information
     * @return The ValueType of the expression, or null if not available
     */
    private fun resolveExpressionType(
        expression: TypedExpression,
        engine: TransformationEngine
    ): com.mdeo.expression.ast.types.ValueType? {
        val typeIndex = expression.evalType
        if (typeIndex < 0 || typeIndex >= engine.types.size) return null
        val type = engine.types[typeIndex]
        return type as? com.mdeo.expression.ast.types.ValueType
    }
    
    /**
     * Checks if a type is a collection type (list, set, bag, etc.).
     * 
     * Uses the type registry to check the type's cardinality property.
     * Collection types have LIST or SET cardinality.
     * 
     * @param type The type to check (nullable)
     * @param engine The transformation engine with type registry
     * @return true if the type is a collection type
     */
    private fun isCollectionType(
        type: com.mdeo.expression.ast.types.ValueType?,
        engine: TransformationEngine
    ): Boolean {
        if (type == null) return false
        
        if (type !is com.mdeo.expression.ast.types.ClassTypeRef) {
            return false
        }
                
        val typeDefinition = engine.typeRegistry.getType(type.type) ?: return false
        val registryCardinality = typeDefinition.cardinality
        return registryCardinality == VertexProperty.Cardinality.list || registryCardinality == VertexProperty.Cardinality.set
    }
    
    /**
     * Adds property update steps for all instances (matched and created).
     * Uses sideEffect pattern for all updates to existing/matched vertices.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addPropertyUpdateSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in elements.matchableInstances) {
            if (instance.objectInstance.modifier == "delete") continue
            result = addMatchedInstanceProperties(result, instance, context, engine, matchedInstanceNames)
        }
        
        return result
    }
    
    /**
     * Adds property updates for a matched/existing instance via sideEffect.
     * 
     * All property updates use sideEffect with select() to access the vertex by its label.
     * For collection properties, existing values are cleared before adding new ones.
     * 
     * @param traversal The main traversal
     * @param instance The matched instance to update
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The extended traversal with property update steps
     */
    @Suppress("UNCHECKED_CAST")
    private fun addMatchedInstanceProperties(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instance: TypedPatternObjectInstanceElement,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        
        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            
            val expressionType = resolveExpressionType(property.value, engine)
            val isCollection = isCollectionType(expressionType, engine)
            
            if (isCollection) {
                val listResult = compilePropertyExpression(property.value, context, engine, matchedInstanceNames)
                if (listResult != null) {
                    result = setListPropertyViaSideEffect(result, name, property.propertyName, listResult, engine)
                }
            } else {
                val value = getPropertyValue(property.value, context, engine, matchedInstanceNames)
                result = result.sideEffect(
                    AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(name))
                        .property(property.propertyName, value)
                ) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        
        return result
    }

    /**
     * Sets a list property values using Cardinality.list via sideEffect.
     * 
     * Clears existing property values, then evaluates the list traversal and adds each
     * emitted value as a separate property value with Cardinality.list.
     * 
     * The list traversal emits multiple values (e.g., union(constant(10), constant(20)) emits 10, then 20).
     * Each emitted value is added as a separate property value.
     * 
     * IMPORTANT: Clears existing list property values before adding new ones.
     * This ensures list updates replace rather than append to existing values.
     * 
     * @param traversal The main traversal to extend
     * @param instanceName The name of the instance to set the property on
     * @param propertyName The name of the property to set
     * @param listResult The GremlinCompilationResult that emits list element values
     * @param engine The transformation engine with the traversal source
     * @return The extended traversal with sideEffect steps for list property management
     */
    @Suppress("UNCHECKED_CAST")
    private fun setListPropertyViaSideEffect(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceName: String,
        propertyName: String,
        listResult: GremlinCompilationResult,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        result = result.sideEffect(
            AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(instanceName))
                .properties<Any>(propertyName)
                .drop()
        ) as GraphTraversal<Vertex, Map<String, Any>>
        
        val listTraversal = listResult.traversal as GraphTraversal<Any, Any>
        val valueLabel = getUniqueId()
        
        result = result.sideEffect(
            listTraversal
                .`as`(valueLabel)
                .select<Any>(VariableBinding.stepLabel(instanceName))
                .property(
                    VertexProperty.Cardinality.list,
                    propertyName,
                    AnonymousTraversal.select<Any, Any>(valueLabel)
                )
        ) as GraphTraversal<Vertex, Map<String, Any>>
        
        return result
    }
    
    /**
     * Adds addE() steps for creating edges.
     * 
     * Handles two cases:
     * - Instances in current pattern: use select() to reference step labels
     * - Instances from previous matches: use V(vertexId) from context bindings
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateEdgeSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        createLinks: List<TypedPatternLinkElement>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        currentPatternInstanceNames: Set<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (link in createLinks) {
            val sourceName = link.link.source.objectName
            val targetName = link.link.target.objectName
            val sourceProperty = link.link.source.propertyName
            val targetProperty = link.link.target.propertyName
            val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty)

            val sourceTraversal =  AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(sourceName))
            val targetTraversal =  AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(targetName))
            
            result = result.sideEffect(
                sourceTraversal.addE(edgeLabel).to(targetTraversal)
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        
        return result
    }
    
    /**
     * Adds drop() steps for deleting edges and vertices.
     * 
     * Uses step labels for selecting instances.
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
                AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(name)).drop()
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        
        return result
    }
    
    /**
     * Adds a step to delete an edge between two instances.
     * 
     * Uses step labels for selecting instances.
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
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty)
        
        return traversal.sideEffect(
            AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(sourceName))
                .outE(edgeLabel)
                .where(AnonymousTraversal.inV().`as`(VariableBinding.stepLabel(targetName)))
                .drop()
        ) as GraphTraversal<Vertex, Map<String, Any>>
    }
    
    /**
     * Builds match clauses for the Gremlin match() step.
     */
    private fun buildMatchClauses(
        instances: List<TypedPatternObjectInstanceElement>,
        links: List<TypedPatternLinkElement>,
        referencedInstances: Set<String>,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): Array<GraphTraversal<Any, Any>> {
        val clauses = mutableListOf<GraphTraversal<Any, Any>>()
        
        for (instance in instances) {
            val binding = context.variableScope.getVariable(instance.objectInstance.name)
            val vertexId = (binding as? VariableBinding.InstanceBinding)?.vertexId
            if (vertexId != null) {
                clauses.add(buildIdVertexMatchClause(vertexId, instance.objectInstance.name))
            } else {
                clauses.add(buildVertexMatchClauses(instance, context, engine))
            }
        }
        val instanceNames = instances.map { it.objectInstance.name }.toSet()
        for (referencedInstance in referencedInstances) {
            if (referencedInstance in instanceNames) {
                continue
            }
            val binding = context.variableScope.getVariable(referencedInstance)
            val vertexId = (binding as? VariableBinding.InstanceBinding)?.vertexId
            if (vertexId != null) {
                clauses.add(buildIdVertexMatchClause(vertexId, referencedInstance))
            }
        }
        
        for (link in links) {
            clauses.add(buildEdgeMatchClause(link))
        }
        
        @Suppress("UNCHECKED_CAST")
        return clauses.toTypedArray()
    }
    
    /**
     * Builds a vertex match clause for an object instance using the anchor pattern.
     * If the instance is pre-bound (has a vertexId in context), uses V(id) instead of V().hasLabel().
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildVertexMatchClauses(
        instance: TypedPatternObjectInstanceElement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): GraphTraversal<Any, Any> {
        val name = instance.objectInstance.name
        val className = instance.objectInstance.className
                
        var clause = AnonymousTraversal.`as`<Any>(ANCHOR_LABEL)
            .V()
            .hasLabel(className)
            .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>
        
        for (property in instance.objectInstance.properties) {
            if (property.operator == "==") {
                val isListLiteral = property.value is com.mdeo.expression.ast.expressions.TypedListLiteralExpression
                if (isListLiteral) {
                    throw IllegalArgumentException("List properties cannot be used in match clause comparisons (== operator) - not supported in Gremlin match patterns")
                }
                
                val value = getPropertyValue(property.value, context, engine, emptyList())
                clause = clause.has(property.propertyName, value) as GraphTraversal<Any, Any>
            }
        }
        
        return clause
    }
    
    /**
     * Builds a vertex match clause for an object instance using V(id) if pre-bound, otherwise the anchor pattern.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildIdVertexMatchClause(
        vertexId: Any,
        name: String
    ): GraphTraversal<Any, Any> {
        return AnonymousTraversal.`as`<Any>(ANCHOR_LABEL)
            .V(vertexId)
            .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>
    }
    
    /**
     * Builds an edge match clause for a link.
     *
     * Computes the edge label from source and target property names using EdgeLabelUtils.
     * 
     * Uses step labels for source and target.
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildEdgeMatchClause(link: TypedPatternLinkElement): GraphTraversal<Any, Any> {
        val sourceName = link.link.source.objectName
        val targetName = link.link.target.objectName
        val sourceProperty = link.link.source.propertyName
        val targetProperty = link.link.target.propertyName
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty)
        
        return AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(sourceName))
            .out(edgeLabel)
            .`as`(VariableBinding.stepLabel(targetName)) as GraphTraversal<Any, Any>
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
            it.objectInstance.name to it.objectInstance.className
        }
        
        for (forbidLink in elements.forbidLinks) {
            result = addForbidLinkConstraint(result, forbidLink, matchableNames, forbidMap)
        }
        
        result = addDisconnectedForbidConstraints(result, elements)
        
        return result
    }
    
    /**
     * Adds a forbid constraint for a single link.
     * 
     * Uses step labels for referencing matched instances.
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
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(sourceProperty, targetProperty)
        
        return when {
            matchableNames.contains(sourceName) && forbidMap.containsKey(targetName) -> {
                val forbidClassName = forbidMap[targetName]!!
                val notClause = AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(sourceName))
                    .out(edgeLabel).hasLabel(forbidClassName) as GraphTraversal<Any, Any>
                traversal.where(AnonymousTraversal.not<Any>(notClause)) 
                    as GraphTraversal<Vertex, Map<String, Any>>
            }
            matchableNames.contains(targetName) && forbidMap.containsKey(sourceName) -> {
                val forbidClassName = forbidMap[sourceName]!!
                val notClause = AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(targetName))
                    .`in`(edgeLabel).hasLabel(forbidClassName) as GraphTraversal<Any, Any>
                traversal.where(AnonymousTraversal.not<Any>(notClause))
                    as GraphTraversal<Vertex, Map<String, Any>>
            }
            matchableNames.contains(sourceName) && matchableNames.contains(targetName) -> {
                val notClause = AnonymousTraversal.`as`<Any>(VariableBinding.stepLabel(sourceName))
                    .out(edgeLabel).`as`(VariableBinding.stepLabel(targetName)) as GraphTraversal<Any, Any>
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
                val className = forbidInstance.objectInstance.className
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
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal

        for ((counter, whereClause) in whereClauses.withIndex()) {
            result = applyTraversalWhereClause(result, whereClause, context, engine, counter)
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
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        counter: Int
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val compiledTraversal = compileExpressionToTraversal(
            whereClause.whereClause.expression, engine, context
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
     * Builds a proper scope chain that includes all levels from 0 up to the match scope level.
     * This ensures expressions with specific scope indices can resolve correctly.
     *
     * @param context The transformation execution context with properly configured scope.
     */
    private fun compileExpressionToTraversal(
        expression: TypedExpression,
        engine: TransformationEngine,
        context: TransformationExecutionContext,
    ): GraphTraversal<*, *>? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            return null
        }
        
        val compilationContext = CompilationContext(
            types = engine.types,
            traversalSource = engine.traversalSource,
            currentScope = context.variableScope,
            typeRegistry = engine.typeRegistry
        )
        
        return engine.expressionCompilerRegistry.compile(expression, compilationContext).traversal
    }
    

    
    /**
     * Adds the select() step for extracting named bindings.
     * 
     * Uses step labels for selecting matched instances.
     * Returns raw step labels - conversion to original names happens after execution.
     * 
     * Note: When selecting a single element, returns that element directly.
     * When selecting multiple, returns a map with step labels as keys.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addSelectStep(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceNames: List<String>
    ): GraphTraversal<Vertex, Any> {
        return when (instanceNames.size) {
            0 -> traversal as GraphTraversal<Vertex, Any>
            1 -> {
                val name = instanceNames[0]
                traversal.select<Any>(VariableBinding.stepLabel(name)) as GraphTraversal<Vertex, Any>
            }
            else -> {
                val stepLabels = instanceNames.map { VariableBinding.stepLabel(it) }
                traversal.select<Any>(
                    stepLabels[0], stepLabels[1], *stepLabels.drop(2).toTypedArray()
                ) as GraphTraversal<Vertex, Any>
            }
        }
    }
    
    /**
     * Executes the traversal and extracts match results.
     * 
     * Converts step labels back to original instance names.
     * Handles the fact that select() with one element returns the element directly,
     * while select() with multiple elements returns a map.
     */
    @Suppress("UNCHECKED_CAST")
    private fun executeTraversalAndExtract(
        traversal: GraphTraversal<Vertex, Any>,
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): List<MatchResult.Matched> {
        val instanceNames = elements.allInstanceNames
        return traversal.toList().map { rawResult ->
            val result: Map<String, Any> = when {
                instanceNames.isEmpty() -> {
                    rawResult as Map<String, Any>
                }
                instanceNames.size == 1 -> {
                    val name = instanceNames[0]
                    mapOf(name to rawResult)
                }
                else -> {
                    val rawMap = rawResult as Map<String, Any>
                    instanceNames.associateWith { name -> 
                        rawMap[VariableBinding.stepLabel(name)]!!
                    }
                }
            }
            extractMatchResult(result, elements, context, engine, matchedInstanceNames)
        }
    }
    
    /**
     * Extracts a MatchResult from a traversal result.
     * 
     * The instance mappings returned will be used by MatchResult.applyTo to update
     * the current scope's bindings (setting vertexId on InstanceBindings).
     * This happens in place - no new scopes are created.
     */
    private fun extractMatchResult(
        result: Map<String, Any>,
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): MatchResult.Matched {
        val nodeClassification = classifyNodes(result, elements, engine)
        val bindings = evaluateVariables(elements.variables, context, engine)
        
        return MatchResult.Matched(
            bindings = bindings,
            instanceMappings = nodeClassification.instanceMappings,
            matchedNodeIds = nodeClassification.matchedNodeIds,
            createdNodeIds = nodeClassification.createdNodeIds,
            deletedNodeIds = nodeClassification.deletedNodeIds
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
    
    private data class NodeClassification(
        val instanceMappings: Map<String, Any>,
        val matchedNodeIds: Set<Any>,
        val createdNodeIds: Set<Any>,
        val deletedNodeIds: Set<Any>
    )
    
    /**
     * Evaluates pattern variables.
     */
    private fun evaluateVariables(
        variables: List<TypedPatternVariableElement>,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): Map<String, Any?> {
        val bindings = mutableMapOf<String, Any?>()
        for (variableElement in variables) {
            val variable = variableElement.variable
            bindings[variable.name] = getPropertyValue(variable.value, context, engine, emptyList())
        }
        return bindings
    }
    
    /**
     * Compiles a property value expression and returns only the computed value.
     * 
     * Executes the compiled traversal to get a constant or computed value.
     * Used for simple property assignments where only the value is needed.
     * 
     * @param expression The expression to compile
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The constant or computed value, or null if compilation fails or produces no result
     */
    @Suppress("UNCHECKED_CAST")
    private fun getPropertyValue(
        expression: TypedExpression,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): Any? {
        val result = compilePropertyExpression(expression, context, engine, matchedInstanceNames)
        if (result == null) return null
        
        if (result is GremlinCompilationResult.ValueResult) {
            return result.value
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
     * Compiles a property value expression and returns the full compilation result.
     * 
     * This variant returns the GremlinCompilationResult, allowing the caller to
     * distinguish between constant and non-constant (traversal) results.
     * 
     * @param expression The expression to compile
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The compilation result, or null if compilation fails
     */
    private fun compilePropertyExpression(
        expression: TypedExpression,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): GremlinCompilationResult? {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            throw IllegalStateException(
                "Expression compiler not found for expression of type '${expression::class.simpleName}'. " +
                "Ensure a compiler is registered for this expression kind."
            )
        }
        
        return try {
            val availableInstanceNames = context.getAllInstances().keys.toList() + matchedInstanceNames
            val compilationContext = buildCompilationContextWithTransformation(engine, context, availableInstanceNames)
            engine.expressionCompilerRegistry.compile(expression, compilationContext)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to compile expression '${expression::class.simpleName}': ${e.message}. " +
                "This may indicate a missing type registration for metamodel types.",
                e
            )
        }
    }
    
    /**
     * Builds a compilation context with access to the transformation execution context.
     * 
     * Uses the context's variableScope
     * 
     * @param engine The transformation engine
     * @param txContext The transformation execution context with bound instances
     * @param availableInstanceNames Names of instances available for reference (unused, kept for compatibility)
     * @return A CompilationContext with transformation context for resolution
     */
    private fun buildCompilationContextWithTransformation(
        engine: TransformationEngine,
        txContext: TransformationExecutionContext,
        availableInstanceNames: List<String>
    ): CompilationContext {
        return CompilationContext(
            types = engine.types,
            traversalSource = engine.traversalSource,
            currentScope = txContext.variableScope,
            typeRegistry = engine.typeRegistry,
            transformationContext = txContext
        )
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
