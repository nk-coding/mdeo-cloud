package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for unary expressions.
 * Tests unary minus (-) for all numeric types.
 * Tests unary not (!) for boolean.
 */
class UnaryExpressionCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Unary Minus with Int ====================
    
    @Test
    fun `negate positive int`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            intLiteral(5, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-5, result)
    }
    
    @Test
    fun `negate negative int returns positive`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            intLiteral(-5, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `negate zero int`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
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
    fun `negate int max value`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            intLiteral(Int.MAX_VALUE, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-Int.MAX_VALUE, result)
    }
    
    @Test
    fun `negate int min value overflows`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            intLiteral(Int.MIN_VALUE, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MIN_VALUE, result)
    }
    
    // ==================== Unary Minus with Long ====================
    
    @Test
    fun `negate positive long`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            longLiteral(5L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-5L, result)
    }
    
    @Test
    fun `negate large positive long`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            longLiteral(5_000_000_000L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-5_000_000_000L, result)
    }
    
    @Test
    fun `negate negative long returns positive`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            longLiteral(-5L, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5L, result)
    }
    
    @Test
    fun `negate zero long`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
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
    
    @Test
    fun `negate long max value`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            longLiteral(Long.MAX_VALUE, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-Long.MAX_VALUE, result)
    }
    
    @Test
    fun `negate long min value overflows`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            longLiteral(Long.MIN_VALUE, longType),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Long.MIN_VALUE, result)
    }
    
    // ==================== Unary Minus with Float ====================
    
    @Test
    fun `negate positive float`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            floatLiteral(3.14f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-3.14f, result)
    }
    
    @Test
    fun `negate negative float returns positive`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            floatLiteral(-3.14f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    @Test
    fun `negate positive zero float`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            floatLiteral(0.0f, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-0.0f, result)
    }
    
    @Test
    fun `negate negative zero float`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            floatLiteral(-0.0f, floatType),
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
    fun `negate positive infinity float`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
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
    fun `negate NaN float remains NaN`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            floatLiteral(Float.NaN, floatType),
                            floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Float
        assertEquals(true, result.isNaN())
    }
    
    // ==================== Unary Minus with Double ====================
    
    @Test
    fun `negate positive double`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(3.14159, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-3.14159, result)
    }
    
    @Test
    fun `negate negative double returns positive`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(-3.14159, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14159, result)
    }
    
    @Test
    fun `negate positive zero double`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(0.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-0.0, result)
    }
    
    @Test
    fun `negate positive infinity double`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
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
    fun `negate NaN double remains NaN`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(Double.NaN, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast) as Double
        assertEquals(true, result.isNaN())
    }
    
    @Test
    fun `negate very large double`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(1e100, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-1e100, result)
    }
    
    @Test
    fun `negate very small double`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            doubleLiteral(1e-100, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-1e-100, result)
    }
    
    // ==================== Unary Not with Boolean ====================
    
    @Test
    fun `not true returns false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "!",
                            booleanLiteral(true, boolType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `not false returns true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "!",
                            booleanLiteral(false, boolType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `double not returns original`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "!",
                            unaryExpr(
                                "!",
                                booleanLiteral(true, boolType),
                                boolType
                            ),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Nested Unary Operations ====================
    
    @Test
    fun `double negation returns original int`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            unaryExpr(
                                "-",
                                intLiteral(42, intType),
                                intType
                            ),
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
    fun `triple negation returns negated int`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            unaryExpr(
                                "-",
                                unaryExpr(
                                    "-",
                                    intLiteral(42, intType),
                                    intType
                                ),
                                intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-42, result)
    }
    
    // ==================== Unary with Binary Expressions ====================
    
    @Test
    fun `negate result of addition`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        unaryExpr(
                            "-",
                            binaryExpr(
                                intLiteral(3, intType),
                                "+",
                                intLiteral(4, intType),
                                intType
                            ),
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
    fun `add negated values`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            unaryExpr("-", intLiteral(3, intType), intType),
                            "+",
                            unaryExpr("-", intLiteral(4, intType), intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-7, result)
    }
}
