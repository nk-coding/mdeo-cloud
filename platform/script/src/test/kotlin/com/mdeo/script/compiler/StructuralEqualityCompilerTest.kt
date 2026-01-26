package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the updated structural equality (==, !=) operators with .equals() semantics.
 *
 * Structural equality uses .equals() for object comparison (Kotlin semantics).
 * For primitives, it compares values directly.
 * All comparisons are null-safe (no exceptions when one side is null).
 */
class StructuralEqualityCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== String structural equality ====================

    @Nested
    inner class StringStructuralEquality {

        @Test
        fun `equal strings return true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringType, stringLiteral("hello", stringType)),
                        varDecl("y", stringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringType, 3),
                                "==",
                                identifier("y", stringType, 3),
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
        fun `different strings return false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringType, stringLiteral("hello", stringType)),
                        varDecl("y", stringType, stringLiteral("world", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringType, 3),
                                "==",
                                identifier("y", stringType, 3),
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
        fun `different strings inequality returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringType, stringLiteral("hello", stringType)),
                        varDecl("y", stringType, stringLiteral("world", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringType, 3),
                                "!=",
                                identifier("y", stringType, 3),
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
        fun `same string inequality returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringType, stringLiteral("hello", stringType)),
                        varDecl("y", stringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringType, 3),
                                "!=",
                                identifier("y", stringType, 3),
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

    // ==================== Null-safe equality ====================

    @Nested
    inner class NullSafeEquality {

        @Test
        fun `null equals null returns true`() {
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
                                "==",
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
        fun `null equals non-null returns false`() {
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
                                "==",
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
        fun `non-null equals null returns false`() {
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
                                "==",
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
        fun `null not equals non-null returns true`() {
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
                                "!=",
                                identifier("x", stringNullable, 3),
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
        fun `null variable equals null returns true`() {
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
                                "==",
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
        fun `two null variables are equal`() {
            val ast = buildTypedAst {
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, nullLiteral(stringNullable)),
                        varDecl("y", stringNullable, nullLiteral(stringNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "==",
                                identifier("y", stringNullable, 3),
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

    // ==================== Primitive equality (unchanged behavior) ====================

    @Nested
    inner class PrimitiveEquality {

        @Test
        fun `int equality - same value`() {
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
        fun `int equality - different values`() {
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
        fun `long equality`() {
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
        fun `double equality`() {
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
                                "==",
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
        fun `boolean equality`() {
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
    }

    // ==================== Collection equality with .equals() ====================

    @Nested
    inner class CollectionEquality {

        @Test
        fun `list equals list with same content returns true`() {
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
            
            val list1 = mutableListOf(1, 2, 3)
            val list2 = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }

        @Test
        fun `list not equals list with different content`() {
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
            
            val list1 = mutableListOf(1, 2, 3)
            val list2 = mutableListOf(1, 2, 4)
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(false, result)
        }

        @Test
        fun `same list reference with strict equality returns true`() {
            val ast = buildTypedAst {
                val listType = listType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(
                        TypedParameter("list1", listType)
                    ),
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                identifier("list1", listType, 2),
                                "===",
                                identifier("list1", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", list1)
            assertEquals(true, result)
        }

        @Test
        fun `different list instances with strict equality returns false even if content equal`() {
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
                                "===",
                                identifier("list2", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf(1, 2, 3)
            val list2 = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(false, result)
        }

        @Test
        fun `list inequality returns true for different content`() {
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
                                "!=",
                                identifier("list2", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf(1, 2, 3)
            val list2 = mutableListOf(4, 5, 6)
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }

        @Test
        fun `list strict inequality returns true for different references`() {
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
                                "!==",
                                identifier("list2", listType, 2),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val list1 = mutableListOf(1, 2, 3)
            val list2 = mutableListOf(1, 2, 3)
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }

        @Test
        fun `empty lists are equal`() {
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
            
            val list1 = mutableListOf<Int>()
            val list2 = mutableListOf<Int>()
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(true, result)
        }
    }
}
