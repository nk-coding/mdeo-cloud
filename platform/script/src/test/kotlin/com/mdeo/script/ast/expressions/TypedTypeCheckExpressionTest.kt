package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedFunctionCallExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import com.mdeo.expression.ast.expressions.TypedTypeCastExpression
import com.mdeo.expression.ast.expressions.TypedTypeCheckExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for deserializing TypedTypeCheckExpression.
 */
class TypedTypeCheckExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize regular type check`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2},
            "checkType": 2,
            "isNegated": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        assertEquals("typeCheck", result.kind)
        assertEquals(0, result.evalType)
        assertEquals(2, result.checkType)
        assertFalse(result.isNegated)
        
        val inner = result.expression
        assertIs<TypedIdentifierExpression>(inner)
        assertEquals("x", inner.name)
    }
    
    @Test
    fun `deserialize negated type check`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {"kind": "identifier", "evalType": 1, "name": "obj", "scope": 2},
            "checkType": 3,
            "isNegated": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        assertTrue(result.isNegated)
        assertEquals(3, result.checkType)
    }
    
    @Test
    fun `deserialize type check on null literal`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {"kind": "nullLiteral", "evalType": 1},
            "checkType": 2,
            "isNegated": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        
        val inner = result.expression
        assertIs<TypedNullLiteralExpression>(inner)
    }
    
    @Test
    fun `deserialize type check on int literal`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {"kind": "intLiteral", "evalType": 1, "value": "42"},
            "checkType": 1,
            "isNegated": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        
        val inner = result.expression
        assertIs<TypedIntLiteralExpression>(inner)
        assertEquals("42", inner.value)
    }
    
    @Test
    fun `deserialize type check on member access`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {
                "kind": "memberAccess",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                "member": "field",
                "isNullChaining": false
            },
            "checkType": 3,
            "isNegated": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        assertTrue(result.isNegated)
        
        val inner = result.expression
        assertIs<TypedMemberAccessExpression>(inner)
        assertEquals("field", inner.member)
    }
    
    @Test
    fun `deserialize type check on function call`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {
                "kind": "functionCall",
                "evalType": 1,
                "name": "getObject",
                "overload": "getObject()",
                "arguments": []
            },
            "checkType": 2,
            "isNegated": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        
        val inner = result.expression
        assertIs<TypedFunctionCallExpression>(inner)
    }
    
    @Test
    fun `deserialize type check in binary expression`() {
        val jsonString = """{
            "kind": "binary",
            "evalType": 0,
            "operator": "&&",
            "left": {
                "kind": "typeCheck",
                "evalType": 0,
                "expression": {"kind": "identifier", "evalType": 1, "name": "x", "scope": 2},
                "checkType": 2,
                "isNegated": false
            },
            "right": {"kind": "booleanLiteral", "evalType": 0, "value": true}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedBinaryExpression>(result)
        assertEquals("&&", result.operator)
        
        val left = result.left
        assertIs<TypedTypeCheckExpression>(left)
    }
    
    @Test
    fun `deserialize type check in ternary condition`() {
        val jsonString = """{
            "kind": "ternary",
            "evalType": 1,
            "condition": {
                "kind": "typeCheck",
                "evalType": 0,
                "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                "checkType": 3,
                "isNegated": false
            },
            "trueExpression": {"kind": "stringLiteral", "evalType": 1, "value": "yes"},
            "falseExpression": {"kind": "stringLiteral", "evalType": 1, "value": "no"}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTernaryExpression>(result)
        
        val condition = result.condition
        assertIs<TypedTypeCheckExpression>(condition)
    }
    
    @Test
    fun `deserialize type check combined with type cast`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {
                "kind": "typeCast",
                "evalType": 1,
                "expression": {"kind": "identifier", "evalType": 2, "name": "x", "scope": 2},
                "targetType": 1,
                "isSafe": true
            },
            "checkType": 3,
            "isNegated": true
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        assertTrue(result.isNegated)
        
        val inner = result.expression
        assertIs<TypedTypeCastExpression>(inner)
    }
    
    @Test
    fun `deserialize type check with different check types`() {
        val jsonString = """{
            "kind": "typeCheck",
            "evalType": 0,
            "expression": {"kind": "doubleLiteral", "evalType": 1, "value": "3.14"},
            "checkType": 5,
            "isNegated": false
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedTypeCheckExpression>(result)
        assertEquals(5, result.checkType)
        
        val inner = result.expression
        assertIs<TypedDoubleLiteralExpression>(inner)
    }
}
