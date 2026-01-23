package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for identifier expression compilation.
 */
class IdentifierCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `read int variable`() {
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
    fun `read long variable`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, longLiteral(9876543210L, longType)),
                    returnStmt(identifier("x", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9876543210L, result)
    }
    
    @Test
    fun `read float variable`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("x", floatType, floatLiteral(3.14f, floatType)),
                    returnStmt(identifier("x", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    @Test
    fun `read double variable`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(2.71828, doubleType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.71828, result)
    }
    
    @Test
    fun `read boolean variable true`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("x", boolType, booleanLiteral(true, boolType)),
                    returnStmt(identifier("x", boolType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `read boolean variable false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("x", boolType, booleanLiteral(false, boolType)),
                    returnStmt(identifier("x", boolType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `read string variable`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    varDecl("x", stringType, stringLiteral("hello world", stringType)),
                    returnStmt(identifier("x", stringType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello world", result)
    }
    
    @Test
    fun `read multiple variables`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("a", intType, intLiteral(10, intType)),
                    varDecl("b", intType, intLiteral(20, intType)),
                    varDecl("c", intType, intLiteral(30, intType)),
                    returnStmt(binaryExpr(
                        binaryExpr(identifier("a", intType, 3), "+", identifier("b", intType, 3), intType),
                        "+",
                        identifier("c", intType, 3),
                        intType
                    ))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(60, result)
    }
    
    @Test
    fun `read variable after modification`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    assignment(identifier("x", intType, 3), intLiteral(42, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `use variable in expression`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    returnStmt(binaryExpr(
                        identifier("x", intType, 3),
                        "*",
                        intLiteral(5, intType),
                        intType
                    ))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(50, result)
    }
    
    @Test
    fun `use variable in complex expression`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    varDecl("y", intType, intLiteral(3, intType)),
                    returnStmt(binaryExpr(
                        binaryExpr(identifier("x", intType, 3), "*", identifier("x", intType, 3), intType),
                        "+",
                        binaryExpr(identifier("y", intType, 3), "*", identifier("y", intType, 3), intType),
                        intType
                    ))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(109, result)
    }
    
    @Test
    fun `read variable from outer scope in while`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(100, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("result", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(1, intType), boolType),
                        body = listOf(
                            assignment(identifier("result", intType, 3), identifier("x", intType, 3)),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100, result)
    }
    
    @Test
    fun `read inner variable from inner scope`() {
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
                            varDecl("inner", intType, intLiteral(42, intType)),
                            assignment(identifier("result", intType, 3), identifier("inner", intType, 4)),
                            assignment(identifier("i", intType, 3), intLiteral(1, intType))
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
    fun `use variable in condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("limit", intType, intLiteral(5, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", identifier("limit", intType, 3), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
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
    fun `read variable with negative value`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(-42, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-42, result)
    }
    
    @Test
    fun `read variable with zero value`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
}
