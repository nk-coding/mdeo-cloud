package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedExpressionSerializer as BaseTypedExpressionSerializer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic serializer for TypedExpression in the script language.
 *
 * Extends the base expression serializer from the expression module to handle
 * script-specific expression types, such as lambda expressions.
 *
 * This serializer uses the "kind" field in the JSON to determine which concrete
 * expression type to deserialize to. It first checks for script-specific
 * kinds, then delegates to the base serializer for common expression types.
 */
object TypedExpressionSerializer : BaseTypedExpressionSerializer() {
    
    /**
     * Selects the appropriate deserializer based on the "kind" field in the JSON element.
     *
     * @param element The JSON element to deserialize.
     * @return The deserializer for the concrete expression type.
     * @throws IllegalArgumentException If the "kind" field is missing.
     */
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedExpression> {
        val kindValue = element.jsonObject["kind"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'kind' field in expression: $element")
        
        return when (kindValue) {
            "lambda" -> TypedLambdaExpression.serializer()
            else -> super.selectDeserializer(element)
        }
    }
}
