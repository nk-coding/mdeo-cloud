package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for integer literal expressions.
 */
class IntLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return int literal zero`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(0, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `return int literal positive small`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(42, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `return int literal negative`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(-1, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-1, result)
    }
    
    @Test
    fun `return int literal negative large`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(-12345, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-12345, result)
    }
    
    @Test
    fun `return int literal ICONST range 1 to 5`() {
        for (i in 1..5) {
            val ast = buildTypedAst {
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(returnStmt(intLiteral(i, intType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(i, result)
        }
    }
    
    @Test
    fun `return int literal BIPUSH range`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(100, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100, result)
    }
    
    @Test
    fun `return int literal SIPUSH range`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(1000, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1000, result)
    }
    
    @Test
    fun `return int literal LDC range`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(100000, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100000, result)
    }
    
    @Test
    fun `return int literal MAX_VALUE`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(Int.MAX_VALUE, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MAX_VALUE, result)
    }
    
    @Test
    fun `return int literal MIN_VALUE`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(Int.MIN_VALUE, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MIN_VALUE, result)
    }
    
    @Test
    fun `return int literal byte boundary positive`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(127, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(127, result)
    }
    
    @Test
    fun `return int literal byte boundary negative`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(-128, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-128, result)
    }
    
    @Test
    fun `return int literal short boundary positive`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(32767, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(32767, result)
    }
    
    @Test
    fun `return int literal short boundary negative`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(-32768, intType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-32768, result)
    }
}
