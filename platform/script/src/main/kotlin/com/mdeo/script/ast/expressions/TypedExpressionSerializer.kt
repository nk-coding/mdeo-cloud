package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic serializer for TypedExpression.
 * Extends the base serializer from the expression module to handle script-specific types.
 */
object TypedExpressionSerializer : com.mdeo.expression.ast.expressions.TypedExpressionSerializer() {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedExpression> {
        val kindValue = element.jsonObject["kind"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'kind' field in expression: $element")
        
        return when (kindValue) {
            "lambda" -> TypedLambdaExpression.serializer()
            else -> super.selectDeserializer(element)
        }
    }
}
