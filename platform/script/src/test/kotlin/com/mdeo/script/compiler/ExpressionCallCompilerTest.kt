package com.mdeo.script.compiler

import com.mdeo.script.ast.types.ClassTypeRef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for expression call compilation.
 * 
 * Expression calls invoke a lambda directly through its variable,
 * without using member access syntax. For example: `f(x)` instead of `f.lambda(x)`.
 */
class ExpressionCallCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `direct lambda call with no arguments returns constant`() {
        // val f = () => 42
        // return f()  // expression call - direct invocation
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(intLiteral(42, intType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `direct lambda call with one argument`() {
        // val f = (x: Int) => x + 1
        // return f(10)  // should return 11
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),  // Lambda param
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(10, intType)),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(11, result)
    }
    
    @Test
    fun `direct lambda call with multiple arguments`() {
        // val add = (a: Int, b: Int) => a + b
        // return add(3, 7)  // should return 10
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", false),
                "b" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("add", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("a", "b"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("a", intType, 4),
                                    "+",
                                    identifier("b", intType, 4),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("add", lambdaTypeIdx, 3),
                            arguments = listOf(
                                intLiteral(3, intType),
                                intLiteral(7, intType)
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    @Test
    fun `direct lambda call that captures outer variable`() {
        // var captured = 100
        // val f = (x: Int) => x + captured
        // return f(5)  // should return 105
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("captured", intType, intLiteral(100, intType)),
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),  // Lambda param
                                    "+",
                                    identifier("captured", intType, 3),  // Captured from outer scope
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(5, intType)),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(105, result)
    }
    
    @Test
    fun `direct lambda call that modifies captured variable`() {
        // var counter = 0
        // val increment = () => { counter = counter + 1; return counter }
        // increment()
        // return increment()  // should return 2
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false)
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
                    // First call via expression statement
                    exprStmt(
                        expressionCall(
                            expression = identifier("increment", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intType
                        )
                    ),
                    // Second call and return result
                    returnStmt(
                        expressionCall(
                            expression = identifier("increment", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `direct lambda call with long return type`() {
        // val f = () => 100L
        // return f()
        val ast = buildTypedAst {
            val longType = longType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.long", false)
            )
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(longLiteral(100L, longType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100L, result)
    }
    
    @Test
    fun `direct lambda call with double return type`() {
        // val f = () => 3.14
        // return f()
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.double", false)
            )
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(doubleLiteral(3.14, doubleType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14, result)
    }
    
    @Test
    fun `direct lambda call with string return type`() {
        // val f = () => "hello"
        // return f()
        val ast = buildTypedAst {
            val stringType = stringType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.string", false)
            )
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(stringLiteral("hello", stringType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello", result)
    }
    
    @Test
    fun `direct lambda call with boolean return type`() {
        // val f = () => true
        // return f()
        val ast = buildTypedAst {
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.boolean", false)
            )
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(booleanLiteral(true, boolType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = emptyList(),
                            resultTypeIndex = boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `chained direct lambda calls`() {
        // val double = (x: Int) => x * 2
        // val triple = (x: Int) => x * 3
        // return triple(double(5))  // should return 30
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("double", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),
                                    "*",
                                    intLiteral(2, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    varDecl("triple", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),
                                    "*",
                                    intLiteral(3, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("triple", lambdaTypeIdx, 3),
                            arguments = listOf(
                                expressionCall(
                                    expression = identifier("double", lambdaTypeIdx, 3),
                                    arguments = listOf(intLiteral(5, intType)),
                                    resultTypeIndex = intType
                                )
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30, result)
    }
}
