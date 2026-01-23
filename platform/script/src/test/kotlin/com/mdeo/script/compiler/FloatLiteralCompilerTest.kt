package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for float literal expressions.
 */
class FloatLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return float literal zero`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(0.0f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0f, result)
    }
    
    @Test
    fun `return float literal one`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(1.0f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1.0f, result)
    }
    
    @Test
    fun `return float literal two`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(2.0f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.0f, result)
    }
    
    @Test
    fun `return float literal positive`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(3.14f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    @Test
    fun `return float literal negative`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(-2.5f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-2.5f, result)
    }
    
    @Test
    fun `return float literal very small`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(0.0001f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0001f, result)
    }
    
    @Test
    fun `return float literal very large`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(1e10f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1e10f, result)
    }
    
    @Test
    fun `return float literal MAX_VALUE`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(Float.MAX_VALUE, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.MAX_VALUE, result)
    }
    
    @Test
    fun `return float literal MIN_VALUE`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(Float.MIN_VALUE, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.MIN_VALUE, result)
    }
    
    @Test
    fun `return float literal negative MAX_VALUE`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(-Float.MAX_VALUE, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-Float.MAX_VALUE, result)
    }
}
