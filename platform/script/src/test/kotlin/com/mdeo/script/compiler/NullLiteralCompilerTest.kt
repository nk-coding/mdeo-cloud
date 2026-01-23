package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertNull

/**
 * Tests for null literal expressions.
 */
class NullLiteralCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `return null for nullable string`() {
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
    fun `return null for nullable int`() {
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
    fun `return null for nullable long`() {
        val ast = buildTypedAst {
            val nullableLongType = longNullableType()
            function(
                name = "testFunction",
                returnType = nullableLongType,
                body = listOf(returnStmt(nullLiteral(nullableLongType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `return null for nullable float`() {
        val ast = buildTypedAst {
            val nullableFloatType = floatNullableType()
            function(
                name = "testFunction",
                returnType = nullableFloatType,
                body = listOf(returnStmt(nullLiteral(nullableFloatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `return null for nullable double`() {
        val ast = buildTypedAst {
            val nullableDoubleType = doubleNullableType()
            function(
                name = "testFunction",
                returnType = nullableDoubleType,
                body = listOf(returnStmt(nullLiteral(nullableDoubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `return null for nullable boolean`() {
        val ast = buildTypedAst {
            val nullableBoolType = booleanNullableType()
            function(
                name = "testFunction",
                returnType = nullableBoolType,
                body = listOf(returnStmt(nullLiteral(nullableBoolType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `return null for any nullable type`() {
        val ast = buildTypedAst {
            val anyNullableType = anyNullableType()
            function(
                name = "testFunction",
                returnType = anyNullableType,
                body = listOf(returnStmt(nullLiteral(anyNullableType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
}
