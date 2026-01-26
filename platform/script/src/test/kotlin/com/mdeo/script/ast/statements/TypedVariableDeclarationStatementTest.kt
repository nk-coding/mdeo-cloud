package com.mdeo.script.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.statements.TypedVariableDeclarationStatement
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deserializing TypedVariableDeclarationStatement.
 */
class TypedVariableDeclarationStatementTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }
    
    @Test
    fun `deserialize variable declaration without initial value`() {
        val jsonString = """{
            "kind": "variableDeclaration",
            "name": "count",
            "type": 0
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedVariableDeclarationStatement>(result)
        assertEquals("variableDeclaration", result.kind)
        assertEquals("count", result.name)
        assertEquals(0, result.type)
        assertNull(result.initialValue)
    }
    
    @Test
    fun `deserialize variable declaration with int initial value`() {
        val jsonString = """{
            "kind": "variableDeclaration",
            "name": "x",
            "type": 0,
            "initialValue": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedVariableDeclarationStatement>(result)
        assertEquals("x", result.name)
        
        val initialValue = result.initialValue
        assertIs<TypedIntLiteralExpression>(initialValue)
        assertEquals("42", initialValue.value)
    }
    
    @Test
    fun `deserialize variable declaration with string initial value`() {
        val jsonString = """{
            "kind": "variableDeclaration",
            "name": "message",
            "type": 1,
            "initialValue": {"kind": "stringLiteral", "evalType": 1, "value": "Hello, World!"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedVariableDeclarationStatement>(result)
        assertEquals("message", result.name)
        assertEquals(1, result.type)
        
        val initialValue = result.initialValue
        assertIs<TypedStringLiteralExpression>(initialValue)
        assertEquals("Hello, World!", initialValue.value)
    }
    
    @Test
    fun `deserialize variable declaration with null initial value`() {
        val jsonString = """{
            "kind": "variableDeclaration",
            "name": "nullableVar",
            "type": 2,
            "initialValue": {"kind": "nullLiteral", "evalType": 2}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedVariableDeclarationStatement>(result)
        assertEquals("nullableVar", result.name)
        
        val initialValue = result.initialValue
        assertIs<TypedNullLiteralExpression>(initialValue)
    }
}
