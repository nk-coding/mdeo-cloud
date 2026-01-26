package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Additional edge case tests for structural equality (==, !=) operators.
 * 
 * These tests focus on:
 * - .equals() semantics with objects
 * - NaN comparison edge cases
 * - Mixed type numeric comparisons
 * - Nullable numeric wrapper comparisons
 */
class StructuralEqualityEdgeCasesTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class NaNComparisons {

        @Test
        fun `double NaN not equal to itself`() {
            // NaN is never equal to anything, including itself
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.NaN, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleType, 3),
                                "==",
                                identifier("x", doubleType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result) // NaN != NaN
        }

        @Test
        fun `double NaN inequality with itself returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.NaN, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleType, 3),
                                "!=",
                                identifier("x", doubleType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result) // NaN != NaN is true
        }

        @Test
        fun `float NaN not equal to itself`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", floatType, floatLiteral(Float.NaN, floatType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", floatType, 3),
                                "==",
                                identifier("x", floatType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result) // NaN != NaN
        }
    }

    @Nested
    inner class PositiveAndNegativeZero {

        @Test
        fun `positive zero equals negative zero for double`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(0.0, doubleType)),
                        varDecl("y", doubleType, doubleLiteral(-0.0, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleType, 3),
                                "==",
                                identifier("y", doubleType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            // In Java, 0.0 == -0.0 returns true
            assertEquals(true, result)
        }

        @Test
        fun `positive zero equals negative zero for float`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", floatType, floatLiteral(0.0f, floatType)),
                        varDecl("y", floatType, floatLiteral(-0.0f, floatType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", floatType, 3),
                                "==",
                                identifier("y", floatType, 3),
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

    @Nested
    inner class InfinityComparisons {

        @Test
        fun `positive infinity equals positive infinity`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.POSITIVE_INFINITY, doubleType)),
                        varDecl("y", doubleType, doubleLiteral(Double.POSITIVE_INFINITY, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleType, 3),
                                "==",
                                identifier("y", doubleType, 3),
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
        fun `negative infinity equals negative infinity`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.NEGATIVE_INFINITY, doubleType)),
                        varDecl("y", doubleType, doubleLiteral(Double.NEGATIVE_INFINITY, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleType, 3),
                                "==",
                                identifier("y", doubleType, 3),
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
        fun `positive infinity not equals negative infinity`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleType, doubleLiteral(Double.POSITIVE_INFINITY, doubleType)),
                        varDecl("y", doubleType, doubleLiteral(Double.NEGATIVE_INFINITY, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleType, 3),
                                "!=",
                                identifier("y", doubleType, 3),
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

    @Nested
    inner class NullableNumericComparisons {

        @Test
        fun `nullable int null vs nullable int null are equal`() {
            val ast = buildTypedAst {
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        varDecl("y", intNullable, nullLiteral(intNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "==",
                                identifier("y", intNullable, 3),
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
        fun `nullable int with value vs null inequality returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        varDecl("y", intNullable, nullLiteral(intNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "!=",
                                identifier("y", intNullable, 3),
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
        fun `nullable double with same value are equal`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(3.14159, doubleType)),
                        varDecl("y", doubleNullable, doubleLiteral(3.14159, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleNullable, 3),
                                "==",
                                identifier("y", doubleNullable, 3),
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

    @Nested
    inner class MixedTypeNullableComparisons {

        @Test
        fun `nullable int vs non-nullable int with same value are equal`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        varDecl("y", intType, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "==",
                                identifier("y", intType, 3),
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
        fun `null nullable int vs non-nullable int are not equal`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        varDecl("y", intType, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "==",
                                identifier("y", intType, 3),
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

    @Nested
    inner class CollectionStructuralEquality {

        @Test
        fun `two lists with same content are structurally equal`() {
            val ast = buildTypedAst {
                val listType = listType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(
                        TypedParameter("list1", listType),
                        TypedParameter("list2", listType)
                    ),
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                identifier("list1", listType, 2),
                                "==",
                                identifier("list2", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf("a", "b", "c")
            val list2 = mutableListOf("a", "b", "c")
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }

        @Test
        fun `empty list equals empty list`() {
            val ast = buildTypedAst {
                val listType = listType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(
                        TypedParameter("list1", listType),
                        TypedParameter("list2", listType)
                    ),
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                identifier("list1", listType, 2),
                                "==",
                                identifier("list2", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf<Any>()
            val list2 = mutableListOf<Any>()
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }

        @Test
        fun `list with null element equals list with null element`() {
            val ast = buildTypedAst {
                val listType = listType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(
                        TypedParameter("list1", listType),
                        TypedParameter("list2", listType)
                    ),
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                identifier("list1", listType, 2),
                                "==",
                                identifier("list2", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf<Any?>(1, null, 3)
            val list2 = mutableListOf<Any?>(1, null, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }
    }
}
