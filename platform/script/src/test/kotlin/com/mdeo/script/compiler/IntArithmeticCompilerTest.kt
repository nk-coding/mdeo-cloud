package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for binary arithmetic expressions with integer operands.
 */
class IntArithmeticCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Addition Tests ====================
    
    @Test
    fun `add two positive integers`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "+",
                            intLiteral(3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(8, result)
    }
    
    @Test
    fun `add positive and negative integer`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "+",
                            intLiteral(-3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7, result)
    }
    
    @Test
    fun `add two negative integers`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(-5, intType),
                            "+",
                            intLiteral(-3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-8, result)
    }
    
    @Test
    fun `add zero to integer`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "+",
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `add integer overflow wraps around`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(Int.MAX_VALUE, intType),
                            "+",
                            intLiteral(1, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MIN_VALUE, result)
    }
    
    // ==================== Subtraction Tests ====================
    
    @Test
    fun `subtract positive integers`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "-",
                            intLiteral(3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7, result)
    }
    
    @Test
    fun `subtract to get negative result`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(3, intType),
                            "-",
                            intLiteral(10, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-7, result)
    }
    
    @Test
    fun `subtract negative number adds`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "-",
                            intLiteral(-3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(8, result)
    }
    
    @Test
    fun `subtract zero`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "-",
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `subtract integer underflow wraps around`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(Int.MIN_VALUE, intType),
                            "-",
                            intLiteral(1, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MAX_VALUE, result)
    }
    
    // ==================== Multiplication Tests ====================
    
    @Test
    fun `multiply positive integers`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(6, intType),
                            "*",
                            intLiteral(7, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `multiply by zero`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "*",
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `multiply by one`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "*",
                            intLiteral(1, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `multiply positive by negative`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(6, intType),
                            "*",
                            intLiteral(-7, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-42, result)
    }
    
    @Test
    fun `multiply two negatives`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(-6, intType),
                            "*",
                            intLiteral(-7, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    // ==================== Division Tests ====================
    
    @Test
    fun `divide evenly`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "/",
                            intLiteral(6, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7, result)
    }
    
    @Test
    fun `divide with remainder truncates`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "/",
                            intLiteral(3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3, result)
    }
    
    @Test
    fun `divide by one`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "/",
                            intLiteral(1, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `divide negative by positive`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(-42, intType),
                            "/",
                            intLiteral(6, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-7, result)
    }
    
    @Test
    fun `divide zero by nonzero`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(0, intType),
                            "/",
                            intLiteral(42, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    // ==================== Modulo Tests ====================
    
    @Test
    fun `modulo with no remainder`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "%",
                            intLiteral(6, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `modulo with remainder`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "%",
                            intLiteral(3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `modulo of negative number`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(-10, intType),
                            "%",
                            intLiteral(3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-1, result)
    }
    
    @Test
    fun `modulo by negative number`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "%",
                            intLiteral(-3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `modulo zero by nonzero`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(0, intType),
                            "%",
                            intLiteral(42, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    // ==================== Chained Operations ====================
    
    @Test
    fun `chained addition`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                intLiteral(1, intType),
                                "+",
                                intLiteral(2, intType),
                                intType
                            ),
                            "+",
                            intLiteral(3, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)
    }
    
    @Test
    fun `mixed operations`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                intLiteral(10, intType),
                                "*",
                                intLiteral(2, intType),
                                intType
                            ),
                            "+",
                            intLiteral(5, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(25, result)
    }
    
    // ==================== Division by Zero Tests ====================
    // Integer division by zero throws ArithmeticException in Java
    // Note: When invoked via reflection, ArithmeticException is wrapped in InvocationTargetException
    
    @Test
    fun `int division by zero throws ArithmeticException`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "/",
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val exception = org.junit.jupiter.api.assertThrows<Throwable> {
            helper.compileAndInvoke(ast)
        }
        // ArithmeticException may be wrapped in InvocationTargetException
        val rootCause = generateSequence(exception) { it.cause }.last()
        assertTrue(rootCause is ArithmeticException, 
            "Expected ArithmeticException but got ${rootCause::class.simpleName}")
    }
    
    @Test
    fun `int modulo by zero throws ArithmeticException`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
                            "%",
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val exception = org.junit.jupiter.api.assertThrows<Throwable> {
            helper.compileAndInvoke(ast)
        }
        // ArithmeticException may be wrapped in InvocationTargetException
        val rootCause = generateSequence(exception) { it.cause }.last()
        assertTrue(rootCause is ArithmeticException, 
            "Expected ArithmeticException but got ${rootCause::class.simpleName}")
    }
}
