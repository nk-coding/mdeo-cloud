package com.mdeo.script.ast.types

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Polymorphic serializer for ReturnType.
 * Determines the concrete type based on JSON structure.
 */
object ReturnTypeSerializer : JsonContentPolymorphicSerializer<ReturnType>(ReturnType::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ReturnType> {
        val jsonObject = element.jsonObject
        return when {
            "kind" in jsonObject -> VoidType.serializer()
            "generic" in jsonObject -> GenericTypeRef.serializer()
            "parameters" in jsonObject -> LambdaType.serializer()
            "type" in jsonObject -> ClassTypeRef.serializer()
            else -> throw IllegalArgumentException("Unknown ReturnType structure: $element")
        }
    }
}

/**
 * Polymorphic serializer for ValueType.
 * Determines the concrete type based on JSON structure.
 */
object ValueTypeSerializer : JsonContentPolymorphicSerializer<ValueType>(ValueType::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ValueType> {
        val jsonObject = element.jsonObject
        return when {
            "generic" in jsonObject -> GenericTypeRef.serializer()
            "parameters" in jsonObject -> LambdaType.serializer()
            "type" in jsonObject -> ClassTypeRef.serializer()
            else -> throw IllegalArgumentException("Unknown ValueType structure: $element")
        }
    }
}
