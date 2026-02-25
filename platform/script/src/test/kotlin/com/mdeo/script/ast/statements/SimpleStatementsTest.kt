package com.mdeo.script.ast.statements

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedFunctionCallExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.statements.TypedBreakStatement
import com.mdeo.expression.ast.statements.TypedContinueStatement
import com.mdeo.expression.ast.statements.TypedExpressionStatement
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.expression.ast.statements.TypedStatement
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deserializing simple statements (expression, break, continue, return).
 */
class SimpleStatementsTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }
    
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
        assertEquals("expression", result.kind)
        
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
        assertEquals("break", result.kind)
    }
    
    @Test
    fun `deserialize continue statement`() {
        val jsonString = """{"kind": "continue"}"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedContinueStatement>(result)
        assertEquals("continue", result.kind)
    }
    
    @Test
    fun `deserialize return statement without value`() {
        val jsonString = """{"kind": "return"}"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedReturnStatement>(result)
        assertEquals("return", result.kind)
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
        assertIs<TypedBinaryExpression>(value)
        assertEquals("*", value.operator)
    }
}
