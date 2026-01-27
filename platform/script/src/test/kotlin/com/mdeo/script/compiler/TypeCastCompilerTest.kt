package com.mdeo.script.compiler

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for the type cast (as / as?) expression compiler.
 *
 * The `as` operator performs a regular Java cast.
 * The `as?` operator is a safe cast that returns null instead of throwing.
 */
class TypeCastCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== Regular Cast (as) - Primitives ====================

    @Nested
    inner class RegularCastPrimitives {

        @Test
        fun `int to int is no-op`() {
            val ast = buildTypedAst {
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `int to long conversion`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42L, result)
        }

        @Test
        fun `int to double conversion`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), doubleType, doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }

        @Test
        fun `int to float conversion`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), floatType, floatType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0f, result)
        }

        @Test
        fun `long to int narrowing conversion`() {
            val ast = buildTypedAst {
                val longType = longType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(longLiteral(42L, longType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `double to int narrowing conversion`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(doubleLiteral(3.7, doubleType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)
        }

        @Test
        fun `double to long narrowing conversion`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        returnStmt(typeCast(doubleLiteral(9999999999.9, doubleType), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(9999999999L, result)
        }

        @Test
        fun `float to int narrowing conversion`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(floatLiteral(2.9f, floatType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2, result)
        }

        @Test
        fun `float to double widening conversion`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(typeCast(floatLiteral(2.5f, floatType), doubleType, doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.5, result)
        }

        @Test
        fun `double to float narrowing conversion`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(typeCast(doubleLiteral(2.5, doubleType), floatType, floatType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.5f, result)
        }
    }

    // ==================== Regular Cast (as) - Boxing/Unboxing ====================

    @Nested
    inner class RegularCastBoxingUnboxing {

        @Test
        fun `int to Any requires boxing`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
            assertIs<Integer>(result)
        }

        @Test
        fun `int to nullable int requires boxing`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), intNullable, intNullable))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
            assertIs<Integer>(result)
        }

        @Test
        fun `nullable int to int requires unboxing`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", intNullable, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `Any to int requires checkcast and unboxing`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `Any to nullable int requires checkcast only`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intNullable, intNullable))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `long to Any requires boxing`() {
            val ast = buildTypedAst {
                val longType = longType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(typeCast(longLiteral(999L, longType), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(999L, result)
            assertIs<Long>(result)
        }

        @Test
        fun `double to Any requires boxing`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(typeCast(doubleLiteral(3.14, doubleType), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14, result)
            assertIs<Double>(result)
        }

        @Test
        fun `boolean to Any requires boxing`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(typeCast(booleanLiteral(true, boolType), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
            assertIs<Boolean>(result)
        }
    }

    // ==================== Regular Cast (as) - Reference Types ====================

    @Nested
    inner class RegularCastReferenceTypes {

        @Test
        fun `Any to String cast`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), stringType, stringType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }

        @Test
        fun `String to Any is no-op`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(typeCast(stringLiteral("hello", stringType), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }

        @Test
        fun `Any to Any is no-op`() {
            val ast = buildTypedAst {
                val anyType = anyNullableType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("test", stringType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("test", result)
        }
    }

    // ==================== Regular Cast (as) - ClassCastException ====================

    @Nested
    inner class RegularCastThrowsClassCastException {

        @Test
        fun `Any containing string cast to int throws ClassCastException`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intType, intType))
                    )
                )
            }
            
            assertFailsWith<ClassCastException> {
                helper.compileAndInvoke(ast)
            }
        }

        @Test
        fun `Any containing int cast to string throws ClassCastException`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), stringType, stringType))
                    )
                )
            }
            
            assertFailsWith<ClassCastException> {
                helper.compileAndInvoke(ast)
            }
        }
    }

    // ==================== Safe Cast (as?) - Basic ====================

    @Nested
    inner class SafeCastBasic {

        @Test
        fun `safe cast Any to int succeeds when compatible`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intNullable, intNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `safe cast Any to int returns null when incompatible`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intNullable = intNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intNullable, intNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }

        @Test
        fun `safe cast null returns null`() {
            val ast = buildTypedAst {
                val intNullable = intNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", anyType, nullLiteral(anyType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intNullable, intNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }

        @Test
        fun `safe cast Any to string succeeds when compatible`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = stringNullable,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), stringNullable, stringNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }

        @Test
        fun `safe cast Any to string returns null when incompatible`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringNullable = stringNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = stringNullable,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), stringNullable, stringNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
    }

    // ==================== Safe Cast (as?) - Primitives ====================

    @Nested
    inner class SafeCastPrimitives {

        @Test
        fun `safe cast Any to long succeeds when compatible`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = longNullable,
                    body = listOf(
                        varDecl("x", anyType, longLiteral(999L, longType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), longNullable, longNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(999L, result)
        }

        @Test
        fun `safe cast Any to double succeeds when compatible`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleNullable,
                    body = listOf(
                        varDecl("x", anyType, doubleLiteral(3.14, doubleType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), doubleNullable, doubleNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14, result)
        }

        @Test
        fun `safe cast Any to float succeeds when compatible`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullable = floatNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = floatNullable,
                    body = listOf(
                        varDecl("x", anyType, floatLiteral(2.5f, floatType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), floatNullable, floatNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.5f, result)
        }

        @Test
        fun `safe cast Any to boolean succeeds when compatible`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = boolNullable,
                    body = listOf(
                        varDecl("x", anyType, booleanLiteral(true, boolType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), boolNullable, boolNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `safe cast Any containing int to long returns null - no auto conversion`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longNullable = longNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = longNullable,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), longNullable, longNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
    }

    // ==================== Compile-time Optimizations ====================

    @Nested
    inner class CompileTimeOptimizations {

        @Test
        fun `int as int is no-op and returns correct value`() {
            val ast = buildTypedAst {
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(100, intType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(100, result)
        }

        @Test
        fun `safe cast same type succeeds`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringNullable,
                    body = listOf(
                        returnStmt(typeCast(stringLiteral("test", stringType), stringNullable, stringNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("test", result)
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    inner class EdgeCases {

        @Test
        fun `cast negative int to long`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        returnStmt(typeCast(unaryExpr("-", intLiteral(100, intType), intType), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-100L, result)
        }

        @Test
        fun `cast large long to int truncates`() {
            val ast = buildTypedAst {
                val longType = longType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(longLiteral(Long.MAX_VALUE, longType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-1, result)
        }

        @Test
        fun `cast double infinity to long`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.POSITIVE_INFINITY, doubleType)),
                        returnStmt(typeCast(identifier("x", doubleType, 3), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Long.MAX_VALUE, result)
        }

        @Test
        fun `cast double NaN to int results in zero`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.NaN, doubleType)),
                        returnStmt(typeCast(identifier("x", doubleType, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }
    }
}
