package com.mdeo.script.compiler

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Tests for the assert non-null (!!) expression compiler.
 *
 * The !! operator evaluates the expression and throws NullPointerException
 * if the result is null. Otherwise, it returns the non-null value.
 */
class AssertNonNullCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== Non-null values pass through ====================

    @Nested
    inner class NonNullValuesPassThrough {

        @Test
        fun `non-null nullable int passes through`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `non-null nullable long passes through`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", longNullable, longLiteral(9999999999L, longType)),
                        returnStmt(assertNonNull(identifier("x", longNullable, 3), longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(9999999999L, result)
        }

        @Test
        fun `non-null nullable double passes through`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, doubleLiteral(3.14159, doubleType)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14159, result)
        }

        @Test
        fun `non-null nullable float passes through`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullable = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        varDecl("x", floatNullable, floatLiteral(2.5f, floatType)),
                        returnStmt(assertNonNull(identifier("x", floatNullable, 3), floatType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.5f, result)
        }

        @Test
        fun `non-null nullable string passes through`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello", stringType)),
                        returnStmt(assertNonNull(identifier("x", stringNullable, 3), stringType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }

        @Test
        fun `non-null nullable boolean passes through - true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(true, boolType)),
                        returnStmt(assertNonNull(identifier("x", boolNullable, 3), boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `non-null nullable boolean passes through - false`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(false, boolType)),
                        returnStmt(assertNonNull(identifier("x", boolNullable, 3), boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== Null values throw NullPointerException ====================

    @Nested
    inner class NullValuesThrow {

        @Test
        fun `null int throws NullPointerException`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast)
            }
            assertIs<NullPointerException>(exception.cause)
        }

        @Test
        fun `null long throws NullPointerException`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", longNullable, nullLiteral(longNullable)),
                        returnStmt(assertNonNull(identifier("x", longNullable, 3), longType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast)
            }
            assertIs<NullPointerException>(exception.cause)
        }

        @Test
        fun `null double throws NullPointerException`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val doubleNullable = doubleNullableType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", doubleNullable, nullLiteral(doubleNullable)),
                        returnStmt(assertNonNull(identifier("x", doubleNullable, 3), doubleType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast)
            }
            assertIs<NullPointerException>(exception.cause)
        }

        @Test
        fun `null string throws NullPointerException`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, nullLiteral(stringNullable)),
                        returnStmt(assertNonNull(identifier("x", stringNullable, 3), stringType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast)
            }
            assertIs<NullPointerException>(exception.cause)
        }

        @Test
        fun `null boolean throws NullPointerException`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, nullLiteral(boolNullable)),
                        returnStmt(assertNonNull(identifier("x", boolNullable, 3), boolType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast)
            }
            assertIs<NullPointerException>(exception.cause)
        }

        @Test
        fun `null float throws NullPointerException`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullable = floatNullableType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        varDecl("x", floatNullable, nullLiteral(floatNullable)),
                        returnStmt(assertNonNull(identifier("x", floatNullable, 3), floatType))
                    )
                )
            }
            
            val exception = assertFailsWith<java.lang.reflect.InvocationTargetException> {
                helper.compileAndInvoke(ast)
            }
            assertIs<NullPointerException>(exception.cause)
        }
    }

    // ==================== Edge cases ====================

    @Nested
    inner class EdgeCases {

        @Test
        fun `zero value passes through not treated as null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(0, intType)),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }

        @Test
        fun `empty string passes through not treated as null`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("", stringType)),
                        returnStmt(assertNonNull(identifier("x", stringNullable, 3), stringType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals("", result)
        }

        @Test
        fun `negative int passes through`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(-42, intType)),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(-42, result)
        }

        @Test
        fun `false boolean passes through`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val boolNullable = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", boolNullable, booleanLiteral(false, boolType)),
                        returnStmt(assertNonNull(identifier("x", boolNullable, 3), boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `max int value passes through`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(Int.MAX_VALUE, intType)),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MAX_VALUE, result)
        }

        @Test
        fun `min int value passes through`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(Int.MIN_VALUE, intType)),
                        returnStmt(assertNonNull(identifier("x", intNullable, 3), intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(Int.MIN_VALUE, result)
        }
    }
}
