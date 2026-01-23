package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedMemberAccessExpression.
 */
class TypedMemberAccessExpressionTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize simple member access`() {
        val jsonString = """{
            "kind": "memberAccess",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "obj", "scope": 2},
            "member": "field",
            "isNullChaining": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberAccessExpression>(result)
        assertEquals(TypedExpressionKind.MemberAccess, result.kind)
        assertEquals(0, result.evalType)
        assertEquals("field", result.member)
        assertEquals(false, result.isNullChaining)
        
        val expr = result.expression
        assertIs<TypedIdentifierExpression>(expr)
        assertEquals("obj", expr.name)
    }
    
    @Test
    fun `deserialize member access with null chaining`() {
        val jsonString = """{
            "kind": "memberAccess",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "nullableObj", "scope": 2},
            "member": "value",
            "isNullChaining": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberAccessExpression>(result)
        assertEquals(true, result.isNullChaining)
    }
    
    @Test
    fun `deserialize nested member access`() {
        val jsonString = """{
            "kind": "memberAccess",
            "evalType": 0,
            "expression": {
                "kind": "memberAccess",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                "member": "inner",
                "isNullChaining": false
            },
            "member": "value",
            "isNullChaining": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberAccessExpression>(result)
        assertEquals("value", result.member)
        
        val inner = result.expression
        assertIs<TypedMemberAccessExpression>(inner)
        assertEquals("inner", inner.member)
    }
    
    @Test
    fun `deserialize member access on function call result`() {
        val jsonString = """{
            "kind": "memberAccess",
            "evalType": 0,
            "expression": {
                "kind": "functionCall",
                "evalType": 1,
                "name": "getObject",
                "overload": "",
                "arguments": []
            },
            "member": "name",
            "isNullChaining": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberAccessExpression>(result)
        assertEquals("name", result.member)
        
        val expr = result.expression
        assertIs<TypedFunctionCallExpression>(expr)
        assertEquals("getObject", expr.name)
    }
}
