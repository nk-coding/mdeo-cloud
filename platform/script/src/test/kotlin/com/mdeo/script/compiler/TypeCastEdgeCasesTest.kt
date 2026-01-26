package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Edge case tests for type cast (as / as?) compiler.
 * These tests cover boundary conditions, special values, and complex scenarios.
 */
class TypeCastEdgeCasesTest {

    private val helper = CompilerTestHelper()

    // ==================== Chained Casts ====================

    @Nested
    inner class ChainedCasts {

        @Test
        fun `int to long to double chain`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            typeCast(
                                typeCast(intLiteral(42, intType), longType, longType),
                                doubleType,
                                doubleType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }

        @Test
        fun `double to int to long chain with truncation`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        returnStmt(
                            typeCast(
                                typeCast(doubleLiteral(3.9, doubleType), intType, intType),
                                longType,
                                longType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3L, result)
        }
    }

    // ==================== Boundary Values ====================

    @Nested
    inner class BoundaryValues {

        @Test
        fun `cast Int MAX_VALUE to long`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(Int.MAX_VALUE, intType), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MAX_VALUE.toLong(), result)
        }

        @Test
        fun `cast Int MIN_VALUE to long`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(Int.MIN_VALUE, intType), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MIN_VALUE.toLong(), result)
        }

        @Test
        fun `cast large positive double to int saturates`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(1e15, doubleType)),
                        returnStmt(typeCast(identifier("x", doubleType, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MAX_VALUE, result)
        }

        @Test
        fun `cast large negative double to int saturates`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(-1e15, doubleType)),
                        returnStmt(typeCast(identifier("x", doubleType, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MIN_VALUE, result)
        }

        @Test
        fun `cast zero double to int`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(doubleLiteral(0.0, doubleType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }

        @Test
        fun `cast negative zero double to int`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(typeCast(doubleLiteral(-0.0, doubleType), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }
    }

    // ==================== Safe Cast Edge Cases ====================

    @Nested
    inner class SafeCastEdgeCases {

        @Test
        fun `safe cast compatible boxed type succeeds`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = longNullable,
                    body = listOf(
                        varDecl("x", anyType, longLiteral(Long.MAX_VALUE, longType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), longNullable, longNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Long.MAX_VALUE, result)
        }

        @Test
        fun `safe cast float to double returns null - different wrapper types`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val doubleNullable = doubleNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleNullable,
                    body = listOf(
                        varDecl("x", anyType, floatLiteral(2.5f, floatType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), doubleNullable, doubleNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }

        @Test
        fun `safe cast boolean to int returns null`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val intNullable = intNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", anyType, booleanLiteral(true, boolType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), intNullable, intNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
    }

    // ==================== Nullable Wrapper Conversions ====================

    @Nested
    inner class NullableWrapperConversions {

        @Test
        fun `nullable int to nullable int same type`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(typeCast(identifier("x", intNullable, 3), intNullable, intNullable))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `nullable double to int cast`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(3.7, doubleType)),
                        returnStmt(typeCast(identifier("x", doubleNullable, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)
        }
    }

    // ==================== Cast in Expressions ====================

    @Nested
    inner class CastInExpressions {

        @Test
        fun `cast result used in arithmetic`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                typeCast(doubleLiteral(3.7, doubleType), intType, intType),
                                "+",
                                intLiteral(10, intType),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(13, result)
        }

        @Test
        fun `cast result used in comparison`() {
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
                                typeCast(intLiteral(42, intType), longType, longType),
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
        fun `safe cast in ternary expression`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(
                            ternaryExpr(
                                booleanLiteral(true, boolType),
                                typeCast(identifier("x", anyType, 3), intNullable, intNullable, isSafe = true),
                                nullLiteral(intNullable),
                                intNullable
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }
    }

    // ==================== Float Special Values ====================

    @Nested
    inner class FloatSpecialValues {

        @Test
        fun `cast float NaN to int`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", floatType, floatLiteral(Float.NaN, floatType)),
                        returnStmt(typeCast(identifier("x", floatType, 3), intType, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }

        @Test
        fun `cast float positive infinity to long`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", floatType, floatLiteral(Float.POSITIVE_INFINITY, floatType)),
                        returnStmt(typeCast(identifier("x", floatType, 3), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Long.MAX_VALUE, result)
        }

        @Test
        fun `cast float negative infinity to long`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val longType = longType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", floatType, floatLiteral(Float.NEGATIVE_INFINITY, floatType)),
                        returnStmt(typeCast(identifier("x", floatType, 3), longType, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Long.MIN_VALUE, result)
        }
    }

    // ==================== String Casts ====================

    @Nested
    inner class StringCasts {

        @Test
        fun `nullable string to string cast succeeds when non-null`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello", stringType)),
                        returnStmt(typeCast(identifier("x", stringNullable, 3), stringType, stringType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }

        @Test
        fun `safe cast Any containing empty string to string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = stringNullable,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("", stringType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), stringNullable, stringNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("", result)
        }
    }

    // ==================== Collection Type Casts ====================

    @Nested
    inner class CollectionTypeCasts {

        @Test
        fun `list to Any cast succeeds`() {
            val ast = buildTypedAst {
                val listType = listType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        returnStmt(typeCast(identifier("list", listType, 2), anyType, anyType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertIs<List<*>>(result)
        }

        @Test
        fun `Any containing list safe cast to list succeeds`() {
            val ast = buildTypedAst {
                val listType = listType()
                val listNullable = listNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = listNullable,
                    parameters = listOf(TypedParameter("value", anyType)),
                    body = listOf(
                        returnStmt(typeCast(identifier("value", anyType, 2), listNullable, listNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertIs<List<*>>(result)
        }

        @Test
        fun `Any containing string safe cast to list returns null`() {
            val ast = buildTypedAst {
                val listNullable = listNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = listNullable,
                    parameters = listOf(TypedParameter("value", anyType)),
                    body = listOf(
                        returnStmt(typeCast(identifier("value", anyType, 2), listNullable, listNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", "not a list")
            assertNull(result)
        }

        @Test
        fun `Any containing int regular cast to list throws ClassCastException`() {
            val ast = buildTypedAst {
                val listType = listType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = listType,
                    parameters = listOf(TypedParameter("value", anyType)),
                    body = listOf(
                        returnStmt(typeCast(identifier("value", anyType, 2), listType, listType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast, "testFunction", 42)
            }
            assertIs<ClassCastException>(exception.cause)
        }
    }

    // ==================== Safe Cast Compile-Time Optimizations ====================

    @Nested
    inner class SafeCastCompileTimeOptimizations {

        @Test
        fun `safe cast int to int nullable succeeds with same value`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intNullable,
                    body = listOf(
                        returnStmt(typeCast(intLiteral(42, intType), intNullable, intNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `safe cast string to string nullable succeeds`() {
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

        @Test
        fun `safe cast with incompatible primitives returns null - int to boolean`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolNullable = booleanNullableType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = boolNullable,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(1, intType)),
                        returnStmt(typeCast(identifier("x", anyType, 3), boolNullable, boolNullable, isSafe = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertNull(result)
        }
    }
}
