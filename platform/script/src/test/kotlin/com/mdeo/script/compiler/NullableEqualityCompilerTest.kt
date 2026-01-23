package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Additional tests for nullable type equality and edge cases.
 * Tests cover:
 * - Nullable wrapper equality with nullable wrappers
 * - Mixed primitive and nullable wrapper equality  
 * - Edge cases with null strings
 * - Nullable string equality
 */
class NullableEqualityCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Nullable Integer Equality ====================
    
    @Nested
    inner class NullableIntegerEquality {
        
        @Test
        fun `nullable int variable equals null - is null`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableIntType, 3),
                                "==",
                                nullLiteral(nullableIntType),
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
        fun `nullable int variable not equals null - is null`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableIntType, 3),
                                "!=",
                                nullLiteral(nullableIntType),
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
        fun `nullable int variable equals null - is not null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableIntType, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableIntType, 3),
                                "==",
                                nullLiteral(nullableIntType),
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
        fun `nullable int variable not equals null - is not null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableIntType, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableIntType, 3),
                                "!=",
                                nullLiteral(nullableIntType),
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
        fun `null equals nullable int variable - is null`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableIntType, nullLiteral(nullableIntType)),
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableIntType),
                                "==",
                                identifier("x", nullableIntType, 3),
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
        fun `null not equals nullable int variable - is not null`() {
            val ast = buildTypedAst {
                val intType = intType()
                val nullableIntType = intNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableIntType, intLiteral(42, intType)),
                        returnStmt(
                            binaryExpr(
                                nullLiteral(nullableIntType),
                                "!=",
                                identifier("x", nullableIntType, 3),
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
    
    // ==================== Nullable String Equality ====================
    
    @Nested
    inner class NullableStringEquality {
        
        @Test
        fun `nullable string variable equals null - is null`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, nullLiteral(nullableStringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                nullLiteral(nullableStringType),
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
        fun `nullable string variable not equals null - is null`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, nullLiteral(nullableStringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "!=",
                                nullLiteral(nullableStringType),
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
        fun `nullable string variable equals null - is not null`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                nullLiteral(nullableStringType),
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
        fun `nullable string variable not equals null - is not null`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "!=",
                                nullLiteral(nullableStringType),
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
        fun `nullable string equals string - same content`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("test", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                stringLiteral("test", stringType),
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
        fun `nullable string equals string - different content`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("hello", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                stringLiteral("world", stringType),
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
        fun `two nullable strings with same content`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("same", stringType)),
                        varDecl("y", nullableStringType, stringLiteral("same", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                identifier("y", nullableStringType, 3),
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
        fun `two nullable strings with different content`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, stringLiteral("abc", stringType)),
                        varDecl("y", nullableStringType, stringLiteral("xyz", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                identifier("y", nullableStringType, 3),
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
        fun `nullable string null equals nullable string null`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, nullLiteral(nullableStringType)),
                        varDecl("y", nullableStringType, nullLiteral(nullableStringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                identifier("y", nullableStringType, 3),
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
        fun `nullable string null not equals nullable string with value`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val nullableStringType = stringNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableStringType, nullLiteral(nullableStringType)),
                        varDecl("y", nullableStringType, stringLiteral("value", stringType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableStringType, 3),
                                "==",
                                identifier("y", nullableStringType, 3),
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
    
    // ==================== Nullable Boolean Equality ====================
    
    @Nested
    inner class NullableBooleanEquality {
        
        @Test
        fun `nullable boolean variable equals null - is null`() {
            val ast = buildTypedAst {
                val nullableBoolType = booleanNullableType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, nullLiteral(nullableBoolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableBoolType, 3),
                                "==",
                                nullLiteral(nullableBoolType),
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
        fun `nullable boolean with value equals null - is not null`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, booleanLiteral(true, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableBoolType, 3),
                                "==",
                                nullLiteral(nullableBoolType),
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
        fun `two nullable booleans with same value`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, booleanLiteral(true, boolType)),
                        varDecl("y", nullableBoolType, booleanLiteral(true, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableBoolType, 3),
                                "==",
                                identifier("y", nullableBoolType, 3),
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
        fun `two nullable booleans with different values`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, booleanLiteral(true, boolType)),
                        varDecl("y", nullableBoolType, booleanLiteral(false, boolType)),
                        returnStmt(
                            binaryExpr(
                                identifier("x", nullableBoolType, 3),
                                "==",
                                identifier("y", nullableBoolType, 3),
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
        fun `primitive boolean equals nullable boolean - same value`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val nullableBoolType = booleanNullableType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        varDecl("x", nullableBoolType, booleanLiteral(true, boolType)),
                        returnStmt(
                            binaryExpr(
                                booleanLiteral(true, boolType),
                                "==",
                                identifier("x", nullableBoolType, 3),
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
    
    // ==================== Edge Cases ====================
    
    @Nested
    inner class EdgeCases {
        
        @Test
        fun `equality result used in if statement`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        ifStmt(
                            binaryExpr(
                                intLiteral(5, intType),
                                "==",
                                intLiteral(5, intType),
                                boolType
                            ),
                            listOf(returnStmt(intLiteral(1, intType))),
                            elseBlock = listOf(returnStmt(intLiteral(0, intType)))
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1, result)
        }
        
        @Test
        fun `inequality result used in if statement`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        ifStmt(
                            binaryExpr(
                                intLiteral(5, intType),
                                "!=",
                                intLiteral(10, intType),
                                boolType
                            ),
                            listOf(returnStmt(intLiteral(1, intType))),
                            elseBlock = listOf(returnStmt(intLiteral(0, intType)))
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1, result)
        }
        
        @Test
        fun `chained equality with logical and`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    intLiteral(1, intType),
                                    "==",
                                    intLiteral(1, intType),
                                    boolType
                                ),
                                "&&",
                                binaryExpr(
                                    intLiteral(2, intType),
                                    "==",
                                    intLiteral(2, intType),
                                    boolType
                                ),
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
        fun `chained equality with logical or`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                binaryExpr(
                                    intLiteral(1, intType),
                                    "==",
                                    intLiteral(2, intType),
                                    boolType
                                ),
                                "||",
                                binaryExpr(
                                    intLiteral(3, intType),
                                    "==",
                                    intLiteral(3, intType),
                                    boolType
                                ),
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
        fun `equality with ternary expression`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            ternaryExpr(
                                binaryExpr(
                                    intLiteral(10, intType),
                                    "==",
                                    intLiteral(10, intType),
                                    boolType
                                ),
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
        fun `inequality with ternary expression`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            ternaryExpr(
                                binaryExpr(
                                    intLiteral(10, intType),
                                    "!=",
                                    intLiteral(20, intType),
                                    boolType
                                ),
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
        fun `equality in while loop condition`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intType, intLiteral(0, intType)),
                        whileStmt(
                            binaryExpr(
                                identifier("x", intType, 3),
                                "!=",
                                intLiteral(5, intType),
                                boolType
                            ),
                            listOf(
                                assignment(
                                    identifier("x", intType, 3),
                                    binaryExpr(
                                        identifier("x", intType, 3),
                                        "+",
                                        intLiteral(1, intType),
                                        intType
                                    )
                                )
                            )
                        ),
                        returnStmt(identifier("x", intType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(5, result)
        }
    }
}
