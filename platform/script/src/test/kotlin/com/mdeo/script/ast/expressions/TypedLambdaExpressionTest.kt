package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import com.mdeo.script.ast.statements.TypedStatementSerializer

import com.mdeo.expression.ast.statements.TypedReturnStatement
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedLambdaExpression.
 */
class TypedLambdaExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }
    
    @Test
    fun `deserialize simple lambda`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {
                "body": [
                    {"kind": "return", "value": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 3}}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals("lambda", result.kind)
        assertEquals(0, result.evalType)
        assertEquals(1, result.parameters.size)
        assertEquals("x", result.parameters[0])
        assertEquals(1, result.body.body.size)
        
        val returnStmt = result.body.body[0]
        assertIs<TypedReturnStatement>(returnStmt)
    }
    
    @Test
    fun `deserialize lambda with no parameters`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [],
            "body": {
                "body": [
                    {"kind": "return", "value": {"kind": "intLiteral", "evalType": 1, "value": "42"}}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals(0, result.parameters.size)
    }
    
    @Test
    fun `deserialize lambda with multiple parameters`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["a", "b", "c"],
            "body": {
                "body": [
                    {"kind": "return", "value": {
                        "kind": "binary",
                        "evalType": 1,
                        "operator": "+",
                        "left": {"kind": "identifier", "evalType": 1, "name": "a", "scope": 3},
                        "right": {"kind": "identifier", "evalType": 1, "name": "b", "scope": 3}
                    }}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals(3, result.parameters.size)
        assertEquals("a", result.parameters[0])
        assertEquals("b", result.parameters[1])
        assertEquals("c", result.parameters[2])
    }
    
    @Test
    fun `deserialize lambda with empty body`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [],
            "body": {
                "body": []
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals(0, result.body.body.size)
    }
    
    @Test
    fun `deserialize lambda with multiple statements`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {
                "body": [
                    {"kind": "variableDeclaration", "name": "y", "type": 1, "initialValue": {"kind": "intLiteral", "evalType": 1, "value": "10"}},
                    {"kind": "return", "value": {
                        "kind": "binary",
                        "evalType": 1,
                        "operator": "+",
                        "left": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 3},
                        "right": {"kind": "identifier", "evalType": 1, "name": "y", "scope": 4}
                    }}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals(2, result.body.body.size)
    }
    
    @Test
    fun `deserialize nested lambda`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {
                "body": [
                    {"kind": "return", "value": {
                        "kind": "lambda",
                        "evalType": 1,
                        "parameters": ["y"],
                        "body": {
                            "body": [
                                {"kind": "return", "value": {
                                    "kind": "binary",
                                    "evalType": 2,
                                    "operator": "+",
                                    "left": {"kind": "identifier", "evalType": 2, "name": "x", "scope": 3},
                                    "right": {"kind": "identifier", "evalType": 2, "name": "y", "scope": 5}
                                }}
                            ]
                        }
                    }}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        
        val returnStmt = result.body.body[0]
        assertIs<TypedReturnStatement>(returnStmt)
        
        val innerLambda = returnStmt.value
        assertIs<TypedLambdaExpression>(innerLambda)
        assertEquals(1, innerLambda.parameters.size)
        assertEquals("y", innerLambda.parameters[0])
    }
}
