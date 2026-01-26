package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedTernaryExpression.
 */
class TypedTernaryExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize simple ternary expression`() {
        val jsonString = """{
            "kind": "ternary",
            "evalType": 0,
            "condition": {"kind": "booleanLiteral", "evalType": 1, "value": true},
            "trueExpression": {"kind": "intLiteral", "evalType": 0, "value": "1"},
            "falseExpression": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTernaryExpression>(result)
        assertEquals("ternary", result.kind)
        assertEquals(0, result.evalType)
        
        val condition = result.condition
        assertIs<TypedBooleanLiteralExpression>(condition)
        assertEquals(true, condition.value)
        
        val trueExpr = result.trueExpression
        assertIs<TypedIntLiteralExpression>(trueExpr)
        assertEquals("1", trueExpr.value)
        
        val falseExpr = result.falseExpression
        assertIs<TypedIntLiteralExpression>(falseExpr)
        assertEquals("0", falseExpr.value)
    }
    
    @Test
    fun `deserialize ternary with binary condition`() {
        val jsonString = """{
            "kind": "ternary",
            "evalType": 1,
            "condition": {
                "kind": "binary",
                "evalType": 2,
                "operator": ">",
                "left": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 2},
                "right": {"kind": "intLiteral", "evalType": 0, "value": "0"}
            },
            "trueExpression": {"kind": "stringLiteral", "evalType": 1, "value": "positive"},
            "falseExpression": {"kind": "stringLiteral", "evalType": 1, "value": "non-positive"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTernaryExpression>(result)
        
        val condition = result.condition
        assertIs<TypedBinaryExpression>(condition)
        assertEquals(">", condition.operator)
    }
    
    @Test
    fun `deserialize nested ternary expressions`() {
        val jsonString = """{
            "kind": "ternary",
            "evalType": 1,
            "condition": {"kind": "booleanLiteral", "evalType": 2, "value": true},
            "trueExpression": {
                "kind": "ternary",
                "evalType": 1,
                "condition": {"kind": "booleanLiteral", "evalType": 2, "value": false},
                "trueExpression": {"kind": "stringLiteral", "evalType": 1, "value": "a"},
                "falseExpression": {"kind": "stringLiteral", "evalType": 1, "value": "b"}
            },
            "falseExpression": {"kind": "stringLiteral", "evalType": 1, "value": "c"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTernaryExpression>(result)
        
        val trueExpr = result.trueExpression
        assertIs<TypedTernaryExpression>(trueExpr)
    }
}
