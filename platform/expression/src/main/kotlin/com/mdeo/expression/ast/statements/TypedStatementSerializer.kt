package com.mdeo.expression.ast.statements

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic serializer for TypedStatement.
 * Determines the concrete statement type based on the "kind" field.
 * This is an open class that can be extended to handle additional statement types.
 */
open class TypedStatementSerializer : JsonContentPolymorphicSerializer<TypedStatement>(TypedStatement::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedStatement> {
        val kindValue = element.jsonObject["kind"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'kind' field in statement: $element")
        
        return when (kindValue) {
            "if" -> TypedIfStatement.serializer()
            "while" -> TypedWhileStatement.serializer()
            "for" -> TypedForStatement.serializer()
            "variableDeclaration" -> TypedVariableDeclarationStatement.serializer()
            "assignment" -> TypedAssignmentStatement.serializer()
            "expression" -> TypedExpressionStatement.serializer()
            "break" -> TypedBreakStatement.serializer()
            "continue" -> TypedContinueStatement.serializer()
            "return" -> TypedReturnStatement.serializer()
            else -> throw IllegalArgumentException("Unknown statement kind: $kindValue")
        }
    }
}
