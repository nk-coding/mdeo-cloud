package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.VariableBinding
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty

/**
 * Appends graph-modification steps (create vertices, update properties, create edges, delete)
 * to a traversal after the match and filter phases are complete.
 */
internal class GraphModificationApplier(
    private val elements: PatternCategories,
    private val expressionSupport: ExpressionSupport,
    private val compilationContext: CompilationContext
) {
    /**
     * Applies all modification steps in order:
     * 1. Create new vertices with properties.
     * 2. Update properties on existing matched instances.
     * 3. Create edges.
     * 4. Delete edges and vertices.
     *
     * @param matchedInstanceNames Names of instances already bound before modifications.
     */
    @Suppress("UNCHECKED_CAST")
    fun apply(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        result = addCreateVertexSteps(result, matchedInstanceNames)
        result = addPropertyUpdateSteps(result, matchedInstanceNames)
        val allInstanceNames = elements.allInstanceNames.toSet()
        result = addCreateEdgeSteps(result, allInstanceNames)
        result = addDeleteSteps(result)
        return result
    }

    // -------------------------------------------------------------------------
    // Create vertices
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun addCreateVertexSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (instance in elements.createInstances) {
            val className = instance.objectInstance.className
            val name = instance.objectInstance.name
            result = result.addV(className).`as`(VariableBinding.stepLabel(name))
                as GraphTraversal<Vertex, Map<String, Any>>
            result = addVertexProperties(result, className, name, instance, matchedInstanceNames)
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun addVertexProperties(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        className: String?,
        instanceName: String,
        instance: TypedPatternObjectInstanceElement,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val props = instance.objectInstance.properties.filter { it.operator == "=" }
        val (listProps, simpleProps) = props.partition { property ->
            val t = expressionSupport.resolveExpressionType(property.value) ?: return@partition false
            expressionSupport.isCollectionType(t)
        }

        for (property in simpleProps) {
            val graphKey = expressionSupport.engine.resolvePropertyGraphKey(className, property.propertyName)
            val compiled = expressionSupport.compilePropertyExpression(property.value, matchedInstanceNames)!!
            result = if (compiled is CompilationResult.ValueResult) {
                if (compiled.value != null)
                    result.property(graphKey, compiled.value) as GraphTraversal<Vertex, Map<String, Any>>
                else result
            } else {
                result.property(graphKey, compiled.traversal) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }

        for (property in listProps) {
            val graphKey = expressionSupport.engine.resolvePropertyGraphKey(className, property.propertyName)
            val listResult = expressionSupport.compilePropertyExpression(property.value, matchedInstanceNames)!!
            @Suppress("UNCHECKED_CAST")
            val listTraversal = listResult.traversal as GraphTraversal<Any, Any>
            val valueLabel = compilationContext.getUniqueId()
            result = result.sideEffect(
                listTraversal.`as`(valueLabel)
                    .select<Any>(VariableBinding.stepLabel(instanceName))
                    .property(
                        VertexProperty.Cardinality.list, graphKey,
                        AnonymousTraversal.select<Any, Any>(valueLabel)
                    )
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Property updates on matched instances
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun addPropertyUpdateSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (instance in elements.matchableInstances) {
            if (instance.objectInstance.modifier == "delete") continue
            result = addMatchedInstanceProperties(result, instance, matchedInstanceNames)
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun addMatchedInstanceProperties(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instance: TypedPatternObjectInstanceElement,
        matchedInstanceNames: List<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        val name = instance.objectInstance.name
        val className = instance.objectInstance.className
        val stepLabel = VariableBinding.stepLabel(name)

        for (property in instance.objectInstance.properties) {
            if (property.operator != "=") continue
            val graphKey = expressionSupport.engine.resolvePropertyGraphKey(className, property.propertyName)
            val propType = expressionSupport.resolveExpressionType(property.value)

            if (expressionSupport.isCollectionType(propType)) {
                val listResult = expressionSupport.compilePropertyExpression(property.value, matchedInstanceNames)
                if (listResult != null) result = setListPropertyViaSideEffect(result, name, graphKey, listResult)
            } else {
                val value = expressionSupport.getPropertyValue(property.value, matchedInstanceNames)
                result = result.sideEffect(
                    AnonymousTraversal.select<Any, Any>(stepLabel).property(graphKey, value)
                ) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun setListPropertyViaSideEffect(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instanceName: String,
        graphKey: String,
        listResult: CompilationResult
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val stepLabel = VariableBinding.stepLabel(instanceName)
        var result = traversal
        result = result.sideEffect(
            AnonymousTraversal.select<Any, Any>(stepLabel).properties<Any>(graphKey).drop()
        ) as GraphTraversal<Vertex, Map<String, Any>>

        @Suppress("UNCHECKED_CAST")
        val listTraversal = listResult.traversal as GraphTraversal<Any, Any>
        val valueLabel = compilationContext.getUniqueId()
        result = result.sideEffect(
            listTraversal.`as`(valueLabel)
                .select<Any>(stepLabel)
                .property(
                    VertexProperty.Cardinality.list, graphKey,
                    AnonymousTraversal.select<Any, Any>(valueLabel)
                )
        ) as GraphTraversal<Vertex, Map<String, Any>>
        return result
    }

    // -------------------------------------------------------------------------
    // Create edges
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun addCreateEdgeSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        currentPatternInstanceNames: Set<String>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (link in elements.createLinks) {
            val src = link.link.source.objectName
            val tgt = link.link.target.objectName
            val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                link.link.source.propertyName, link.link.target.propertyName
            )
            result = result.sideEffect(
                AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(src))
                    .addE(edgeLabel)
                    .to(AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(tgt)))
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun addDeleteSteps(
        traversal: GraphTraversal<Vertex, Map<String, Any>>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (link in elements.deleteLinks) {
            result = addDeleteEdgeStep(result, link)
        }
        for (instance in elements.deleteInstances) {
            result = result.sideEffect(
                AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(instance.objectInstance.name)).drop()
            ) as GraphTraversal<Vertex, Map<String, Any>>
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun addDeleteEdgeStep(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        link: TypedPatternLinkElement
    ): GraphTraversal<Vertex, Map<String, Any>> {
        val src = link.link.source.objectName
        val tgt = link.link.target.objectName
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
            link.link.source.propertyName, link.link.target.propertyName
        )
        return traversal.sideEffect(
            AnonymousTraversal.select<Any, Any>(VariableBinding.stepLabel(src))
                .outE(edgeLabel)
                .where(AnonymousTraversal.inV().`as`(VariableBinding.stepLabel(tgt)))
                .drop()
        ) as GraphTraversal<Vertex, Map<String, Any>>
    }
}
