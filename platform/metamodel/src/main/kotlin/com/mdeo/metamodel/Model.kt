package com.mdeo.metamodel

import com.mdeo.metamodel.data.*
import java.util.IdentityHashMap

/**
 * A loaded model backed by a [Metamodel].
 *
 * Constructed via [Metamodel.loadModel], the model holds all JVM instances keyed by name,
 * and can be queried by class (including subtypes). It can also be serialized back to [ModelData].
 *
 * @property metamodel The compiled metamodel that defines the structure of instances.
 * @property metamodelPath The path of the metamodel file this model was loaded from.
 * @property instancesByName All instances keyed by their instance name.
 */
class Model(
    val metamodel: Metamodel,
    val metamodelPath: String,
    val instancesByName: Map<String, ModelInstance>
) {
    /**
     * Returns all instances of the given [className], including instances of subtypes.
     *
     * Computed on the fly from [instancesByName] so that live mutations to the backing
     * map (e.g. from [com.mdeo.modeltransformation.graph.mdeo.MdeoGraph]) are always reflected.
     *
     * @param className The metamodel class name to query.
     * @return List of all matching instances, or an empty list if none exist.
     */
    fun getAllInstances(className: String): List<ModelInstance> {
        val subtypes = metamodel.metadata.classHierarchy[className] ?: return emptyList()
        return instancesByName.values.filter { metamodel.classNameOf(it) in subtypes }
    }

    /**
     * Installs this model as the current model provider on the current thread,
     * executes [block], then restores the previous provider.
     *
     * This enables class container `all()` calls inside [block] to resolve against this model.
     *
     * @param T The return type of [block].
     * @param block The block to execute with this model as the active provider.
     * @return The value returned by [block].
     */
    fun <T> withModelProvider(block: () -> T): T {
        val previous = Metamodel.currentModelProvider.get()
        Metamodel.currentModelProvider.set(Metamodel.ModelProvider { className -> getAllInstances(className) })
        try {
            return block()
        } finally {
            Metamodel.currentModelProvider.set(previous)
        }
    }

    /**
     * Converts this model back to [ModelData].
     *
     * Serializes all instances and links into data transfer objects suitable for
     * storage or transmission. Bidirectional links are emitted only once.
     *
     * @return A [ModelData] representation of this model.
     */
    fun toModelData(): ModelData {
        val dataInstances = mutableListOf<ModelDataInstance>()
        val dataLinks = mutableListOf<ModelDataLink>()
        val emittedLinks = mutableSetOf<String>()

        val nameByInstance = IdentityHashMap<ModelInstance, String>(instancesByName.size * 2)
        for ((name, instance) in instancesByName) {
            nameByInstance[instance] = name
        }

        for ((name, instance) in instancesByName) {
            val className = metamodel.classNameOf(instance)
            val meta = metamodel.metadata.classes[className] ?: continue

            val properties = mutableMapOf<String, ModelDataPropertyValue>()
            for ((propName, mapping) in meta.propertyFields) {
                val rawValue = getField(instance, mapping.fieldIndex)
                properties[propName] = toModelDataPropertyValue(rawValue, mapping)
            }
            dataInstances.add(ModelDataInstance(name, className, properties))

            for ((roleName, linkMapping) in meta.linkFields) {
                val rawValue = getField(instance, linkMapping.fieldIndex)

                val targets: List<ModelInstance> = when {
                    rawValue == null -> emptyList()
                    rawValue is Set<*> -> rawValue.filterIsInstance<ModelInstance>()
                    else -> emptyList()
                }

                for (target in targets) {
                    val targetName = nameByInstance[target] ?: continue
                    val linkKey = if (linkMapping.isOutgoing) {
                        "$name->$roleName->$targetName"
                    } else {
                        "$targetName->${linkMapping.oppositeFieldName}->$name"
                    }
                    if (linkKey in emittedLinks) continue
                    emittedLinks.add(linkKey)

                    if (linkMapping.isOutgoing) {
                        if (linkMapping.oppositeFieldName != null) {
                            emittedLinks.add("$targetName->${linkMapping.oppositeFieldName}->$name")
                        }
                        dataLinks.add(
                            ModelDataLink(
                                sourceName = name,
                                sourceProperty = roleName,
                                targetName = targetName,
                                targetProperty = linkMapping.oppositeFieldName
                            )
                        )
                    } else {
                        if (linkMapping.oppositeFieldName != null) {
                            emittedLinks.add("$targetName->${linkMapping.oppositeFieldName}->$name")
                        }
                        dataLinks.add(
                            ModelDataLink(
                                sourceName = targetName,
                                sourceProperty = linkMapping.oppositeFieldName,
                                targetName = name,
                                targetProperty = roleName
                            )
                        )
                    }
                }
            }
        }

        return ModelData(
            metamodelPath = metamodelPath,
            instances = dataInstances,
            links = dataLinks
        )
    }

    /**
     * Gets the value of a `prop_X` field from a [ModelInstance] via reflection.
     *
     * @param instance The model instance to read from.
     * @param fieldIndex The index X in the field name `prop_X`.
     * @return The current value of the field.
     */
    private fun getField(instance: ModelInstance, fieldIndex: Int): Any? {
        val field = instance.javaClass.getField("prop_$fieldIndex")
        return field.get(instance)
    }

    /**
     * Converts a raw JVM field value to [ModelDataPropertyValue] for serialization.
     *
     * All property fields are Set-backed. For single-valued properties (upper == 1),
     * the single element is extracted. For multi-valued properties, all elements are
     * serialized as a [ModelDataPropertyValue.ListValue].
     *
     * @param rawValue The raw value retrieved from a generated field.
     * @param mapping The field mapping describing multiplicity and type.
     * @return The corresponding [ModelDataPropertyValue].
     */
    private fun toModelDataPropertyValue(rawValue: Any?, mapping: PropertyFieldMapping): ModelDataPropertyValue {
        if (rawValue == null) return ModelDataPropertyValue.NullValue
        if (mapping.isCollection) {
            if (rawValue is List<*>) {
                return ModelDataPropertyValue.ListValue(
                    rawValue.mapNotNull { toSinglePropertyValue(it) }
                )
            }
            return ModelDataPropertyValue.NullValue
        }
        return toSinglePropertyValue(rawValue)
    }

    /**
     * Converts a single JVM value (non-collection) to a [ModelDataPropertyValue].
     *
     * For enum values, uses the `getEntry()` method on the generated enum value class
     * to retrieve the entry name string.
     *
     * @param value The single raw value to convert.
     * @return The corresponding [ModelDataPropertyValue].
     */
    private fun toSinglePropertyValue(value: Any?): ModelDataPropertyValue {
        if (value == null) return ModelDataPropertyValue.NullValue
        return when (value) {
            is String -> ModelDataPropertyValue.StringValue(value)
            is Int -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Long -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Float -> ModelDataPropertyValue.NumberValue(value.toDouble())
            is Double -> ModelDataPropertyValue.NumberValue(value)
            is Boolean -> ModelDataPropertyValue.BooleanValue(value)
            else -> {
                try {
                    val getEntry = value.javaClass.getMethod("getEntry")
                    val entry = getEntry.invoke(value) as String
                    ModelDataPropertyValue.EnumValue(entry)
                } catch (_: Exception) {
                    ModelDataPropertyValue.StringValue(value.toString())
                }
            }
        }
    }
}
