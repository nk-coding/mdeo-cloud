package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for break and continue statement compilation.
 * 
 * These tests verify:
 * - Basic break functionality
 * - Basic continue functionality
 * - Break in nested loops
 * - Continue in nested loops
 * - Break/continue with conditions
 */
class BreakContinueCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `break exits loop immediately`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = booleanLiteral(true, boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            breakStmt()
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `break inside if condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(100, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), ">=", intLiteral(5, intType), boolType),
                                thenBlock = listOf(breakStmt())
                            )
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `continue skips rest of loop body`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), "<=", intLiteral(5, intType), boolType),
                                thenBlock = listOf(continueStmt())
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6 + 7 + 8 + 9 + 10, result)
    }
    
    @Test
    fun `continue in unconditional context`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            continueStmt(),
                            assignment(
                                identifier("count", intType, 3),
                                intLiteral(999, intType)
                            )
                        )
                    ),
                    returnStmt(identifier("count", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `break in nested loop - inner loop only`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("outer", intType, intLiteral(0, intType)),
                    varDecl("total", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("outer", intType, 3), "<", intLiteral(3, intType), boolType),
                        body = listOf(
                            varDecl("inner", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(identifier("inner", intType, 4), "<", intLiteral(10, intType), boolType),
                                body = listOf(
                                    assignment(
                                        identifier("inner", intType, 4),
                                        binaryExpr(identifier("inner", intType, 4), "+", intLiteral(1, intType), intType)
                                    ),
                                    ifStmt(
                                        condition = binaryExpr(identifier("inner", intType, 4), ">=", intLiteral(2, intType), boolType),
                                        thenBlock = listOf(breakStmt())
                                    )
                                )
                            ),
                            assignment(
                                identifier("total", intType, 3),
                                binaryExpr(identifier("total", intType, 3), "+", identifier("inner", intType, 4), intType)
                            ),
                            assignment(
                                identifier("outer", intType, 3),
                                binaryExpr(identifier("outer", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("total", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)
    }
    
    @Test
    fun `continue in nested loop - inner loop only`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("outer", intType, intLiteral(0, intType)),
                    varDecl("total", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("outer", intType, 3), "<", intLiteral(2, intType), boolType),
                        body = listOf(
                            varDecl("inner", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(identifier("inner", intType, 4), "<", intLiteral(5, intType), boolType),
                                body = listOf(
                                    assignment(
                                        identifier("inner", intType, 4),
                                        binaryExpr(identifier("inner", intType, 4), "+", intLiteral(1, intType), intType)
                                    ),
                                    ifStmt(
                                        condition = binaryExpr(identifier("inner", intType, 4), "<=", intLiteral(2, intType), boolType),
                                        thenBlock = listOf(continueStmt())
                                    ),
                                    assignment(
                                        identifier("total", intType, 3),
                                        binaryExpr(identifier("total", intType, 3), "+", identifier("inner", intType, 4), intType)
                                    )
                                )
                            ),
                            assignment(
                                identifier("outer", intType, 3),
                                binaryExpr(identifier("outer", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("total", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(24, result)
    }
    
    @Test
    fun `break with complex condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(100, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(
                                    binaryExpr(identifier("i", intType, 3), ">", intLiteral(5, intType), boolType),
                                    "&&",
                                    binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                                    boolType
                                ),
                                thenBlock = listOf(breakStmt())
                            )
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)
    }
    
    @Test
    fun `continue with complex condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(
                                    binaryExpr(identifier("i", intType, 3), ">", intLiteral(3, intType), boolType),
                                    "&&",
                                    binaryExpr(identifier("i", intType, 3), "<", intLiteral(8, intType), boolType),
                                    boolType
                                ),
                                thenBlock = listOf(continueStmt())
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1 + 2 + 3 + 8 + 9 + 10, result)
    }
    
    @Test
    fun `break in else branch`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(100, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                                thenBlock = listOf(),
                                elseBlock = listOf(breakStmt())
                            )
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `continue in else branch`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), "<=", intLiteral(5, intType), boolType),
                                thenBlock = listOf(
                                    assignment(
                                        identifier("sum", intType, 3),
                                        binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                                    )
                                ),
                                elseBlock = listOf(continueStmt())
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1 + 2 + 3 + 4 + 5, result)
    }
    
    @Test
    fun `break with ternary condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("limit", intType, 
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = intLiteral(5, intType),
                            falseExpr = intLiteral(10, intType),
                            resultTypeIndex = intType
                        )
                    ),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(100, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), ">=", identifier("limit", intType, 3), boolType),
                                thenBlock = listOf(breakStmt())
                            )
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `three level nested loops with break`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("a", intType, intLiteral(0, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("a", intType, 3), "<", intLiteral(3, intType), boolType),
                        body = listOf(
                            varDecl("b", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(identifier("b", intType, 4), "<", intLiteral(3, intType), boolType),
                                body = listOf(
                                    varDecl("c", intType, intLiteral(0, intType)),
                                    whileStmt(
                                        condition = binaryExpr(identifier("c", intType, 5), "<", intLiteral(3, intType), boolType),
                                        body = listOf(
                                            assignment(
                                                identifier("count", intType, 3),
                                                binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                                            ),
                                            ifStmt(
                                                condition = binaryExpr(identifier("c", intType, 5), ">=", intLiteral(1, intType), boolType),
                                                thenBlock = listOf(breakStmt())
                                            ),
                                            assignment(
                                                identifier("c", intType, 5),
                                                binaryExpr(identifier("c", intType, 5), "+", intLiteral(1, intType), intType)
                                            )
                                        )
                                    ),
                                    assignment(
                                        identifier("b", intType, 4),
                                        binaryExpr(identifier("b", intType, 4), "+", intLiteral(1, intType), intType)
                                    )
                                )
                            ),
                            assignment(
                                identifier("a", intType, 3),
                                binaryExpr(identifier("a", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("count", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(18, result)
    }
    
    @Test
    fun `break followed by dead code`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = booleanLiteral(true, boolType),
                        body = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(42, intType)),
                            breakStmt(),
                            assignment(identifier("x", intType, 3), intLiteral(999, intType))
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
    fun `multiple breaks in different branches`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(100, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), ">", intLiteral(10, intType), boolType),
                                thenBlock = listOf(breakStmt()),
                                elseIfs = listOf(
                                    elseIfClause(
                                        condition = binaryExpr(identifier("i", intType, 3), ">", intLiteral(5, intType), boolType),
                                        thenBlock = listOf(breakStmt())
                                    )
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)
    }
    
    @Test
    fun `continue with multiple conditions using OR`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(
                                    binaryExpr(identifier("i", intType, 3), "<", intLiteral(3, intType), boolType),
                                    "||",
                                    binaryExpr(identifier("i", intType, 3), ">", intLiteral(7, intType), boolType),
                                    boolType
                                ),
                                thenBlock = listOf(continueStmt())
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3 + 4 + 5 + 6 + 7, result)
    }
    
    @Test
    fun `break and continue in same loop`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(100, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(3, intType), boolType),
                                thenBlock = listOf(continueStmt())
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), ">", intLiteral(7, intType), boolType),
                                thenBlock = listOf(breakStmt())
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3 + 4 + 5 + 6 + 7, result)
    }
    
    @Test
    fun `break with long counter`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("i", longType, longLiteral(0L, longType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", longType, 3), "<", longLiteral(1000000L, longType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", longType, 3),
                                binaryExpr(identifier("i", longType, 3), "+", longLiteral(1L, longType), longType)
                            ),
                            ifStmt(
                                condition = binaryExpr(identifier("i", longType, 3), ">=", longLiteral(100L, longType), boolType),
                                thenBlock = listOf(breakStmt())
                            )
                        )
                    ),
                    returnStmt(identifier("i", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100L, result)
    }
    
    @Test
    fun `break outside of loop throws CompilationException`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    breakStmt(),  // Break outside of loop!
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val exception = assertThrows<CompilationException> {
            helper.compileAndInvoke(ast)
        }
        
        assertTrue(exception.message?.contains("Break") == true || 
                   exception.message?.contains("break") == true ||
                   exception.message?.contains("loop") == true,
            "Exception should mention break or loop: ${exception.message}")
    }
    
    @Test
    fun `continue outside of loop throws CompilationException`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    continueStmt(),  // Continue outside of loop!
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val exception = assertThrows<CompilationException> {
            helper.compileAndInvoke(ast)
        }
        
        assertTrue(exception.message?.contains("Continue") == true || 
                   exception.message?.contains("continue") == true ||
                   exception.message?.contains("loop") == true,
            "Exception should mention continue or loop: ${exception.message}")
    }
    
    @Test
    fun `break in if block outside of loop throws CompilationException`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(true, boolType),
                        thenBlock = listOf(
                            breakStmt()  // Break inside if but outside of loop!
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val exception = assertThrows<CompilationException> {
            helper.compileAndInvoke(ast)
        }
        
        assertTrue(exception.message?.contains("Break") == true || 
                   exception.message?.contains("break") == true ||
                   exception.message?.contains("loop") == true,
            "Exception should mention break or loop: ${exception.message}")
    }
    
    @Test
    fun `continue in if block outside of loop throws CompilationException`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(true, boolType),
                        thenBlock = listOf(
                            continueStmt()  // Continue inside if but outside of loop!
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val exception = assertThrows<CompilationException> {
            helper.compileAndInvoke(ast)
        }
        
        assertTrue(exception.message?.contains("Continue") == true || 
                   exception.message?.contains("continue") == true ||
                   exception.message?.contains("loop") == true,
            "Exception should mention continue or loop: ${exception.message}")
    }
}
