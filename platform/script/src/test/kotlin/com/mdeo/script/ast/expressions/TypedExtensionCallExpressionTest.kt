package com.mdeo.script.ast.expressions

import com.mdeo.script.ast.TypedExpressionKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedExtensionCallExpression.
 */
class TypedExtensionCallExpressionTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize simple extension call`() {
        val jsonString = """{
            "kind": "extensionCall",
            "evalType": 0,
            "name": "customExtension",
            "arguments": [],
            "overload": ""
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExtensionCallExpression>(result)
        assertEquals(TypedExpressionKind.ExtensionCall, result.kind)
        assertEquals(0, result.evalType)
        assertEquals("customExtension", result.name)
        assertEquals("", result.overload)
        assertEquals(0, result.arguments.size)
    }
    
    @Test
    fun `deserialize extension call with arguments`() {
        val jsonString = """{
            "kind": "extensionCall",
            "evalType": 0,
            "name": "myExtension",
            "arguments": [
                {"name": "param1", "value": {"kind": "intLiteral", "evalType": 0, "value": "10"}},
                {"name": "param2", "value": {"kind": "stringLiteral", "evalType": 1, "value": "test"}}
            ],
            "overload": "main"
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExtensionCallExpression>(result)
        assertEquals("myExtension", result.name)
        assertEquals("main", result.overload)
        assertEquals(2, result.arguments.size)
        
        val arg1 = result.arguments[0]
        assertEquals("param1", arg1.name)
        assertIs<TypedIntLiteralExpression>(arg1.value)
        
        val arg2 = result.arguments[1]
        assertEquals("param2", arg2.name)
        assertIs<TypedStringLiteralExpression>(arg2.value)
    }
    
    @Test
    fun `deserialize extension call with duplicate argument names`() {
        val jsonString = """{
            "kind": "extensionCall",
            "evalType": 0,
            "name": "listExtension",
            "arguments": [
                {"name": "item", "value": {"kind": "intLiteral", "evalType": 0, "value": "1"}},
                {"name": "item", "value": {"kind": "intLiteral", "evalType": 0, "value": "2"}},
                {"name": "item", "value": {"kind": "intLiteral", "evalType": 0, "value": "3"}}
            ],
            "overload": ""
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExtensionCallExpression>(result)
        assertEquals(3, result.arguments.size)
        
        assertEquals("item", result.arguments[0].name)
        assertEquals("item", result.arguments[1].name)
        assertEquals("item", result.arguments[2].name)
    }
    
    @Test
    fun `deserialize extension call with complex argument values`() {
        val jsonString = """{
            "kind": "extensionCall",
            "evalType": 0,
            "name": "complexExtension",
            "arguments": [
                {
                    "name": "expr",
                    "value": {
                        "kind": "binary",
                        "evalType": 0,
                        "operator": "+",
                        "left": {"kind": "intLiteral", "evalType": 0, "value": "1"},
                        "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
                    }
                }
            ],
            "overload": ""
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExtensionCallExpression>(result)
        
        val arg = result.arguments[0]
        assertEquals("expr", arg.name)
        assertIs<TypedBinaryExpression>(arg.value)
    }
}
