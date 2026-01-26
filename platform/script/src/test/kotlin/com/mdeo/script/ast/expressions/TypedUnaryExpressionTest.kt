package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedUnaryExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedUnaryExpression.
 */
class TypedUnaryExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize negation unary expression`() {
        val jsonString = """{
            "kind": "unary",
            "evalType": 0,
            "operator": "-",
            "expression": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedUnaryExpression>(result)
        assertEquals("unary", result.kind)
        assertEquals(0, result.evalType)
        assertEquals("-", result.operator)
        
        val inner = result.expression
        assertIs<TypedIntLiteralExpression>(inner)
        assertEquals("42", inner.value)
    }
    
    @Test
    fun `deserialize logical not unary expression`() {
        val jsonString = """{
            "kind": "unary",
            "evalType": 1,
            "operator": "!",
            "expression": {"kind": "booleanLiteral", "evalType": 1, "value": true}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedUnaryExpression>(result)
        assertEquals("!", result.operator)
        
        val inner = result.expression
        assertIs<TypedBooleanLiteralExpression>(inner)
        assertEquals(true, inner.value)
    }
    
    @Test
    fun `deserialize nested unary expressions`() {
        val jsonString = """{
            "kind": "unary",
            "evalType": 0,
            "operator": "-",
            "expression": {
                "kind": "unary",
                "evalType": 0,
                "operator": "-",
                "expression": {"kind": "intLiteral", "evalType": 0, "value": "5"}
            }
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedUnaryExpression>(result)
        assertEquals("-", result.operator)
        
        val inner = result.expression
        assertIs<TypedUnaryExpression>(inner)
        assertEquals("-", inner.operator)
    }
    
    @Test
    fun `deserialize unary on identifier`() {
        val jsonString = """{
            "kind": "unary",
            "evalType": 0,
            "operator": "-",
            "expression": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 2}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedUnaryExpression>(result)
        
        val inner = result.expression
        assertIs<TypedIdentifierExpression>(inner)
        assertEquals("x", inner.name)
    }
}
