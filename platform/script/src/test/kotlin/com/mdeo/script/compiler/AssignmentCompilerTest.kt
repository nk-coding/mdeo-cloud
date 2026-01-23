package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for assignment statements.
 */
class AssignmentCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `assign int to variable`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(0, intType)),
                    assignment(identifier("x", intType, 3), intLiteral(42, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `assign long to variable`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, longLiteral(0L, longType)),
                    assignment(identifier("x", longType, 3), longLiteral(9876543210L, longType)),
                    returnStmt(identifier("x", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9876543210L, result)
    }
    
    @Test
    fun `assign float to variable`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("x", floatType, floatLiteral(0f, floatType)),
                    assignment(identifier("x", floatType, 3), floatLiteral(3.14f, floatType)),
                    returnStmt(identifier("x", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    @Test
    fun `assign double to variable`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(0.0, doubleType)),
                    assignment(identifier("x", doubleType, 3), doubleLiteral(2.71828, doubleType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.71828, result)
    }
    
    @Test
    fun `assign boolean to variable`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("x", boolType, booleanLiteral(false, boolType)),
                    assignment(identifier("x", boolType, 3), booleanLiteral(true, boolType)),
                    returnStmt(identifier("x", boolType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `assign string to variable`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    varDecl("x", stringType, stringLiteral("initial", stringType)),
                    assignment(identifier("x", stringType, 3), stringLiteral("updated", stringType)),
                    returnStmt(identifier("x", stringType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("updated", result)
    }
    
    @Test
    fun `multiple assignments to same variable`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(1, intType)),
                    assignment(identifier("x", intType, 3), intLiteral(2, intType)),
                    assignment(identifier("x", intType, 3), intLiteral(3, intType)),
                    assignment(identifier("x", intType, 3), intLiteral(42, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `assign expression result to variable`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    varDecl("y", intType, intLiteral(20, intType)),
                    varDecl("result", intType),
                    assignment(
                        identifier("result", intType, 3),
                        binaryExpr(identifier("x", intType, 3), "+", identifier("y", intType, 3), intType)
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30, result)
    }
    
    @Test
    fun `assign with type conversion int to long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, longLiteral(0L, longType)),
                    assignment(identifier("x", longType, 3), intLiteral(42, intType)),
                    returnStmt(identifier("x", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    @Test
    fun `assign with type conversion int to double`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(0.0, doubleType)),
                    assignment(identifier("x", doubleType, 3), intLiteral(42, intType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0, result)
    }
    
    @Test
    fun `assign with type conversion int to float`() {
        val ast = buildTypedAst {
            val intType = intType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("x", floatType, floatLiteral(0f, floatType)),
                    assignment(identifier("x", floatType, 3), intLiteral(42, intType)),
                    returnStmt(identifier("x", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0f, result)
    }
    
    @Test
    fun `assign with type conversion long to double`() {
        val ast = buildTypedAst {
            val longType = longType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(0.0, doubleType)),
                    assignment(identifier("x", doubleType, 3), longLiteral(42L, longType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0, result)
    }
    
    @Test
    fun `assign with type conversion float to double`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(0.0, doubleType)),
                    assignment(identifier("x", doubleType, 3), floatLiteral(3.14f, floatType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f.toDouble(), result)
    }
    
    @Test
    fun `assign from another variable`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(42, intType)),
                    varDecl("y", intType),
                    assignment(identifier("y", intType, 3), identifier("x", intType, 3)),
                    returnStmt(identifier("y", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `swap values using temp variable`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("a", intType, intLiteral(10, intType)),
                    varDecl("b", intType, intLiteral(20, intType)),
                    varDecl("temp", intType),
                    assignment(identifier("temp", intType, 3), identifier("a", intType, 3)),
                    assignment(identifier("a", intType, 3), identifier("b", intType, 3)),
                    assignment(identifier("b", intType, 3), identifier("temp", intType, 3)),
                    returnStmt(identifier("a", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(20, result)
    }
    
    @Test
    fun `compound calculation with assignments`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(identifier("sum", intType, 3), "+", intLiteral(10, intType), intType)
                    ),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(identifier("sum", intType, 3), "+", intLiteral(20, intType), intType)
                    ),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(identifier("sum", intType, 3), "+", intLiteral(30, intType), intType)
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(60, result)
    }
    
    @Test
    fun `assign negative value`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(42, intType)),
                    assignment(identifier("x", intType, 3), intLiteral(-100, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(-100, result)
    }
    
    @Test
    fun `assign to multiple different variables`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("a", intType),
                    varDecl("b", intType),
                    varDecl("c", intType),
                    assignment(identifier("a", intType, 3), intLiteral(1, intType)),
                    assignment(identifier("b", intType, 3), intLiteral(2, intType)),
                    assignment(identifier("c", intType, 3), intLiteral(3, intType)),
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
        assertEquals(6, result)
    }
}
