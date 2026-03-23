package com.mdeo.script.compiler.stdlib

import com.mdeo.script.compiler.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for sqrt and trigonometric methods on Double and Float types.
 */
class NumericMathMethodsIntegrationTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class DoubleSqrtMethod {

        @Test
        fun `sqrt of 4 returns 2`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(4.0, doubleType), "sqrt", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(2.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `sqrt of 9 returns 3`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(9.0, doubleType), "sqrt", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(3.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `sqrt of 2 returns approximately 1_414`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(2.0, doubleType), "sqrt", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.41421356, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleSinMethod {

        @Test
        fun `sin of 0 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "sin", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `sin of pi over 2 returns 1`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(PI / 2, doubleType), "sin", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `sin of pi returns approximately 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(PI, doubleType), "sin", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleCosMethod {

        @Test
        fun `cos of 0 returns 1`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "cos", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `cos of pi returns -1`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(PI, doubleType), "cos", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(-1.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleTanMethod {

        @Test
        fun `tan of 0 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "tan", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `tan of pi over 4 returns 1`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(PI / 4, doubleType), "tan", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleAsinMethod {

        @Test
        fun `asin of 0 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "asin", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `asin of 1 returns pi over 2`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(1.0, doubleType), "asin", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(PI / 2, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleAcosMethod {

        @Test
        fun `acos of 1 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(1.0, doubleType), "acos", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `acos of -1 returns pi`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(-1.0, doubleType), "acos", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(PI, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleAtanMethod {

        @Test
        fun `atan of 0 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "atan", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `atan of 1 returns pi over 4`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(1.0, doubleType), "atan", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(PI / 4, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleSinhMethod {

        @Test
        fun `sinh of 0 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "sinh", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `sinh of 1 is approximately 1_175`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(1.0, doubleType), "sinh", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.1752012, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleCoshMethod {

        @Test
        fun `cosh of 0 returns 1`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "cosh", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `cosh of 1 is approximately 1_543`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(1.0, doubleType), "cosh", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(1.5430806, helper.compileAndInvoke(ast) as Double, 0.0001)
        }
    }

    @Nested
    inner class DoubleTanhMethod {

        @Test
        fun `tanh of 0 returns 0`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(0.0, doubleType), "tanh", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            assertEquals(0.0, helper.compileAndInvoke(ast) as Double, 0.0001)
        }

        @Test
        fun `tanh result is between -1 and 1`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(doubleLiteral(2.0, doubleType), "tanh", "", emptyList(), resultTypeIndex = doubleType)
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast) as Double
            assertTrue(result > -1.0 && result < 1.0)
        }
    }

    @Nested
    inner class FloatSqrtMethod {

        @Test
        fun `sqrt of 4 returns 2`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(4.0f, floatType), "sqrt", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(2.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }

        @Test
        fun `sqrt of 9 returns 3`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(9.0f, floatType), "sqrt", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(3.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }
    }

    @Nested
    inner class FloatSinMethod {

        @Test
        fun `sin of 0 returns 0`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(0.0f, floatType), "sin", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(0.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }

        @Test
        fun `sin of pi over 2 returns 1`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral((PI / 2).toFloat(), floatType), "sin", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(1.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }
    }

    @Nested
    inner class FloatCosMethod {

        @Test
        fun `cos of 0 returns 1`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(0.0f, floatType), "cos", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(1.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }

        @Test
        fun `cos of pi returns -1`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(PI.toFloat(), floatType), "cos", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(-1.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }
    }

    @Nested
    inner class FloatTanhMethod {

        @Test
        fun `tanh of 0 returns 0`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(0.0f, floatType), "tanh", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            assertEquals(0.0f, helper.compileAndInvoke(ast) as Float, 0.0001f)
        }

        @Test
        fun `tanh result is between -1 and 1`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(floatLiteral(2.0f, floatType), "tanh", "", emptyList(), resultTypeIndex = floatType)
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast) as Float
            assertTrue(result > -1.0f && result < 1.0f)
        }
    }
}
