package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Comprehensive tests for boxing and unboxing functionality.
 * Tests cover:
 * - Boxing: primitive → wrapper for each type
 * - Unboxing: wrapper → primitive for each type  
 * - Null assignment to nullable types
 * - Variable declarations with nullable types
 * - Assignments to nullable variables
 * - Return values from nullable functions
 */
class BoxingUnboxingCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Int Boxing Tests ====================
    
    @Nested
    inner class IntBoxing {
        
        @Test
        fun `box int to nullable Int - literal value`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(returnStmt(intLiteral(42, nullableIntType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }
        
        @Test
        fun `box int to nullable Int - zero value`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(returnStmt(intLiteral(0, nullableIntType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }
        
        @Test
        fun `box int to nullable Int - negative value`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(returnStmt(intLiteral(-100, nullableIntType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-100, result)
        }
        
        @Test
        fun `box int to nullable Int - max value`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(returnStmt(intLiteral(Int.MAX_VALUE, nullableIntType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MAX_VALUE, result)
        }
        
        @Test
        fun `box int to nullable Int - min value`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(returnStmt(intLiteral(Int.MIN_VALUE, nullableIntType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MIN_VALUE, result)
        }
        
        @Test
        fun `nullable Int can be null`() {
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
        fun `declare nullable Int variable with int literal`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(
                        varDecl("x", nullableIntType, intLiteral(55, intType)),
                        returnStmt(identifier("x", nullableIntType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(55, result)
        }
        
        @Test
        fun `declare nullable Int variable with null`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(
                        varDecl("x", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(identifier("x", nullableIntType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
        
        @Test
        fun `assign int literal to nullable Int variable`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(
                        varDecl("x", nullableIntType, nullLiteral(nullableIntType)),
                        assignment(
                            identifier("x", nullableIntType, 3),
                            intLiteral(99, intType)
                        ),
                        returnStmt(identifier("x", nullableIntType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(99, result)
        }
        
        @Test
        fun `assign null to nullable Int variable`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableIntType,
                    body = listOf(
                        varDecl("x", nullableIntType, intLiteral(50, intType)),
                        assignment(
                            identifier("x", nullableIntType, 3),
                            nullLiteral(nullableIntType)
                        ),
                        returnStmt(identifier("x", nullableIntType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
    }
    
    // ==================== Long Boxing Tests ====================
    
    @Nested
    inner class LongBoxing {
        
        @Test
        fun `box long to nullable Long - literal value`() {
            val ast = buildTypedAst {
                val nullableLongType = longNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableLongType,
                    body = listOf(returnStmt(longLiteral(9999999999L, nullableLongType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(9999999999L, result)
        }
        
        @Test
        fun `box long to nullable Long - zero value`() {
            val ast = buildTypedAst {
                val nullableLongType = longNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableLongType,
                    body = listOf(returnStmt(longLiteral(0L, nullableLongType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0L, result)
        }
        
        @Test
        fun `box long to nullable Long - negative value`() {
            val ast = buildTypedAst {
                val nullableLongType = longNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableLongType,
                    body = listOf(returnStmt(longLiteral(-5000000000L, nullableLongType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-5000000000L, result)
        }
        
        @Test
        fun `nullable Long can be null`() {
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
        fun `declare nullable Long variable with long literal`() {
            val ast = buildTypedAst {
                val longType = longType()
                val nullableLongType = longNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableLongType,
                    body = listOf(
                        varDecl("x", nullableLongType, longLiteral(123456789L, longType)),
                        returnStmt(identifier("x", nullableLongType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(123456789L, result)
        }
        
        @Test
        fun `assign long literal to nullable Long variable`() {
            val ast = buildTypedAst {
                val longType = longType()
                val nullableLongType = longNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableLongType,
                    body = listOf(
                        varDecl("x", nullableLongType, nullLiteral(nullableLongType)),
                        assignment(
                            identifier("x", nullableLongType, 3),
                            longLiteral(987654321L, longType)
                        ),
                        returnStmt(identifier("x", nullableLongType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(987654321L, result)
        }
    }
    
    // ==================== Float Boxing Tests ====================
    
    @Nested
    inner class FloatBoxing {
        
        @Test
        fun `box float to nullable Float - literal value`() {
            val ast = buildTypedAst {
                val nullableFloatType = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableFloatType,
                    body = listOf(returnStmt(floatLiteral(3.14f, nullableFloatType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14f, result)
        }
        
        @Test
        fun `box float to nullable Float - zero value`() {
            val ast = buildTypedAst {
                val nullableFloatType = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableFloatType,
                    body = listOf(returnStmt(floatLiteral(0.0f, nullableFloatType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0.0f, result)
        }
        
        @Test
        fun `box float to nullable Float - negative value`() {
            val ast = buildTypedAst {
                val nullableFloatType = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableFloatType,
                    body = listOf(returnStmt(floatLiteral(-2.5f, nullableFloatType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-2.5f, result)
        }
        
        @Test
        fun `nullable Float can be null`() {
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
        fun `declare nullable Float variable with float literal`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val nullableFloatType = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableFloatType,
                    body = listOf(
                        varDecl("x", nullableFloatType, floatLiteral(1.5f, floatType)),
                        returnStmt(identifier("x", nullableFloatType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1.5f, result)
        }
        
        @Test
        fun `assign float literal to nullable Float variable`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val nullableFloatType = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableFloatType,
                    body = listOf(
                        varDecl("x", nullableFloatType, nullLiteral(nullableFloatType)),
                        assignment(
                            identifier("x", nullableFloatType, 3),
                            floatLiteral(7.77f, floatType)
                        ),
                        returnStmt(identifier("x", nullableFloatType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(7.77f, result)
        }
    }
    
    // ==================== Double Boxing Tests ====================
    
    @Nested
    inner class DoubleBoxing {
        
        @Test
        fun `box double to nullable Double - literal value`() {
            val ast = buildTypedAst {
                val nullableDoubleType = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableDoubleType,
                    body = listOf(returnStmt(doubleLiteral(2.71828, nullableDoubleType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.71828, result)
        }
        
        @Test
        fun `box double to nullable Double - zero value`() {
            val ast = buildTypedAst {
                val nullableDoubleType = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableDoubleType,
                    body = listOf(returnStmt(doubleLiteral(0.0, nullableDoubleType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0.0, result)
        }
        
        @Test
        fun `box double to nullable Double - negative value`() {
            val ast = buildTypedAst {
                val nullableDoubleType = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableDoubleType,
                    body = listOf(returnStmt(doubleLiteral(-100.001, nullableDoubleType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-100.001, result)
        }
        
        @Test
        fun `nullable Double can be null`() {
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
        fun `declare nullable Double variable with double literal`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val nullableDoubleType = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableDoubleType,
                    body = listOf(
                        varDecl("x", nullableDoubleType, doubleLiteral(99.99, doubleType)),
                        returnStmt(identifier("x", nullableDoubleType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(99.99, result)
        }
        
        @Test
        fun `assign double literal to nullable Double variable`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val nullableDoubleType = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableDoubleType,
                    body = listOf(
                        varDecl("x", nullableDoubleType, nullLiteral(nullableDoubleType)),
                        assignment(
                            identifier("x", nullableDoubleType, 3),
                            doubleLiteral(123.456, doubleType)
                        ),
                        returnStmt(identifier("x", nullableDoubleType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(123.456, result)
        }
    }
    
    // ==================== Boolean Boxing Tests ====================
    
    @Nested
    inner class BooleanBoxing {
        
        @Test
        fun `box boolean true to nullable Boolean`() {
            val ast = buildTypedAst {
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableBoolType,
                    body = listOf(returnStmt(booleanLiteral(true, nullableBoolType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
        
        @Test
        fun `box boolean false to nullable Boolean`() {
            val ast = buildTypedAst {
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableBoolType,
                    body = listOf(returnStmt(booleanLiteral(false, nullableBoolType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
        
        @Test
        fun `nullable Boolean can be null`() {
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
        fun `declare nullable Boolean variable with boolean literal`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableBoolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, booleanLiteral(true, boolType)),
                        returnStmt(identifier("x", nullableBoolType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
        
        @Test
        fun `assign boolean literal to nullable Boolean variable`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableBoolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, nullLiteral(nullableBoolType)),
                        assignment(
                            identifier("x", nullableBoolType, 3),
                            booleanLiteral(false, boolType)
                        ),
                        returnStmt(identifier("x", nullableBoolType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
        
        @Test
        fun `assign null to nullable Boolean variable`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableBoolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, booleanLiteral(true, boolType)),
                        assignment(
                            identifier("x", nullableBoolType, 3),
                            nullLiteral(nullableBoolType)
                        ),
                        returnStmt(identifier("x", nullableBoolType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
    }
    
    // ==================== Nullable String Tests ====================
    
    @Nested
    inner class NullableString {
        
        @Test
        fun `nullable String can hold string value`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableStringType,
                    body = listOf(returnStmt(stringLiteral("hello", nullableStringType)))
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }
        
        @Test
        fun `nullable String can be null`() {
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
        fun `declare nullable String variable with value`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableStringType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("world", stringType)),
                        returnStmt(identifier("x", nullableStringType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("world", result)
        }
        
        @Test
        fun `assign null to nullable String variable`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableStringType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("test", stringType)),
                        assignment(
                            identifier("x", nullableStringType, 3),
                            nullLiteral(nullableStringType)
                        ),
                        returnStmt(identifier("x", nullableStringType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
        
        @Test
        fun `assign string to nullable String variable`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = nullableStringType,
                    body = listOf(
                        varDecl("x", nullableStringType, nullLiteral(nullableStringType)),
                        assignment(
                            identifier("x", nullableStringType, 3),
                            stringLiteral("assigned", stringType)
                        ),
                        returnStmt(identifier("x", nullableStringType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("assigned", result)
        }
    }
}
