package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Comprehensive tests for equality operators (== and !=) with all types.
 * Tests cover:
 * - Primitive type equality (int, long, float, double, boolean)
 * - Wrapper type equality (Integer, Long, Float, Double, Boolean)
 * - Mixed primitive and wrapper type equality
 * - String equality (uses .equals, not reference equality)
 * - Null comparisons
 * - Different numeric type comparisons (int == double, etc.)
 */
class EqualityOperatorCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Primitive Integer Equality ====================
    
    @Nested
    inner class IntegerEquality {
        
        @Test
        fun `int equals int - same values returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "==",
                                intLiteral(42, intType),
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
        fun `int equals int - different values returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "==",
                                intLiteral(43, intType),
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
        fun `int not equals int - same values returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "!=",
                                intLiteral(42, intType),
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
        fun `int not equals int - different values returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "!=",
                                intLiteral(43, intType),
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
        fun `int equals int - zero values`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(0, intType),
                                "==",
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
        fun `int equals int - negative values`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(-100, intType),
                                "==",
                                intLiteral(-100, intType),
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
        fun `int equals int - max values`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(Int.MAX_VALUE, intType),
                                "==",
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
        fun `int equals int - min values`() {
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
                                "==",
                                intLiteral(Int.MIN_VALUE, intType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Long Equality ====================
    
    @Nested
    inner class LongEquality {
        
        @Test
        fun `long equals long - same values returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(9999999999L, longType),
                                "==",
                                longLiteral(9999999999L, longType),
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
        fun `long equals long - different values returns false`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(9999999999L, longType),
                                "==",
                                longLiteral(9999999998L, longType),
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
        fun `long not equals long - same values returns false`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(123L, longType),
                                "!=",
                                longLiteral(123L, longType),
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
        fun `long not equals long - different values returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(123L, longType),
                                "!=",
                                longLiteral(456L, longType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Float Equality ====================
    
    @Nested
    inner class FloatEquality {
        
        @Test
        fun `float equals float - same values returns true`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                floatLiteral(3.14f, floatType),
                                "==",
                                floatLiteral(3.14f, floatType),
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
        fun `float equals float - different values returns false`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                floatLiteral(3.14f, floatType),
                                "==",
                                floatLiteral(3.15f, floatType),
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
        fun `float not equals float - same values returns false`() {
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
                                "!=",
                                floatLiteral(1.5f, floatType),
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
        fun `float not equals float - different values returns true`() {
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
                                "!=",
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
    }
    
    // ==================== Double Equality ====================
    
    @Nested
    inner class DoubleEquality {
        
        @Test
        fun `double equals double - same values returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(2.71828, doubleType),
                                "==",
                                doubleLiteral(2.71828, doubleType),
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
        fun `double equals double - different values returns false`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(2.71828, doubleType),
                                "==",
                                doubleLiteral(3.14159, doubleType),
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
        fun `double not equals double - same values returns false`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(100.0, doubleType),
                                "!=",
                                doubleLiteral(100.0, doubleType),
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
        fun `double not equals double - different values returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(100.0, doubleType),
                                "!=",
                                doubleLiteral(200.0, doubleType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Boolean Equality ====================
    
    @Nested
    inner class BooleanEquality {
        
        @Test
        fun `boolean equals boolean - true equals true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "==",
                                booleanLiteral(true, boolType),
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
        fun `boolean equals boolean - false equals false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(false, boolType),
                                "==",
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
        fun `boolean equals boolean - true not equals false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "==",
                                booleanLiteral(false, boolType),
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
        fun `boolean not equals boolean - true not equals false returns true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "!=",
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
        fun `boolean not equals boolean - true not equals true returns false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "!=",
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
    }
    
    // ==================== String Equality ====================
    
    @Nested
    inner class StringEquality {
        
        @Test
        fun `string equals string - same content returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("hello", stringType),
                                "==",
                                stringLiteral("hello", stringType),
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
        fun `string equals string - different content returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("hello", stringType),
                                "==",
                                stringLiteral("world", stringType),
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
        fun `string not equals string - same content returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("test", stringType),
                                "!=",
                                stringLiteral("test", stringType),
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
        fun `string not equals string - different content returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("abc", stringType),
                                "!=",
                                stringLiteral("xyz", stringType),
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
        fun `string equals empty string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("", stringType),
                                "==",
                                stringLiteral("", stringType),
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
        fun `string equals case sensitive`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("Hello", stringType),
                                "==",
                                stringLiteral("hello", stringType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }
    
    // ==================== Null Comparisons ====================
    
    @Nested
    inner class NullComparisons {
        
        @Test
        fun `null equals null returns true`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableIntType),
                                "==",
                                nullLiteral(nullableIntType),
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
        fun `null not equals null returns false`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableIntType),
                                "!=",
                                nullLiteral(nullableIntType),
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
        fun `null equals nullable string returns true for null value`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableStringType),
                                "==",
                                nullLiteral(nullableStringType),
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
        fun `int literal not equals null returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "!=",
                                nullLiteral(nullableIntType),
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
        fun `null not equals int literal returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableIntType),
                                "!=",
                                intLiteral(42, intType),
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
        fun `int literal equals null returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "==",
                                nullLiteral(nullableIntType),
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
        fun `null equals int literal returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableIntType),
                                "==",
                                intLiteral(42, intType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }
    
    // ==================== Mixed Numeric Type Comparisons ====================
    
    @Nested
    inner class MixedNumericEquality {
        
        @Test
        fun `int equals long - same value returns true`() {
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
                                intLiteral(42, intType),
                                "==",
                                longLiteral(42L, longType),
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
        fun `int equals double - same value returns true`() {
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
                                intLiteral(42, intType),
                                "==",
                                doubleLiteral(42.0, doubleType),
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
        fun `int not equals double - different value returns true`() {
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
                                intLiteral(42, intType),
                                "!=",
                                doubleLiteral(42.5, doubleType),
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
        fun `long equals double - same value returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(100L, longType),
                                "==",
                                doubleLiteral(100.0, doubleType),
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
        fun `float equals double - same value returns true`() {
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
                                floatLiteral(1.5f, floatType),
                                "==",
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
        
        @Test
        fun `int equals float - same value returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(10, intType),
                                "==",
                                floatLiteral(10.0f, floatType),
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
        fun `long equals float - same value returns true`() {
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
                                longLiteral(100L, longType),
                                "==",
                                floatLiteral(100.0f, floatType),
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
        fun `int not equals long - different value returns true`() {
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
                                intLiteral(42, intType),
                                "!=",
                                longLiteral(43L, longType),
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
        fun `long not equals float - different value returns true`() {
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
                                longLiteral(100L, longType),
                                "!=",
                                floatLiteral(100.5f, floatType),
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
        fun `float not equals double - different value returns true`() {
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
                                floatLiteral(1.5f, floatType),
                                "!=",
                                doubleLiteral(1.6, doubleType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Nullable Wrapper Type Equality ====================
    
    @Nested
    inner class NullableWrapperEquality {
        
        @Test
        fun `nullable Integer equals nullable Integer - same value via ternary returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        // Create two Integer wrappers with value 42 using ternary
                        varDecl("a", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableIntType, 3),
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
        fun `nullable Integer equals nullable Integer - different values via ternary returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(43, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableIntType, 3),
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
        fun `nullable Integer not equals nullable Integer - same value returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(100, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(100, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "!=",
                                identifier("b", nullableIntType, 3),
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
        fun `nullable Integer not equals nullable Integer - different values returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(100, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(200, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "!=",
                                identifier("b", nullableIntType, 3),
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
        fun `nullable Long equals nullable Long - same value returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableLongType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(9999999999L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        varDecl("b", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(9999999999L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableLongType, 3),
                                "==",
                                identifier("b", nullableLongType, 3),
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
        fun `nullable Long equals nullable Long - different values returns false`() {
            val ast = buildTypedAst {
                val longType = longType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableLongType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(100L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        varDecl("b", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(200L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableLongType, 3),
                                "==",
                                identifier("b", nullableLongType, 3),
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
        fun `nullable Float equals nullable Float - same value returns true`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val nullableFloatType = floatNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableFloatType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(3.14f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        varDecl("b", nullableFloatType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(3.14f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableFloatType, 3),
                                "==",
                                identifier("b", nullableFloatType, 3),
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
        fun `nullable Float equals nullable Float - different values returns false`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val nullableFloatType = floatNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableFloatType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(1.0f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        varDecl("b", nullableFloatType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(2.0f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableFloatType, 3),
                                "==",
                                identifier("b", nullableFloatType, 3),
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
        fun `nullable Double equals nullable Double - same value returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableDoubleType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(2.71828, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        varDecl("b", nullableDoubleType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(2.71828, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableDoubleType, 3),
                                "==",
                                identifier("b", nullableDoubleType, 3),
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
        fun `nullable Double equals nullable Double - different values returns false`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableDoubleType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(1.0, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        varDecl("b", nullableDoubleType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(2.0, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableDoubleType, 3),
                                "==",
                                identifier("b", nullableDoubleType, 3),
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
        fun `nullable Boolean equals nullable Boolean - true equals true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableBoolType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                booleanLiteral(true, boolType),
                                nullLiteral(nullableBoolType),
                                nullableBoolType
                            )
                        ),
                        varDecl("b", nullableBoolType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                booleanLiteral(true, boolType),
                                nullLiteral(nullableBoolType),
                                nullableBoolType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableBoolType, 3),
                                "==",
                                identifier("b", nullableBoolType, 3),
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
        fun `nullable Boolean equals nullable Boolean - true not equals false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableBoolType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                booleanLiteral(true, boolType),
                                nullLiteral(nullableBoolType),
                                nullableBoolType
                            )
                        ),
                        varDecl("b", nullableBoolType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                booleanLiteral(false, boolType),
                                nullLiteral(nullableBoolType),
                                nullableBoolType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableBoolType, 3),
                                "==",
                                identifier("b", nullableBoolType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }
    
    // ==================== Mixed Primitive and Wrapper Equality ====================
    
    @Nested
    inner class MixedPrimitiveWrapperEquality {
        
        @Test
        fun `int equals nullable Integer - same value returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "==",
                                identifier("wrapper", nullableIntType, 3),
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
        fun `int equals nullable Integer - different value returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                intLiteral(100, intType),
                                "==",
                                identifier("wrapper", nullableIntType, 3),
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
        fun `nullable Integer equals int - same value returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("wrapper", nullableIntType, 3),
                                "==",
                                intLiteral(42, intType),
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
        fun `int not equals nullable Integer - same value returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "!=",
                                identifier("wrapper", nullableIntType, 3),
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
        fun `int not equals nullable Integer - different value returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                intLiteral(100, intType),
                                "!=",
                                identifier("wrapper", nullableIntType, 3),
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
        fun `int equals null Integer - returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "==",
                                identifier("wrapper", nullableIntType, 3),
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
        fun `int not equals null Integer - returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "!=",
                                identifier("wrapper", nullableIntType, 3),
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
        fun `null Integer equals int - returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                identifier("wrapper", nullableIntType, 3),
                                "==",
                                intLiteral(42, intType),
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
        fun `long equals nullable Long - same value returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableLongType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(9999999999L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                longLiteral(9999999999L, longType),
                                "==",
                                identifier("wrapper", nullableLongType, 3),
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
        fun `double equals nullable Double - same value returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableDoubleType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(3.14159, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(3.14159, doubleType),
                                "==",
                                identifier("wrapper", nullableDoubleType, 3),
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
        fun `float equals nullable Float - same value returns true`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val nullableFloatType = floatNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableFloatType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(2.5f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                floatLiteral(2.5f, floatType),
                                "==",
                                identifier("wrapper", nullableFloatType, 3),
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
        fun `boolean equals nullable Boolean - true equals true returns true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableBoolType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                booleanLiteral(true, boolType),
                                nullLiteral(nullableBoolType),
                                nullableBoolType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "==",
                                identifier("wrapper", nullableBoolType, 3),
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
        fun `boolean not equals nullable Boolean - true not equals false returns true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("wrapper", nullableBoolType, 
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                booleanLiteral(false, boolType),
                                nullLiteral(nullableBoolType),
                                nullableBoolType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "!=",
                                identifier("wrapper", nullableBoolType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== String with Null Comparisons ====================
    
    @Nested
    inner class StringNullComparisons {
        
        @Test
        fun `null equals string literal returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableStringType),
                                "==",
                                stringLiteral("hello", stringType),
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
        fun `string literal equals null returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("hello", stringType),
                                "==",
                                nullLiteral(nullableStringType),
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
        fun `null not equals string literal returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableStringType),
                                "!=",
                                stringLiteral("hello", stringType),
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
        fun `string literal not equals null returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                stringLiteral("hello", stringType),
                                "!=",
                                nullLiteral(nullableStringType),
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
        fun `nullable string with value equals same string - returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("s", nullableStringType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                stringLiteral("test", stringType),
                                nullLiteral(nullableStringType),
                                nullableStringType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("s", nullableStringType, 3),
                                "==",
                                stringLiteral("test", stringType),
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
        fun `nullable string with value equals different string - returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("s", nullableStringType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                stringLiteral("test", stringType),
                                nullLiteral(nullableStringType),
                                nullableStringType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("s", nullableStringType, 3),
                                "==",
                                stringLiteral("other", stringType),
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
        fun `null string equals null string - returns true`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableStringType, nullLiteral(nullableStringType)),
                        varDecl("b", nullableStringType, nullLiteral(nullableStringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableStringType, 3),
                                "==",
                                identifier("b", nullableStringType, 3),
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
        fun `null string not equals non-null string - returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableStringType, nullLiteral(nullableStringType)),
                        varDecl("b", nullableStringType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                stringLiteral("value", stringType),
                                nullLiteral(nullableStringType),
                                nullableStringType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableStringType, 3),
                                "!=",
                                identifier("b", nullableStringType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Nullable Wrapper with Null Value Comparisons ====================
    
    @Nested
    inner class NullableWrapperNullValueComparisons {
        
        @Test
        fun `null Integer equals null Integer - returns true`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, nullLiteral(nullableIntType)),
                        varDecl("b", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableIntType, 3),
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
        fun `null Integer not equals null Integer - returns false`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, nullLiteral(nullableIntType)),
                        varDecl("b", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "!=",
                                identifier("b", nullableIntType, 3),
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
        fun `null Integer equals non-null Integer - returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, nullLiteral(nullableIntType)),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableIntType, 3),
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
        fun `non-null Integer equals null Integer - returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableIntType, 3),
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
        fun `null Integer not equals non-null Integer - returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, nullLiteral(nullableIntType)),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "!=",
                                identifier("b", nullableIntType, 3),
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
        fun `null Long equals null Long - returns true`() {
            val ast = buildTypedAst {
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableLongType, nullLiteral(nullableLongType)),
                        varDecl("b", nullableLongType, nullLiteral(nullableLongType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableLongType, 3),
                                "==",
                                identifier("b", nullableLongType, 3),
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
        fun `null Double equals null Double - returns true`() {
            val ast = buildTypedAst {
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableDoubleType, nullLiteral(nullableDoubleType)),
                        varDecl("b", nullableDoubleType, nullLiteral(nullableDoubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableDoubleType, 3),
                                "==",
                                identifier("b", nullableDoubleType, 3),
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
        fun `null Boolean equals null Boolean - returns true`() {
            val ast = buildTypedAst {
                val nullableBoolType = booleanNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableBoolType, nullLiteral(nullableBoolType)),
                        varDecl("b", nullableBoolType, nullLiteral(nullableBoolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableBoolType, 3),
                                "==",
                                identifier("b", nullableBoolType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Additional Edge Cases ====================
    
    @Nested
    inner class EdgeCases {
        
        @Test
        fun `int equals int - positive vs negative same magnitude returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(42, intType),
                                "==",
                                intLiteral(-42, intType),
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
        fun `long equals int - same value returns true`() {
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
                                longLiteral(42L, longType),
                                "==",
                                intLiteral(42, intType),
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
        fun `double equals int - same value returns true`() {
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
                                doubleLiteral(100.0, doubleType),
                                "==",
                                intLiteral(100, intType),
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
        fun `double equals long - same value returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(1000.0, doubleType),
                                "==",
                                longLiteral(1000L, longType),
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
        fun `float equals int - same value returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                floatLiteral(50.0f, floatType),
                                "==",
                                intLiteral(50, intType),
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
        fun `float equals long - same value returns true`() {
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
                                floatLiteral(200.0f, floatType),
                                "==",
                                longLiteral(200L, longType),
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
        fun `double equals float - same value returns true`() {
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
                                doubleLiteral(2.5, doubleType),
                                "==",
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
        fun `int not equals float - different values returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(10, intType),
                                "!=",
                                floatLiteral(10.5f, floatType),
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
        fun `long not equals double - different values returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(100L, longType),
                                "!=",
                                doubleLiteral(100.1, doubleType),
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
        fun `string equals uses equals not reference - constructed strings are equal`() {
            // This test verifies that string comparison uses .equals(), not reference equality
            // Even if two string literals produce the same content, they should be equal
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", stringType, stringLiteral("test", stringType)),
                        varDecl("b", stringType, stringLiteral("test", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", stringType, 3),
                                "==",
                                identifier("b", stringType, 3),
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
        fun `string not equals - different strings from variables`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", stringType, stringLiteral("hello", stringType)),
                        varDecl("b", stringType, stringLiteral("world", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", stringType, 3),
                                "!=",
                                identifier("b", stringType, 3),
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
        fun `int literal 0 equals int literal 0`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(0, intType),
                                "==",
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
        fun `long literal 0L equals long literal 0L`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(0L, longType),
                                "==",
                                longLiteral(0L, longType),
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
        fun `double literal 0 dot 0 equals double literal 0 dot 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(0.0, doubleType),
                                "==",
                                doubleLiteral(0.0, doubleType),
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
        fun `int 0 equals long 0L`() {
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
                                intLiteral(0, intType),
                                "==",
                                longLiteral(0L, longType),
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
        fun `int 0 equals double 0 dot 0`() {
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
                                intLiteral(0, intType),
                                "==",
                                doubleLiteral(0.0, doubleType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
    
    // ==================== Not Equals Operator Comprehensive Tests ====================
    
    @Nested
    inner class NotEqualsComprehensive {
        
        @Test
        fun `long not equals long - same values returns false`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(999L, longType),
                                "!=",
                                longLiteral(999L, longType),
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
        fun `float not equals float - same values returns false`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                floatLiteral(7.77f, floatType),
                                "!=",
                                floatLiteral(7.77f, floatType),
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
        fun `double not equals double - same values returns false`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(123.456, doubleType),
                                "!=",
                                doubleLiteral(123.456, doubleType),
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
        fun `long not equals long - different values returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(1L, longType),
                                "!=",
                                longLiteral(2L, longType),
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
        fun `float not equals float - different values returns true`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                floatLiteral(1.1f, floatType),
                                "!=",
                                floatLiteral(1.2f, floatType),
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
        fun `double not equals double - different values returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(99.9, doubleType),
                                "!=",
                                doubleLiteral(99.8, doubleType),
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
        fun `int not equals long - same value returns false`() {
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
                                intLiteral(42, intType),
                                "!=",
                                longLiteral(42L, longType),
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
        fun `int not equals double - same value returns false`() {
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
                                intLiteral(100, intType),
                                "!=",
                                doubleLiteral(100.0, doubleType),
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
        fun `long not equals double - same value returns false`() {
            val ast = buildTypedAst {
                val longType = longType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                longLiteral(500L, longType),
                                "!=",
                                doubleLiteral(500.0, doubleType),
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
        fun `float not equals double - same value returns false`() {
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
                                floatLiteral(3.5f, floatType),
                                "!=",
                                doubleLiteral(3.5, doubleType),
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
        fun `int not equals float - same value returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                intLiteral(25, intType),
                                "!=",
                                floatLiteral(25.0f, floatType),
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
        fun `boolean false not equals boolean true returns true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(false, boolType),
                                "!=",
                                booleanLiteral(true, boolType),
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
        fun `boolean false not equals boolean false returns false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(false, boolType),
                                "!=",
                                booleanLiteral(false, boolType),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }
    
    // ==================== Mixed Nullable Numeric Type Equality ====================
    // Tests for comparing nullable wrappers of different numeric types (e.g., Int? vs Long?)
    
    @Nested
    inner class MixedNullableNumericTypeEquality {
        
        @Test
        fun `nullable Integer equals nullable Long - same value returns true`() {
            // This tests comparing Int?(42) == Long?(42L) - should return true
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val nullableIntType = intNullableType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(42L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableLongType, 3),
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
        fun `nullable Integer not equals nullable Long - same value returns false`() {
            // This tests comparing Int?(42) != Long?(42L) - should return false
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val nullableIntType = intNullableType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(42L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "!=",
                                identifier("b", nullableLongType, 3),
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
        fun `nullable Long equals nullable Integer - same value returns true`() {
            // This tests comparing Long?(100L) == Int?(100) - should return true
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val nullableIntType = intNullableType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(100L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        varDecl("b", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(100, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableLongType, 3),
                                "==",
                                identifier("b", nullableIntType, 3),
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
        fun `nullable Integer equals nullable Double - same value returns true`() {
            // This tests comparing Int?(50) == Double?(50.0) - should return true
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val nullableIntType = intNullableType()
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(50, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableDoubleType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(50.0, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableDoubleType, 3),
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
        fun `nullable Integer equals nullable Double - different values returns false`() {
            // This tests comparing Int?(50) == Double?(50.5) - should return false
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val nullableIntType = intNullableType()
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(50, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableDoubleType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(50.5, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableDoubleType, 3),
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
        fun `nullable Float equals nullable Double - same value returns true`() {
            // This tests comparing Float?(2.5f) == Double?(2.5) - should return true
            val ast = buildTypedAst {
                val floatType = floatType()
                val doubleType = doubleType()
                val nullableFloatType = floatNullableType()
                val nullableDoubleType = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableFloatType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(2.5f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        varDecl("b", nullableDoubleType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                doubleLiteral(2.5, doubleType),
                                nullLiteral(nullableDoubleType),
                                nullableDoubleType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableFloatType, 3),
                                "==",
                                identifier("b", nullableDoubleType, 3),
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
        fun `null Integer equals null Long - returns true`() {
            // Both are null, should return true regardless of type
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, nullLiteral(nullableIntType)),
                        varDecl("b", nullableLongType, nullLiteral(nullableLongType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableLongType, 3),
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
        fun `null Integer not equals non-null Long - returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val nullableIntType = intNullableType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType, nullLiteral(nullableIntType)),
                        varDecl("b", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(42L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "!=",
                                identifier("b", nullableLongType, 3),
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
        fun `non-null Integer equals null Long - returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val nullableLongType = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableIntType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                intLiteral(42, intType),
                                nullLiteral(nullableIntType),
                                nullableIntType
                            )
                        ),
                        varDecl("b", nullableLongType, nullLiteral(nullableLongType)),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableIntType, 3),
                                "==",
                                identifier("b", nullableLongType, 3),
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
        fun `nullable Long equals nullable Float - same value returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val floatType = floatType()
                val nullableLongType = longNullableType()
                val nullableFloatType = floatNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("a", nullableLongType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                longLiteral(100L, longType),
                                nullLiteral(nullableLongType),
                                nullableLongType
                            )
                        ),
                        varDecl("b", nullableFloatType,
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                floatLiteral(100.0f, floatType),
                                nullLiteral(nullableFloatType),
                                nullableFloatType
                            )
                        ),
                        returnStmt(
                            binaryExpr(
                                identifier("a", nullableLongType, 3),
                                "==",
                                identifier("b", nullableFloatType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }
}
