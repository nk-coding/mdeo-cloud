package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for binary arithmetic expressions with float operands.
 */
class FloatArithmeticCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Addition Tests ====================
    
    @Test
    fun `add two positive floats`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.5f, floatType),
                            "+",
                            floatLiteral(2.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(4.0f, result)
    }
    
    @Test
    fun `add float with decimal result`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.1f, floatType),
                            "+",
                            floatLiteral(2.2f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(3.3f, result, 0.0001f)
    }
    
    @Test
    fun `add negative floats`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(-1.5f, floatType),
                            "+",
                            floatLiteral(-2.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-4.0f, result)
    }
    
    @Test
    fun `add float with zero`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(3.14f, floatType),
                            "+",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    // ==================== Subtraction Tests ====================
    
    @Test
    fun `subtract positive floats`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(5.5f, floatType),
                            "-",
                            floatLiteral(2.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.0f, result)
    }
    
    @Test
    fun `subtract to get negative result`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.5f, floatType),
                            "-",
                            floatLiteral(5.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-3.0f, result)
    }
    
    // ==================== Multiplication Tests ====================
    
    @Test
    fun `multiply positive floats`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.0f, floatType),
                            "*",
                            floatLiteral(3.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7.0f, result)
    }
    
    @Test
    fun `multiply by zero`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(3.14f, floatType),
                            "*",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0f, result)
    }
    
    @Test
    fun `multiply positive by negative`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.0f, floatType),
                            "*",
                            floatLiteral(-3.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-6.0f, result)
    }
    
    // ==================== Division Tests ====================
    
    @Test
    fun `divide floats evenly`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(6.0f, floatType),
                            "/",
                            floatLiteral(2.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.0f, result)
    }
    
    @Test
    fun `divide floats with decimal result`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(7.0f, floatType),
                            "/",
                            floatLiteral(2.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.5f, result)
    }
    
    @Test
    fun `divide by zero returns infinity`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.0f, floatType),
                            "/",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.POSITIVE_INFINITY, result)
    }
    
    @Test
    fun `negative divide by zero returns negative infinity`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(-1.0f, floatType),
                            "/",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.NEGATIVE_INFINITY, result)
    }
    
    @Test
    fun `zero divide by zero returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(0.0f, floatType),
                            "/",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN())
    }
    
    // ==================== Modulo Tests ====================
    
    @Test
    fun `modulo float with no remainder`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(6.0f, floatType),
                            "%",
                            floatLiteral(2.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0f, result)
    }
    
    @Test
    fun `modulo float with remainder`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(7.5f, floatType),
                            "%",
                            floatLiteral(2.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1.5f, result)
    }
    
    // ==================== NaN Arithmetic Tests ====================
    // Any arithmetic operation with NaN results in NaN
    
    @Test
    fun `add NaN to number returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            "+",
                            floatLiteral(5.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "NaN + 5.0f should be NaN")
    }
    
    @Test
    fun `subtract NaN from number returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(5.0f, floatType),
                            "-",
                            floatLiteral(Float.NaN, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "5.0f - NaN should be NaN")
    }
    
    @Test
    fun `multiply NaN by number returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            "*",
                            floatLiteral(5.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "NaN * 5.0f should be NaN")
    }
    
    @Test
    fun `divide NaN by number returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            "/",
                            floatLiteral(5.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "NaN / 5.0f should be NaN")
    }
    
    // ==================== Infinity Arithmetic Tests ====================
    
    @Test
    fun `infinity minus infinity returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            "-",
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "Infinity - Infinity should be NaN")
    }
    
    @Test
    fun `infinity divided by infinity returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            "/",
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "Infinity / Infinity should be NaN")
    }
    
    @Test
    fun `infinity times zero returns NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            "*",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN(), "Infinity * 0 should be NaN")
    }
    
    @Test
    fun `number plus infinity returns infinity`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(5.0f, floatType),
                            "+",
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Float.POSITIVE_INFINITY, result, "5.0f + Infinity should be Infinity")
    }
}
