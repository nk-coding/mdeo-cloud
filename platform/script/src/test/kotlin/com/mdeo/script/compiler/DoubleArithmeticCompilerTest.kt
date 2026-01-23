package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for binary arithmetic expressions with double operands.
 */
class DoubleArithmeticCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Addition Tests ====================
    
    @Test
    fun `add two positive doubles`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.5, doubleType),
                            "+",
                            doubleLiteral(2.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(4.0, result)
    }
    
    @Test
    fun `add double with high precision`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(0.1, doubleType),
                            "+",
                            doubleLiteral(0.2, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Double
        assertEquals(0.3, result, 0.0000001)
    }
    
    @Test
    fun `add very large doubles`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1e100, doubleType),
                            "+",
                            doubleLiteral(1e100, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2e100, result)
    }
    
    @Test
    fun `add very small doubles`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1e-100, doubleType),
                            "+",
                            doubleLiteral(1e-100, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2e-100, result)
    }
    
    // ==================== Subtraction Tests ====================
    
    @Test
    fun `subtract positive doubles`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(5.5, doubleType),
                            "-",
                            doubleLiteral(2.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.0, result)
    }
    
    @Test
    fun `subtract with very small difference`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.0000001, doubleType),
                            "-",
                            doubleLiteral(1.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Double
        assertEquals(0.0000001, result, 1e-10)
    }
    
    // ==================== Multiplication Tests ====================
    
    @Test
    fun `multiply positive doubles`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(2.5, doubleType),
                            "*",
                            doubleLiteral(4.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.0, result)
    }
    
    @Test
    fun `multiply to get very large result`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1e100, doubleType),
                            "*",
                            doubleLiteral(1e100, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1e200, result)
    }
    
    @Test
    fun `multiply by zero`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(3.14159, doubleType),
                            "*",
                            doubleLiteral(0.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0, result)
    }
    
    // ==================== Division Tests ====================
    
    @Test
    fun `divide doubles evenly`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(10.0, doubleType),
                            "/",
                            doubleLiteral(4.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.5, result)
    }
    
    @Test
    fun `divide by zero returns infinity`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.0, doubleType),
                            "/",
                            doubleLiteral(0.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.POSITIVE_INFINITY, result)
    }
    
    @Test
    fun `negative divide by zero returns negative infinity`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(-1.0, doubleType),
                            "/",
                            doubleLiteral(0.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.NEGATIVE_INFINITY, result)
    }
    
    @Test
    fun `zero divide by zero returns NaN`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(0.0, doubleType),
                            "/",
                            doubleLiteral(0.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Double
        assertEquals(true, result.isNaN())
    }
    
    // ==================== Modulo Tests ====================
    
    @Test
    fun `modulo double with no remainder`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(6.0, doubleType),
                            "%",
                            doubleLiteral(2.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0, result)
    }
    
    @Test
    fun `modulo double with remainder`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(7.5, doubleType),
                            "%",
                            doubleLiteral(2.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1.5, result)
    }
    
    // ==================== Special Values ====================
    
    @Test
    fun `add with positive infinity`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            "+",
                            doubleLiteral(1.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Double.POSITIVE_INFINITY, result)
    }
    
    @Test
    fun `subtract infinity from infinity returns NaN`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            "-",
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Double
        assertEquals(true, result.isNaN())
    }
}
