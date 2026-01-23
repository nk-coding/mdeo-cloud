package com.mdeo.script.ast

import com.mdeo.script.ast.statements.TypedReturnStatement
import com.mdeo.script.ast.expressions.TypedIntLiteralExpression
import com.mdeo.script.ast.types.ClassTypeRef
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing core AST types.
 */
class TypedAstTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize TypedParameter`() {
        val jsonString = """{"name": "x", "type": 0}"""
        val result = json.decodeFromString(TypedParameter.serializer(), jsonString)
        
        assertEquals("x", result.name)
        assertEquals(0, result.type)
    }
    
    @Test
    fun `deserialize TypedImport`() {
        val jsonString = """{
            "name": "myFunc",
            "ref": "originalFunc",
            "uri": "file:///path/to/file.ms"
        }"""
        val result = json.decodeFromString(TypedImport.serializer(), jsonString)
        
        assertEquals("myFunc", result.name)
        assertEquals("originalFunc", result.ref)
        assertEquals("file:///path/to/file.ms", result.uri)
    }
    
    @Test
    fun `deserialize TypedCallableBody`() {
        val jsonString = """{
            "body": [
                {"kind": "return", "value": {"kind": "intLiteral", "evalType": 0, "value": "42"}}
            ]
        }"""
        val result = json.decodeFromString(TypedCallableBody.serializer(), jsonString)
        
        assertEquals(1, result.body.size)
        val stmt = result.body[0]
        assertIs<TypedReturnStatement>(stmt)
        
        val expr = stmt.value
        assertIs<TypedIntLiteralExpression>(expr)
        assertEquals("42", expr.value)
    }
    
    @Test
    fun `deserialize TypedCallableBody with empty body`() {
        val jsonString = """{"body": []}"""
        val result = json.decodeFromString(TypedCallableBody.serializer(), jsonString)
        
        assertEquals(0, result.body.size)
    }
    
    @Test
    fun `deserialize TypedFunction`() {
        val jsonString = """{
            "name": "add",
            "parameters": [
                {"name": "a", "type": 0},
                {"name": "b", "type": 0}
            ],
            "returnType": 0,
            "body": {
                "body": [
                    {"kind": "return", "value": {"kind": "intLiteral", "evalType": 0, "value": "0"}}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedFunction.serializer(), jsonString)
        
        assertEquals("add", result.name)
        assertEquals(2, result.parameters.size)
        assertEquals("a", result.parameters[0].name)
        assertEquals("b", result.parameters[1].name)
        assertEquals(0, result.returnType)
        assertEquals(1, result.body.body.size)
    }
    
    @Test
    fun `deserialize TypedFunction with no parameters`() {
        val jsonString = """{
            "name": "getZero",
            "parameters": [],
            "returnType": 0,
            "body": {
                "body": [
                    {"kind": "return", "value": {"kind": "intLiteral", "evalType": 0, "value": "0"}}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedFunction.serializer(), jsonString)
        
        assertEquals("getZero", result.name)
        assertEquals(0, result.parameters.size)
    }
    
    @Test
    fun `deserialize complete TypedAst`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.int", "isNullable": false},
                {"type": "builtin.string", "isNullable": false}
            ],
            "imports": [
                {"name": "log", "ref": "console_log", "uri": "file:///stdlib.ms"}
            ],
            "functions": [
                {
                    "name": "main",
                    "parameters": [],
                    "returnType": 0,
                    "body": {
                        "body": [
                            {"kind": "return", "value": {"kind": "intLiteral", "evalType": 0, "value": "0"}}
                        ]
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(2, result.types.size)
        assertIs<ClassTypeRef>(result.types[0])
        assertEquals("builtin.int", (result.types[0] as ClassTypeRef).type)
        
        assertEquals(1, result.imports.size)
        assertEquals("log", result.imports[0].name)
        
        assertEquals(1, result.functions.size)
        assertEquals("main", result.functions[0].name)
    }
    
    @Test
    fun `deserialize TypedAst with void return type`() {
        val jsonString = """{
            "types": [
                {"kind": "void"}
            ],
            "imports": [],
            "functions": [
                {
                    "name": "noReturn",
                    "parameters": [],
                    "returnType": 0,
                    "body": {"body": []}
                }
            ]
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertIs<com.mdeo.script.ast.types.VoidType>(result.types[0])
    }
    
    @Test
    fun `deserialize TypedAst with lambda type`() {
        val jsonString = """{
            "types": [
                {
                    "returnType": {"type": "builtin.int", "isNullable": false},
                    "parameters": [
                        {"name": "x", "type": {"type": "builtin.int", "isNullable": false}}
                    ],
                    "isNullable": false
                }
            ],
            "imports": [],
            "functions": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertIs<com.mdeo.script.ast.types.LambdaType>(result.types[0])
    }
}
