package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Edge case tests for type check (is / !is) compiler.
 * These tests cover boundary conditions and complex scenarios.
 */
class TypeCheckEdgeCasesTest {

    private val helper = CompilerTestHelper()

    // ==================== Type Check in Control Flow ====================

    @Nested
    inner class TypeCheckInControlFlow {

        @Test
        fun `is check in if condition`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        ifStmt(
                            condition = typeCheck(identifier("x", anyType, 3), intType, boolType),
                            thenBlock = listOf(returnStmt(intLiteral(1, intType))),
                            elseBlock = listOf(returnStmt(intLiteral(0, intType)))
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1, result)
        }

        @Test
        fun `is check false in if condition takes else branch`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        ifStmt(
                            condition = typeCheck(identifier("x", anyType, 3), intType, boolType),
                            thenBlock = listOf(returnStmt(intLiteral(1, intType))),
                            elseBlock = listOf(returnStmt(intLiteral(0, intType)))
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }

        @Test
        fun `!is check in if condition`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        ifStmt(
                            condition = typeCheck(identifier("x", anyType, 3), intType, boolType, isNegated = true),
                            thenBlock = listOf(returnStmt(intLiteral(1, intType))),
                            elseBlock = listOf(returnStmt(intLiteral(0, intType)))
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1, result)
        }
    }

    // ==================== Combined Type Checks ====================

    @Nested
    inner class CombinedTypeChecks {

        @Test
        fun `is check combined with && operator`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                typeCheck(identifier("x", anyType, 3), intType, boolType),
                                "&&",
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
        fun `is check combined with or operator`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                typeCheck(identifier("x", anyType, 3), intType, boolType),
                                "||",
                                typeCheck(identifier("x", anyType, 3), stringType, boolType),
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
        fun `multiple is checks with &&`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        varDecl("y", anyType, intLiteral(100, intType)),
                        returnStmt(
                            binaryExpr(
                                typeCheck(identifier("x", anyType, 3), intType, boolType),
                                "&&",
                                typeCheck(identifier("y", anyType, 3), intType, boolType),
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

    // ==================== Type Check with Null Handling ====================

    @Nested
    inner class TypeCheckWithNullHandling {

        @Test
        fun `nullable int null value is int check returns false`() {
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
        fun `nullable long null value !is long check returns true`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullable = longNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", longNullable, nullLiteral(longNullable)),
                        returnStmt(typeCheck(identifier("x", longNullable, 3), longType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any null is Any returns false due to instanceof semantics`() {
            val ast = buildTypedAst {
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, nullLiteral(anyType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), anyType, boolType))
                    )
                )
            }
            
            // null instanceof Object returns false in JVM
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== Type Check Result Used in Expressions ====================

    @Nested
    inner class TypeCheckResultUsedInExpressions {

        @Test
        fun `is check result in ternary condition`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(
                            ternaryExpr(
                                typeCheck(identifier("x", anyType, 3), intType, boolType),
                                intLiteral(100, intType),
                                intLiteral(0, intType),
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
        fun `is check result assigned to variable`() {
            val ast = buildTypedAst {
                val intType = intType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        varDecl("result", boolType, typeCheck(identifier("x", anyType, 3), intType, boolType)),
                        returnStmt(identifier("result", boolType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }

    // ==================== All Primitive Type Combinations ====================

    @Nested
    inner class AllPrimitiveTypeCombinations {

        @Test
        fun `int is boolean returns false`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(intLiteral(1, intType), boolType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `boolean is double returns false`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(booleanLiteral(true, boolType), doubleType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `long is boolean returns false`() {
            val ast = buildTypedAst {
                val longType = longType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(longLiteral(1L, longType), boolType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `float is boolean returns false`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(floatLiteral(1.0f, floatType), boolType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== Wrapper Type Checks via Any ====================

    @Nested
    inner class WrapperTypeChecksViaAny {

        @Test
        fun `Any containing Integer is int returns true`() {
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
        fun `Any containing Long is long returns true`() {
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
        fun `Any containing Float is float returns true`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, floatLiteral(2.5f, floatType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), floatType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing Boolean is boolean returns true`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, booleanLiteral(false, boolType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), boolType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `Any containing Integer is long returns false - different wrapper`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, intLiteral(42, intType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), longType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `Any containing Double is float returns false - different wrapper`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val floatType = floatType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", anyType, doubleLiteral(3.14, doubleType)),
                        returnStmt(typeCheck(identifier("x", anyType, 3), floatType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    // ==================== String Type Checks ====================

    @Nested
    inner class StringTypeChecks {

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
        fun `string is int returns false`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(stringLiteral("42", stringType), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
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

        @Test
        fun `empty string is string returns true`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(typeCheck(stringLiteral("", stringType), stringType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }

    // ==================== Collection Type Checks ====================

    @Nested
    inner class CollectionTypeChecks {

        @Test
        fun `list is Any nullable returns true - compile time optimization`() {
            val ast = buildTypedAst {
                val listType = listType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        returnStmt(typeCheck(identifier("list", listType, 2), anyType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertEquals(true, result)
        }

        @Test
        fun `list is not Any nullable returns false - negation of compile time true`() {
            val ast = buildTypedAst {
                val listType = listType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        returnStmt(typeCheck(identifier("list", listType, 2), anyType, boolType, isNegated = true))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertEquals(false, result)
        }

        @Test
        fun `list is list returns true`() {
            val ast = buildTypedAst {
                val listType = listType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        returnStmt(typeCheck(identifier("list", listType, 2), listType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertEquals(true, result)
        }

        @Test
        fun `list is int returns false - incompatible types`() {
            val ast = buildTypedAst {
                val listType = listType()
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        returnStmt(typeCheck(identifier("list", listType, 2), intType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertEquals(false, result)
        }

        @Test
        fun `Any containing list is list returns true - runtime check`() {
            val ast = buildTypedAst {
                val listType = listType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("value", anyType)),
                    body = listOf(
                        returnStmt(typeCheck(identifier("value", anyType, 2), listType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
            assertEquals(true, result)
        }

        @Test
        fun `Any containing string is list returns false - runtime check`() {
            val ast = buildTypedAst {
                val listType = listType()
                val anyType = anyNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    parameters = listOf(TypedParameter("value", anyType)),
                    body = listOf(
                        returnStmt(typeCheck(identifier("value", anyType, 2), listType, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast, "testFunction", "not a list")
            assertEquals(false, result)
        }
    }

    // ==================== Nullable Source Type Edge Cases ====================

    @Nested
    inner class NullableSourceTypeEdgeCases {

        @Test
        fun `nullable int with value is nullable int returns true`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, intLiteral(42, intType)),
                        returnStmt(typeCheck(identifier("x", intNullable, 3), intNullable, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `nullable int with null is nullable int returns true - nullable check`() {
            val ast = buildTypedAst {
                val intNullable = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", intNullable, nullLiteral(intNullable)),
                        returnStmt(typeCheck(identifier("x", intNullable, 3), intNullable, boolType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `string is Any returns true - compile time`() {
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
    }
}
