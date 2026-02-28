package com.mdeo.modeltransformation.service

import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataLink
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * Converts a TinkerPop graph back to ModelData format.
 */
class GraphToModelDataConverter(
    private val metamodelData: MetamodelData,
    private val types: List<ReturnType>,
    private val typeRegistry: TypeRegistry
) {

    private val classMap: Map<String, ClassData> = metamodelData.classes.associateBy { it.name }

    fun convert(
        g: GraphTraversalSource,
        metamodelUri: String,
        nameRegistry: InstanceNameRegistry
    ): ModelData {
        val instances = extractInstances(g, nameRegistry)
        val links = extractLinks(g, nameRegistry)

        return ModelData(
            metamodelUri = metamodelUri,
            instances = instances,
            links = links
        )
    }

    private fun extractInstances(
        g: GraphTraversalSource,
        nameRegistry: InstanceNameRegistry
    ): List<ModelDataInstance> {
        return g.V().toList().map { vertex ->
            convertVertexToInstance(vertex, nameRegistry)
        }
    }

    private fun convertVertexToInstance(
        vertex: Vertex,
        nameRegistry: InstanceNameRegistry
    ): ModelDataInstance {
        val name = nameRegistry.getName(vertex.id()) ?: vertex.id().toString()
        val className = vertex.label()
        val properties = extractProperties(vertex, className)

        return ModelDataInstance(
            name = name,
            className = className,
            properties = properties
        )
    }

    private fun findPropertyInHierarchy(className: String, propertyName: String): com.mdeo.expression.ast.types.PropertyData? {
        val classData = classMap[className] ?: return null
        val property = classData.properties.find { it.name == propertyName }
        if (property != null) return property
        for (parentName in classData.extends) {
            val parentProperty = findPropertyInHierarchy(parentName, propertyName)
            if (parentProperty != null) return parentProperty
        }
        return null
    }

    private fun extractProperties(vertex: Vertex, className: String): Map<String, ModelDataPropertyValue> {
        val classData = classMap[className]
            ?: throw IllegalStateException("No metamodel class found for $className")

        val vertexPropertyMap = mutableMapOf<String, MutableList<Any?>>()
        vertex.properties<Any>().forEachRemaining { vertexProperty ->
            val key = vertexProperty.key()
            if (!vertexPropertyMap.containsKey(key)) {
                vertexPropertyMap[key] = mutableListOf()
            }
            vertexPropertyMap[key]!!.add(vertexProperty.value())
        }

        return vertexPropertyMap.mapNotNull { (propertyName, values) ->
            val propData = findPropertyInHierarchy(className, propertyName)
                ?: throw IllegalStateException(
                    "No type information found for property $propertyName of class $className in metamodel"
                )

            val isCollectionType = propData.multiplicity.isMultiple()

            val propertyValue = if (isCollectionType) {
                ModelDataPropertyValue.ListValue(
                    values.map { convertToScalarPropertyValue(it, propData.enumType) }
                )
            } else {
                if (values.size > 1) {
                    throw IllegalStateException(
                        "Multiple values found for non-collection property $propertyName of class $className in vertex ${vertex.id()}"
                    )
                }
                convertToScalarPropertyValue(values.firstOrNull(), propData.enumType)
            }

            propertyName to propertyValue
        }.toMap()
    }

    private fun convertToScalarPropertyValue(
        value: Any?,
        enumTypeName: String? = null
    ): ModelDataPropertyValue {
        return when (value) {
            null -> ModelDataPropertyValue.NullValue
            is String -> {
                if (enumTypeName != null) {
                    ModelDataPropertyValue.EnumValue(parseEnumEntryName(value))
                } else {
                    ModelDataPropertyValue.StringValue(value)
                }
            }
            is Number -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Boolean -> ModelDataPropertyValue.BooleanValue(value)
            else -> ModelDataPropertyValue.StringValue(value.toString())
        }
    }

    private fun parseEnumEntryName(value: String): String {
        val regex = Regex("`[^`]+`\\.`([^`]+)`")
        val match = regex.matchEntire(value)
        return match?.groupValues?.get(1) ?: value
    }

    private fun extractLinks(
        g: GraphTraversalSource,
        nameRegistry: InstanceNameRegistry
    ): List<ModelDataLink> {
        return g.E().toList().map { edge ->
            convertEdgeToLink(edge, nameRegistry)
        }
    }

    private fun convertEdgeToLink(
        edge: Edge,
        nameRegistry: InstanceNameRegistry
    ): ModelDataLink {
        val sourceVertex = edge.outVertex()
        val targetVertex = edge.inVertex()

        val sourceName = nameRegistry.getName(sourceVertex.id()) ?: sourceVertex.id().toString()
        val targetName = nameRegistry.getName(targetVertex.id()) ?: targetVertex.id().toString()

        val (sourceProperty, targetProperty) = EdgeLabelUtils.parseEdgeLabel(edge.label())

        return ModelDataLink(
            sourceName = sourceName,
            sourceProperty = sourceProperty,
            targetName = targetName,
            targetProperty = targetProperty
        )
    }
}
