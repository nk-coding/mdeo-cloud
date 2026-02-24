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
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.expressions.EqualityCompilerUtil
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
         * Anchor label for match clauses to ensure all clauses are connected.
         */
        private const val ANCHOR_LABEL = "_"
    }
    
    /**
     * Executes a pattern match with modifications, returning the first match.
     * 
     * @param pattern The pattern to match against the graph
     * @param context The current transformation execution context
     * @param engine The transformation engine providing access to the graph and compilers
     * @return A MatchResult containing the matched bindings, or NoMatch if no match is found
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
     * 
     * @param pattern The pattern to match against the graph
     * @param context The current transformation execution context
     * @param engine The transformation engine providing access to the graph and compilers
     * @return A list of all MatchResult.Matched instances found in the graph
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
     * 
     * Analyzes the pattern elements, builds match clauses, constructs a unified traversal,
     * and executes it to return matched results with all modifications applied.
     * 
     * @param elements Categorized pattern elements (instances, links, variables, etc.)
     * @param context The transformation execution context with variable bindings
     * @param engine The transformation engine providing graph access and compilers
     * @param limit Maximum number of matches to return (-1 for unlimited)
     * @return List of matched results with instance mappings and node classifications
     */
    private fun executeUnifiedTraversal(
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long
    ): List<MatchResult.Matched> {
        val compilationContext = CompilationContext(
            types = engine.types,
            currentScope = context.variableScope,
            traversalSource = engine.traversalSource,
            typeRegistry = engine.typeRegistry
        )
        
        for (variableElement in elements.variables) {
            val variableName = variableElement.variable.name
            val variableLabel = VariableBinding.variableLabel(variableName)
            context.variableScope.setBinding(variableName, VariableBinding.LabelBinding(variableLabel))
        }
        
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
        
        val matchBuildResult = buildMatchClauses(allMatchableInstances, allMatchableLinks, elements.variables, referencedInstances, context, engine, compilationContext)
        
        val traversal = buildUnifiedTraversal(elements, matchBuildResult.clauses, matchBuildResult.compiledVariables, context, engine, limit, compilationContext)
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
     * 
     * @param elements Categorized pattern elements (instances, links, variables, where clauses)
     * @param matchClauses Array of match clause traversals to combine
     * @param compiledVariables Compiled variable expressions indexed by variable label
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param limit Maximum number of matches (-1 for unlimited)
     * @return A unified traversal that executes match, modifications, and returns selected values
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildUnifiedTraversal(
        elements: PatternCategories,
        matchClauses: Array<GraphTraversal<Any, Any>>,
        compiledVariables: CompiledVariables,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        limit: Long,
        compilationContext: CompilationContext
    ): GraphTraversal<Vertex, Any> {
        val g = engine.traversalSource
        
        val allMatchableInstances = elements.matchableInstances + elements.deleteInstances
        
        var traversal: GraphTraversal<Vertex, Map<String, Any>> = if (matchClauses.isEmpty()) {
            var traversal = g.inject(emptyMap<String, Any>()) as GraphTraversal<Vertex, Map<String, Any>>
            traversal = addForbidConstraints(traversal, elements)
            traversal = addPropertyWhereConstraints(traversal, allMatchableInstances, compiledVariables, context, engine)
            traversal = addWhereClauseConstraints(traversal, elements.whereClauses, compiledVariables, context, engine)
            traversal
        } else {
            var traversal = buildMatchStep(
                g.inject(emptyMap<String, Any>()).`as`(ANCHOR_LABEL) as GraphTraversal<Vertex, Vertex>,
                matchClauses
            )
            traversal = addForbidConstraints(traversal, elements)
            traversal = addPropertyWhereConstraints(traversal, allMatchableInstances, compiledVariables, context, engine)
            traversal = addWhereClauseConstraints(traversal, elements.whereClauses, compiledVariables, context, engine)
            traversal
        }
        
        traversal = applyLimit(traversal, limit)
        
        val matchedInstanceNames = elements.matchableInstances.map { it.objectInstance.name }
        traversal = addCreateVertexSteps(traversal, elements.createInstances, context, engine, matchedInstanceNames, compilationContext)
        
        traversal = addPropertyUpdateSteps(traversal, elements, compiledVariables, context, engine, matchedInstanceNames, compilationContext)
        val currentPatternInstanceNames = elements.allInstanceNames.toSet()
        traversal = addCreateEdgeSteps(traversal, elements.createLinks, context, engine, currentPatternInstanceNames)
        traversal = addDeleteSteps(traversal, elements)
        
        val variableLabels = elements.variables.map { VariableBinding.variableLabel(it.variable.name) }
        return addSelectStep(traversal, elements.allInstanceNames, variableLabels)
    }
    
    /**
     * Builds the match() step from the clauses.
     * 
     * Handles both single and multiple match clauses using the appropriate overload
     * of the match() step. Gremlin requires different syntax for one vs many clauses.
     * 
     * @param traversal The base traversal to append the match step to
     * @param matchClauses Array of match clause traversals
     * @return The traversal with match step applied, returning a map of matched elements
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
     * 
     * @param traversal The traversal to apply the limit to
     * @param limit Maximum number of results to return (values <= 0 mean no limit)
     * @return The traversal with limit applied if limit > 0, otherwise unchanged
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
     * 
     * @param traversal The traversal to extend with vertex creation steps
     * @param createInstances List of instance elements marked for creation
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances that can be referenced
     * @return The extended traversal with all vertex creation steps added
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateVertexSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        createInstances: List<TypedPatternObjectInstanceElement>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>,
        compilationContext: CompilationContext
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in createInstances) {
            val className = instance.objectInstance.className
            val name = instance.objectInstance.name
            
            result = result.addV(className).`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Vertex, Map<String, Any>>
            result = addCreateVertexProperties(result, name, instance.objectInstance.properties, context, engine, matchedInstanceNames, compilationContext)
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
     * @param compilationContext The compilation context for expression compilation
     * @return The extended traversal
     */
    @Suppress("UNCHECKED_CAST")
    private fun addCreateVertexProperties(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceName: String,
        properties: List<TypedPatternPropertyAssignment>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>,
        compilationContext: CompilationContext
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
            val valueLabel = compilationContext.getUniqueId()
            
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
                
        val typeDefinition = engine.typeRegistry.getType(type)!!
        val registryCardinality = typeDefinition.cardinality
        return registryCardinality == VertexProperty.Cardinality.list || registryCardinality == VertexProperty.Cardinality.set
    }
    
    /**
     * Adds property update steps for all instances (matched and created).
     * Uses sideEffect pattern for all updates to existing/matched vertices.
     * 
     * @param traversal The traversal to extend with property update steps
     * @param elements Categorized pattern elements containing instances to update
     * @param compiledVariables Compiled variable expressions for property value resolution
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances available for reference
     * @return The extended traversal with all property update steps added
     */
    @Suppress("UNCHECKED_CAST")
    private fun addPropertyUpdateSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories,
        compiledVariables: CompiledVariables,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>,
        compilationContext: CompilationContext
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        for (instance in elements.matchableInstances) {
            if (instance.objectInstance.modifier == "delete") continue
            result = addMatchedInstanceProperties(result, instance, compiledVariables, context, engine, matchedInstanceNames, compilationContext)
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
     * @param compilationContext The compilation context for expression compilation
     * @return The extended traversal with property update steps
     */
    @Suppress("UNCHECKED_CAST")
    private fun addMatchedInstanceProperties(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instance: TypedPatternObjectInstanceElement,
        compiledVariables: CompiledVariables,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>,
        compilationContext: CompilationContext
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
                    result = setListPropertyViaSideEffect(result, name, property.propertyName, listResult, engine, compilationContext)
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
     * @param compilationContext The compilation context for unique ID generation
     * @return The extended traversal with sideEffect steps for list property management
     */
    @Suppress("UNCHECKED_CAST")
    private fun setListPropertyViaSideEffect(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceName: String,
        propertyName: String,
        listResult: GremlinCompilationResult,
        engine: TransformationEngine,
        compilationContext: CompilationContext
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        
        result = result.sideEffect(
            AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(instanceName))
                .properties<Any>(propertyName)
                .drop()
        ) as GraphTraversal<Vertex, Map<String, Any>>
        
        val listTraversal = listResult.traversal as GraphTraversal<Any, Any>
        val valueLabel = compilationContext.getUniqueId()
        
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
     * 
     * @param traversal The traversal to extend with edge creation steps
     * @param createLinks List of link elements marked for creation
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param currentPatternInstanceNames Set of instance names in the current pattern
     * @return The extended traversal with all edge creation steps added
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
     * Deletes links first, then vertices to avoid foreign key constraint issues.
     * 
     * @param traversal The traversal to extend with deletion steps
     * @param elements Categorized pattern elements containing items to delete
     * @return The extended traversal with all deletion steps added
     */
    /**
     * Adds delete steps for deleteInstances and deleteLinks.
     * 
     * Delete instances have a "delete" modifier and may or may not have a className:
     * - With className: matched first, then deleted
     * - Without className: directly deletes an already named node from previous match
     * 
     * @param traversal The traversal to extend with delete operations
     * @param elements Categorized pattern elements containing delete instances
     * @return The traversal with delete steps added
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
     * Computes the edge label from property names and filters edges to ensure
     * only edges between the specific source and target vertices are deleted.
     * 
     * @param traversal The traversal to extend with the edge deletion step
     * @param link The link element representing the edge to delete
     * @return The extended traversal with the edge deletion step added
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
     * 
     * Order of match clauses:
     * 1. Vertex/instance clauses (for pattern instances and referenced instances)
     * 2. Edge/link clauses  
     * 3. Variable clauses
     * 
     * @param instances List of object instance elements to match
     * @param links List of link elements to match
     * @param variables List of variable elements to evaluate
     * @param referencedInstances Set of instance names referenced but not explicitly matched
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @return MatchClauseBuildResult containing the clause array and compiled variables
     */
    private fun buildMatchClauses(
        instances: List<TypedPatternObjectInstanceElement>,
        links: List<TypedPatternLinkElement>,
        variables: List<TypedPatternVariableElement>,
        referencedInstances: Set<String>,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        compilationContext: CompilationContext
    ): MatchClauseBuildResult {
        val clauses = mutableListOf<GraphTraversal<Any, Any>>()
        
        for (instance in instances) {
            val binding = context.variableScope.getVariable(instance.objectInstance.name)
            val vertexId = (binding as? VariableBinding.InstanceBinding)?.vertexId
            
            if (vertexId != null) {
                clauses.add(buildIdVertexMatchClause(vertexId, instance.objectInstance.name))
            } else if (instance.objectInstance.className != null) {
                clauses.add(buildVertexMatchClauses(instance, context, engine))
            } else {
                continue
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
        
        val compiledVariablesMap = mutableMapOf<String, GremlinCompilationResult>()
        for (variableElement in variables) {
            val variableName = variableElement.variable.name
            val variableLabel = VariableBinding.variableLabel(variableName)
            val (variableClause, compilationResult) = buildVariableMatchClause(variableElement, variableLabel, context, engine, compilationContext)
            clauses.add(variableClause)
            compiledVariablesMap[variableLabel] = compilationResult
        }
        
        @Suppress("UNCHECKED_CAST")
        return MatchClauseBuildResult(
            clauses = clauses.toTypedArray(),
            compiledVariables = CompiledVariables(compiledVariablesMap)
        )
    }
    
    /**
     * Builds a match clause for a variable.
     * 
     * The variable expression is compiled to a traversal and labeled.
     * For variables that reference instances, the traversal will use select() to reference them.
     * The result is: select(instance).expression.as(variableLabel) or constant().as(variableLabel)
     * 
     * @param variableElement The variable element to compile
     * @param variableLabel The label to use for the compiled variable in the traversal
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param compilationContext The compilation context for expression compilation
     * @return A pair of (match clause traversal, compilation result)
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildVariableMatchClause(
        variableElement: TypedPatternVariableElement,
        variableLabel: String,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        compilationContext: CompilationContext
    ): Pair<GraphTraversal<Any, Any>, GremlinCompilationResult> {
        val anchorTraversal = AnonymousTraversal.`as`<Any>(ANCHOR_LABEL)
        
        val compilationResult = engine.expressionCompilerRegistry.compile(
            variableElement.variable.value,
            compilationContext,
            anchorTraversal
        )
        
        val traversal = compilationResult.traversal.`as`(variableLabel) as GraphTraversal<Any, Any>
        
        return Pair(traversal, compilationResult)
    }
    
    /**
     * Builds a vertex match clause for an object instance using the anchor pattern.
     * If the instance is pre-bound (has a vertexId in context), uses V(id) instead of V().hasLabel().
     * 
     * Supports two modes:
     * 1. Full matching: When className is provided, matches vertices by class and properties
     * 2. Reference mode: When className is null, references a previously matched node by name
     *    and only applies property comparisons (== operator)
     * 
     * Only handles constant property comparisons (ValueResult). Non-constant property 
     * constraints are handled separately via addPropertyWhereConstraints().
     * 
     * @param instance The object instance element to match
     * @param context The transformation execution context for property compilation
     * @param engine The transformation engine
     * @return A match clause traversal for the vertex with property filters
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildVertexMatchClauses(
        instance: TypedPatternObjectInstanceElement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): GraphTraversal<Any, Any> {
        val name = instance.objectInstance.name
        val className = instance.objectInstance.className
        
        var clause: GraphTraversal<Any, Any> = if (className == null) {
            AnonymousTraversal.`as`<Any>(ANCHOR_LABEL)
                .select<Any>(VariableBinding.stepLabel(name))
                .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>
        } else {
            AnonymousTraversal.`as`<Any>(ANCHOR_LABEL)
                .V()
                .hasLabel(className)
                .`as`(VariableBinding.stepLabel(name)) as GraphTraversal<Any, Any>
        }
        
        for (property in instance.objectInstance.properties) {
            if (property.operator == "==") {
                if (isCollectionType(resolveExpressionType(property.value, engine), engine)) {
                    throw IllegalArgumentException("List properties cannot be used in match clause comparisons (== operator) - not supported in Gremlin match patterns")
                }
                
                val compilationResult = compilePropertyExpression(property.value, context, engine, emptyList())                
                if (compilationResult == null) {
                    throw IllegalStateException("Failed to compile property expression for ${property.propertyName}")
                }
                
                if (compilationResult is GremlinCompilationResult.ValueResult) {
                    clause = clause.has(property.propertyName, compilationResult.value) as GraphTraversal<Any, Any>
                }
            }
        }
        
        return clause
    }
    

    
    /**
     * Builds a vertex match clause for an object instance using V(id) for pre-bound instances.
     * 
     * @param vertexId The vertex ID to match directly
     * @param name The instance name to use as the step label
     * @return A match clause traversal starting from the specified vertex ID
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
     * Uses step labels for source and target.
     * 
     * @param link The link element representing the edge to match
     * @return A match clause traversal for the edge from source to target
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
     * 
     * Forbid constraints ensure that certain patterns do NOT exist in the match.
     * Handles both connected forbid instances (via links) and disconnected ones.
     * 
     * @param traversal The traversal to extend with forbid constraints
     * @param elements Categorized pattern elements containing forbid instances and links
     * @return The extended traversal with forbid constraints added
     */
    @Suppress("UNCHECKED_CAST")
    private fun addForbidConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        elements: PatternCategories
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val matchableNames = elements.matchableInstances.map { it.objectInstance.name }.toSet()
        val forbidMap = elements.forbidInstances.mapNotNull { forbidInstance ->
            val className = forbidInstance.objectInstance.className
            if (className != null) {
                forbidInstance.objectInstance.name to className
            } else {
                null
            }
        }.toMap()
        
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
     * Handles three cases:
     * 1. Matched source, forbid target: forbid outgoing edge to specific class
     * 2. Matched target, forbid source: forbid incoming edge from specific class
     * 3. Both matched: forbid edge between specific instances
     * 
     * @param traversal The traversal to extend with the forbid link constraint
     * @param forbidLink The link element to forbid
     * @param matchableNames Set of instance names that are being matched
     * @param forbidMap Map of forbid instance names to their class names
     * @return The extended traversal with the forbid link constraint added
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
     * 
     * For forbid instances that don't appear in any forbid links, adds a constraint
     * that no vertex of that class exists in the entire graph.
     * 
     * @param traversal The traversal to extend with disconnected forbid constraints
     * @param elements Categorized pattern elements containing forbid instances and links
     * @return The extended traversal with disconnected forbid constraints added
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
                if (className != null) {
                    val notClause = AnonymousTraversal.V<Any>()
                        .hasLabel(className).count().`is`(P.eq(0L)) as GraphTraversal<Any, Any>
                    result = result.where(notClause) as GraphTraversal<Vertex, Map<String, Any>>
                }
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
     * 
     * @param traversal The traversal to extend with where clause constraints
     * @param whereClauses List of where clause elements to apply
     * @param compiledVariables Compiled variable expressions for reference in where clauses
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @return The extended traversal with all where clause constraints added
     */
    @Suppress("UNCHECKED_CAST")
    private fun addWhereClauseConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        whereClauses: List<TypedPatternWhereClauseElement>,
        compiledVariables: CompiledVariables,
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
     * Adds property comparison constraints as where() filters for non-constant property values.
     * 
     * This method handles property constraints (with "==" operator) where the property value
     * is not a compile-time constant. It compiles equality comparisons between the property
     * value and the expression using EqualityCompilerUtil.
     * 
     * @param traversal The main traversal to extend
     * @param instances The instances with property constraints to check
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @return The extended traversal with property where constraints
     */
    @Suppress("UNCHECKED_CAST")
    private fun addPropertyWhereConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instances: List<TypedPatternObjectInstanceElement>,
        compiledVariables: CompiledVariables,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        var labelCounter = 0
        
        for (instance in instances) {
            val instanceName = instance.objectInstance.name
            val instanceLabel = VariableBinding.stepLabel(instanceName)
            val className = instance.objectInstance.className
            
            for (property in instance.objectInstance.properties) {
                if (property.operator == "==") {
                    val compilationResult = compilePropertyExpression(property.value, context, engine, emptyList())
                    
                    if (className == null && compilationResult is GremlinCompilationResult.ValueResult) {
                        result = result.where(
                            AnonymousTraversal.select<Any, Any>(instanceLabel)
                                .has(property.propertyName, compilationResult.value)
                        ) as GraphTraversal<Vertex, Map<String, Any>>
                    } else if (compilationResult != null && compilationResult !is GremlinCompilationResult.ValueResult) {
                        val propertyTraversal = AnonymousTraversal.select<Any, Any>(instanceLabel)
                            .values<Any>(property.propertyName) as GraphTraversal<Any, Any>
                        
                        val anchorTraversal = AnonymousTraversal.`as`<Any>(ANCHOR_LABEL)
                        val expressionTraversal = compileExpressionToTraversal(
                            property.value,
                            engine,
                            context,
                            anchorTraversal
                        ) as GraphTraversal<Any, Any>
                        
                        val propertyType = resolveExpressionType(property.value, engine)
                            ?: throw IllegalStateException("Cannot resolve type for property expression: ${property.propertyName}")
                        
                        val leftLabel = "id_${labelCounter++}"
                        val rightLabel = "id_${labelCounter++}"
                        val equalityTraversal = EqualityCompilerUtil.buildEqualityTraversal(
                            "==",
                            propertyTraversal,
                            expressionTraversal,
                            propertyType,
                            propertyType,
                            engine.typeRegistry,
                            leftLabel,
                            rightLabel
                        )
                        
                        result = result.where(equalityTraversal.`is`(true)) 
                            as GraphTraversal<Vertex, Map<String, Any>>
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * Applies a where clause using traversal-based expression compilation.
     *
     * Compiles the where clause expression to a traversal and applies it as a filter.
     * The traversal produces a boolean value which is compared against true.
     * 
     * @param traversal The traversal to extend with the where clause
     * @param whereClause The where clause element to apply
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param counter The index of this where clause (for debugging/tracking)
     * @return The extended traversal with the where clause constraint added
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
        
        return traversal.where(
            compiledTraversal.`is`(true)
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
     * @param expression The expression to compile
     * @param engine The transformation engine with compiler registry
     * @param context The transformation execution context with properly configured scope
     * @param initialTraversal Optional initial traversal to start from (for match contexts)
     * @return The compiled traversal
     */
    private fun compileExpressionToTraversal(
        expression: TypedExpression,
        engine: TransformationEngine,
        context: TransformationExecutionContext,
        initialTraversal: GraphTraversal<*, *>? = null
    ): GraphTraversal<*, *> {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            throw IllegalStateException(
                "Cannot compile expression to traversal. " +
                "Expression type: ${expression.javaClass.simpleName} is not supported."
            )
        }
        
        val compilationContext = CompilationContext(
            types = engine.types,
            currentScope = context.variableScope,
            traversalSource = engine.traversalSource,
            typeRegistry = engine.typeRegistry
        )
        
        return engine.expressionCompilerRegistry.compile(
            expression, 
            compilationContext, 
            initialTraversal
        ).traversal
    }
    

    
    /**
     * Adds the select() step for extracting named bindings.
     * 
     * Uses step labels for selecting matched instances and variable labels for variables.
     * Returns raw step labels - conversion to original names happens after execution.
     * 
     * Note: When selecting a single element, returns that element directly.
     * When selecting multiple, returns a map with step labels as keys.
     * 
     * @param traversal The traversal to extend with the select step
     * @param instanceNames List of instance names (will be converted to step labels)
     * @param variableLabels List of variable labels (already in label form, used as-is)
     * @return The extended traversal with select step for extracting named bindings
     */
    @Suppress("UNCHECKED_CAST")
    private fun addSelectStep(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceNames: List<String>,
        variableLabels: List<String> = emptyList()
    ): GraphTraversal<Vertex, Any> {
        val instanceStepLabels = instanceNames.map { VariableBinding.stepLabel(it) }
        val allLabels = instanceStepLabels + variableLabels
        
        return when (allLabels.size) {
            0 -> traversal as GraphTraversal<Vertex, Any>
            1 -> {
                traversal.select<Any>(allLabels[0]) as GraphTraversal<Vertex, Any>
            }
            else -> {
                traversal.select<Any>(
                    allLabels[0], allLabels[1], *allLabels.drop(2).toTypedArray()
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
     * 
     * Variables are not included in the select - they're already evaluated and stored in the scope.
     * 
     * @param traversal The unified traversal to execute
     * @param elements Categorized pattern elements for result extraction
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances (for classification)
     * @return List of matched results with classified nodes and bindings
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
        val variableLabels = elements.variables.map { VariableBinding.variableLabel(it.variable.name) }
        val allLabels = instanceNames + variableLabels
        
        return traversal.toList().map { rawResult ->
            val result: Map<String, Any> = when {
                allLabels.isEmpty() -> {
                    rawResult as Map<String, Any>
                }
                allLabels.size == 1 -> {
                    val label = allLabels[0]
                    mapOf(label to rawResult)
                }
                else -> {
                    val rawMap = rawResult as Map<String, Any>
                    val instanceMap = instanceNames.associateWith { name -> 
                        rawMap[VariableBinding.stepLabel(name)]!!
                    }
                    val variableMap = variableLabels.associateWith { label ->
                        rawMap[label]!!
                    }
                    instanceMap + variableMap
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
     * 
     * Variables are extracted from the scope (where they were stored during evaluation)
     * and their LabelBinding is replaced with ValueBinding for use in subsequent scopes.
     * 
     * @param result The raw result map from traversal execution (step labels to values)
     * @param elements Categorized pattern elements for extracting variables and instances
     * @param context The transformation execution context
     * @param engine The transformation engine
     * @param matchedInstanceNames Names of matched instances for node classification
     * @return A MatchResult.Matched with bindings and classified node IDs
     */
    private fun extractMatchResult(
        result: Map<String, Any>,
        elements: PatternCategories,
        context: TransformationExecutionContext,
        engine: TransformationEngine,
        matchedInstanceNames: List<String>
    ): MatchResult.Matched {        
        val bindings = mutableMapOf<String, Any?>()
        for (variableElement in elements.variables) {
            val variableName = variableElement.variable.name
            val variableLabel = VariableBinding.variableLabel(variableName)
            val value = result[variableLabel]
            bindings[variableName] = value
            
            context.variableScope.setBinding(variableName, VariableBinding.ValueBinding(value))
        }
        
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
        
        return MatchResult.Matched(bindings, instanceMappings, matchedNodeIds, createdNodeIds, deletedNodeIds)
    }
    
    /**
     * Classifies a single node into the appropriate category.
     * 
     * Determines whether a node is matched, created, or deleted based on the instance name
     * and updates the appropriate collections. Registers created nodes with the instance
     * name registry for lookup by name.
     * 
     * @param name The instance name
     * @param value The vertex or value from the traversal result
     * @param createInstanceNames Set of instance names marked for creation
     * @param deleteInstanceNames Set of instance names marked for deletion
     * @param instanceMappings Mutable map to populate with instance name to vertex ID mappings
     * @param matchedNodeIds Mutable set to add matched node IDs to
     * @param createdNodeIds Mutable set to add created node IDs to
     * @param deletedNodeIds Mutable set to add deleted node IDs to
     * @param engine The transformation engine for instance name registration
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
            val compilationContext = CompilationContext(
                types = engine.types,
                currentScope = context.variableScope,
                traversalSource = engine.traversalSource,
                typeRegistry = engine.typeRegistry
            )
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
     * Result of building match clauses.
     *
     * Contains both the match clause traversals and the compiled variable expressions.
     * This avoids recompiling variables when they are referenced later in property constraints.
     *
     * @property clauses The array of match clause traversals
     * @property compiledVariables The compiled variable expressions indexed by variable label
     */
    private data class MatchClauseBuildResult(
        val clauses: Array<GraphTraversal<Any, Any>>,
        val compiledVariables: CompiledVariables
    ) {
        /**
         * Compares this result with another for equality.
         *
         * @param other The other object to compare with
         * @return True if both have equal clauses and compiled variables
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as MatchClauseBuildResult
            
            if (!clauses.contentEquals(other.clauses)) return false
            if (compiledVariables != other.compiledVariables) return false
            
            return true
        }
        
        /**
         * Computes hash code based on clauses and compiled variables.
         *
         * @return The hash code
         */
        override fun hashCode(): Int {
            var result = clauses.contentHashCode()
            result = 31 * result + compiledVariables.hashCode()
            return result
        }
    }
}

/**
 * Classification of nodes from a match result.
 *
 * Categorizes nodes into matched, created, and deleted sets, and provides
 * mappings from instance names to vertex IDs or values.
 *
 * @property instanceMappings Map from instance name to vertex ID or value
 * @property matchedNodeIds Set of vertex IDs that were matched (but not created)
 * @property createdNodeIds Set of vertex IDs that were created in this match
 * @property deletedNodeIds Set of vertex IDs that will be deleted
 */
private data class NodeClassification(
    val instanceMappings: Map<String, Any>,
    val matchedNodeIds: Set<Any>,
    val createdNodeIds: Set<Any>,
    val deletedNodeIds: Set<Any>
)