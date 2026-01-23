package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for long literal expressions.
 */
class LongLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return long literal zero`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(0L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0L, result)
    }
    
    @Test
    fun `return long literal one`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(1L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1L, result)
    }
    
    @Test
    fun `return long literal positive`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(42L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    @Test
    fun `return long literal negative`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(-100L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-100L, result)
    }
    
    @Test
    fun `return long literal large positive`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(9876543210L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9876543210L, result)
    }
    
    @Test
    fun `return long literal large negative`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(-9876543210L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-9876543210L, result)
    }
    
    @Test
    fun `return long literal MAX_VALUE`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(Long.MAX_VALUE, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Long.MAX_VALUE, result)
    }
    
    @Test
    fun `return long literal MIN_VALUE`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(Long.MIN_VALUE, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Long.MIN_VALUE, result)
    }
    
    @Test
    fun `return long literal int max plus one`() {
        val value = Int.MAX_VALUE.toLong() + 1
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(value, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(value, result)
    }
    
    @Test
    fun `return long literal int min minus one`() {
        val value = Int.MIN_VALUE.toLong() - 1
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(value, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(value, result)
    }
}
