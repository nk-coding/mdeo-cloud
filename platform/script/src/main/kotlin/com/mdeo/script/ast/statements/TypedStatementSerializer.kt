package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Polymorphic serializer for TypedStatement.
 * Determines the concrete statement type based on the "kind" field.
 */
object TypedStatementSerializer : JsonContentPolymorphicSerializer<TypedStatement>(TypedStatement::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedStatement> {
        val kindValue = element.jsonObject["kind"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing 'kind' field in statement: $element")
        
        return when (kindValue) {
            TypedStatementKind.If.value -> TypedIfStatement.serializer()
            TypedStatementKind.While.value -> TypedWhileStatement.serializer()
            TypedStatementKind.For.value -> TypedForStatement.serializer()
            TypedStatementKind.VariableDeclaration.value -> TypedVariableDeclarationStatement.serializer()
            TypedStatementKind.Assignment.value -> TypedAssignmentStatement.serializer()
            TypedStatementKind.Expression.value -> TypedExpressionStatement.serializer()
            TypedStatementKind.Break.value -> TypedBreakStatement.serializer()
            TypedStatementKind.Continue.value -> TypedContinueStatement.serializer()
            TypedStatementKind.Return.value -> TypedReturnStatement.serializer()
            else -> throw IllegalArgumentException("Unknown statement kind: $kindValue")
        }
    }
}
