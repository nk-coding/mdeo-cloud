package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedAssertNonNullExpression
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedFunctionCallExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedAssertNonNullExpression.
 */
class TypedAssertNonNullExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize assert non-null on identifier`() {
        val jsonString = """{
            "kind": "assertNonNull",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedAssertNonNullExpression>(result)
        assertEquals("assertNonNull", result.kind)
        assertEquals(0, result.evalType)
        
        val inner = result.expression
        assertIs<TypedIdentifierExpression>(inner)
        assertEquals("x", inner.name)
    }
    
    @Test
    fun `deserialize assert non-null on null literal`() {
        val jsonString = """{
            "kind": "assertNonNull",
            "evalType": 0,
            "expression": {"kind": "nullLiteral", "evalType": 1}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedAssertNonNullExpression>(result)
        
        val inner = result.expression
        assertIs<TypedNullLiteralExpression>(inner)
    }
    
    @Test
    fun `deserialize assert non-null on member access`() {
        val jsonString = """{
            "kind": "assertNonNull",
            "evalType": 0,
            "expression": {
                "kind": "memberAccess",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                "member": "value",
                "isNullChaining": false
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedAssertNonNullExpression>(result)
        
        val inner = result.expression
        assertIs<TypedMemberAccessExpression>(inner)
        assertEquals("value", inner.member)
    }
    
    @Test
    fun `deserialize assert non-null on function call`() {
        val jsonString = """{
            "kind": "assertNonNull",
            "evalType": 0,
            "expression": {
                "kind": "functionCall",
                "evalType": 1,
                "name": "getValue",
                "overload": "getValue()",
                "arguments": []
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedAssertNonNullExpression>(result)
        
        val inner = result.expression
        assertIs<TypedFunctionCallExpression>(inner)
        assertEquals("getValue", inner.name)
    }
    
    @Test
    fun `deserialize nested assert non-null expressions`() {
        val jsonString = """{
            "kind": "assertNonNull",
            "evalType": 0,
            "expression": {
                "kind": "assertNonNull",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "x", "scope": 2}
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedAssertNonNullExpression>(result)
        
        val inner = result.expression
        assertIs<TypedAssertNonNullExpression>(inner)
        
        val innermost = inner.expression
        assertIs<TypedIdentifierExpression>(innermost)
    }
    
    @Test
    fun `deserialize assert non-null in binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "+",
            "left": {
                "kind": "assertNonNull",
                "evalType": 0,
                "expression": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2}
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "1"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        
        val left = result.left
        assertIs<TypedAssertNonNullExpression>(left)
    }
    
    @Test
    fun `deserialize assert non-null on ternary result`() {
        val jsonString = """{
            "kind": "assertNonNull",
            "evalType": 0,
            "expression": {
                "kind": "ternary",
                "evalType": 1,
                "condition": {"kind": "booleanLiteral", "evalType": 2, "value": true},
                "trueExpression": {"kind": "intLiteral", "evalType": 0, "value": "1"},
                "falseExpression": {"kind": "nullLiteral", "evalType": 1}
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedAssertNonNullExpression>(result)
        
        val inner = result.expression
        assertIs<TypedTernaryExpression>(inner)
    }
}
