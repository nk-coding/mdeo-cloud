package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for double literal expressions.
 */
class DoubleLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return double literal zero`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(0.0, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0, result)
    }
    
    @Test
    fun `return double literal one`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(1.0, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1.0, result)
    }
    
    @Test
    fun `return double literal positive`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(3.14159265359, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14159265359, result)
    }
    
    @Test
    fun `return double literal negative`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(-2.71828, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-2.71828, result)
    }
    
    @Test
    fun `return double literal very small`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(0.000000001, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.000000001, result)
    }
    
    @Test
    fun `return double literal very large`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(1e100, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1e100, result)
    }
    
    @Test
    fun `return double literal MAX_VALUE`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(Double.MAX_VALUE, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.MAX_VALUE, result)
    }
    
    @Test
    fun `return double literal MIN_VALUE`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(Double.MIN_VALUE, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.MIN_VALUE, result)
    }
    
    @Test
    fun `return double literal negative MAX_VALUE`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(-Double.MAX_VALUE, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-Double.MAX_VALUE, result)
    }
    
    @Test
    fun `return double literal precise decimal`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(123.456789012345, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(123.456789012345, result)
    }
}
