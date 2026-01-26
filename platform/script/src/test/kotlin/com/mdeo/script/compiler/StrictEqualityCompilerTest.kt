package com.mdeo.script.compiler

import com.mdeo.script.stdlib.impl.collections.ListImpl
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the strict equality (===, !==) operators compiler.
 *
 * Strict equality uses reference comparison for objects (like Java's ==).
 * For primitives, it compares values directly.
 */
class StrictEqualityCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== Reference equality with same object ====================

    @Nested
    inner class SameReferenceIsEqual {

        @Test
        fun `same string reference is strictly equal`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringType, 3),
                                "===",
                                identifier("x", stringType, 3),
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
        fun `same reference strict inequality is false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringType, 3),
                                "!==",
                                identifier("x", stringType, 3),
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

    // ==================== Primitive strict equality ====================

    @Nested
    inner class PrimitiveStrictEquality {

        @Test
        fun `int strict equality - same value`() {
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
                                "===",
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
        fun `int strict equality - different value`() {
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
                                "===",
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
        fun `int strict inequality - same value`() {
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
                                "!==",
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
        fun `int strict inequality - different value`() {
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
                                "!==",
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
        fun `long strict equality`() {
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
                                "===",
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
        fun `double strict equality`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                doubleLiteral(3.14, doubleType),
                                "===",
                                doubleLiteral(3.14, doubleType),
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
        fun `boolean strict equality - both true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "===",
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
        fun `boolean strict equality - both false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(false, boolType),
                                "===",
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
    }

    // ==================== Null comparisons ====================

    @Nested
    inner class NullComparisons {

        @Test
        fun `null strictly equals null`() {
            val ast = buildTypedAst {
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                nullLiteral(stringNullable),
                                "===",
                                nullLiteral(stringNullable),
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
        fun `null strictly not equals non-null`() {
            val ast = buildTypedAst {
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello", stringNullable)),
                        returnStmt(
                            binaryExpr(
                                nullLiteral(stringNullable),
                                "===",
                                identifier("x", stringNullable, 3),
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
        fun `non-null strictly not equals null`() {
            val ast = buildTypedAst {
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello", stringNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "===",
                                nullLiteral(stringNullable),
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
        fun `null variable strictly equals null`() {
            val ast = buildTypedAst {
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, nullLiteral(stringNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "===",
                                nullLiteral(stringNullable),
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
