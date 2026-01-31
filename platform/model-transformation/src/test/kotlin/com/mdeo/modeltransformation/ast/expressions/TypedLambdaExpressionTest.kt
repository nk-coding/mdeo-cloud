package com.mdeo.modeltransformation.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for TypedLambdaExpression and its serialization.
 */
class TypedLambdaExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }
    
    // ========== Basic Deserialization Tests ==========
    
    @Test
    fun `deserialize TypedLambdaExpression with empty parameters and simple body`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals("lambda", result.kind)
        assertEquals(0, result.evalType)
        assertEquals(0, result.parameters.size)
        assertIs<TypedIntLiteralExpression>(result.body)
        assertEquals("0", result.body.value)
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with single parameter`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 1,
            "parameters": ["x"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(1, result.parameters.size)
        assertEquals("x", result.parameters[0])
        assertIs<TypedIntLiteralExpression>(result.body)
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with multiple parameters`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 2,
            "parameters": ["a", "b", "c"],
            "body": {"kind": "booleanLiteral", "evalType": 1, "value": true}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(3, result.parameters.size)
        assertEquals("a", result.parameters[0])
        assertEquals("b", result.parameters[1])
        assertEquals("c", result.parameters[2])
        assertIs<TypedBooleanLiteralExpression>(result.body)
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with integer literal body`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertIs<TypedIntLiteralExpression>(result.body)
        assertEquals("42", result.body.value)
    }
    
    // ========== Parameter Tests ==========
    
    @Test
    fun `deserialize TypedLambdaExpression with empty parameter name`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [""],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(1, result.parameters.size)
        assertEquals("", result.parameters[0])
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with underscore parameter`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["_"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "1"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals("_", result.parameters[0])
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with long parameter names`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["veryLongParameterName", "anotherVeryLongParameterName"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "99"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(2, result.parameters.size)
        assertEquals("veryLongParameterName", result.parameters[0])
        assertEquals("anotherVeryLongParameterName", result.parameters[1])
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with many parameters`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "10"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(10, result.parameters.size)
        for (i in 1..10) {
            assertEquals("p$i", result.parameters[i - 1])
        }
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with unicode parameter names`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["参数", "パラメータ", "параметр"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "3"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(3, result.parameters.size)
        assertEquals("参数", result.parameters[0])
        assertEquals("パラメータ", result.parameters[1])
        assertEquals("параметр", result.parameters[2])
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with special characters in parameter names`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["my_param", "param123", "camelCaseParam"],
            "body": {"kind": "booleanLiteral", "evalType": 1, "value": false}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(3, result.parameters.size)
        assertEquals("my_param", result.parameters[0])
        assertEquals("param123", result.parameters[1])
        assertEquals("camelCaseParam", result.parameters[2])
    }
    
    // ========== EvalType Tests ==========
    
    @Test
    fun `deserialize TypedLambdaExpression with zero evalType`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(0, result.evalType)
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with large evalType`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 9999,
            "parameters": [],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "123"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(9999, result.evalType)
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with different evalType values`() {
        for (evalType in listOf(1, 5, 10, 100, 500)) {
            val jsonString = """{
                "kind": "lambda",
                "evalType": $evalType,
                "parameters": [],
                "body": {"kind": "intLiteral", "evalType": 0, "value": "$evalType"}
            }"""
            val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
            
            assertEquals(evalType, result.evalType)
        }
    }
    
    // ========== Body Tests ==========
    
    @Test
    fun `deserialize TypedLambdaExpression with binary expression body`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {
                "kind": "binary",
                "evalType": 0,
                "operator": "+",
                "left": {"kind": "intLiteral", "evalType": 0, "value": "1"},
                "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
            }
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertIs<TypedBinaryExpression>(result.body)
        val binaryExpr = result.body
        assertEquals("+", binaryExpr.operator)
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with boolean literal body`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [],
            "body": {"kind": "booleanLiteral", "evalType": 1, "value": true}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertIs<TypedBooleanLiteralExpression>(result.body)
        assertEquals(true, result.body.value)
    }
    
    // ========== Polymorphic Deserialization Tests ==========
    
    @Test
    fun `deserialize TypedLambdaExpression as polymorphic TypedExpression`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "5"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals("lambda", result.kind)
        assertEquals(1, result.parameters.size)
    }
    
    @Test
    fun `deserialize nested lambda expressions`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x"],
            "body": {
                "kind": "lambda",
                "evalType": 1,
                "parameters": ["y"],
                "body": {"kind": "intLiteral", "evalType": 2, "value": "42"}
            }
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertIs<TypedLambdaExpression>(result.body)
        val innerLambda = result.body
        assertEquals("y", innerLambda.parameters[0])
        assertEquals(1, innerLambda.evalType)
        assertIs<TypedIntLiteralExpression>(innerLambda.body)
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `deserialize TypedLambdaExpression preserves parameter order`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["first", "second", "third", "fourth", "fifth"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "5"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(5, result.parameters.size)
        assertEquals("first", result.parameters[0])
        assertEquals("second", result.parameters[1])
        assertEquals("third", result.parameters[2])
        assertEquals("fourth", result.parameters[3])
        assertEquals("fifth", result.parameters[4])
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with duplicate parameter names`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["x", "x", "x"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "3"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(3, result.parameters.size)
        assertTrue(result.parameters.all { it == "x" })
    }
    
    @Test
    fun `deserialize TypedLambdaExpression with it parameter`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["it"],
            "body": {"kind": "booleanLiteral", "evalType": 1, "value": true}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(1, result.parameters.size)
        assertEquals("it", result.parameters[0])
    }
    
    // ========== Complex Lambda Tests ==========
    
    @Test
    fun `deserialize complex lambda with integer literal body`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 5,
            "parameters": ["item", "index"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "0"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(5, result.evalType)
        assertEquals(2, result.parameters.size)
        assertEquals("item", result.parameters[0])
        assertEquals("index", result.parameters[1])
        assertIs<TypedIntLiteralExpression>(result.body)
    }
    
    @Test
    fun `deserialize lambda used in filter operation pattern`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 10,
            "parameters": ["element"],
            "body": {"kind": "booleanLiteral", "evalType": 1, "value": true}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals("element", result.parameters[0])
        assertIs<TypedBooleanLiteralExpression>(result.body)
        assertEquals(1, result.body.evalType)
    }
    
    @Test
    fun `deserialize lambda used in map operation pattern`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 20,
            "parameters": ["value"],
            "body": {"kind": "intLiteral", "evalType": 0, "value": "100"}
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals(20, result.evalType)
        assertEquals("value", result.parameters[0])
        assertIs<TypedIntLiteralExpression>(result.body)
    }
    
    @Test
    fun `deserialize deeply nested lambdas`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": ["a"],
            "body": {
                "kind": "lambda",
                "evalType": 1,
                "parameters": ["b"],
                "body": {
                    "kind": "lambda",
                    "evalType": 2,
                    "parameters": ["c"],
                    "body": {"kind": "intLiteral", "evalType": 3, "value": "999"}
                }
            }
        }"""
        val result = json.decodeFromString(TypedLambdaExpression.serializer(), jsonString)
        
        assertEquals("a", result.parameters[0])
        val lambda2 = result.body as TypedLambdaExpression
        assertEquals("b", lambda2.parameters[0])
        val lambda3 = lambda2.body as TypedLambdaExpression
        assertEquals("c", lambda3.parameters[0])
        val intLit = lambda3.body as TypedIntLiteralExpression
        assertEquals("999", intLit.value)
    }
}
