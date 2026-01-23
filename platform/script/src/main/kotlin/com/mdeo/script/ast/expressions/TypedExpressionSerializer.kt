package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic serializer for TypedExpression.
 * Determines the concrete expression type based on the "kind" field.
 */
object TypedExpressionSerializer : JsonContentPolymorphicSerializer<TypedExpression>(TypedExpression::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedExpression> {
        val kindValue = element.jsonObject["kind"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'kind' field in expression: $element")
        
        return when (kindValue) {
            TypedExpressionKind.Unary.value -> TypedUnaryExpression.serializer()
            TypedExpressionKind.Binary.value -> TypedBinaryExpression.serializer()
            TypedExpressionKind.Ternary.value -> TypedTernaryExpression.serializer()
            TypedExpressionKind.ExpressionCall.value -> TypedExpressionCallExpression.serializer()
            TypedExpressionKind.FunctionCall.value -> TypedFunctionCallExpression.serializer()
            TypedExpressionKind.MemberCall.value -> TypedMemberCallExpression.serializer()
            TypedExpressionKind.ExtensionCall.value -> TypedExtensionCallExpression.serializer()
            TypedExpressionKind.MemberAccess.value -> TypedMemberAccessExpression.serializer()
            TypedExpressionKind.Identifier.value -> TypedIdentifierExpression.serializer()
            TypedExpressionKind.StringLiteral.value -> TypedStringLiteralExpression.serializer()
            TypedExpressionKind.IntLiteral.value -> TypedIntLiteralExpression.serializer()
            TypedExpressionKind.LongLiteral.value -> TypedLongLiteralExpression.serializer()
            TypedExpressionKind.FloatLiteral.value -> TypedFloatLiteralExpression.serializer()
            TypedExpressionKind.DoubleLiteral.value -> TypedDoubleLiteralExpression.serializer()
            TypedExpressionKind.BooleanLiteral.value -> TypedBooleanLiteralExpression.serializer()
            TypedExpressionKind.NullLiteral.value -> TypedNullLiteralExpression.serializer()
            TypedExpressionKind.Lambda.value -> TypedLambdaExpression.serializer()
            else -> throw IllegalArgumentException("Unknown expression kind: $kindValue")
        }
    }
}
