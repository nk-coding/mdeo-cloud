package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for type promotion in arithmetic expressions.
 * Verifies that operands are correctly promoted following Java's binary numeric promotion rules:
 * 1. If either operand is double, the result is double
 * 2. Otherwise, if either operand is float, the result is float
 * 3. Otherwise, if either operand is long, the result is long
 * 4. Otherwise, the result is int
 */
class TypePromotionCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Int + Long -> Long ====================
    
    @Test
    fun `int plus long returns long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "+",
                            longLiteral(20L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30L, result)
    }
    
    @Test
    fun `long plus int returns long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(20L, longType),
                            "+",
                            intLiteral(10, intType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30L, result)
    }
    
    @Test
    fun `int minus long returns long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(50, intType),
                            "-",
                            longLiteral(20L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30L, result)
    }
    
    @Test
    fun `int times long returns long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(6, intType),
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
    fun `int divide long returns long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(42, intType),
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
    
    // ==================== Int + Float -> Float ====================
    
    @Test
    fun `int plus float returns float`() {
        val ast = buildTypedAst {
            val intType = intType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "+",
                            floatLiteral(2.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(12.5f, result)
    }
    
    @Test
    fun `float minus int returns float`() {
        val ast = buildTypedAst {
            val intType = intType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(15.5f, floatType),
                            "-",
                            intLiteral(5, intType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.5f, result)
    }
    
    @Test
    fun `int times float returns float`() {
        val ast = buildTypedAst {
            val intType = intType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(3, intType),
                            "*",
                            floatLiteral(2.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(7.5f, result)
    }
    
    // ==================== Int + Double -> Double ====================
    
    @Test
    fun `int plus double returns double`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "+",
                            doubleLiteral(2.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(12.5, result)
    }
    
    @Test
    fun `double minus int returns double`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(15.5, doubleType),
                            "-",
                            intLiteral(5, intType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.5, result)
    }
    
    // ==================== Long + Float -> Float ====================
    
    @Test
    fun `long plus float returns float`() {
        val ast = buildTypedAst {
            val longType = longType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            "+",
                            floatLiteral(2.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(12.5f, result)
    }
    
    @Test
    fun `float times long returns float`() {
        val ast = buildTypedAst {
            val longType = longType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.5f, floatType),
                            "*",
                            longLiteral(4L, longType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.0f, result)
    }
    
    // ==================== Long + Double -> Double ====================
    
    @Test
    fun `long plus double returns double`() {
        val ast = buildTypedAst {
            val longType = longType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            "+",
                            doubleLiteral(2.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(12.5, result)
    }
    
    @Test
    fun `double divide long returns double`() {
        val ast = buildTypedAst {
            val longType = longType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(21.0, doubleType),
                            "/",
                            longLiteral(2L, longType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.5, result)
    }
    
    // ==================== Float + Double -> Double ====================
    
    @Test
    fun `float plus double returns double`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.5f, floatType),
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
    fun `double minus float returns double`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(10.5, doubleType),
                            "-",
                            floatLiteral(2.5f, floatType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(8.0, result)
    }
    
    // ==================== Complex Chained Promotions ====================
    
    @Test
    fun `int plus long plus float returns float`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                intLiteral(1, intType),
                                "+",
                                longLiteral(2L, longType),
                                longType
                            ),
                            "+",
                            floatLiteral(3.5f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6.5f, result)
    }
    
    @Test
    fun `int times long plus double returns double`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                intLiteral(2, intType),
                                "*",
                                longLiteral(3L, longType),
                                longType
                            ),
                            "+",
                            doubleLiteral(0.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6.5, result)
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    fun `int to long promotion preserves value beyond int range`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(Int.MAX_VALUE, intType),
                            "+",
                            longLiteral(1L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MAX_VALUE.toLong() + 1L, result)
    }
    
    @Test
    fun `modulo with type promotion`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
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
    fun `division with int and double preserves precision`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(7, intType),
                            "/",
                            doubleLiteral(2.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.5, result)
    }
}
