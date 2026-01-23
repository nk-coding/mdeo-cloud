package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for string literal edge cases.
 */
class StringEdgeCasesTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `very long string literal`() {
        val longString = "a".repeat(10000)
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
        assertEquals(10000, (result as String).length)
    }
    
    @Test
    fun `string with null character`() {
        val stringWithNull = "hello\u0000world"
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral(stringWithNull, stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(stringWithNull, result)
        assertEquals(11, (result as String).length) // "hello" + null + "world" = 11 chars
    }
    
    @Test
    fun `string with various unicode characters`() {
        val unicodeString = "αβγδε日本語한국어🎉🎊🎄"
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral(unicodeString, stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(unicodeString, result)
    }
    
    @Test
    fun `string with all printable ASCII characters`() {
        val asciiString = (32..126).map { it.toChar() }.joinToString("")
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral(asciiString, stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(asciiString, result)
    }
}
