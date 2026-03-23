package com.mdeo.metamodel.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Represents an object instance in a model.
 *
 * Contains the instance name (unique within the model), the class name
 * (from the metamodel), and all property assignments as a map.
 *
 * @property name Unique name/identifier of this instance within the model.
 * @property className Fully qualified name of the class this instance belongs to.
 * @property properties Map of property names to their values.
 *                      Values can be single values or arrays (represented as ListValue).
 */
@Serializable
data class ModelDataInstance(
    val name: String,
    val className: String,
    @Serializable(with = PropertiesMapSerializer::class)
    val properties: Map<String, ModelDataPropertyValue>
)

/**
 * Represents a link between two model instances.
 *
 * Links connect instances through associations defined in the metamodel.
 * Both ends can optionally have a property name for navigation.
 *
 * @property sourceName Name of the source instance.
 * @property sourceProperty Property name on the source side. Null if unnamed.
 * @property targetName Name of the target instance.
 * @property targetProperty Property name on the target side. Null if unnamed.
 */
@Serializable
data class ModelDataLink(
    val sourceName: String,
    val sourceProperty: String?,
    val targetName: String,
    val targetProperty: String?
)

/**
 * Root container for a model in the ModelData format.
 *
 * This is a lightweight, readonly data format for models designed for
 * serialization and inter-service communication. It contains the metamodel
 * reference, all object instances, and all links between instances.
 *
 * @property metamodelPath Path of the imported metamodel file.
 * @property instances All object instances in the model.
 * @property links All links between instances in the model.
 */
@Serializable
data class ModelData(
    val metamodelPath: String,
    val instances: List<ModelDataInstance>,
    val links: List<ModelDataLink>
)


/**
 * Represents a property value in a model instance.
 *
 * Property values can be primitive values (string, number, boolean),
 * enum references, null for unset optional properties, or a list of values for multi-valued properties.
 * This sealed class hierarchy enables type-safe handling of all possible value types.
 *
 * @see ModelDataPropertyValueSerializer for JSON serialization/deserialization.
 */
@Serializable(with = ModelDataPropertyValueSerializer::class)
sealed class ModelDataPropertyValue {

    /**
     * Represents a string property value.
     *
     * @property value The string value.
     */
    @Serializable
    data class StringValue(val value: String) : ModelDataPropertyValue()

    /**
     * Represents a numeric property value.
     *
     * Numbers are stored as Double to handle both integer and floating-point values.
     *
     * @property value The numeric value.
     */
    @Serializable
    data class NumberValue(val value: Double) : ModelDataPropertyValue()

    /**
     * Represents a boolean property value.
     *
     * @property value The boolean value.
     */
    @Serializable
    data class BooleanValue(val value: Boolean) : ModelDataPropertyValue()

    /**
     * Represents an enum reference.
     *
     * @property enumEntry The name of the enum entry.
     */
    @Serializable
    data class EnumValue(val enumEntry: String) : ModelDataPropertyValue()

    /**
     * Represents a null/unset property value.
     *
     * Used for optional properties (multiplicity 0..1) that are not set.
     */
    @Serializable
    data object NullValue : ModelDataPropertyValue()

    /**
     * Represents a list of property values for multi-valued properties.
     *
     * @property values The list of values. Can contain any ModelDataPropertyValue type.
     */
    @Serializable
    data class ListValue(val values: List<ModelDataPropertyValue>) : ModelDataPropertyValue()
}

/**
 * Binary-format DTO for [ModelDataPropertyValue] used when serializing to non-JSON formats
 * such as CBOR. The [t] field is a type tag and the remaining fields carry the payload
 * for that type (only one is populated at a time).
 *
 * Type tags: 0=string, 1=number, 2=boolean, 3=enum, 4=null, 5=list
 */
@Serializable
private data class MdpvDto(
    val t: Int,
    val s: String? = null,
    val n: Double? = null,
    val b: Boolean? = null,
    val l: List<MdpvDto>? = null
) {
    companion object {
        const val TYPE_STRING = 0
        const val TYPE_NUMBER = 1
        const val TYPE_BOOLEAN = 2
        const val TYPE_ENUM = 3
        const val TYPE_NULL = 4
        const val TYPE_LIST = 5
    }
}

private fun ModelDataPropertyValue.toDto(): MdpvDto = when (this) {
    is ModelDataPropertyValue.StringValue -> MdpvDto(MdpvDto.TYPE_STRING, s = value)
    is ModelDataPropertyValue.NumberValue -> MdpvDto(MdpvDto.TYPE_NUMBER, n = value)
    is ModelDataPropertyValue.BooleanValue -> MdpvDto(MdpvDto.TYPE_BOOLEAN, b = value)
    is ModelDataPropertyValue.EnumValue -> MdpvDto(MdpvDto.TYPE_ENUM, s = enumEntry)
    is ModelDataPropertyValue.NullValue -> MdpvDto(MdpvDto.TYPE_NULL)
    is ModelDataPropertyValue.ListValue -> MdpvDto(MdpvDto.TYPE_LIST, l = values.map { it.toDto() })
}

private fun MdpvDto.toValue(): ModelDataPropertyValue = when (t) {
    MdpvDto.TYPE_STRING -> ModelDataPropertyValue.StringValue(s ?: error("Missing string value in MdpvDto"))
    MdpvDto.TYPE_NUMBER -> ModelDataPropertyValue.NumberValue(n ?: error("Missing number value in MdpvDto"))
    MdpvDto.TYPE_BOOLEAN -> ModelDataPropertyValue.BooleanValue(b ?: error("Missing boolean value in MdpvDto"))
    MdpvDto.TYPE_ENUM -> ModelDataPropertyValue.EnumValue(s ?: error("Missing enum entry in MdpvDto"))
    MdpvDto.TYPE_NULL -> ModelDataPropertyValue.NullValue
    MdpvDto.TYPE_LIST -> ModelDataPropertyValue.ListValue((l ?: emptyList()).map { it.toValue() })
    else -> error("Unknown ModelDataPropertyValue type tag: $t")
}

/**
 * Custom serializer for ModelDataPropertyValue that handles the union type.
 *
 * In JSON, values are represented compactly as native primitives/objects (no type
 * discriminator). For non-JSON formats such as CBOR the value is encoded as an [MdpvDto]
 * struct tagged with an integer type discriminator.
 */
object ModelDataPropertyValueSerializer : KSerializer<ModelDataPropertyValue> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ModelDataPropertyValue")

    override fun serialize(encoder: Encoder, value: ModelDataPropertyValue) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(serializeToJsonElement(value))
        } else {
            encoder.encodeSerializableValue(MdpvDto.serializer(), value.toDto())
        }
    }

    override fun deserialize(decoder: Decoder): ModelDataPropertyValue {
        if (decoder is JsonDecoder) {
            return deserializeFromJsonElement(decoder.decodeJsonElement())
        } else {
            return decoder.decodeSerializableValue(MdpvDto.serializer()).toValue()
        }
    }

    private fun serializeToJsonElement(value: ModelDataPropertyValue): JsonElement {
        return when (value) {
            is ModelDataPropertyValue.StringValue -> JsonPrimitive(value.value)
            is ModelDataPropertyValue.NumberValue -> JsonPrimitive(value.value)
            is ModelDataPropertyValue.BooleanValue -> JsonPrimitive(value.value)
            is ModelDataPropertyValue.EnumValue -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("enum", JsonPrimitive(value.enumEntry))
                }
            }
            is ModelDataPropertyValue.NullValue -> JsonNull
            is ModelDataPropertyValue.ListValue -> JsonArray(value.values.map { serializeToJsonElement(it) })
        }
    }

    private fun deserializeFromJsonElement(element: JsonElement): ModelDataPropertyValue {
        return when (element) {
            is JsonNull -> ModelDataPropertyValue.NullValue
            is JsonPrimitive -> deserializePrimitive(element)
            is JsonArray -> ModelDataPropertyValue.ListValue(
                element.map { deserializeFromJsonElement(it) }
            )
            is kotlinx.serialization.json.JsonObject -> {
                val enumValue = element["enum"]
                if (enumValue is JsonPrimitive && enumValue.isString) {
                    ModelDataPropertyValue.EnumValue(enumValue.content)
                } else {
                    throw IllegalArgumentException("Unexpected JSON object structure: $element")
                }
            }
        }
    }

    private fun deserializePrimitive(primitive: JsonPrimitive): ModelDataPropertyValue {
        return when {
            primitive.isString -> ModelDataPropertyValue.StringValue(primitive.content)
            primitive.booleanOrNull != null -> ModelDataPropertyValue.BooleanValue(primitive.booleanOrNull!!)
            primitive.intOrNull != null -> ModelDataPropertyValue.NumberValue(primitive.intOrNull!!.toDouble())
            primitive.longOrNull != null -> ModelDataPropertyValue.NumberValue(primitive.longOrNull!!.toDouble())
            primitive.doubleOrNull != null -> ModelDataPropertyValue.NumberValue(primitive.doubleOrNull!!)
            else -> throw IllegalArgumentException("Unknown primitive type: $primitive")
        }
    }
}

/**
 * Custom serializer for the properties map in ModelDataInstance.
 *
 * In JSON, the format is: `{ "propertyName": value or [values] }` (compact, no type
 * discriminator). For non-JSON formats such as CBOR, the map values are encoded as
 * [MdpvDto] structs so that explicit type tags are preserved.
 */
object PropertiesMapSerializer : KSerializer<Map<String, ModelDataPropertyValue>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PropertiesMap")

    private val dtoMapSerializer = MapSerializer(String.serializer(), MdpvDto.serializer())

    override fun serialize(encoder: Encoder, value: Map<String, ModelDataPropertyValue>) {
        if (encoder !is JsonEncoder) {
            encoder.encodeSerializableValue(dtoMapSerializer, value.mapValues { it.value.toDto() })
            return
        }

        val jsonObject = kotlinx.serialization.json.buildJsonObject {
            value.forEach { (key, propValue) ->
                put(key, when (propValue) {
                    is ModelDataPropertyValue.ListValue -> {
                        JsonArray(propValue.values.map { serializePropertyValue(it) })
                    }
                    else -> serializePropertyValue(propValue)
                })
            }
        }
        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Map<String, ModelDataPropertyValue> {
        if (decoder !is JsonDecoder) {
            return decoder.decodeSerializableValue(dtoMapSerializer).mapValues { it.value.toValue() }
        }

        val jsonObject = decoder.decodeJsonElement().jsonObject
        val result = mutableMapOf<String, ModelDataPropertyValue>()

        jsonObject.forEach { (key, element) ->
            result[key] = when (element) {
                is JsonArray -> {
                    ModelDataPropertyValue.ListValue(
                        element.map { deserializePropertyValue(it) }
                    )
                }
                else -> deserializePropertyValue(element)
            }
        }

        return result
    }

    private fun serializePropertyValue(value: ModelDataPropertyValue): JsonElement {
        return when (value) {
            is ModelDataPropertyValue.StringValue -> JsonPrimitive(value.value)
            is ModelDataPropertyValue.NumberValue -> JsonPrimitive(value.value)
            is ModelDataPropertyValue.BooleanValue -> JsonPrimitive(value.value)
            is ModelDataPropertyValue.EnumValue -> {
                kotlinx.serialization.json.buildJsonObject {
                    put("enum", JsonPrimitive(value.enumEntry))
                }
            }
            is ModelDataPropertyValue.NullValue -> JsonNull
            is ModelDataPropertyValue.ListValue -> {
                JsonArray(value.values.map { serializePropertyValue(it) })
            }
        }
    }

    private fun deserializePropertyValue(element: JsonElement): ModelDataPropertyValue {
        return when (element) {
            is JsonNull -> ModelDataPropertyValue.NullValue
            is JsonPrimitive -> deserializePrimitiveValue(element)
            is kotlinx.serialization.json.JsonObject -> {
                val enumValue = element["enum"]
                if (enumValue is JsonPrimitive && enumValue.isString) {
                    ModelDataPropertyValue.EnumValue(enumValue.content)
                } else {
                    throw IllegalArgumentException("Unexpected JSON object structure: $element")
                }
            }
            is JsonArray -> {
                ModelDataPropertyValue.ListValue(
                    element.map { deserializePropertyValue(it) }
                )
            }
        }
    }

    private fun deserializePrimitiveValue(primitive: JsonPrimitive): ModelDataPropertyValue {
        return when {
            primitive.isString -> ModelDataPropertyValue.StringValue(primitive.content)
            primitive.booleanOrNull != null -> ModelDataPropertyValue.BooleanValue(primitive.booleanOrNull!!)
            primitive.intOrNull != null -> ModelDataPropertyValue.NumberValue(primitive.intOrNull!!.toDouble())
            primitive.longOrNull != null -> ModelDataPropertyValue.NumberValue(primitive.longOrNull!!.toDouble())
            primitive.doubleOrNull != null -> ModelDataPropertyValue.NumberValue(primitive.doubleOrNull!!)
            else -> throw IllegalArgumentException("Unknown primitive type: $primitive")
        }
    }
}
