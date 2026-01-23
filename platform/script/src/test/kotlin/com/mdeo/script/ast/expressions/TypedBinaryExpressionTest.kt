package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedBinaryExpression.
 */
class TypedBinaryExpressionTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize addition binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "+",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "1"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals(TypedExpressionKind.Binary, result.kind)
        assertEquals(0, result.evalType)
        assertEquals("+", result.operator)
        
        assertIs<TypedIntLiteralExpression>(result.left)
        assertIs<TypedIntLiteralExpression>(result.right)
    }
    
    @Test
    fun `deserialize subtraction binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "-",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "10"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "3"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("-", result.operator)
    }
    
    @Test
    fun `deserialize multiplication binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "*",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "5"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "4"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("*", result.operator)
    }
    
    @Test
    fun `deserialize division binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "/",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "20"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "4"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("/", result.operator)
    }
    
    @Test
    fun `deserialize modulo binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "%",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "7"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "3"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("%", result.operator)
    }
    
    @Test
    fun `deserialize logical and binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "&&",
            "left": {"kind": "booleanLiteral", "evalType": 1, "value": true},
            "right": {"kind": "booleanLiteral", "evalType": 1, "value": false}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("&&", result.operator)
    }
    
    @Test
    fun `deserialize logical or binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "||",
            "left": {"kind": "booleanLiteral", "evalType": 1, "value": false},
            "right": {"kind": "booleanLiteral", "evalType": 1, "value": true}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("||", result.operator)
    }
    
    @Test
    fun `deserialize equality binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "==",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "5"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "5"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("==", result.operator)
    }
    
    @Test
    fun `deserialize inequality binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "!=",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "1"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("!=", result.operator)
    }
    
    @Test
    fun `deserialize less than binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "<",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "1"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("<", result.operator)
    }
    
    @Test
    fun `deserialize greater than binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": ">",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "5"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "3"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals(">", result.operator)
    }
    
    @Test
    fun `deserialize less than or equal binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "<=",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "5"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "5"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("<=", result.operator)
    }
    
    @Test
    fun `deserialize greater than or equal binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": ">=",
            "left": {"kind": "intLiteral", "evalType": 0, "value": "10"},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "5"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals(">=", result.operator)
    }
    
    @Test
    fun `deserialize nested binary expressions`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "+",
            "left": {
                "kind": "binary",
                "evalType": 0,
                "operator": "*",
                "left": {"kind": "intLiteral", "evalType": 0, "value": "2"},
                "right": {"kind": "intLiteral", "evalType": 0, "value": "3"}
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "4"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("+", result.operator)
        
        val left = result.left
        assertIs<TypedBinaryExpression>(left)
        assertEquals("*", left.operator)
    }
}
