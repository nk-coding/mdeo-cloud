package com.mdeo.modeltransformation.service

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.ClassData
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.metamodel.data.ModelData
import com.mdeo.metamodel.data.ModelDataInstance
import com.mdeo.metamodel.data.ModelDataLink
import com.mdeo.metamodel.data.ModelDataPropertyValue
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * Converts a TinkerPop graph back to [ModelData] format.
 *
 * Vertex properties are stored under `prop_X` graph keys (matching the compiled
 * instance class field names from the [Metamodel]). This converter maps them back
 * to the original metamodel property names.
 *
 * @param metamodel The compiled metamodel for graph key resolution.
 */
class GraphToModelDataConverter(
    private val metamodel: Metamodel
) {

    private val metamodelData: MetamodelData = metamodel.data
    private val classMap: Map<String, ClassData> = metamodelData.classes.associateBy { it.name }

    /**
     * Builds a reverse mapping from graph key (e.g. "prop_2") to property name for a class.
     * Includes inherited property fields.
     */
    private fun buildGraphKeyToPropertyName(className: String): Map<String, String> {
        val classMeta = metamodel.metadata.classes[className] ?: return emptyMap()
        return classMeta.propertyFields.entries.associate { (name, mapping) ->
            "prop_${mapping.fieldIndex}" to name
        }
    }

    fun convert(
        g: GraphTraversalSource,
        metamodelPath: String,
        nameRegistry: InstanceNameRegistry
    ): ModelData {
        val instances = extractInstances(g, nameRegistry)
        val links = extractLinks(g, nameRegistry)

        return ModelData(
            metamodelPath = metamodelPath,
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

    private fun findPropertyInHierarchy(className: String, propertyName: String): com.mdeo.metamodel.data.PropertyData? {
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
        val graphKeyToName = buildGraphKeyToPropertyName(className)

        val vertexPropertyMap = mutableMapOf<String, MutableList<Any?>>()
        vertex.properties<Any>().forEachRemaining { vertexProperty ->
            val key = vertexProperty.key()
            vertexPropertyMap.getOrPut(key) { mutableListOf() }.add(vertexProperty.value())
        }

        return vertexPropertyMap.mapNotNull { (graphKey, values) ->
            val propertyName = graphKeyToName[graphKey] ?: graphKey

            val propData = findPropertyInHierarchy(className, propertyName)
                ?: throw IllegalStateException(
                    "No type information found for property $propertyName (graph key: $graphKey) of class $className in metamodel"
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
