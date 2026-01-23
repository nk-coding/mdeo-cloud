package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for if-else-if-else statement compilation.
 * 
 * These tests verify:
 * - Simple if statements
 * - If-else statements
 * - If-else if-else statements
 * - Nested if statements
 * - If statements with variable assignments
 * - Scope handling within if blocks
 */
class IfStatementCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `simple if - condition true executes then block`() {
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
    fun `simple if - condition false skips then block`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(42, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `if-else - condition true executes then block`() {
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
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if-else - condition false executes else block`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `if-else if-else - first condition true`() {
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
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(3, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if-else if-else - second condition true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(3, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `if-else if-else - all conditions false goes to else`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(3, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3, result)
    }
    
    @Test
    fun `if-else if without else - second condition true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `if-else if without else - all conditions false`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(99, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(99, result)
    }
    
    @Test
    fun `if with multiple else-if clauses - third condition true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            ),
                            elseIfClause(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(3, intType))
                                )
                            ),
                            elseIfClause(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(4, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(5, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3, result)
    }
    
    @Test
    fun `if with comparison condition - greater than`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(identifier("x", intType, 3), ">", intLiteral(5, intType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if with comparison condition - less than or equal`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(5, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(identifier("x", intType, 3), "<=", intLiteral(5, intType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `nested if statements - both true`() {
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
                            ifStmt(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(42, intType))
                                )
                            )
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
    fun `nested if statements - outer true inner false`() {
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
                            assignment(identifier("x", intType, 3), intLiteral(1, intType)),
                            ifStmt(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(42, intType))
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `nested if statements - outer false skips inner`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType)),
                            ifStmt(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(42, intType))
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `if in else block`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            ifStmt(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(42, intType))
                                )
                            )
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
    fun `if with AND condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(true, boolType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if with OR condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(true, boolType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if with NOT condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = unaryExpr("!", booleanLiteral(false, boolType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if with multiple statements in then block`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    varDecl("y", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(true, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType)),
                            assignment(identifier("y", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(binaryExpr(identifier("x", intType, 3), "+", identifier("y", intType, 3), intType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3, result)
    }
    
    @Test
    fun `if inside while loop`() {
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
                            ifStmt(
                                condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                                thenBlock = listOf(
                                    assignment(
                                        identifier("sum", intType, 3),
                                        binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                                    )
                                )
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    @Test
    fun `if with long type comparison`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, longLiteral(100L, longType)),
                    varDecl("result", longType, longLiteral(0L, longType)),
                    ifStmt(
                        condition = binaryExpr(identifier("x", longType, 3), ">", longLiteral(50L, longType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("result", longType, 3), longLiteral(1L, longType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", longType, 3), longLiteral(2L, longType))
                        )
                    ),
                    returnStmt(identifier("result", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1L, result)
    }
    
    @Test
    fun `if with double type comparison`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(3.14, doubleType)),
                    varDecl("result", doubleType, doubleLiteral(0.0, doubleType)),
                    ifStmt(
                        condition = binaryExpr(identifier("x", doubleType, 3), ">", doubleLiteral(3.0, doubleType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("result", doubleType, 3), doubleLiteral(1.0, doubleType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", doubleType, 3), doubleLiteral(2.0, doubleType))
                        )
                    ),
                    returnStmt(identifier("result", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1.0, result)
    }
    
    @Test
    fun `if with float type comparison`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("x", floatType, floatLiteral(2.5f, floatType)),
                    varDecl("result", floatType, floatLiteral(0.0f, floatType)),
                    ifStmt(
                        condition = binaryExpr(identifier("x", floatType, 3), "<", floatLiteral(3.0f, floatType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("result", floatType, 3), floatLiteral(1.0f, floatType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", floatType, 3), floatLiteral(2.0f, floatType))
                        )
                    ),
                    returnStmt(identifier("result", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1.0f, result)
    }
    
    @Test
    fun `deeply nested if-else-if-else`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(false, boolType),
                        thenBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(2, intType))
                                )
                            ),
                            elseIfClause(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(3, intType))
                                )
                            ),
                            elseIfClause(
                                condition = booleanLiteral(false, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(4, intType))
                                )
                            ),
                            elseIfClause(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    assignment(identifier("x", intType, 3), intLiteral(5, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("x", intType, 3), intLiteral(6, intType))
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `inequality comparison in if statement`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(5, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(
                            binaryExpr(identifier("x", intType, 3), ">=", intLiteral(5, intType), boolType),
                            "&&",
                            binaryExpr(identifier("x", intType, 3), "<=", intLiteral(5, intType), boolType),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if with short-circuit AND - left false skips right`() {
        // If left is false, right (division by zero) is NOT evaluated
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(
                            booleanLiteral(false, boolType),
                            "&&",
                            binaryExpr(
                                binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                ">",
                                intLiteral(0, intType),
                                boolType
                            ),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result) // Should execute else since condition is false
    }
    
    @Test
    fun `if with short-circuit OR - left true skips right`() {
        // If left is true, right (division by zero) is NOT evaluated
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(
                            booleanLiteral(true, boolType),
                            "||",
                            binaryExpr(
                                binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                ">",
                                intLiteral(0, intType),
                                boolType
                            ),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result) // Should execute then since condition is true
    }
    
    @Test
    fun `deeply nested if else chain`() {
        // 4 levels of nesting
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = booleanLiteral(true, boolType),
                        thenBlock = listOf(
                            ifStmt(
                                condition = booleanLiteral(true, boolType),
                                thenBlock = listOf(
                                    ifStmt(
                                        condition = booleanLiteral(true, boolType),
                                        thenBlock = listOf(
                                            ifStmt(
                                                condition = booleanLiteral(true, boolType),
                                                thenBlock = listOf(
                                                    assignment(identifier("result", intType, 3), intLiteral(42, intType))
                                                ),
                                                elseBlock = listOf(
                                                    assignment(identifier("result", intType, 3), intLiteral(1, intType))
                                                )
                                            )
                                        ),
                                        elseBlock = listOf(
                                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                                        )
                                    )
                                ),
                                elseBlock = listOf(
                                    assignment(identifier("result", intType, 3), intLiteral(3, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(4, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `if with ternary in condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(5, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = ternaryExpr(
                            condition = binaryExpr(identifier("x", intType, 3), ">", intLiteral(0, intType), boolType),
                            trueExpr = booleanLiteral(true, boolType),
                            falseExpr = booleanLiteral(false, boolType),
                            resultTypeIndex = boolType
                        ),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(2, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `if-else if-else with all branches having different outcomes`() {
        // Tests that only the correct branch executes
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(3, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    ifStmt(
                        condition = binaryExpr(identifier("x", intType, 3), "<", intLiteral(0, intType), boolType),
                        thenBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(1, intType))
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = binaryExpr(identifier("x", intType, 3), "<", intLiteral(2, intType), boolType),
                                thenBlock = listOf(
                                    assignment(identifier("result", intType, 3), intLiteral(2, intType))
                                )
                            ),
                            elseIfClause(
                                condition = binaryExpr(identifier("x", intType, 3), "<", intLiteral(4, intType), boolType),
                                thenBlock = listOf(
                                    assignment(identifier("result", intType, 3), intLiteral(3, intType))
                                )
                            ),
                            elseIfClause(
                                condition = binaryExpr(identifier("x", intType, 3), "<", intLiteral(6, intType), boolType),
                                thenBlock = listOf(
                                    assignment(identifier("result", intType, 3), intLiteral(4, intType))
                                )
                            )
                        ),
                        elseBlock = listOf(
                            assignment(identifier("result", intType, 3), intLiteral(5, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3, result) // x=3, so x<4 is true (third else-if)
    }
}
