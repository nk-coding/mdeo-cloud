package com.mdeo.script.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.statements.TypedBreakStatement
import com.mdeo.expression.ast.statements.TypedElseIfClause
import com.mdeo.expression.ast.statements.TypedExpressionStatement
import com.mdeo.expression.ast.statements.TypedIfStatement
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.statements.TypedVariableDeclarationStatement
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deserializing TypedIfStatement.
 */
class TypedIfStatementTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }
    
    @Test
    fun `deserialize simple if statement`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "1"}}
            ],
            "elseIfs": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertEquals("if", result.kind)
        
        val condition = result.condition
        assertIs<TypedBooleanLiteralExpression>(condition)
        assertEquals(true, condition.value)
        
        assertEquals(1, result.thenBlock.size)
        assertEquals(0, result.elseIfs.size)
        assertNull(result.elseBlock)
    }
    
    @Test
    fun `deserialize if statement with else block`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
            "thenBlock": [
                {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "1"}}
            ],
            "elseIfs": [],
            "elseBlock": [
                {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "0"}}
            ]
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertEquals(1, result.elseBlock?.size)
        
        val elseReturn = result.elseBlock?.get(0)
        assertIs<TypedReturnStatement>(elseReturn)
    }
    
    @Test
    fun `deserialize if statement with else-if clauses`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfs": [
                {
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "thenBlock": [
                        {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "2"}}
                    ]
                },
                {
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                    "thenBlock": [
                        {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "3"}}
                    ]
                }
            ],
            "elseBlock": [
                {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "4"}}
            ]
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertEquals(2, result.elseIfs.size)
        
        val firstElseIf = result.elseIfs[0]
        assertIs<TypedBooleanLiteralExpression>(firstElseIf.condition)
        assertEquals(false, (firstElseIf.condition as TypedBooleanLiteralExpression).value)
        assertEquals(1, firstElseIf.thenBlock.size)
        
        val secondElseIf = result.elseIfs[1]
        assertEquals(1, secondElseIf.thenBlock.size)
    }
    
    @Test
    fun `deserialize nested if statements`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {
                    "kind": "if",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "thenBlock": [{"kind": "break"}],
                    "elseIfs": []
                }
            ],
            "elseIfs": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        val innerIf = result.thenBlock[0]
        assertIs<TypedIfStatement>(innerIf)
        assertEquals("if", innerIf.kind)
    }
}
