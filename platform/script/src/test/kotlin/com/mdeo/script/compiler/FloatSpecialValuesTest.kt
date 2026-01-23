package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for special floating-point values (NaN, Infinity, -0.0).
 * These are edge cases that may not be handled correctly by bytecode generation.
 */
class FloatSpecialValuesTest {
    
    private val helper = CompilerTestHelper()
    
    // Float special values tests
    
    @Test
    fun `return float NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(Float.NaN, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertTrue((result as Float).isNaN(), "Result should be NaN")
    }
    
    @Test
    fun `return float positive infinity`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(Float.POSITIVE_INFINITY, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.POSITIVE_INFINITY, result)
    }
    
    @Test
    fun `return float negative infinity`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(Float.NEGATIVE_INFINITY, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.NEGATIVE_INFINITY, result)
    }
    
    /**
     * BUG: FloatLiteralCompiler uses value comparison (==) which treats -0.0f as equal to 0.0f.
     * This causes -0.0f to be compiled as FCONST_0 (positive zero) instead of using LDC.
     * 
     * The fix is to use bit representation comparison in FloatLiteralCompiler:
     * java.lang.Float.floatToRawIntBits(value) == java.lang.Float.floatToRawIntBits(0.0f)
     */
    @Test
    fun `return float negative zero - BUG negative zero incorrectly becomes positive zero`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(-0.0f, floatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        // BUG: This assertion fails because -0.0f is incorrectly compiled as FCONST_0 (positive zero)
        // The expected bit representation is 0x80000000 (negative zero)
        // But the actual bit representation is 0x00000000 (positive zero)
        val expectedBits = java.lang.Float.floatToRawIntBits(-0.0f)
        val actualBits = java.lang.Float.floatToRawIntBits(result as Float)
        assertEquals(expectedBits, actualBits, 
            "Expected -0.0f (bits: ${expectedBits.toString(16)}) but got ${result} (bits: ${actualBits.toString(16)})")
    }
    
    // Double special values tests
    
    @Test
    fun `return double NaN`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(Double.NaN, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertTrue((result as Double).isNaN(), "Result should be NaN")
    }
    
    @Test
    fun `return double positive infinity`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(Double.POSITIVE_INFINITY, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.POSITIVE_INFINITY, result)
    }
    
    @Test
    fun `return double negative infinity`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(Double.NEGATIVE_INFINITY, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.NEGATIVE_INFINITY, result)
    }
    
    /**
     * BUG: DoubleLiteralCompiler uses value comparison (==) which treats -0.0 as equal to 0.0.
     * This causes -0.0 to be compiled as DCONST_0 (positive zero) instead of using LDC.
     * 
     * The fix is to use bit representation comparison in DoubleLiteralCompiler:
     * java.lang.Double.doubleToRawLongBits(value) == java.lang.Double.doubleToRawLongBits(0.0)
     */
    @Test
    fun `return double negative zero - BUG negative zero incorrectly becomes positive zero`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(-0.0, doubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        // BUG: This assertion fails because -0.0 is incorrectly compiled as DCONST_0 (positive zero)
        // The expected bit representation is 0x8000000000000000 (negative zero)
        // But the actual bit representation is 0x0000000000000000 (positive zero)
        val expectedBits = java.lang.Double.doubleToRawLongBits(-0.0)
        val actualBits = java.lang.Double.doubleToRawLongBits(result as Double)
        assertEquals(expectedBits, actualBits, 
            "Expected -0.0 (bits: ${expectedBits.toString(16)}) but got ${result} (bits: ${actualBits.toString(16)})")
    }
}
