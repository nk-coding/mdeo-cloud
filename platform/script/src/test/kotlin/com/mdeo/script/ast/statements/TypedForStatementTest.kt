package com.mdeo.script.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.statements.TypedBreakStatement
import com.mdeo.expression.ast.statements.TypedContinueStatement
import com.mdeo.expression.ast.statements.TypedExpressionStatement
import com.mdeo.expression.ast.statements.TypedForStatement
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedForStatement.
 */
class TypedForStatementTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }
    
    @Test
    fun `deserialize simple for statement`() {
        val jsonString = """{
            "kind": "for",
            "variableName": "item",
            "variableType": 0,
            "iterable": {"kind": "identifier", "evalType": 1, "name": "items", "scope": 2},
            "body": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedForStatement>(result)
        assertEquals("for", result.kind)
        assertEquals("item", result.variableName)
        assertEquals(0, result.variableType)
        
        val iterable = result.iterable
        assertIs<TypedIdentifierExpression>(iterable)
        assertEquals("items", iterable.name)
        assertEquals(2, iterable.scope)
        
        assertEquals(0, result.body.size)
    }
    
    @Test
    fun `deserialize for statement with body`() {
        val jsonString = """{
            "kind": "for",
            "variableName": "i",
            "variableType": 0,
            "iterable": {"kind": "identifier", "evalType": 1, "name": "numbers", "scope": 3},
            "body": [
                {"kind": "expression", "expression": {"kind": "identifier", "evalType": 0, "name": "i", "scope": 4}},
                {"kind": "break"}
            ]
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedForStatement>(result)
        assertEquals(2, result.body.size)
        
        assertIs<TypedExpressionStatement>(result.body[0])
        assertIs<TypedBreakStatement>(result.body[1])
    }
    
    @Test
    fun `deserialize for statement with member call iterable`() {
        val jsonString = """{
            "kind": "for",
            "variableName": "char",
            "variableType": 0,
            "iterable": {
                "kind": "memberCall",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "text", "scope": 2},
                "member": "toCharArray",
                "isNullChaining": false,
                "overload": "",
                "arguments": []
            },
            "body": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedForStatement>(result)
        val iterable = result.iterable
        assertIs<TypedMemberCallExpression>(iterable)
        assertEquals("toCharArray", iterable.member)
    }
    
    @Test
    fun `deserialize nested for statements`() {
        val jsonString = """{
            "kind": "for",
            "variableName": "row",
            "variableType": 0,
            "iterable": {"kind": "identifier", "evalType": 1, "name": "matrix", "scope": 2},
            "body": [
                {
                    "kind": "for",
                    "variableName": "col",
                    "variableType": 0,
                    "iterable": {"kind": "identifier", "evalType": 1, "name": "row", "scope": 4},
                    "body": [{"kind": "continue"}]
                }
            ]
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedForStatement>(result)
        assertEquals("row", result.variableName)
        
        val innerFor = result.body[0]
        assertIs<TypedForStatement>(innerFor)
        assertEquals("col", innerFor.variableName)
    }
}
