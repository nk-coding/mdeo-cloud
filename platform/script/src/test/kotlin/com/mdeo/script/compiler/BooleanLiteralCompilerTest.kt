package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for boolean literal expressions.
 */
class BooleanLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return boolean literal true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(true, boolType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertTrue(result as Boolean)
    }
    
    @Test
    fun `return boolean literal false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(false, boolType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertFalse(result as Boolean)
    }
    
    @Test
    fun `return boolean true equals true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(true, boolType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `return boolean false equals false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(false, boolType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `boolean true and false are distinct`() {
        val astTrue = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(true, boolType)))
            )
        }
        
        val astFalse = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(false, boolType)))
            )
        }
        
        val resultTrue = helper.compileAndInvoke(astTrue)
        val resultFalse = helper.compileAndInvoke(astFalse)
        
        assertTrue(resultTrue as Boolean)
        assertFalse(resultFalse as Boolean)
        assertTrue(resultTrue != resultFalse)
    }
}
