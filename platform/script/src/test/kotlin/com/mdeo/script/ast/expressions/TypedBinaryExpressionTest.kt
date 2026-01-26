package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedBinaryExpression.
 */
class TypedBinaryExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
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
        assertEquals("binary", result.kind)
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
    
    @Test
    fun `deserialize null coalescing binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "??",
            "left": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("??", result.operator)
    }
    
    @Test
    fun `deserialize strict equality binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "===",
            "left": {"kind": "identifier", "evalType": 0, "name": "a", "scope": 2},
            "right": {"kind": "identifier", "evalType": 0, "name": "b", "scope": 2}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("===", result.operator)
    }
    
    @Test
    fun `deserialize strict inequality binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 1,
            "operator": "!==",
            "left": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 2},
            "right": {"kind": "nullLiteral", "evalType": 1}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("!==", result.operator)
    }
    
    @Test
    fun `deserialize null coalescing with nested expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "??",
            "left": {
                "kind": "memberAccess",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                "member": "value",
                "isNullChaining": false
            },
            "right": {"kind": "stringLiteral", "evalType": 0, "value": "default"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("??", result.operator)
        
        val left = result.left
        assertIs<TypedMemberAccessExpression>(left)
    }
    
    @Test
    fun `deserialize chained null coalescing`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "??",
            "left": {
                "kind": "binary",
                "evalType": 1,
                "operator": "??",
                "left": {"kind": "identifier", "evalType": 2, "name": "a", "scope": 2},
                "right": {"kind": "identifier", "evalType": 1, "name": "b", "scope": 2}
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("??", result.operator)
        
        val left = result.left
        assertIs<TypedBinaryExpression>(left)
        assertEquals("??", left.operator)
    }
}
