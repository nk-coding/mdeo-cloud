package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for variable declaration statements.
 */
class VariableDeclarationCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `declare int variable with initial value`() {
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
    fun `declare int variable without initial value then assign`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType),
                    assignment(identifier("x", intType, 3), intLiteral(100, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100, result)
    }
    
    @Test
    fun `declare long variable with initial value`() {
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
    fun `declare float variable with initial value`() {
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
    fun `declare double variable with initial value`() {
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
    fun `declare boolean variable with true`() {
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
    fun `declare boolean variable with false`() {
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
    fun `declare string variable with initial value`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    varDecl("x", stringType, stringLiteral("hello", stringType)),
                    returnStmt(identifier("x", stringType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello", result)
    }
    
    @Test
    fun `declare multiple variables of same type`() {
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
    fun `declare variables of different types`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("i", intType, intLiteral(10, intType)),
                    varDecl("l", longType, longLiteral(20L, longType)),
                    varDecl("d", doubleType, doubleLiteral(30.5, doubleType)),
                    returnStmt(binaryExpr(
                        binaryExpr(identifier("i", intType, 3), "+", identifier("l", longType, 3), longType),
                        "+",
                        identifier("d", doubleType, 3),
                        doubleType
                    ))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(60.5, result)
    }
    
    @Test
    fun `variable declaration with type conversion int to long`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, intLiteral(42, intType)),
                    returnStmt(identifier("x", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    @Test
    fun `variable declaration with type conversion int to double`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, intLiteral(42, intType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0, result)
    }
    
    @Test
    fun `variable declaration with type conversion int to float`() {
        val ast = buildTypedAst {
            val intType = intType()
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("x", floatType, intLiteral(42, intType)),
                    returnStmt(identifier("x", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0f, result)
    }
    
    @Test
    fun `variable declaration with type conversion long to double`() {
        val ast = buildTypedAst {
            val longType = longType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, longLiteral(42L, longType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0, result)
    }
    
    @Test
    fun `variable declaration with type conversion float to double`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, floatLiteral(3.14f, floatType)),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f.toDouble(), result)
    }
    
    @Test
    fun `variable with zero initial value`() {
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
    
    @Test
    fun `variable with negative initial value`() {
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
    fun `variable with max int value`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(Int.MAX_VALUE, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MAX_VALUE, result)
    }
    
    @Test
    fun `variable with min int value`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(Int.MIN_VALUE, intType)),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(Int.MIN_VALUE, result)
    }
    
    @Test
    fun `uninitialized int variable has default value 0`() {
        // JVM spec says local variables are not implicitly initialized.
        // However, with COMPUTE_FRAMES the verifier handles uninitialized locals.
        // This test verifies that reading an uninitialized int gives 0.
        // Note: This relies on JVM implementation behavior with COMPUTE_FRAMES.
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType),  // no initial value
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `uninitialized long variable has default value 0L`() {
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType),  // no initial value
                    returnStmt(identifier("x", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0L, result)
    }
    
    @Test
    fun `uninitialized boolean variable has default value false`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("x", boolType),  // no initial value
                    returnStmt(identifier("x", boolType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `uninitialized float variable has default value 0f`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("x", floatType),  // no initial value
                    returnStmt(identifier("x", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0f, result)
    }
    
    @Test
    fun `uninitialized double variable has default value 0_0`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType),  // no initial value
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0.0, result)
    }
}
