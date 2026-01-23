package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for short-circuit boolean operators (&& and ||).
 * 
 * These tests verify:
 * - Basic AND/OR behavior
 * - Short-circuit evaluation (right side NOT evaluated when result is determined)
 * - Nested boolean expressions
 * - Combinations with other expressions
 */
class ShortCircuitBooleanCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `logical AND returns true when both operands are true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(true, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `logical AND returns false when left is false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(false, boolType), "&&", booleanLiteral(true, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `logical AND returns false when right is false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(false, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `logical AND returns false when both are false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(false, boolType), "&&", booleanLiteral(false, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `logical OR returns true when left is true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(true, boolType), "||", booleanLiteral(false, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `logical OR returns true when right is true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(true, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `logical OR returns true when both are true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(true, boolType), "||", booleanLiteral(true, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `logical OR returns false when both are false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(false, boolType), boolType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `logical AND short-circuits when left is false - division by zero not executed`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(false, boolType),
                            "&&",
                            binaryExpr(
                                binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                ">",
                                intLiteral(0, intType),
                                boolType
                            ),
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
    fun `logical OR short-circuits when left is true - division by zero not executed`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(true, boolType),
                            "||",
                            binaryExpr(
                                binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                ">",
                                intLiteral(0, intType),
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
    fun `nested AND expressions - all true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(true, boolType), boolType),
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
    fun `nested AND expressions - one false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(false, boolType), boolType),
                            "&&",
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
    fun `nested OR expressions - all false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(false, boolType), boolType),
                            "||",
                            booleanLiteral(false, boolType),
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
    fun `nested OR expressions - one true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(true, boolType), boolType),
                            "||",
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
    fun `mixed AND and OR - AND has higher precedence in structure`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(false, boolType),
                            "||",
                            binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(true, boolType), boolType),
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
    fun `AND with comparison expressions`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(intLiteral(5, intType), ">", intLiteral(3, intType), boolType),
                            "&&",
                            binaryExpr(intLiteral(10, intType), "<", intLiteral(20, intType), boolType),
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
    fun `OR with comparison expressions`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(intLiteral(5, intType), "<", intLiteral(3, intType), boolType),
                            "||",
                            binaryExpr(intLiteral(10, intType), "<", intLiteral(20, intType), boolType),
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
    fun `AND with NOT operator`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(true, boolType),
                            "&&",
                            unaryExpr("!", booleanLiteral(false, boolType), boolType),
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
    fun `OR with NOT operator`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(false, boolType),
                            "||",
                            unaryExpr("!", booleanLiteral(false, boolType), boolType),
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
    fun `deeply nested short-circuit - first false short-circuits all`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(false, boolType),
                            "&&",
                            binaryExpr(
                                binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                ">",
                                intLiteral(0, intType),
                                boolType
                            ),
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
    fun `AND with variable conditions`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("x", intType, intLiteral(5, intType)),
                    varDecl("y", intType, intLiteral(10, intType)),
                    returnStmt(
                        binaryExpr(
                            binaryExpr(identifier("x", intType, 3), ">", intLiteral(0, intType), boolType),
                            "&&",
                            binaryExpr(identifier("y", intType, 3), ">", identifier("x", intType, 3), boolType),
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
    fun `OR short-circuit preserves counter variable`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(
                            booleanLiteral(true, boolType),
                            "||",
                            booleanLiteral(true, boolType),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(42, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `four AND expressions chained`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(true, boolType), boolType),
                                "&&",
                                booleanLiteral(true, boolType),
                                boolType
                            ),
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
    fun `four OR expressions chained all false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(false, boolType), boolType),
                                "||",
                                booleanLiteral(false, boolType),
                                boolType
                            ),
                            "||",
                            booleanLiteral(false, boolType),
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
    fun `short-circuit AND with long type division by zero`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(false, boolType),
                            "&&",
                            binaryExpr(
                                binaryExpr(longLiteral(1L, longType), "/", longLiteral(0L, longType), longType),
                                ">",
                                longLiteral(0L, longType),
                                boolType
                            ),
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
    fun `short-circuit OR with long type division by zero`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            booleanLiteral(true, boolType),
                            "||",
                            binaryExpr(
                                binaryExpr(longLiteral(1L, longType), "/", longLiteral(0L, longType), longType),
                                ">",
                                longLiteral(0L, longType),
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
}
