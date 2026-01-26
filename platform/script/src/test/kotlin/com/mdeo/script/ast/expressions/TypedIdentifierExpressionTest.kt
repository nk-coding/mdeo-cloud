package com.mdeo.script.ast.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer

import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedIdentifierExpression.
 */
class TypedIdentifierExpressionTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize simple identifier`() {
        val jsonString = """{
            "kind": "identifier",
            "evalType": 0,
            "name": "myVariable",
            "scope": 2
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals("identifier", result.kind)
        assertEquals(0, result.evalType)
        assertEquals("myVariable", result.name)
        assertEquals(2, result.scope)
    }
    
    @Test
    fun `deserialize identifier in global scope`() {
        val jsonString = """{
            "kind": "identifier",
            "evalType": 0,
            "name": "globalVar",
            "scope": 0
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals("globalVar", result.name)
        assertEquals(0, result.scope)
    }
    
    @Test
    fun `deserialize identifier in file scope`() {
        val jsonString = """{
            "kind": "identifier",
            "evalType": 0,
            "name": "fileVar",
            "scope": 1
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals("fileVar", result.name)
        assertEquals(1, result.scope)
    }
    
    @Test
    fun `deserialize identifier in function parameter scope`() {
        val jsonString = """{
            "kind": "identifier",
            "evalType": 0,
            "name": "param",
            "scope": 2
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals("param", result.name)
        assertEquals(2, result.scope)
    }
    
    @Test
    fun `deserialize identifier in function body scope`() {
        val jsonString = """{
            "kind": "identifier",
            "evalType": 0,
            "name": "localVar",
            "scope": 3
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals("localVar", result.name)
        assertEquals(3, result.scope)
    }
    
    @Test
    fun `deserialize identifier in deeply nested scope`() {
        val jsonString = """{
            "kind": "identifier",
            "evalType": 0,
            "name": "deepVar",
            "scope": 10
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals("deepVar", result.name)
        assertEquals(10, result.scope)
    }
}
