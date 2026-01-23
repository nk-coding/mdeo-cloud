package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedBinaryExpression
import com.mdeo.script.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.script.ast.expressions.TypedIdentifierExpression
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedWhileStatement.
 */
class TypedWhileStatementTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize simple while statement`() {
        val jsonString = """{
            "kind": "while",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "body": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedWhileStatement>(result)
        assertEquals(TypedStatementKind.While, result.kind)
        
        val condition = result.condition
        assertIs<TypedBooleanLiteralExpression>(condition)
        assertEquals(true, condition.value)
        
        assertEquals(0, result.body.size)
    }
    
    @Test
    fun `deserialize while statement with body`() {
        val jsonString = """{
            "kind": "while",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "body": [
                {"kind": "break"},
                {"kind": "continue"}
            ]
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedWhileStatement>(result)
        assertEquals(2, result.body.size)
        
        assertIs<TypedBreakStatement>(result.body[0])
        assertIs<TypedContinueStatement>(result.body[1])
    }
    
    @Test
    fun `deserialize while statement with binary condition`() {
        val jsonString = """{
            "kind": "while",
            "condition": {
                "kind": "binary",
                "evalType": 0,
                "operator": "<",
                "left": {"kind": "identifier", "evalType": 1, "name": "i", "scope": 2},
                "right": {"kind": "intLiteral", "evalType": 1, "value": "10"}
            },
            "body": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedWhileStatement>(result)
        val condition = result.condition
        assertIs<TypedBinaryExpression>(condition)
        assertEquals("<", condition.operator)
        
        assertIs<TypedIdentifierExpression>(condition.left)
        assertEquals("i", (condition.left as TypedIdentifierExpression).name)
    }
    
    @Test
    fun `deserialize nested while statements`() {
        val jsonString = """{
            "kind": "while",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "body": [
                {
                    "kind": "while",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "body": [{"kind": "break"}]
                }
            ]
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedWhileStatement>(result)
        val innerWhile = result.body[0]
        assertIs<TypedWhileStatement>(innerWhile)
        assertEquals(1, innerWhile.body.size)
    }
}
