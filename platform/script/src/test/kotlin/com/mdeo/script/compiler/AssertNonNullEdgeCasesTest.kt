package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Additional edge case tests for the assert non-null (!!) operator.
 * 
 * These tests focus on:
 * - Chained usage
 * - Combinations with other operators
 * - Object types (not just primitives)
 * - Error message verification
 */
class AssertNonNullEdgeCasesTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class ChainedUsage {

        @Test
        fun `assert non-null after variable reassignment`() {
            // Variable starts non-null, then is reassigned
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        assignment(
                            identifier("x", intNullable, 3),
                            intLiteral(100, intType)
                        ),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(100, result)
        }
    }

    @Nested
    inner class WithNullCoalescing {

        @Test
        fun `assert non-null on result of null coalescing - non-null left`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(
                            assertNonNull(
                                binaryExpr(
                                    identifier("x", intNullable, 3),
                                    "??",
                                    intLiteral(0, intType),
                                    intNullable
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
        fun `assert non-null on result of null coalescing - null left uses right`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        returnStmt(
                            assertNonNull(
                                binaryExpr(
                                    identifier("x", intNullable, 3),
                                    "??",
                                    intLiteral(999, intType),
                                    intNullable
                                ),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(999, result)
        }
    }

    @Nested
    inner class ObjectTypes {

        @Test
        fun `assert non-null on non-null list passes through`() {
            val ast = buildTypedAst {
                val listType = listType()
                val listNullable = listNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("list", listNullable)),
                    body = listOf(
                        varDecl("result", listType, 
                            assertNonNull(identifier("list", listNullable, 2), listType)
                        ),
                        // Verify same reference
                        returnStmt(
                            binaryExpr(
                                identifier("result", listType, 3),
                                "===",
                                identifier("list", listNullable, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", list)
            assertEquals(true, result)
        }

        @Test
        fun `assert non-null on null list throws NullPointerException`() {
            val ast = buildTypedAst {
                val listType = listType()
                val listNullable = listNullableType()
                function(
                    name = "testFunction",
                    returnType = listType,
                    parameters = listOf(TypedParameter("list", listNullable)),
                    body = listOf(
                        returnStmt(assertNonNull(identifier("list", listNullable, 2), listType))
                    )
                )
            }
            
            assertFailsWith<NullPointerException> {
                helper.compileAndInvoke(ast, "testFunction", null)
            }
        }

        @Test
        fun `assert non-null on non-null string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello world", stringType)),
                        returnStmt(assertNonNull(identifier("x", stringNullable, 3), stringType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello world", result)
        }
    }

    @Nested
    inner class SpecialValues {

        @Test
        fun `assert non-null on double NaN passes through`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.NaN, doubleType)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, (result as Double).isNaN())
        }

        @Test
        fun `assert non-null on positive infinity passes through`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.POSITIVE_INFINITY, doubleType)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Double.POSITIVE_INFINITY, result)
        }

        @Test
        fun `assert non-null on negative infinity passes through`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.NEGATIVE_INFINITY, doubleType)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Double.NEGATIVE_INFINITY, result)
        }

        @Test
        fun `assert non-null on min double value passes through`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.MIN_VALUE, doubleType)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Double.MIN_VALUE, result)
        }

        @Test
        fun `assert non-null on max double value passes through`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.MAX_VALUE, doubleType)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Double.MAX_VALUE, result)
        }
    }

    @Nested
    inner class InExpressionContext {

        @Test
        fun `assert non-null used in arithmetic expression`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(10, intType)),
                        varDecl("y", intNullable, intLiteral(5, intType)),
                        returnStmt(
                            binaryExpr(
                                assertNonNull(identifier("x", intNullable, 3), intType),
                                "+",
                                assertNonNull(identifier("y", intNullable, 3), intType),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(15, result)
        }

        @Test
        fun `assert non-null in comparison expression`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(10, intType)),
                        varDecl("y", intNullable, intLiteral(5, intType)),
                        returnStmt(
                            binaryExpr(
                                assertNonNull(identifier("x", intNullable, 3), intType),
                                ">",
                                assertNonNull(identifier("y", intNullable, 3), intType),
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
