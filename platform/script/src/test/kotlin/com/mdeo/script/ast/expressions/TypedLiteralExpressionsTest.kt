package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing literal expressions.
 */
class TypedLiteralExpressionsTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize string literal`() {
        val jsonString = """{
            "kind": "stringLiteral",
            "evalType": 0,
            "value": "Hello, World!"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedStringLiteralExpression>(result)
        assertEquals(TypedExpressionKind.StringLiteral, result.kind)
        assertEquals(0, result.evalType)
        assertEquals("Hello, World!", result.value)
    }
    
    @Test
    fun `deserialize empty string literal`() {
        val jsonString = """{
            "kind": "stringLiteral",
            "evalType": 0,
            "value": ""
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedStringLiteralExpression>(result)
        assertEquals("", result.value)
    }
    
    @Test
    fun `deserialize string literal with special characters`() {
        val jsonString = """{
            "kind": "stringLiteral",
            "evalType": 0,
            "value": "Line1\nLine2\tTabbed"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedStringLiteralExpression>(result)
        assertEquals("Line1\nLine2\tTabbed", result.value)
    }
    
    @Test
    fun `deserialize int literal`() {
        val jsonString = """{
            "kind": "intLiteral",
            "evalType": 0,
            "value": "42"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIntLiteralExpression>(result)
        assertEquals(TypedExpressionKind.IntLiteral, result.kind)
        assertEquals("42", result.value)
    }
    
    @Test
    fun `deserialize negative int literal`() {
        val jsonString = """{
            "kind": "intLiteral",
            "evalType": 0,
            "value": "-100"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIntLiteralExpression>(result)
        assertEquals("-100", result.value)
    }
    
    @Test
    fun `deserialize zero int literal`() {
        val jsonString = """{
            "kind": "intLiteral",
            "evalType": 0,
            "value": "0"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIntLiteralExpression>(result)
        assertEquals("0", result.value)
    }
    
    @Test
    fun `deserialize long literal`() {
        val jsonString = """{
            "kind": "longLiteral",
            "evalType": 0,
            "value": "9223372036854775807"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLongLiteralExpression>(result)
        assertEquals(TypedExpressionKind.LongLiteral, result.kind)
        assertEquals("9223372036854775807", result.value)
    }
    
    @Test
    fun `deserialize negative long literal`() {
        val jsonString = """{
            "kind": "longLiteral",
            "evalType": 0,
            "value": "-9223372036854775808"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLongLiteralExpression>(result)
        assertEquals("-9223372036854775808", result.value)
    }
    
    @Test
    fun `deserialize float literal`() {
        val jsonString = """{
            "kind": "floatLiteral",
            "evalType": 0,
            "value": "3.14"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedFloatLiteralExpression>(result)
        assertEquals(TypedExpressionKind.FloatLiteral, result.kind)
        assertEquals("3.14", result.value)
    }
    
    @Test
    fun `deserialize negative float literal`() {
        val jsonString = """{
            "kind": "floatLiteral",
            "evalType": 0,
            "value": "-2.5"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedFloatLiteralExpression>(result)
        assertEquals("-2.5", result.value)
    }
    
    @Test
    fun `deserialize double literal`() {
        val jsonString = """{
            "kind": "doubleLiteral",
            "evalType": 0,
            "value": "3.141592653589793"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedDoubleLiteralExpression>(result)
        assertEquals(TypedExpressionKind.DoubleLiteral, result.kind)
        assertEquals("3.141592653589793", result.value)
    }
    
    @Test
    fun `deserialize scientific notation double literal`() {
        val jsonString = """{
            "kind": "doubleLiteral",
            "evalType": 0,
            "value": "1.23e10"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedDoubleLiteralExpression>(result)
        assertEquals("1.23e10", result.value)
    }
    
    @Test
    fun `deserialize boolean true literal`() {
        val jsonString = """{
            "kind": "booleanLiteral",
            "evalType": 0,
            "value": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBooleanLiteralExpression>(result)
        assertEquals(TypedExpressionKind.BooleanLiteral, result.kind)
        assertEquals(true, result.value)
    }
    
    @Test
    fun `deserialize boolean false literal`() {
        val jsonString = """{
            "kind": "booleanLiteral",
            "evalType": 0,
            "value": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBooleanLiteralExpression>(result)
        assertEquals(false, result.value)
    }
    
    @Test
    fun `deserialize null literal`() {
        val jsonString = """{
            "kind": "nullLiteral",
            "evalType": 0
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedNullLiteralExpression>(result)
        assertEquals(TypedExpressionKind.NullLiteral, result.kind)
        assertEquals(0, result.evalType)
    }
    
    @Test
    fun `deserialize null literal with different eval type`() {
        val jsonString = """{
            "kind": "nullLiteral",
            "evalType": 5
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedNullLiteralExpression>(result)
        assertEquals(5, result.evalType)
    }
}
