package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for binary arithmetic expressions with long operands.
 */
class LongArithmeticCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Addition Tests ====================
    
    @Test
    fun `add two positive longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(5L, longType),
                            "+",
                            longLiteral(3L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(8L, result)
    }
    
    @Test
    fun `add large longs beyond int range`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(3_000_000_000L, longType),
                            "+",
                            longLiteral(2_000_000_000L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5_000_000_000L, result)
    }
    
    @Test
    fun `add negative longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(-5L, longType),
                            "+",
                            longLiteral(-3L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-8L, result)
    }
    
    @Test
    fun `add long overflow wraps around`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(Long.MAX_VALUE, longType),
                            "+",
                            longLiteral(1L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Long.MIN_VALUE, result)
    }
    
    // ==================== Subtraction Tests ====================
    
    @Test
    fun `subtract positive longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            "-",
                            longLiteral(3L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7L, result)
    }
    
    @Test
    fun `subtract large longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(5_000_000_000L, longType),
                            "-",
                            longLiteral(3_000_000_000L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2_000_000_000L, result)
    }
    
    @Test
    fun `subtract long underflow wraps around`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(Long.MIN_VALUE, longType),
                            "-",
                            longLiteral(1L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Long.MAX_VALUE, result)
    }
    
    // ==================== Multiplication Tests ====================
    
    @Test
    fun `multiply positive longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(6L, longType),
                            "*",
                            longLiteral(7L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    @Test
    fun `multiply large longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(1_000_000L, longType),
                            "*",
                            longLiteral(1_000_000L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1_000_000_000_000L, result)
    }
    
    @Test
    fun `multiply by zero`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(42L, longType),
                            "*",
                            longLiteral(0L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0L, result)
    }
    
    // ==================== Division Tests ====================
    
    @Test
    fun `divide longs evenly`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(42L, longType),
                            "/",
                            longLiteral(6L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7L, result)
    }
    
    @Test
    fun `divide large longs`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(1_000_000_000_000L, longType),
                            "/",
                            longLiteral(1_000_000L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1_000_000L, result)
    }
    
    @Test
    fun `divide with remainder truncates`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            "/",
                            longLiteral(3L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3L, result)
    }
    
    // ==================== Modulo Tests ====================
    
    @Test
    fun `modulo long with no remainder`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(42L, longType),
                            "%",
                            longLiteral(6L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0L, result)
    }
    
    @Test
    fun `modulo long with remainder`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            "%",
                            longLiteral(3L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1L, result)
    }
    
    @Test
    fun `modulo large long`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(1_000_000_000_001L, longType),
                            "%",
                            longLiteral(1_000_000_000_000L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1L, result)
    }
    
    // ==================== Division by Zero Tests ====================
    // Long division by zero throws ArithmeticException in Java
    // Note: When invoked via reflection, ArithmeticException is wrapped in InvocationTargetException
    
    @Test
    fun `long division by zero throws ArithmeticException`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(42L, longType),
                            "/",
                            longLiteral(0L, longType),
                            longType
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
    fun `long modulo by zero throws ArithmeticException`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(42L, longType),
                            "%",
                            longLiteral(0L, longType),
                            longType
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
