package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedAssertNonNullExpression
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedFunctionCallExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.expression.ast.expressions.TypedTypeCastExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for deserializing TypedTypeCastExpression.
 */
class TypedTypeCastExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize regular type cast`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2},
            "targetType": 0,
            "isSafe": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        assertEquals("typeCast", result.kind)
        assertEquals(0, result.evalType)
        assertEquals(0, result.targetType)
        assertFalse(result.isSafe)
        
        val inner = result.expression
        assertIs<TypedIdentifierExpression>(inner)
        assertEquals("x", inner.name)
    }
    
    @Test
    fun `deserialize safe type cast`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 1,
            "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
            "targetType": 1,
            "isSafe": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        assertTrue(result.isSafe)
        assertEquals(1, result.targetType)
    }
    
    @Test
    fun `deserialize type cast on null literal`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {"kind": "nullLiteral", "evalType": 1},
            "targetType": 0,
            "isSafe": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        
        val inner = result.expression
        assertIs<TypedNullLiteralExpression>(inner)
    }
    
    @Test
    fun `deserialize type cast on int literal`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {"kind": "intLiteral", "evalType": 1, "value": "42"},
            "targetType": 2,
            "isSafe": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        
        val inner = result.expression
        assertIs<TypedIntLiteralExpression>(inner)
        assertEquals("42", inner.value)
    }
    
    @Test
    fun `deserialize type cast on member access`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {
                "kind": "memberAccess",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                "member": "property",
                "isNullChaining": false
            },
            "targetType": 0,
            "isSafe": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        
        val inner = result.expression
        assertIs<TypedMemberAccessExpression>(inner)
        assertEquals("property", inner.member)
    }
    
    @Test
    fun `deserialize type cast on function call`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {
                "kind": "functionCall",
                "evalType": 1,
                "name": "getObject",
                "overload": "getObject()",
                "arguments": []
            },
            "targetType": 0,
            "isSafe": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        assertTrue(result.isSafe)
        
        val inner = result.expression
        assertIs<TypedFunctionCallExpression>(inner)
    }
    
    @Test
    fun `deserialize nested type cast expressions`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {
                "kind": "typeCast",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "x", "scope": 2},
                "targetType": 1,
                "isSafe": false
            },
            "targetType": 0,
            "isSafe": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        assertTrue(result.isSafe)
        
        val inner = result.expression
        assertIs<TypedTypeCastExpression>(inner)
        assertFalse(inner.isSafe)
    }
    
    @Test
    fun `deserialize type cast in binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "+",
            "left": {
                "kind": "typeCast",
                "evalType": 0,
                "expression": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2},
                "targetType": 0,
                "isSafe": false
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "1"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        
        val left = result.left
        assertIs<TypedTypeCastExpression>(left)
    }
    
    @Test
    fun `deserialize type cast with different target types`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 5,
            "expression": {"kind": "doubleLiteral", "evalType": 3, "value": "3.14"},
            "targetType": 5,
            "isSafe": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        assertEquals(5, result.targetType)
        assertEquals(5, result.evalType)
        
        val inner = result.expression
        assertIs<TypedDoubleLiteralExpression>(inner)
    }
    
    @Test
    fun `deserialize type cast combined with assert non-null`() {
        val jsonString = """{
            "kind": "typeCast",
            "evalType": 0,
            "expression": {
                "kind": "assertNonNull",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "x", "scope": 2}
            },
            "targetType": 0,
            "isSafe": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCastExpression>(result)
        
        val inner = result.expression
        assertIs<TypedAssertNonNullExpression>(inner)
    }
}
