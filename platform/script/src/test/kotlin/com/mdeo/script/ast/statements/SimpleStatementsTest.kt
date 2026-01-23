package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedFunctionCallExpression
import com.mdeo.script.ast.expressions.TypedIdentifierExpression
import com.mdeo.script.ast.expressions.TypedIntLiteralExpression
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deserializing simple statements (expression, break, continue, return).
 */
class SimpleStatementsTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize expression statement`() {
        val jsonString = """{
            "kind": "expression",
            "expression": {
                "kind": "functionCall",
                "evalType": 0,
                "name": "println",
                "overload": "",
                "arguments": [
                    {"kind": "stringLiteral", "evalType": 1, "value": "Hello"}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedExpressionStatement>(result)
        assertEquals(TypedStatementKind.Expression, result.kind)
        
        val expr = result.expression
        assertIs<TypedFunctionCallExpression>(expr)
        assertEquals("println", expr.name)
        assertEquals(1, expr.arguments.size)
    }
    
    @Test
    fun `deserialize expression statement with identifier`() {
        val jsonString = """{
            "kind": "expression",
            "expression": {"kind": "identifier", "evalType": 0, "name": "unusedVar", "scope": 2}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedExpressionStatement>(result)
        val expr = result.expression
        assertIs<TypedIdentifierExpression>(expr)
        assertEquals("unusedVar", expr.name)
    }
    
    @Test
    fun `deserialize break statement`() {
        val jsonString = """{"kind": "break"}"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedBreakStatement>(result)
        assertEquals(TypedStatementKind.Break, result.kind)
    }
    
    @Test
    fun `deserialize continue statement`() {
        val jsonString = """{"kind": "continue"}"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedContinueStatement>(result)
        assertEquals(TypedStatementKind.Continue, result.kind)
    }
    
    @Test
    fun `deserialize return statement without value`() {
        val jsonString = """{"kind": "return"}"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedReturnStatement>(result)
        assertEquals(TypedStatementKind.Return, result.kind)
        assertNull(result.value)
    }
    
    @Test
    fun `deserialize return statement with int value`() {
        val jsonString = """{
            "kind": "return",
            "value": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedReturnStatement>(result)
        
        val value = result.value
        assertIs<TypedIntLiteralExpression>(value)
        assertEquals("42", value.value)
    }
    
    @Test
    fun `deserialize return statement with expression`() {
        val jsonString = """{
            "kind": "return",
            "value": {
                "kind": "binary",
                "evalType": 0,
                "operator": "*",
                "left": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 2},
                "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
            }
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedReturnStatement>(result)
        
        val value = result.value
        assertIs<com.mdeo.script.ast.expressions.TypedBinaryExpression>(value)
        assertEquals("*", value.operator)
    }
}
