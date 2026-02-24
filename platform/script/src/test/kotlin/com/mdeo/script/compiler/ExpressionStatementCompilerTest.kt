package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassTypeRef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for expression statement compilation.
 * 
 * Expression statements are expressions evaluated for their side effects,
 * where the return value is discarded.
 */
class ExpressionStatementCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `expression statement with lambda call that modifies captured variable`() {
        // var counter = 0
        // val increment = () => { counter = counter + 1; return counter }
        // increment()  // expression statement - discard result
        // increment()  // expression statement - discard result
        // return counter  // should be 2
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("increment", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            assignment(
                                identifier("counter", intType, 3),
                                binaryExpr(
                                    identifier("counter", intType, 3),
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Expression statement: call the lambda, discard result
                    exprStmt(
                        expressionCall(
                            expression = identifier("increment", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intType
                        )
                    ),
                    // Expression statement: call the lambda again, discard result
                    exprStmt(
                        expressionCall(
                            expression = identifier("increment", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intType
                        )
                    ),
                    // Return the counter value
                    returnStmt(identifier("counter", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `expression statement with int result gets popped`() {
        // This test ensures that an int result is properly popped from the stack.
        // var x = 10
        // x + 5  // expression statement, result should be discarded
        // return x  // should still be 10
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    exprStmt(
                        binaryExpr(
                            identifier("x", intType, 3),
                            "+",
                            intLiteral(5, intType),
                            intType
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    @Test
    fun `expression statement with long result gets popped`() {
        // var x = 10L
        // x + 5L  // expression statement with long result, needs POP2
        // return x
        val ast = buildTypedAst {
            val longType = longType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, longLiteral(10L, longType)),
                    exprStmt(
                        binaryExpr(
                            identifier("x", longType, 3),
                            "+",
                            longLiteral(5L, longType),
                            longType
                        )
                    ),
                    returnStmt(identifier("x", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10L, result)
    }
    
    @Test
    fun `expression statement with double result gets popped`() {
        // var x = 10.0
        // x + 5.0  // expression statement with double result, needs POP2
        // return x
        val ast = buildTypedAst {
            val doubleType = doubleType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("x", doubleType, doubleLiteral(10.0, doubleType)),
                    exprStmt(
                        binaryExpr(
                            identifier("x", doubleType, 3),
                            "+",
                            doubleLiteral(5.0, doubleType),
                            doubleType
                        )
                    ),
                    returnStmt(identifier("x", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.0, result)
    }
    
    @Test
    fun `expression statement with string result gets popped`() {
        // var s = "hello"
        // s + " world"  // expression statement with string result
        // return s
        val ast = buildTypedAst {
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    varDecl("s", stringType, stringLiteral("hello", stringType)),
                    exprStmt(
                        binaryExpr(
                            identifier("s", stringType, 3),
                            "+",
                            stringLiteral(" world", stringType),
                            stringType
                        )
                    ),
                    returnStmt(identifier("s", stringType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello", result)
    }
    
    @Test
    fun `expression statement with boolean result gets popped`() {
        // var b = true
        // b && false  // expression statement with boolean result
        // return b
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("b", boolType, booleanLiteral(true, boolType)),
                    exprStmt(
                        binaryExpr(
                            identifier("b", boolType, 3),
                            "&&",
                            booleanLiteral(false, boolType),
                            boolType
                        )
                    ),
                    returnStmt(identifier("b", boolType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `expression statement with float result gets popped`() {
        // var f = 10.0f
        // f + 5.0f  // expression statement with float result
        // return f
        val ast = buildTypedAst {
            val floatType = floatType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    varDecl("f", floatType, floatLiteral(10.0f, floatType)),
                    exprStmt(
                        binaryExpr(
                            identifier("f", floatType, 3),
                            "+",
                            floatLiteral(5.0f, floatType),
                            floatType
                        )
                    ),
                    returnStmt(identifier("f", floatType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.0f, result)
    }
    
    @Test
    fun `multiple expression statements in sequence`() {
        // Tests that multiple expression statements don't corrupt the stack
        // var x = 1
        // x + 1   // pop int
        // x + 2   // pop int
        // x + 3   // pop int
        // return x
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(1, intType)),
                    exprStmt(
                        binaryExpr(identifier("x", intType, 3), "+", intLiteral(1, intType), intType)
                    ),
                    exprStmt(
                        binaryExpr(identifier("x", intType, 3), "+", intLiteral(2, intType), intType)
                    ),
                    exprStmt(
                        binaryExpr(identifier("x", intType, 3), "+", intLiteral(3, intType), intType)
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
}
