package com.mdeo.expression.ast.expressions

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic serializer for TypedExpression.
 * Determines the concrete expression type based on the "kind" field.
 * This is an open class that can be extended to handle additional expression types.
 */
open class TypedExpressionSerializer : JsonContentPolymorphicSerializer<TypedExpression>(TypedExpression::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedExpression> {
        val kindValue = element.jsonObject["kind"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'kind' field in expression: $element")
        
        return when (kindValue) {
            "unary" -> TypedUnaryExpression.serializer()
            "binary" -> TypedBinaryExpression.serializer()
            "ternary" -> TypedTernaryExpression.serializer()
            "memberAccess" -> TypedMemberAccessExpression.serializer()
            "identifier" -> TypedIdentifierExpression.serializer()
            "stringLiteral" -> TypedStringLiteralExpression.serializer()
            "intLiteral" -> TypedIntLiteralExpression.serializer()
            "longLiteral" -> TypedLongLiteralExpression.serializer()
            "floatLiteral" -> TypedFloatLiteralExpression.serializer()
            "doubleLiteral" -> TypedDoubleLiteralExpression.serializer()
            "booleanLiteral" -> TypedBooleanLiteralExpression.serializer()
            "nullLiteral" -> TypedNullLiteralExpression.serializer()
            "assertNonNull" -> TypedAssertNonNullExpression.serializer()
            "typeCast" -> TypedTypeCastExpression.serializer()
            "typeCheck" -> TypedTypeCheckExpression.serializer()
            "call" -> TypedExpressionCallExpression.serializer()
            "functionCall" -> TypedFunctionCallExpression.serializer()
            "memberCall" -> TypedMemberCallExpression.serializer()
            "extensionCall" -> TypedExtensionCallExpression.serializer()
            else -> throw IllegalArgumentException("Unknown expression kind: $kindValue")
        }
    }
}
