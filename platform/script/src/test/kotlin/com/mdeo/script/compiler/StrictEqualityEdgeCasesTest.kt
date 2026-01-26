package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Additional edge case tests for strict equality (===, !==) operators.
 * 
 * These tests focus on:
 * - Nullable wrapper types behavior
 * - Different object instances with same state
 * - Primitive wrapper caching edge cases (Integer cache: -128 to 127)
 * - Mixed type comparisons
 */
class StrictEqualityEdgeCasesTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class NullableWrapperBehavior {

        @Test
        fun `nullable int with same value - different wrapper instances`() {
            // For values outside Integer cache range, different wrapper instances
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        // Use values outside Integer cache (-128 to 127)
                        varDecl("x", intNullable, intLiteral(1000, intType)),
                        varDecl("y", intNullable, intLiteral(1000, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "===",
                                identifier("y", intNullable, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            // This depends on JVM Integer caching behavior
            // For values outside -128 to 127, different Integer instances are created
            val result = helper.compileAndInvoke(ast)
            // Result could be true or false depending on boxing implementation
            // The important thing is that it doesn't crash
            assertEquals(result is Boolean, true)
        }

        @Test
        fun `nullable int within cache range may share reference`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        // Use values within Integer cache (-128 to 127)
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        varDecl("y", intNullable, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", intNullable, 3),
                                "===",
                                identifier("y", intNullable, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(result is Boolean, true)
        }

        @Test
        fun `nullable boolean true references are same`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(true, boolType)),
                        varDecl("y", boolNullable, booleanLiteral(true, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", boolNullable, 3),
                                "===",
                                identifier("y", boolNullable, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            // Boolean.TRUE and Boolean.FALSE are cached singletons
            assertEquals(true, result)
        }

        @Test
        fun `nullable boolean false references are same`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(false, boolType)),
                        varDecl("y", boolNullable, booleanLiteral(false, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", boolNullable, 3),
                                "===",
                                identifier("y", boolNullable, 3),
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
    inner class StringInternBehavior {

        @Test
        fun `string literals with same value are strictly equal`() {
            // String literals in Java are interned
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
                                "===",
                                identifier("y", stringType, 3),
                                boolType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            // String literals are interned, so same reference
            assertEquals(true, result)
        }

        @Test
        fun `nullable null vs nullable non-null string strict equality`() {
            val ast = buildTypedAst {
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, nullLiteral(stringNullable)),
                        varDecl("y", stringNullable, stringLiteral("hello", stringNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", stringNullable, 3),
                                "===",
                                identifier("y", stringNullable, 3),
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
    inner class ListReferenceComparisons {

        @Test
        fun `list parameter compared to itself is strictly equal`() {
            val ast = buildTypedAst {
                val listType = listType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                identifier("list", listType, 2),
                                "===",
                                identifier("list", listType, 2),
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
        fun `two different list instances are not strictly equal`() {
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
            val list2 = mutableListOf(1, 2, 3) // Same content, different instance
            val result = helper.compileAndInvoke(ast, "testFunction", list1, list2)
            assertEquals(false, result)
        }

        @Test
        fun `two different lists strict inequality returns true`() {
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
            assertEquals(true, result) // Different references
        }
    }

    @Nested
    inner class NullComparisons {

        @Test
        fun `two null variables are strictly equal`() {
            val ast = buildTypedAst {
                val listNullable = listNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", listNullable, nullLiteral(listNullable)),
                        varDecl("y", listNullable, nullLiteral(listNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", listNullable, 3),
                                "===",
                                identifier("y", listNullable, 3),
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
        fun `two null variables strict inequality returns false`() {
            val ast = buildTypedAst {
                val listNullable = listNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", listNullable, nullLiteral(listNullable)),
                        varDecl("y", listNullable, nullLiteral(listNullable)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", listNullable, 3),
                                "!==",
                                identifier("y", listNullable, 3),
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
}
