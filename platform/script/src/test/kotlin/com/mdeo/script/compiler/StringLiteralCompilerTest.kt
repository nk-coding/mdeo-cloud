package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for string literal expressions.
 */
class StringLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return empty string`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("", result)
    }
    
    @Test
    fun `return simple string`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello", result)
    }
    
    @Test
    fun `return string with spaces`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello world", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello world", result)
    }
    
    @Test
    fun `return string with special characters`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello\nworld\ttab", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello\nworld\ttab", result)
    }
    
    @Test
    fun `return string with unicode`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("Hello 世界 🌍", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("Hello 世界 🌍", result)
    }
    
    @Test
    fun `return string with numbers`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("12345", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("12345", result)
    }
    
    @Test
    fun `return string with quotes`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("say \"hello\"", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("say \"hello\"", result)
    }
    
    @Test
    fun `return long string`() {
        val longString = "a".repeat(1000)
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral(longString, stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(longString, result)
    }
    
    @Test
    fun `return string with backslash`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("path\\to\\file", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("path\\to\\file", result)
    }
    
    @Test
    fun `return single character string`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("x", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("x", result)
    }
}
