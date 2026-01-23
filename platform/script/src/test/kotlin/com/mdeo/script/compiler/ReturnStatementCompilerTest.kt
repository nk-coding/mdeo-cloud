package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for return statements with various literal types.
 */
class ReturnStatementCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return void from void function`() {
        val ast = buildTypedAst {
            val voidType = voidType()
            function(
                name = "testFunction",
                returnType = voidType,
                body = listOf(returnVoid())
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `return int from int function`() {
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
    fun `return long from long function`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(123456789012L, longType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(123456789012L, result)
    }
    
    @Test
    fun `return float from float function`() {
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
    fun `return double from double function`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(2.718281828, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.718281828, result)
    }
    
    @Test
    fun `return boolean true from boolean function`() {
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
    fun `return boolean false from boolean function`() {
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
    fun `return string from string function`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("test result", stringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("test result", result)
    }
    
    @Test
    fun `return null from nullable string function`() {
        val ast = buildTypedAst {
            val nullableStringType = stringNullableType()
            function(
                name = "testFunction",
                returnType = nullableStringType,
                body = listOf(returnStmt(nullLiteral(nullableStringType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `return null from nullable int function`() {
        val ast = buildTypedAst {
            val nullableIntType = intNullableType()
            function(
                name = "testFunction",
                returnType = nullableIntType,
                body = listOf(returnStmt(nullLiteral(nullableIntType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `multiple functions can be compiled and invoked`() {
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            
            function(
                name = "getInt",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(100, intType)))
            )
            
            function(
                name = "getString",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello", stringType)))
            )
        }
        
        assertEquals(100, helper.compileAndInvoke(ast, "getInt"))
        assertEquals("hello", helper.compileAndInvoke(ast, "getString"))
    }
}
