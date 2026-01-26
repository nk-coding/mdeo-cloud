package com.mdeo.script.compiler

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the type check (is / !is) expression compiler.
 *
 * The `is` operator checks if a value is of a specific type at runtime.
 * The `!is` operator is the negation of `is`.
 */
class TypeCheckCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== is - Primitives Same Type ====================

    @Nested
    inner class IsCheckPrimitivesSameType {

        @Test
        fun `int is int returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(42, intType), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `long is long returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(longLiteral(999L, longType), longType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `double is double returns true`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(doubleLiteral(3.14, doubleType), doubleType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `float is float returns true`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(floatLiteral(2.5f, floatType), floatType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `boolean is boolean returns true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(booleanLiteral(true, boolType), boolType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }

    // ==================== is - Primitives Different Type ====================

    @Nested
    inner class IsCheckPrimitivesDifferentType {

        @Test
        fun `int is long returns false - compile time`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(42, intType), longType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `int is double returns false - compile time`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(42, intType), doubleType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `double is int returns false - compile time`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(doubleLiteral(3.14, doubleType), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `float is double returns false - compile time`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(floatLiteral(2.5f, floatType), doubleType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `long is float returns false - compile time`() {
            val ast = buildTypedAst {
                val longType = longType()
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(longLiteral(999L, longType), floatType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== is - Any Type ====================

    @Nested
    inner class IsCheckAnyType {

        @Test
        fun `int is Any returns true - compile time`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(42, intType), anyType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `string is Any returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(stringLiteral("hello", stringType), anyType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing int is int returns true - runtime check`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing string is int returns false - runtime check`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `Any containing long is long returns true - runtime check`() {
            val ast = buildTypedAst {
                val longType = longType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, longLiteral(999L, longType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), longType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing double is double returns true - runtime check`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, doubleLiteral(3.14, doubleType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), doubleType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }

    // ==================== !is - Negation ====================

    @Nested
    inner class IsNegatedCheck {

        @Test
        fun `int !is int returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(42, intType), intType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `int !is long returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(42, intType), longType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing int !is string returns true - runtime check`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), stringType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing string !is string returns false - runtime check`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), stringType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `string !is Any returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(stringLiteral("hello", stringType), anyType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== is - Nullable Types ====================

    @Nested
    inner class IsCheckNullableTypes {

        @Test
        fun `nullable int with value is int returns true - runtime check`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(typeCheck(identifier("x", intNullable, 3), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `nullable int with null is int returns false - runtime check`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        returnStmt(typeCheck(identifier("x", intNullable, 3), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `nullable string with value is string returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, stringLiteral("hello", stringType)),
                        returnStmt(typeCheck(identifier("x", stringNullable, 3), stringType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `nullable string with null is string returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val stringNullable = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", stringNullable, nullLiteral(stringNullable)),
                        returnStmt(typeCheck(identifier("x", stringNullable, 3), stringType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== is - Reference Types ====================

    @Nested
    inner class IsCheckReferenceTypes {

        @Test
        fun `string is string returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(stringLiteral("hello", stringType), stringType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing string is string returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), stringType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }

    // ==================== Compile-time Optimizations ====================

    @Nested
    inner class CompileTimeOptimizations {

        @Test
        fun `primitive is same primitive is always true - no runtime check`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intType, intLiteral(100, intType)),
                        returnStmt(typeCheck(identifier("x", intType, 3), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `primitive is different primitive is always false - no runtime check`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intType, intLiteral(100, intType)),
                        returnStmt(typeCheck(identifier("x", intType, 3), doubleType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    inner class EdgeCases {

        @Test
        fun `null Any is int returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, nullLiteral(anyType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `null Any !is int returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, nullLiteral(anyType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), intType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any is Any returns true`() {
            val ast = buildTypedAst {
                val anyType = anyNullableType()
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), anyType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `double is boolean returns false`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(doubleLiteral(3.14, doubleType), boolType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `boolean is int returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(booleanLiteral(true, boolType), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }
}
