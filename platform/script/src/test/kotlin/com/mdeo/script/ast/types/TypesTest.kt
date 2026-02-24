package com.mdeo.script.ast.types

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import com.mdeo.expression.ast.types.Parameter
import com.mdeo.expression.ast.types.ReturnTypeSerializer
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.GenericTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.expression.ast.types.VoidType

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for deserializing type definitions.
 */
class TypesTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
        }
    }
    
    @Test
    fun `deserialize VoidType`() {
        val jsonString = """{"kind": "void"}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<VoidType>(result)
        assertEquals("void", result.kind)
    }
    
    @Test
    fun `deserialize ClassTypeRef with nullable`() {
        val jsonString = """{"package": "builtin", "type": "string", "isNullable": true}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("builtin", result.`package`)
        assertEquals("string", result.type)
        assertEquals(true, result.isNullable)
        assertNull(result.typeArgs)
    }
    
    @Test
    fun `deserialize ClassTypeRef with type arguments`() {
        val jsonString = """{
            "package": "builtin",
            "type": "List",
            "isNullable": false,
            "typeArgs": {
                "T": {"package": "builtin", "type": "string", "isNullable": false}
            }
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("builtin", result.`package`)
        assertEquals("List", result.type)
        assertEquals(false, result.isNullable)
        assertEquals(1, result.typeArgs?.size)
        
        val typeArg = result.typeArgs?.get("T")
        assertIs<ClassTypeRef>(typeArg)
        assertEquals("string", typeArg.type)
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
            "returnType": {"package": "builtin", "type": "int", "isNullable": false},
            "parameters": [
                {"name": "x", "type": {"package": "builtin", "type": "int", "isNullable": false}},
                {"name": "y", "type": {"package": "builtin", "type": "int", "isNullable": false}}
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
        assertEquals("int", returnType.type)
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
            "type": {"package": "builtin", "type": "double", "isNullable": false}
        }"""
        val result = json.decodeFromString(Parameter.serializer(), jsonString)
        
        assertEquals("param1", result.name)
        val paramType = result.type
        assertIs<ClassTypeRef>(paramType)
        assertEquals("double", paramType.type)
    }
    
    @Test
    fun `deserialize nested type arguments`() {
        val jsonString = """{
            "package": "builtin",
            "type": "map",
            "isNullable": false,
            "typeArgs": {
                "K": {"package": "builtin", "type": "string", "isNullable": false},
                "V": {"package": "builtin", "type": "List", "isNullable": false, "typeArgs": {"T": {"package": "builtin", "type": "int", "isNullable": false}}}
            }
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("map", result.type)
        assertEquals(2, result.typeArgs?.size)
        
        val kType = result.typeArgs?.get("K")
        assertIs<ClassTypeRef>(kType)
        assertEquals("string", kType.type)
        
        val vType = result.typeArgs?.get("V")
        assertIs<ClassTypeRef>(vType)
        assertEquals("List", vType.type)
        
        val innerT = vType.typeArgs?.get("T")
        assertIs<ClassTypeRef>(innerT)
        assertEquals("int", innerT.type)
    }
}
