package com.mdeo.script.runtime.model

import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataLink
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue

/**
 * Implementation of ScriptModel backed by ModelData.
 *
 * This is a simple implementation that converts the entire model eagerly
 * on construction, creating all ModelInstance objects upfront.
 *
 * @param modelData The model data containing instances and links.
 * @param metamodelData The metamodel for type information and inheritance.
 * @param factory The factory for creating ModelInstance and enum objects.
 */
class ModelDataScriptModel(
    private val modelData: ModelData,
    private val metamodelData: MetamodelData,
    private val factory: ModelInstanceFactory
) : ScriptModel {

    /**
     * Pre-computed map of class name to list of instances (including subclasses).
     */
    private val instancesByClass: Map<String, List<ModelInstance>>

    /**
     * Map of instance name to its ModelInstance for link resolution.
     */
    private val instancesByName: Map<String, ModelInstance>

    /**
     * Map of (sourceName, sourceProperty) to list of target instance names.
     */
    private val linksBySource: Map<Pair<String, String?>, List<String>>

    init {
        // Build subtype closure map: className -> all subclass names (including self)
        val subtypeClosure = buildSubtypeClosure()

        // Build links map for efficient lookup
        linksBySource = buildLinksMap()

        // Create all instances eagerly
        val instances = mutableMapOf<String, ModelInstance>()
        for (dataInstance in modelData.instances) {
            val backing = ModelDataInstanceBacking(
                instance = dataInstance,
                modelData = modelData,
                metamodelData = metamodelData,
                factory = factory,
                instanceResolver = { name -> instances[name] },
                linkResolver = { sourceName, sourceProperty ->
                    linksBySource[Pair(sourceName, sourceProperty)]
                        ?.mapNotNull { instances[it] }
                        ?: emptyList()
                }
            )
            instances[dataInstance.name] = factory.createInstance(dataInstance.className, backing)
        }

        instancesByName = instances

        // Build instancesByClass with subtype closure
        val byClass = mutableMapOf<String, MutableList<ModelInstance>>()
        for ((name, instance) in instances) {
            val className = modelData.instances.find { it.name == name }?.className ?: continue
            // Add to all superclasses in the closure
            for ((baseClass, subClasses) in subtypeClosure) {
                if (className in subClasses) {
                    byClass.getOrPut(baseClass) { mutableListOf() }.add(instance)
                }
            }
        }

        instancesByClass = byClass
    }

    override fun getAllInstances(className: String): List<ModelInstance> {
        return instancesByClass[className] ?: emptyList()
    }

    /**
     * Builds a map from each class name to all its subtypes (including itself).
     */
    private fun buildSubtypeClosure(): Map<String, Set<String>> {
        val classMap = metamodelData.classes.associateBy { it.name }
        val result = mutableMapOf<String, MutableSet<String>>()

        // Initialize each class's closure with itself
        for (classData in metamodelData.classes) {
            result.getOrPut(classData.name) { mutableSetOf() }.add(classData.name)
        }

        // Add each class to its parent's closure (and transitively to all ancestors)
        for (classData in metamodelData.classes) {
            var current = classData
            while (current.extends.isNotEmpty()) {
                val parentName = current.extends.first()
                result.getOrPut(parentName) { mutableSetOf() }.add(classData.name)
                current = classMap[parentName] ?: break
            }
        }

        return result
    }

    /**
     * Builds a map for efficient link lookup.
     */
    private fun buildLinksMap(): Map<Pair<String, String?>, List<String>> {
        val result = mutableMapOf<Pair<String, String?>, MutableList<String>>()

        for (link in modelData.links) {
            // Forward direction: source.property -> target
            if (link.sourceProperty != null) {
                result.getOrPut(Pair(link.sourceName, link.sourceProperty)) { mutableListOf() }
                    .add(link.targetName)
            }
            // Reverse direction: target.property -> source
            if (link.targetProperty != null) {
                result.getOrPut(Pair(link.targetName, link.targetProperty)) { mutableListOf() }
                    .add(link.sourceName)
            }
        }

        return result
    }
}

/**
 * ModelInstanceBacking implementation for ModelDataInstance.
 *
 * Provides property access by looking up values in the model data
 * and converting them to the appropriate JVM types.
 */
internal class ModelDataInstanceBacking(
    private val instance: ModelDataInstance,
    private val modelData: ModelData,
    private val metamodelData: MetamodelData,
    private val factory: ModelInstanceFactory,
    private val instanceResolver: (String) -> ModelInstance?,
    private val linkResolver: (String, String?) -> List<ModelInstance>
) : ModelInstanceBacking {

    override fun getProperty(name: String): Any? {
        // First check if it's a direct property
        val propValue = instance.properties[name]
        if (propValue != null) {
            return convertPropertyValue(propValue, name)
        }

        // Check if it's an association property
        val links = linkResolver(instance.name, name)
        if (links.isNotEmpty()) {
            // Check if this is a single-valued or multi-valued association
            val isMultiple = isMultiValuedProperty(name)
            return if (isMultiple) {
                links
            } else {
                links.firstOrNull()
            }
        }

        return null
    }

    override fun getInstanceName(): String = instance.name

    override fun getClassName(): String = instance.className

    /**
     * Converts a ModelDataPropertyValue to a JVM value.
     */
    private fun convertPropertyValue(value: ModelDataPropertyValue, propertyName: String): Any? {
        return when (value) {
            is ModelDataPropertyValue.StringValue -> value.value
            is ModelDataPropertyValue.NumberValue -> convertNumber(value.value, propertyName)
            is ModelDataPropertyValue.BooleanValue -> value.value
            is ModelDataPropertyValue.EnumValue -> convertEnumValue(value.enumEntry, propertyName)
            is ModelDataPropertyValue.NullValue -> null
            is ModelDataPropertyValue.ListValue -> value.values.map { convertPropertyValue(it, propertyName) }
        }
    }

    /**
     * Converts a number to the appropriate type based on property metadata.
     */
    private fun convertNumber(value: Double, propertyName: String): Any {
        // Find the property in the metamodel to determine exact type
        val propData = findPropertyData(propertyName)
        return when (propData?.primitiveType?.lowercase()) {
            "int", "integer" -> value.toInt()
            "long" -> value.toLong()
            "float" -> value.toFloat()
            "double" -> value
            else -> value // Default to Double
        }
    }

    /**
     * Converts an enum entry to the appropriate enum value singleton.
     */
    private fun convertEnumValue(enumEntry: String, propertyName: String): Any {
        val propData = findPropertyData(propertyName)
        val enumName = propData?.enumType ?: throw IllegalStateException(
            "Property '$propertyName' has enum value but no enum type in metamodel"
        )
        return factory.createEnumValue(enumName, enumEntry)
    }

    /**
     * Finds the PropertyData for a property name in the class hierarchy.
     */
    private fun findPropertyData(propertyName: String): PropertyData? {
        val classMap = metamodelData.classes.associateBy { it.name }
        var current = classMap[instance.className]

        while (current != null) {
            val prop = current.properties.find { it.name == propertyName }
            if (prop != null) return prop

            current = current.extends.firstOrNull()?.let { classMap[it] }
        }

        return null
    }

    /**
     * Checks if a property is multi-valued.
     */
    private fun isMultiValuedProperty(propertyName: String): Boolean {
        // Check class properties
        val propData = findPropertyData(propertyName)
        if (propData != null) {
            return propData.multiplicity.isMultiple()
        }

        // Check associations
        for (assoc in metamodelData.associations) {
            if (assoc.source.className == instance.className && assoc.source.name == propertyName) {
                return assoc.source.multiplicity.isMultiple()
            }
            if (assoc.target.className == instance.className && assoc.target.name == propertyName) {
                return assoc.target.multiplicity.isMultiple()
            }
        }

        return false
    }
}
