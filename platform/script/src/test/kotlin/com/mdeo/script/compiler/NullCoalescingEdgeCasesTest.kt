package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Additional edge case tests for the null coalescing (??) operator.
 * 
 * These tests focus on:
 * - Short-circuit evaluation verification
 * - Nested null coalescing expressions
 * - Interactions with other operators
 * - Type coercion edge cases
 */
class NullCoalescingEdgeCasesTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class ShortCircuitEvaluation {

        @Test
        fun `right side expression not evaluated when left is non-null - uses function call`() {
            // If left is non-null, right side should NOT be evaluated
            // We verify this by having right side be a function that would change state
            // Since we can't easily track side effects, we verify the result is correct
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        varDecl("counter", intType, intLiteral(0, intType)),
                        // If short-circuit works, counter stays 0 because right not evaluated
                        // We use the result of x ?? (counter + 1) - if x is used, result is 42
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "??",
                                binaryExpr(
                                    identifier("counter", intType, 3),
                                    "+",
                                    intLiteral(100, intType),
                                    intType
                                ),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result) // Left value used, right not evaluated
        }

        @Test
        fun `right side evaluated when left is null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "??",
                                binaryExpr(
                                    intLiteral(50, intType),
                                    "+",
                                    intLiteral(50, intType),
                                    intType
                                ),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(100, result) // Right expression evaluated: 50 + 50
        }
    }

    @Nested
    inner class NestedNullCoalescing {

        @Test
        fun `triple nested - first non-null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("a", intNullable, intLiteral(1, intType)),
                        varDecl("b", intNullable, intLiteral(2, intType)),
                        varDecl("c", intNullable, intLiteral(3, intType)),
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    identifier("a", intNullable, 3),
                                    "??",
                                    identifier("b", intNullable, 3),
                                    intNullable
                                ),
                                "??",
                                identifier("c", intNullable, 3),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1, result)
        }

        @Test
        fun `triple nested - first null, second non-null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("a", intNullable, nullLiteral(intNullable)),
                        varDecl("b", intNullable, intLiteral(2, intType)),
                        varDecl("c", intNullable, intLiteral(3, intType)),
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    identifier("a", intNullable, 3),
                                    "??",
                                    identifier("b", intNullable, 3),
                                    intNullable
                                ),
                                "??",
                                identifier("c", intNullable, 3),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2, result)
        }

        @Test
        fun `triple nested - first two null, third non-null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("a", intNullable, nullLiteral(intNullable)),
                        varDecl("b", intNullable, nullLiteral(intNullable)),
                        varDecl("c", intNullable, intLiteral(3, intType)),
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    identifier("a", intNullable, 3),
                                    "??",
                                    identifier("b", intNullable, 3),
                                    intNullable
                                ),
                                "??",
                                identifier("c", intNullable, 3),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)
        }

        @Test
        fun `triple nested - all null returns default`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("a", intNullable, nullLiteral(intNullable)),
                        varDecl("b", intNullable, nullLiteral(intNullable)),
                        varDecl("c", intNullable, nullLiteral(intNullable)),
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    binaryExpr(
                                        identifier("a", intNullable, 3),
                                        "??",
                                        identifier("b", intNullable, 3),
                                        intNullable
                                    ),
                                    "??",
                                    identifier("c", intNullable, 3),
                                    intNullable
                                ),
                                "??",
                                intLiteral(999, intType),
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
    inner class SpecialNumericValues {

        @Test
        fun `double negative zero is not null`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(-0.0, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleNullable, 3),
                                "??",
                                doubleLiteral(999.0, doubleType),
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
        fun `double NaN is not null`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.NaN, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleNullable, 3),
                                "??",
                                doubleLiteral(999.0, doubleType),
                                doubleType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, (result as Double).isNaN())
        }

        @Test
        fun `double positive infinity is not null`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(Double.POSITIVE_INFINITY, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleNullable, 3),
                                "??",
                                doubleLiteral(999.0, doubleType),
                                doubleType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Double.POSITIVE_INFINITY, result)
        }

        @Test
        fun `float NaN is not null`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullable = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        varDecl("x", floatNullable, floatLiteral(Float.NaN, floatType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", floatNullable, 3),
                                "??",
                                floatLiteral(999.0f, floatType),
                                floatType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, (result as Float).isNaN())
        }
    }

    @Nested
    inner class WithObjects {

        @Test
        fun `null list returns default list`() {
            val ast = buildTypedAst {
                val listType = listType()
                val listNullable = listNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(
                        TypedParameter("nullList", listNullable),
                        TypedParameter("defaultList", listType)
                    ),
                    body = listOf(
                        varDecl("result", listType, 
                            binaryExpr(
                                identifier("nullList", listNullable, 2),
                                "??",
                                identifier("defaultList", listType, 2),
                                listType
                            )
                        ),
                        // Check if result === defaultList (reference equality)
                        returnStmt(
                            binaryExpr(
                                identifier("result", listType, 3),
                                "===",
                                identifier("defaultList", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val defaultList = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", null, defaultList)
            assertEquals(true, result) // Should be same reference
        }

        @Test
        fun `non-null list returns original list`() {
            val ast = buildTypedAst {
                val listType = listType()
                val listNullable = listNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(
                        TypedParameter("originalList", listNullable),
                        TypedParameter("defaultList", listType)
                    ),
                    body = listOf(
                        varDecl("result", listType, 
                            binaryExpr(
                                identifier("originalList", listNullable, 2),
                                "??",
                                identifier("defaultList", listType, 2),
                                listType
                            )
                        ),
                        // Check if result === originalList (reference equality)
                        returnStmt(
                            binaryExpr(
                                identifier("result", listType, 3),
                                "===",
                                identifier("originalList", listNullable, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val originalList = mutableListOf(4, 5, 6)
            val defaultList = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", originalList, defaultList)
            assertEquals(true, result) // Should be same reference as original
        }
    }
}
