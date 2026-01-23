package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing call expressions.
 */
class TypedCallExpressionsTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize function call expression`() {
        val jsonString = """{
            "kind": "functionCall",
            "evalType": 0,
            "name": "add",
            "overload": "",
            "arguments": [
                {"kind": "intLiteral", "evalType": 0, "value": "1"},
                {"kind": "intLiteral", "evalType": 0, "value": "2"}
            ]
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedFunctionCallExpression>(result)
        assertEquals(TypedExpressionKind.FunctionCall, result.kind)
        assertEquals(0, result.evalType)
        assertEquals("add", result.name)
        assertEquals("", result.overload)
        assertEquals(2, result.arguments.size)
        
        assertIs<TypedIntLiteralExpression>(result.arguments[0])
        assertIs<TypedIntLiteralExpression>(result.arguments[1])
    }
    
    @Test
    fun `deserialize function call with overload`() {
        val jsonString = """{
            "kind": "functionCall",
            "evalType": 0,
            "name": "print",
            "overload": "int",
            "arguments": [
                {"kind": "intLiteral", "evalType": 0, "value": "42"}
            ]
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedFunctionCallExpression>(result)
        assertEquals("print", result.name)
        assertEquals("int", result.overload)
    }
    
    @Test
    fun `deserialize function call with no arguments`() {
        val jsonString = """{
            "kind": "functionCall",
            "evalType": 0,
            "name": "getTime",
            "overload": "",
            "arguments": []
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedFunctionCallExpression>(result)
        assertEquals("getTime", result.name)
        assertEquals(0, result.arguments.size)
    }
    
    @Test
    fun `deserialize member call expression`() {
        val jsonString = """{
            "kind": "memberCall",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "list", "scope": 2},
            "member": "size",
            "isNullChaining": false,
            "overload": "",
            "arguments": []
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberCallExpression>(result)
        assertEquals(TypedExpressionKind.MemberCall, result.kind)
        assertEquals(0, result.evalType)
        assertEquals("size", result.member)
        assertEquals(false, result.isNullChaining)
        assertEquals("", result.overload)
        
        val expr = result.expression
        assertIs<TypedIdentifierExpression>(expr)
        assertEquals("list", expr.name)
    }
    
    @Test
    fun `deserialize member call with null chaining`() {
        val jsonString = """{
            "kind": "memberCall",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "nullableObj", "scope": 2},
            "member": "getValue",
            "isNullChaining": true,
            "overload": "",
            "arguments": []
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberCallExpression>(result)
        assertEquals(true, result.isNullChaining)
    }
    
    @Test
    fun `deserialize member call with arguments`() {
        val jsonString = """{
            "kind": "memberCall",
            "evalType": 1,
            "expression": {"kind": "identifier", "evalType": 2, "name": "str", "scope": 2},
            "member": "substring",
            "isNullChaining": false,
            "overload": "",
            "arguments": [
                {"kind": "intLiteral", "evalType": 0, "value": "0"},
                {"kind": "intLiteral", "evalType": 0, "value": "5"}
            ]
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberCallExpression>(result)
        assertEquals("substring", result.member)
        assertEquals(2, result.arguments.size)
    }
    
    @Test
    fun `deserialize expression call expression`() {
        val jsonString = """{
            "kind": "call",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "callback", "scope": 2},
            "arguments": [
                {"kind": "intLiteral", "evalType": 0, "value": "42"}
            ]
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExpressionCallExpression>(result)
        assertEquals(TypedExpressionKind.ExpressionCall, result.kind)
        assertEquals(0, result.evalType)
        
        val expr = result.expression
        assertIs<TypedIdentifierExpression>(expr)
        assertEquals("callback", expr.name)
        
        assertEquals(1, result.arguments.size)
    }
    
    @Test
    fun `deserialize expression call with lambda expression`() {
        val jsonString = """{
            "kind": "call",
            "evalType": 0,
            "expression": {
                "kind": "lambda",
                "evalType": 1,
                "parameters": ["x"],
                "body": {
                    "body": [
                        {"kind": "return", "value": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 3}}
                    ]
                }
            },
            "arguments": [
                {"kind": "intLiteral", "evalType": 0, "value": "5"}
            ]
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExpressionCallExpression>(result)
        
        val expr = result.expression
        assertIs<TypedLambdaExpression>(expr)
    }
    
    @Test
    fun `deserialize chained member calls`() {
        val jsonString = """{
            "kind": "memberCall",
            "evalType": 0,
            "expression": {
                "kind": "memberCall",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "list", "scope": 2},
                "member": "filter",
                "isNullChaining": false,
                "overload": "",
                "arguments": []
            },
            "member": "first",
            "isNullChaining": false,
            "overload": "",
            "arguments": []
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedMemberCallExpression>(result)
        assertEquals("first", result.member)
        
        val inner = result.expression
        assertIs<TypedMemberCallExpression>(inner)
        assertEquals("filter", inner.member)
    }
}
