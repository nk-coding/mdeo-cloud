package com.mdeo.script.ast.types

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deserializing type definitions.
 */
class TypesTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize VoidType`() {
        val jsonString = """{"kind": "void"}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<VoidType>(result)
        assertEquals("void", result.kind)
    }
    
    @Test
    fun `deserialize ClassTypeRef with nullable`() {
        val jsonString = """{"type": "builtin.string", "isNullable": true}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("builtin.string", result.type)
        assertEquals(true, result.isNullable)
        assertNull(result.typeArgs)
    }
    
    @Test
    fun `deserialize ClassTypeRef with type arguments`() {
        val jsonString = """{
            "type": "builtin.list",
            "isNullable": false,
            "typeArgs": {
                "T": {"type": "builtin.string", "isNullable": false}
            }
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("builtin.list", result.type)
        assertEquals(false, result.isNullable)
        assertEquals(1, result.typeArgs?.size)
        
        val typeArg = result.typeArgs?.get("T")
        assertIs<ClassTypeRef>(typeArg)
        assertEquals("builtin.string", typeArg.type)
    }
    
    @Test
    fun `deserialize GenericTypeRef`() {
        val jsonString = """{"generic": "T"}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<GenericTypeRef>(result)
        assertEquals("T", result.generic)
        assertNull(result.isNullable)
    }
    
    @Test
    fun `deserialize GenericTypeRef with nullable`() {
        val jsonString = """{"generic": "U", "isNullable": true}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<GenericTypeRef>(result)
        assertEquals("U", result.generic)
        assertEquals(true, result.isNullable)
    }
    
    @Test
    fun `deserialize LambdaType`() {
        val jsonString = """{
            "returnType": {"type": "builtin.int", "isNullable": false},
            "parameters": [
                {"name": "x", "type": {"type": "builtin.int", "isNullable": false}},
                {"name": "y", "type": {"type": "builtin.int", "isNullable": false}}
            ],
            "isNullable": false
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<LambdaType>(result)
        assertEquals(false, result.isNullable)
        assertEquals(2, result.parameters.size)
        assertEquals("x", result.parameters[0].name)
        assertEquals("y", result.parameters[1].name)
        
        val returnType = result.returnType
        assertIs<ClassTypeRef>(returnType)
        assertEquals("builtin.int", returnType.type)
    }
    
    @Test
    fun `deserialize LambdaType with void return`() {
        val jsonString = """{
            "returnType": {"kind": "void"},
            "parameters": [],
            "isNullable": true
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<LambdaType>(result)
        assertEquals(true, result.isNullable)
        assertEquals(0, result.parameters.size)
        
        val returnType = result.returnType
        assertIs<VoidType>(returnType)
    }
    
    @Test
    fun `deserialize Parameter`() {
        val jsonString = """{
            "name": "param1",
            "type": {"type": "builtin.double", "isNullable": false}
        }"""
        val result = json.decodeFromString(Parameter.serializer(), jsonString)
        
        assertEquals("param1", result.name)
        val paramType = result.type
        assertIs<ClassTypeRef>(paramType)
        assertEquals("builtin.double", paramType.type)
    }
    
    @Test
    fun `deserialize nested type arguments`() {
        val jsonString = """{
            "type": "builtin.map",
            "isNullable": false,
            "typeArgs": {
                "K": {"type": "builtin.string", "isNullable": false},
                "V": {"type": "builtin.list", "isNullable": false, "typeArgs": {"T": {"type": "builtin.int", "isNullable": false}}}
            }
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("builtin.map", result.type)
        assertEquals(2, result.typeArgs?.size)
        
        val kType = result.typeArgs?.get("K")
        assertIs<ClassTypeRef>(kType)
        assertEquals("builtin.string", kType.type)
        
        val vType = result.typeArgs?.get("V")
        assertIs<ClassTypeRef>(vType)
        assertEquals("builtin.list", vType.type)
        
        val innerT = vType.typeArgs?.get("T")
        assertIs<ClassTypeRef>(innerT)
        assertEquals("builtin.int", innerT.type)
    }
}
