package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for scope handling and variable shadowing.
 */
class ScopeCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `variable shadowing in while scope`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(100, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            // This x shadows the outer x
                            varDecl("x", intType, intLiteral(42, intType)),
                            assignment(
                                // i is declared at scope 3 (function body)
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    // Return outer x (scope 3)
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100, result)
    }
    
    @Test
    fun `access outer variable from while scope`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("outer", intType, intLiteral(10, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("outer", intType, 3),
                                binaryExpr(identifier("outer", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("outer", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(15, result)
    }
    
    @Test
    fun `scope level 3 for function body variables`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(42, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `scope level 4 for while body variables`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            varDecl("inner", intType, intLiteral(99, intType)),
                            assignment(identifier("result", intType, 3), identifier("inner", intType, 4)),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(99, result)
    }
    
    @Test
    fun `scope level 5 for nested while body`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            varDecl("j", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(identifier("j", intType, 4), "<", intLiteral(1, intType), boolType),
                                body = listOf(
                                    varDecl("deep", intType, intLiteral(123, intType)),
                                    assignment(identifier("result", intType, 3), identifier("deep", intType, 5)),
                                    assignment(identifier("j", intType, 4), intLiteral(1, intType))
                                )
                            ),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(123, result)
    }
    
    @Test
    fun `variable slot reuse in sequential while loops`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(3, intType), boolType),
                        body = listOf(
                            varDecl("temp", intType, intLiteral(10, intType)),
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "+", identifier("temp", intType, 4), intType)
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    assignment(identifier("i", intType, 3), intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(2, intType), boolType),
                        body = listOf(
                            varDecl("other", intType, intLiteral(5, intType)),
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "+", identifier("other", intType, 4), intType)
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(40, result)
    }
    
    @Test
    fun `multiple variables with same name in sibling scopes`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            varDecl("x", intType, intLiteral(100, intType)),
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "+", identifier("x", intType, 4), intType)
                            ),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    assignment(identifier("i", intType, 3), intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            varDecl("x", intType, intLiteral(50, intType)),
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "+", identifier("x", intType, 4), intType)
                            ),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(150, result)
    }
    
    @Test
    fun `variable declared in while visible only in that scope`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(1, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(3, intType), boolType),
                        body = listOf(
                            varDecl("inner", intType, identifier("i", intType, 3)),
                            assignment(
                                identifier("x", intType, 3),
                                binaryExpr(identifier("x", intType, 3), "+", identifier("inner", intType, 4), intType)
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(4, result)
    }
    
    @Test
    fun `deeply nested scopes with different variable types`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("a", intType, intLiteral(1, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            varDecl("b", longType, longLiteral(2L, longType)),
                            varDecl("j", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(identifier("j", intType, 4), "<", intLiteral(1, intType), boolType),
                                body = listOf(
                                    varDecl("c", doubleType, doubleLiteral(3.0, doubleType)),
                                    assignment(identifier("j", intType, 4), intLiteral(1, intType))
                                )
                            ),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    returnStmt(doubleLiteral(6.0, doubleType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6.0, result)
    }
    
    @Test
    fun `long and double variables take 2 slots`() {
        val ast = buildTypedAst {
            val longType = longType()
            val doubleType = doubleType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("a", longType, longLiteral(1L, longType)),
                    varDecl("b", doubleType, doubleLiteral(2.0, doubleType)),
                    varDecl("c", longType, longLiteral(3L, longType)),
                    varDecl("d", intType, intLiteral(4, intType)),
                    returnStmt(binaryExpr(
                        identifier("a", longType, 3),
                        "+",
                        identifier("c", longType, 3),
                        longType
                    ))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(4L, result)
    }
    
    @Test
    fun `access both outer and shadowing inner variable with same name`() {
        // This test verifies that we can access both an outer variable and an inner
        // variable with the same name, by using the correct scope level in the identifier.
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // outer x at scope 3
                    varDecl("x", intType, intLiteral(100, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            // inner x at scope 4, shadows outer x
                            varDecl("x", intType, intLiteral(10, intType)),
                            // result = outer.x + inner.x = 100 + 10 = 110
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(
                                    identifier("x", intType, 3), // outer x
                                    "+",
                                    identifier("x", intType, 4), // inner x
                                    intType
                                )
                            ),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(110, result)
    }
}
