package com.mdeo.script.runtime.model

import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import com.mdeo.script.compiler.CompiledProgram
import com.mdeo.script.runtime.ScriptClassLoader

/**
 * Implementation of [ScriptModel] backed by [ModelData].
 *
 * This is a simple implementation that converts the entire model eagerly on construction,
 * creating all [ModelInstance] objects upfront.
 *
 * Class loading is delegated to [ScriptModel]: the [classLoader] and [program] are forwarded
 * to the base class, which resolves the generated JVM classes for model instances and enum
 * containers automatically.
 *
 * @param modelData     The model data containing instances and links.
 * @param metamodelData The metamodel for type information and inheritance.
 * @param classLoader   The class loader containing generated script classes.
 * @param program       The compiled program that maps metamodel names to JVM binary class names.
 */
class ModelDataScriptModel(
    private val modelData: ModelData,
    private val metamodelData: MetamodelData,
    classLoader: ScriptClassLoader,
    program: CompiledProgram
) : ScriptModel(classLoader, program) {

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
        val subtypeClosure = buildSubtypeClosure()

        linksBySource = buildLinksMap()

        val instances = mutableMapOf<String, ModelInstance>()
        for (dataInstance in modelData.instances) {
            val backing = ModelDataInstanceBacking(
                instance = dataInstance,
                metamodelData = metamodelData,
                enumValueResolver = { enumName, enumEntry -> createEnumValue(enumName, enumEntry) },
                linkResolver = { sourceName, sourceProperty ->
                    linksBySource[Pair(sourceName, sourceProperty)]
                        ?.mapNotNull { instances[it] }
                        ?: emptyList()
                }
            )
            instances[dataInstance.name] = createInstance(dataInstance.className, backing)
        }

        instancesByName = instances

        val byClass = mutableMapOf<String, MutableList<ModelInstance>>()
        for ((name, instance) in instances) {
            val className = modelData.instances.find { it.name == name }?.className ?: continue
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

        for (classData in metamodelData.classes) {
            result.getOrPut(classData.name) { mutableSetOf() }.add(classData.name)
        }

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
            if (link.sourceProperty != null) {
                result.getOrPut(Pair(link.sourceName, link.sourceProperty)) { mutableListOf() }
                    .add(link.targetName)
            }
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
    private val metamodelData: MetamodelData,
    private val enumValueResolver: (String, String) -> Any,
    private val linkResolver: (String, String?) -> List<ModelInstance>
) : ModelInstanceBacking {

    override fun getProperty(name: String): Any? {
        val propValue = instance.properties[name]
        if (propValue != null) {
            return convertPropertyValue(propValue, name)
        }

        val links = linkResolver(instance.name, name)
        if (links.isNotEmpty()) {
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
        val propData = findPropertyData(propertyName)
        return when (propData?.primitiveType?.lowercase()) {
            "int", "integer" -> value.toInt()
            "long" -> value.toLong()
            "float" -> value.toFloat()
            "double" -> value
            else -> value
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
        return enumValueResolver(enumName, enumEntry)
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
        val propData = findPropertyData(propertyName)
        if (propData != null) {
            return propData.multiplicity.isMultiple()
        }

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
