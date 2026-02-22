package com.mdeo.script.ast.statements

import com.mdeo.expression.ast.statements.TypedStatement
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement

/**
 * Polymorphic serializer for TypedStatement.
 * Extends the base serializer from the expression module to handle script-specific types.
 */
object TypedStatementSerializer : com.mdeo.expression.ast.statements.TypedStatementSerializer() {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TypedStatement> {
        return super.selectDeserializer(element)
    }
}
