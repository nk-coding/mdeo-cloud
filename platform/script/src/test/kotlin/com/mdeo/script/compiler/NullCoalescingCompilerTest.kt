package com.mdeo.script.compiler

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the null coalescing (??) operator compiler.
 *
 * The ?? operator returns the left operand if it's not null,
 * otherwise evaluates and returns the right operand.
 */
class NullCoalescingCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== Left is non-null, returns left ====================

    @Nested
    inner class NonNullLeftReturnsLeft {

        @Test
        fun `non-null int returns left value`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "??",
                                intLiteral(100, intType),
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
        fun `non-null long returns left value`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", longNullable, longLiteral(9999999999L, longType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", longNullable, 3),
                                "??",
                                longLiteral(0L, longType),
                                longType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(9999999999L, result)
        }

        @Test
        fun `non-null double returns left value`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(3.14159, doubleType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleNullable, 3),
                                "??",
                                doubleLiteral(0.0, doubleType),
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
        fun `non-null float returns left value`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullable = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        varDecl("x", floatNullable, floatLiteral(2.5f, floatType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", floatNullable, 3),
                                "??",
                                floatLiteral(0.0f, floatType),
                                floatType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.5f, result)
        }

        @Test
        fun `non-null string returns left value`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "??",
                                stringLiteral("default", stringType),
                                stringType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }

        @Test
        fun `non-null boolean returns left value - true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(true, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", boolNullable, 3),
                                "??",
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
        fun `non-null boolean returns left value - false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(false, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", boolNullable, 3),
                                "??",
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

    // ==================== Left is null, returns right ====================

    @Nested
    inner class NullLeftReturnsRight {

        @Test
        fun `null int returns right value`() {
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
                                intLiteral(100, intType),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(100, result)
        }

        @Test
        fun `null long returns right value`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", longNullable, nullLiteral(longNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", longNullable, 3),
                                "??",
                                longLiteral(123456789L, longType),
                                longType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(123456789L, result)
        }

        @Test
        fun `null double returns right value`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, nullLiteral(doubleNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", doubleNullable, 3),
                                "??",
                                doubleLiteral(2.71828, doubleType),
                                doubleType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.71828, result)
        }

        @Test
        fun `null float returns right value`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullable = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        varDecl("x", floatNullable, nullLiteral(floatNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", floatNullable, 3),
                                "??",
                                floatLiteral(1.5f, floatType),
                                floatType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1.5f, result)
        }

        @Test
        fun `null string returns right value`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, nullLiteral(stringNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "??",
                                stringLiteral("default", stringType),
                                stringType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("default", result)
        }

        @Test
        fun `null boolean returns right value`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, nullLiteral(boolNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", boolNullable, 3),
                                "??",
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
    }

    // ==================== Edge cases ====================

    @Nested
    inner class EdgeCases {

        @Test
        fun `zero is not treated as null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(0, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "??",
                                intLiteral(999, intType),
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
        fun `empty string is not treated as null`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "??",
                                stringLiteral("default", stringType),
                                stringType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("", result)
        }

        @Test
        fun `false is not treated as null`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(false, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", boolNullable, 3),
                                "??",
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
        fun `negative number is not treated as null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(-1, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "??",
                                intLiteral(999, intType),
                                intType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-1, result)
        }

        @Test
        fun `chained null coalescing with first null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        varDecl("y", intNullable, intLiteral(50, intType)),
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    identifier("x", intNullable, 3),
                                    "??",
                                    identifier("y", intNullable, 3),
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
            assertEquals(50, result)
        }

        @Test
        fun `chained null coalescing with both null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        varDecl("y", intNullable, nullLiteral(intNullable)),
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    identifier("x", intNullable, 3),
                                    "??",
                                    identifier("y", intNullable, 3),
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
}
