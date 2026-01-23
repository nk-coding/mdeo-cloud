package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for comparison expressions with all numeric types.
 * Tests operators: <, >, <=, >=
 */
class ComparisonCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Less Than (<) Tests ====================
    
    @Test
    fun `int less than returns true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "<",
                            intLiteral(10, intType),
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
    fun `int less than returns false when greater`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "<",
                            intLiteral(5, intType),
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
    fun `int less than returns false when equal`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "<",
                            intLiteral(5, intType),
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
    fun `long less than returns true`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(5L, longType),
                            "<",
                            longLiteral(10L, longType),
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
    fun `float less than returns true`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.5f, floatType),
                            "<",
                            floatLiteral(2.5f, floatType),
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
    fun `double less than returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.5, doubleType),
                            "<",
                            doubleLiteral(2.5, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Greater Than (>) Tests ====================
    
    @Test
    fun `int greater than returns true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            ">",
                            intLiteral(5, intType),
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
    fun `int greater than returns false when less`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            ">",
                            intLiteral(10, intType),
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
    fun `int greater than returns false when equal`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            ">",
                            intLiteral(5, intType),
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
    fun `long greater than returns true`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            ">",
                            longLiteral(5L, longType),
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
    fun `float greater than returns true`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.5f, floatType),
                            ">",
                            floatLiteral(1.5f, floatType),
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
    fun `double greater than returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(2.5, doubleType),
                            ">",
                            doubleLiteral(1.5, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Less Than or Equal (<=) Tests ====================
    
    @Test
    fun `int less than or equal returns true when less`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "<=",
                            intLiteral(10, intType),
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
    fun `int less than or equal returns true when equal`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "<=",
                            intLiteral(5, intType),
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
    fun `int less than or equal returns false when greater`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "<=",
                            intLiteral(5, intType),
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
    fun `long less than or equal returns true`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(5L, longType),
                            "<=",
                            longLiteral(5L, longType),
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
    fun `float less than or equal returns true`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.5f, floatType),
                            "<=",
                            floatLiteral(2.5f, floatType),
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
    fun `double less than or equal returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(2.5, doubleType),
                            "<=",
                            doubleLiteral(2.5, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Greater Than or Equal (>=) Tests ====================
    
    @Test
    fun `int greater than or equal returns true when greater`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            ">=",
                            intLiteral(5, intType),
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
    fun `int greater than or equal returns true when equal`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            ">=",
                            intLiteral(5, intType),
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
    fun `int greater than or equal returns false when less`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            ">=",
                            intLiteral(10, intType),
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
    fun `long greater than or equal returns true`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(10L, longType),
                            ">=",
                            longLiteral(5L, longType),
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
    fun `float greater than or equal returns true`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(2.5f, floatType),
                            ">=",
                            floatLiteral(1.5f, floatType),
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
    fun `double greater than or equal returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(2.5, doubleType),
                            ">=",
                            doubleLiteral(1.5, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Comparison with Type Promotion ====================
    
    @Test
    fun `int less than long with promotion`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(5, intType),
                            "<",
                            longLiteral(10L, longType),
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
    fun `int greater than double with promotion`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            ">",
                            doubleLiteral(5.5, doubleType),
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
    fun `long less than or equal float with promotion`() {
        val ast = buildTypedAst {
            val longType = longType()
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            longLiteral(5L, longType),
                            "<=",
                            floatLiteral(5.0f, floatType),
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
    fun `float greater than or equal double with promotion`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(10.0f, floatType),
                            ">=",
                            doubleLiteral(5.5, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    fun `compare negative integers`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(-10, intType),
                            "<",
                            intLiteral(-5, intType),
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
    fun `compare with zero`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(-1, intType),
                            "<",
                            intLiteral(0, intType),
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
    fun `compare int max and min values`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(Int.MIN_VALUE, intType),
                            "<",
                            intLiteral(Int.MAX_VALUE, intType),
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
    fun `compare very close double values`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.0000001, doubleType),
                            ">",
                            doubleLiteral(1.0, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== NaN Comparison Tests ====================
    // According to IEEE 754, NaN comparisons have special behavior:
    // - NaN < anything = false
    // - NaN > anything = false  
    // - NaN <= anything = false
    // - NaN >= anything = false
    // - anything < NaN = false
    // - anything > NaN = false
    // The FCMPG/FCMPL and DCMPG/DCMPL opcodes must be chosen correctly
    // to ensure this behavior.
    
    @Test
    fun `float NaN less than normal returns false`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            "<",
                            floatLiteral(1.0f, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN < 1.0f should be false")
    }
    
    @Test
    fun `float NaN greater than normal returns false`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            ">",
                            floatLiteral(1.0f, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN > 1.0f should be false")
    }
    
    @Test
    fun `float NaN less than or equal normal returns false`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            "<=",
                            floatLiteral(1.0f, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN <= 1.0f should be false")
    }
    
    @Test
    fun `float NaN greater than or equal normal returns false`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            ">=",
                            floatLiteral(1.0f, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN >= 1.0f should be false")
    }
    
    @Test
    fun `float normal less than NaN returns false`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.0f, floatType),
                            "<",
                            floatLiteral(Float.NaN, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "1.0f < NaN should be false")
    }
    
    @Test
    fun `float normal greater than NaN returns false`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(1.0f, floatType),
                            ">",
                            floatLiteral(Float.NaN, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "1.0f > NaN should be false")
    }
    
    @Test
    fun `float NaN compared to NaN returns false for less than`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NaN, floatType),
                            "<",
                            floatLiteral(Float.NaN, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN < NaN should be false")
    }
    
    @Test
    fun `double NaN less than normal returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.NaN, doubleType),
                            "<",
                            doubleLiteral(1.0, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN < 1.0 should be false")
    }
    
    @Test
    fun `double NaN greater than normal returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.NaN, doubleType),
                            ">",
                            doubleLiteral(1.0, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN > 1.0 should be false")
    }
    
    @Test
    fun `double NaN less than or equal normal returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.NaN, doubleType),
                            "<=",
                            doubleLiteral(1.0, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN <= 1.0 should be false")
    }
    
    @Test
    fun `double NaN greater than or equal normal returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.NaN, doubleType),
                            ">=",
                            doubleLiteral(1.0, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "NaN >= 1.0 should be false")
    }
    
    @Test
    fun `double normal less than NaN returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.0, doubleType),
                            "<",
                            doubleLiteral(Double.NaN, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "1.0 < NaN should be false")
    }
    
    @Test
    fun `double normal greater than NaN returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(1.0, doubleType),
                            ">",
                            doubleLiteral(Double.NaN, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "1.0 > NaN should be false")
    }
    
    // ==================== Infinity Comparison Tests ====================
    
    @Test
    fun `float positive infinity greater than normal returns true`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.POSITIVE_INFINITY, floatType),
                            ">",
                            floatLiteral(Float.MAX_VALUE, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result, "POSITIVE_INFINITY > MAX_VALUE should be true")
    }
    
    @Test
    fun `float negative infinity less than normal returns true`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            floatLiteral(Float.NEGATIVE_INFINITY, floatType),
                            "<",
                            floatLiteral(-Float.MAX_VALUE, floatType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result, "NEGATIVE_INFINITY < -MAX_VALUE should be true")
    }
    
    @Test
    fun `double positive infinity greater than normal returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            ">",
                            doubleLiteral(Double.MAX_VALUE, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result, "POSITIVE_INFINITY > MAX_VALUE should be true")
    }
    
    @Test
    fun `double negative infinity less than normal returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.NEGATIVE_INFINITY, doubleType),
                            "<",
                            doubleLiteral(-Double.MAX_VALUE, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result, "NEGATIVE_INFINITY < -MAX_VALUE should be true")
    }
    
    @Test
    fun `positive infinity compared to itself with less than returns false`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            "<",
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result, "POSITIVE_INFINITY < POSITIVE_INFINITY should be false")
    }
    
    @Test
    fun `positive infinity compared to itself with less than or equal returns true`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            "<=",
                            doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result, "POSITIVE_INFINITY <= POSITIVE_INFINITY should be true")
    }
}
