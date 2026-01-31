package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.VoidType
import org.junit.jupiter.api.Test
import java.util.function.Function
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for lambda expression compilation.
 */
class LambdaCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `simple lambda that returns a constant value`() {
        // Lambda: () => 42
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // Create lambda and call it
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(intLiteral(42, intType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call the lambda by getting it as a Supplier and calling get()
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
    fun `lambda with one parameter that returns a value`() {
        // Lambda: (x) => x + 1
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
                                    identifier("x", intType, 4),  // Lambda param at scope 4
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call lambda.lambda(10) should return 11
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
    fun `lambda that captures and reads an outer variable`() {
        // captured = 100
        // Lambda: (x) => x + captured
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
                    // Call lambda.lambda(5) should return 105 (5 + 100)
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
    fun `lambda that captures and modifies an outer variable`() {
        // counter = 0
        // Lambda: () => { counter = counter + 1; return counter }
        // Call lambda twice, counter should be 2
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
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
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
                    // Call lambda twice
                    varDecl("result1", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    ),
                    varDecl("result2", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    ),
                    // Return counter value (should be 2)
                    returnStmt(identifier("counter", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `lambda modifies outer variable from if block`() {
        // counter = 0
        // Lambda: (cond) => { if (cond) counter = 10; return counter }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "cond" to ClassTypeRef("builtin.boolean", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("cond"),
                        body = listOf(
                            ifStmt(
                                condition = identifier("cond", boolType, 4),
                                thenBlock = listOf(
                                    assignment(
                                        identifier("counter", intType, 3),
                                        intLiteral(10, intType)
                                    )
                                )
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call lambda with true
                    varDecl("result", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(booleanLiteral(true, boolType)),
                        resultTypeIndex = intType
                    )
                    ),
                    returnStmt(identifier("counter", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    @Test
    fun `lambda modifies outer variable in nested scope`() {
        // counter = 0
        // Lambda: (n) => { i = 0; while (i < n) { counter = counter + 1; i = i + 1 }; return counter }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "n" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("n"),
                        body = listOf(
                            varDecl("i", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(
                                    identifier("i", intType, 5),  // i is lambda local at level 5
                                    "<",
                                    identifier("n", intType, 4),  // n is lambda param at level 4
                                    boolType
                                ),
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
                                    assignment(
                                        identifier("i", intType, 5),
                                        binaryExpr(
                                            identifier("i", intType, 5),
                                            "+",
                                            intLiteral(1, intType),
                                            intType
                                        )
                                    )
                                )
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call lambda with n=5
                    varDecl("result", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(5, intType)),
                        resultTypeIndex = intType
                    )
                    ),
                    returnStmt(identifier("counter", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `multiple lambdas modifying the same variable`() {
        // counter = 0
        // inc = () => { counter = counter + 1 }
        // dec = () => { counter = counter - 1 }
        // Call inc 3 times, dec once, counter should be 2
        val ast = buildTypedAst {
            val intType = intType()
            val voidType = voidType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("inc", lambdaTypeIdx, lambdaExpr(
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
                    varDecl("dec", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            assignment(
                                identifier("counter", intType, 3),
                                binaryExpr(
                                    identifier("counter", intType, 3),
                                    "-",
                                    intLiteral(1, intType),
                                    intType
                                )
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call inc 3 times
                    varDecl("r1", intType, 
                        expressionCall(
                        expression = identifier("inc", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    ),
                    varDecl("r2", intType, 
                        expressionCall(
                        expression = identifier("inc", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    ),
                    varDecl("r3", intType, 
                        expressionCall(
                        expression = identifier("inc", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    ),
                    // Call dec once
                    varDecl("r4", intType, 
                        expressionCall(
                        expression = identifier("dec", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    ),
                    returnStmt(identifier("counter", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    @Test
    fun `lambda implements Function interface`() {
        // Verify the lambda can be cast to Function and used
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
                                    identifier("x", intType, 4),
                                    "*",
                                    intLiteral(2, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call via lambda method
                    returnStmt(
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(21, intType)),
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
    fun `lambda with arithmetic operations`() {
        // Lambda: (a, b) => a * b + 10
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
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("a", "b"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    binaryExpr(
                                        identifier("a", intType, 4),
                                        "*",
                                        identifier("b", intType, 4),
                                        intType
                                    ),
                                    "+",
                                    intLiteral(10, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call lambda(3, 4) should return 3*4+10 = 22
                    returnStmt(
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(3, intType), intLiteral(4, intType)),
                        resultTypeIndex = intType
                    )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(22, result)
    }
    
    @Test
    fun `lambda with else-if block returns correct value`() {
        // counter = 0
        // Lambda: (n) => { if (n == 1) return 10; else if (n == 2) return 20; else return 30 }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "n" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("n"),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("n", intType, 4),
                                    "==",
                                    intLiteral(1, intType),
                                    boolType
                                ),
                                thenBlock = listOf(
                                    returnStmt(intLiteral(10, intType))
                                ),
                                elseIfs = listOf(
                                    elseIfClause(
                                        condition = binaryExpr(
                                            identifier("n", intType, 4),
                                            "==",
                                            intLiteral(2, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            returnStmt(intLiteral(20, intType))
                                        )
                                    )
                                ),
                                elseBlock = listOf(
                                    returnStmt(intLiteral(30, intType))
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call lambda with n=2, should return 20 (from else-if branch)
                    returnStmt(
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(2, intType)),
                        resultTypeIndex = intType
                    )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(20, result)
    }
    
    @Test
    fun `lambda with multiple else-if blocks`() {
        // Lambda: (n) => { if (n == 1) return 10; else if (n == 2) return 20; else if (n == 3) return 30; else return 40 }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "n" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("n"),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("n", intType, 4),
                                    "==",
                                    intLiteral(1, intType),
                                    boolType
                                ),
                                thenBlock = listOf(
                                    returnStmt(intLiteral(10, intType))
                                ),
                                elseIfs = listOf(
                                    elseIfClause(
                                        condition = binaryExpr(
                                            identifier("n", intType, 4),
                                            "==",
                                            intLiteral(2, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            returnStmt(intLiteral(20, intType))
                                        )
                                    ),
                                    elseIfClause(
                                        condition = binaryExpr(
                                            identifier("n", intType, 4),
                                            "==",
                                            intLiteral(3, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            returnStmt(intLiteral(30, intType))
                                        )
                                    )
                                ),
                                elseBlock = listOf(
                                    returnStmt(intLiteral(40, intType))
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Test all cases
                    varDecl("r1", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(1, intType)),
                        resultTypeIndex = intType
                    )
                    ),
                    varDecl("r2", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(2, intType)),
                        resultTypeIndex = intType
                    )
                    ),
                    varDecl("r3", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(3, intType)),
                        resultTypeIndex = intType
                    )
                    ),
                    varDecl("r4", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(4, intType)),
                        resultTypeIndex = intType
                    )
                    ),
                    // Return sum: 10 + 20 + 30 + 40 = 100
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    identifier("r1", intType, 3),
                                    "+",
                                    identifier("r2", intType, 3),
                                    intType
                                ),
                                "+",
                                identifier("r3", intType, 3),
                                intType
                            ),
                            "+",
                            identifier("r4", intType, 3),
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
    fun `lambda with else-if modifying captured variable`() {
        // counter = 0
        // Lambda: (n) => { if (n == 1) counter = 10; else if (n == 2) counter = 20; else counter = 30; return counter }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "n" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("n"),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("n", intType, 4),
                                    "==",
                                    intLiteral(1, intType),
                                    boolType
                                ),
                                thenBlock = listOf(
                                    assignment(
                                        identifier("counter", intType, 3),
                                        intLiteral(10, intType)
                                    )
                                ),
                                elseIfs = listOf(
                                    elseIfClause(
                                        condition = binaryExpr(
                                            identifier("n", intType, 4),
                                            "==",
                                            intLiteral(2, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            assignment(
                                                identifier("counter", intType, 3),
                                                intLiteral(20, intType)
                                            )
                                        )
                                    )
                                ),
                                elseBlock = listOf(
                                    assignment(
                                        identifier("counter", intType, 3),
                                        intLiteral(30, intType)
                                    )
                                )
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call with n=2, should set counter to 20 via else-if
                    varDecl("result", intType, 
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(intLiteral(2, intType)),
                        resultTypeIndex = intType
                    )
                    ),
                    returnStmt(identifier("counter", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(20, result)
    }
    
    @Test
    fun `lambda with long comparison in if condition`() {
        // Lambda: (n) => { if (n > 10L) return 1; else return 0 }
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val boolType = booleanType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false),
                "n" to ClassTypeRef("builtin.long", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("n"),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("n", longType, 4),
                                    ">",
                                    longLiteral(10L, longType),
                                    boolType
                                ),
                                thenBlock = listOf(
                                    returnStmt(intLiteral(1, intType))
                                ),
                                elseBlock = listOf(
                                    returnStmt(intLiteral(0, intType))
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Test: 15 > 10 should return 1
                    returnStmt(
                        expressionCall(
                        expression = identifier("f", lambdaTypeIdx, 3),
                        arguments = listOf(longLiteral(15L, longType)),
                        resultTypeIndex = intType
                    )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
    
    @Test
    fun `lambda with double comparison in while condition`() {
        // Lambda: () => { i = 0; d = 0.5; while (d < 2.0) { d = d + 0.5; i = i + 1 }; return i }
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            val boolType = booleanType()
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
                            varDecl("i", intType, intLiteral(0, intType)),
                            varDecl("d", doubleType, doubleLiteral(0.5, doubleType)),
                            whileStmt(
                                condition = binaryExpr(
                                    identifier("d", doubleType, 5),
                                    "<",
                                    doubleLiteral(2.0, doubleType),
                                    boolType
                                ),
                                body = listOf(
                                    assignment(
                                        identifier("d", doubleType, 5),
                                        binaryExpr(
                                            identifier("d", doubleType, 5),
                                            "+",
                                            doubleLiteral(0.5, doubleType),
                                            doubleType
                                        )
                                    ),
                                    assignment(
                                        identifier("i", intType, 5),
                                        binaryExpr(
                                            identifier("i", intType, 5),
                                            "+",
                                            intLiteral(1, intType),
                                            intType
                                        )
                                    )
                                )
                            ),
                            returnStmt(identifier("i", intType, 5))  // should be 3 (0.5->1.0->1.5->2.0)
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
        assertEquals(3, result)
    }
    
    @Test
    fun `lambda with break statement in while loop`() {
        // Lambda: () => { i = 0; while (true) { i = i + 1; if (i == 5) break }; return i }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
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
                            varDecl("i", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = booleanLiteral(true, boolType),
                                body = listOf(
                                    assignment(
                                        identifier("i", intType, 5),
                                        binaryExpr(
                                            identifier("i", intType, 5),
                                            "+",
                                            intLiteral(1, intType),
                                            intType
                                        )
                                    ),
                                    ifStmt(
                                        condition = binaryExpr(
                                            identifier("i", intType, 5),
                                            "==",
                                            intLiteral(5, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            breakStmt()
                                        )
                                    )
                                )
                            ),
                            returnStmt(identifier("i", intType, 5))
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
        assertEquals(5, result)
    }
    
    @Test
    fun `lambda with continue statement in while loop`() {
        // Lambda: () => { i = 0; sum = 0; while (i < 10) { i = i + 1; if (i % 2 == 0) continue; sum = sum + i }; return sum }
        // Sum of odd numbers 1 + 3 + 5 + 7 + 9 = 25
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
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
                            varDecl("i", intType, intLiteral(0, intType)),
                            varDecl("sum", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(
                                    identifier("i", intType, 5),
                                    "<",
                                    intLiteral(10, intType),
                                    boolType
                                ),
                                body = listOf(
                                    assignment(
                                        identifier("i", intType, 5),
                                        binaryExpr(
                                            identifier("i", intType, 5),
                                            "+",
                                            intLiteral(1, intType),
                                            intType
                                        )
                                    ),
                                    ifStmt(
                                        condition = binaryExpr(
                                            binaryExpr(
                                                identifier("i", intType, 5),
                                                "%",
                                                intLiteral(2, intType),
                                                intType
                                            ),
                                            "==",
                                            intLiteral(0, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            continueStmt()
                                        )
                                    ),
                                    assignment(
                                        identifier("sum", intType, 5),
                                        binaryExpr(
                                            identifier("sum", intType, 5),
                                            "+",
                                            identifier("i", intType, 5),
                                            intType
                                        )
                                    )
                                )
                            ),
                            returnStmt(identifier("sum", intType, 5))
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
        assertEquals(25, result)
    }
    
    @Test
    fun `nested lambda reads outer variable`() {
        /* 
         * outer = 100
         * outerLambda = () => {
         *     innerLambda = () => { return outer }
         *     return innerLambda.lambda()
         * }
         * return outerLambda.lambda()
         */
        val ast = buildTypedAst {
            val intType = intType()
            val outerLambdaTypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            val innerLambdaTypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("outer", intType, intLiteral(100, intType)),
                    varDecl("outerLambda", outerLambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            varDecl("innerLambda", innerLambdaTypeIdx, lambdaExpr(
                                parameters = emptyList(),
                                body = listOf(
                                    returnStmt(identifier("outer", intType, 3))
                                ),
                                lambdaTypeIndex = innerLambdaTypeIdx
                            )),
                            returnStmt(
                                expressionCall(
                        expression = identifier("innerLambda", innerLambdaTypeIdx, 5),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                            )
                        ),
                        lambdaTypeIndex = outerLambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                        expression = identifier("outerLambda", outerLambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100, result)
    }
    
    @Test
    fun `nested lambda modifies outer variable`() {
        /* 
         * counter = 0
         * outerLambda = () => {
         *     innerLambda = () => { counter = counter + 10 }
         *     innerLambda.lambda()
         *     return counter
         * }
         * return outerLambda.lambda()
         */
        val ast = buildTypedAst {
            val intType = intType()
            val voidType = voidType()
            val outerLambdaTypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            val innerLambdaTypeIdx = lambdaType(VoidType())
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("outerLambda", outerLambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            varDecl("innerLambda", innerLambdaTypeIdx, lambdaExpr(
                                parameters = emptyList(),
                                body = listOf(
                                    assignment(
                                        identifier("counter", intType, 3),
                                        binaryExpr(
                                            identifier("counter", intType, 3),
                                            "+",
                                            intLiteral(10, intType),
                                            intType
                                        )
                                    )
                                ),
                                lambdaTypeIndex = innerLambdaTypeIdx
                            )),
                            exprStmt(
                                expressionCall(
                        expression = identifier("innerLambda", innerLambdaTypeIdx, 5),
                        arguments = emptyList(),
                        resultTypeIndex = voidType
                    )
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = outerLambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                        expression = identifier("outerLambda", outerLambdaTypeIdx, 3),
                        arguments = emptyList(),
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
    fun `deeply nested lambda accesses outer variable`() {
        /* 
         * value = 42
         * f1 = () => {
         *     f2 = () => {
         *         f3 = () => { return value }
         *         return f3.lambda()
         *     }
         *     return f2.lambda()
         * }
         * return f1.lambda()
         */
        val ast = buildTypedAst {
            val intType = intType()
            val f1TypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            val f2TypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            val f3TypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("value", intType, intLiteral(42, intType)),
                    varDecl("f1", f1TypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            varDecl("f2", f2TypeIdx, lambdaExpr(
                                parameters = emptyList(),
                                body = listOf(
                                    varDecl("f3", f3TypeIdx, lambdaExpr(
                                        parameters = emptyList(),
                                        body = listOf(
                                            returnStmt(identifier("value", intType, 3))
                                        ),
                                        lambdaTypeIndex = f3TypeIdx
                                    )),
                                    returnStmt(
                                        expressionCall(
                        expression = identifier("f3", f3TypeIdx, 7),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                                    )
                                ),
                                lambdaTypeIndex = f2TypeIdx
                            )),
                            returnStmt(
                                expressionCall(
                        expression = identifier("f2", f2TypeIdx, 5),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                            )
                        ),
                        lambdaTypeIndex = f1TypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                        expression = identifier("f1", f1TypeIdx, 3),
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
    fun `lambda with for loop accessing outer variable`() {
        /*
         * This test verifies that for loops inside lambdas correctly 
         * handle captured variables from outer scopes.
         * 
         * outer = 10
         * myLambda = () => {
         *     sum = 0
         *     for (item in list) {
         *         sum = sum + item + outer
         *     }
         *     return sum
         * }
         * return myLambda.lambda()
         * 
         * With list = [1, 2, 3], expected result = (1+10) + (2+10) + (3+10) = 36
         */
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            val lambdaTypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("outer", intType, intLiteral(10, intType)),
                    varDecl("myLambda", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            varDecl("sum", intType, intLiteral(0, intType)),
                            forStmt(
                                variableName = "item",
                                variableType = intType,
                                iterable = identifier("list", listType, 2),
                                body = listOf(
                                    assignment(
                                        identifier("sum", intType, 5),
                                        binaryExpr(
                                            binaryExpr(
                                                identifier("sum", intType, 5), 
                                                "+", 
                                                identifier("item", intType, 6),
                                                intType
                                            ),
                                            "+",
                                            identifier("outer", intType, 3),
                                            intType
                                        )
                                    )
                                )
                            ),
                            returnStmt(identifier("sum", intType, 5))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                        expression = identifier("myLambda", lambdaTypeIdx, 3),
                        arguments = emptyList(),
                        resultTypeIndex = intType
                    )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3))
        assertEquals(36, result)
    }

    @Test
    fun `lambda toString returns string representation`() {
        // Test that toString() works on lambdas (inherited from Any)
        val ast = buildTypedAst {
            val stringType = stringType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    // Create lambda
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(intLiteral(42, intType()))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Call toString() on the lambda
                    returnStmt(
                        memberCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            member = "toString",
                            overload = "",
                            arguments = emptyList(),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction") as String
        // Lambda should return some string representation (implementation defined)
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
    
    // =============== Lambda Type Coercion Tests ===============
    
    @Test
    fun `lambda with fewer params adapts to expected type with more params`() {
        // Lambda expression takes 1 param, but expected type takes 2 params
        // The lambda ignores the second param
        // val f: (int, int) => int = (x) => x + 10
        // return f(5, 100)  // second param ignored, should return 15
        val ast = buildTypedAst {
            val intType = intType()
            // Actual lambda type: (int) => int
            val actualLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            // Expected lambda type: (int, int) => int
            val expectedLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", false),
                "b" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // Declare variable with expected type, but assign lambda with actual type
                    varDecl("f", expectedLambdaType, lambdaExpr(
                        parameters = listOf("x"),  // Only 1 param
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),
                                    "+",
                                    intLiteral(10, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = actualLambdaType
                    )),
                    // Call with 2 params - second is ignored
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", expectedLambdaType, 3),
                            arguments = listOf(
                                intLiteral(5, intType),
                                intLiteral(100, intType)  // This is ignored
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(15, result)
    }
    
    @Test
    fun `lambda with parameter type coercion - boxed to primitive`() {
        // Lambda expects primitive int, but expected type provides nullable Int (boxed)
        // val f: (int?) => int = (x) => x + 1  -- actual lambda unboxes
        val ast = buildTypedAst {
            val intType = intType()
            val intNullable = intNullableType()
            // Actual lambda type: (int) => int
            val actualLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            // Expected lambda type: (int?) => int
            val expectedLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", true)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", expectedLambdaType, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = actualLambdaType
                    )),
                    // Call with a boxed int (simulated by passing primitive, runtime will box)
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", expectedLambdaType, 3),
                            arguments = listOf(intLiteral(41, intType)),
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
    fun `lambda with return type coercion - primitive to boxed`() {
        // Lambda returns primitive int, but expected type expects nullable Int (boxed)
        // val f: () => int? = () => 42
        val ast = buildTypedAst {
            val intType = intType()
            val intNullable = intNullableType()
            // Actual lambda type: () => int
            val actualLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false)
            )
            // Expected lambda type: () => int?
            val expectedLambdaType = lambdaType(
                ClassTypeRef("builtin.int", true)
            )
            function(
                name = "testFunction",
                returnType = intNullable,
                body = listOf(
                    varDecl("f", expectedLambdaType, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(intLiteral(42, intType))
                        ),
                        lambdaTypeIndex = actualLambdaType
                    )),
                    // Call - result should be boxed
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", expectedLambdaType, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intNullable
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `lambda with combined param count and type coercion`() {
        // Lambda takes 1 int param, expected takes 2 params (int?, int)
        // Tests both extra params and parameter type coercion
        val ast = buildTypedAst {
            val intType = intType()
            val intNullable = intNullableType()
            // Actual: (int) => int
            val actualLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            // Expected: (int?, int) => int
            val expectedLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", true),
                "b" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("f", expectedLambdaType, lambdaExpr(
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
                        lambdaTypeIndex = actualLambdaType
                    )),
                    // Call with boxed first param (will be unboxed), second param ignored
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", expectedLambdaType, 3),
                            arguments = listOf(
                                intLiteral(21, intType),
                                intLiteral(999, intType)  // ignored
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    // =============== Lambda Coercion via Variable Reassignment Tests ===============
    // These tests verify that lambda type coercion works when a lambda is assigned to
    // a variable of a different compatible type. This goes through ExpressionCompiler.compile()
    // which now uses CoercionUtil.emitCoercion with lambda coercion support.
    
    @Test
    fun `reassigning lambda to variable with different parameter count`() {
        // Test that a lambda with fewer params can be assigned to a variable
        // expecting more params, and the result is correctly wrapped.
        // val source: (int) => int = (x) => x * 2
        // val target: (int, int) => int = source  // wrapped to accept extra param
        // return target(21, 999)  // second param ignored, should return 42
        val ast = buildTypedAst {
            val intType = intType()
            // Source lambda type: (int) => int
            val sourceLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            // Target lambda type: (int, int) => int
            val targetLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", false),
                "b" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // Declare source lambda
                    varDecl("source", sourceLambdaType, lambdaExpr(
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
                        lambdaTypeIndex = sourceLambdaType
                    )),
                    // Assign source lambda to target variable with different type
                    // This should trigger lambda coercion through ExpressionCompiler
                    varDecl("target", targetLambdaType, 
                        identifier("source", sourceLambdaType, 3)
                    ),
                    // Call target with 2 params (second is ignored)
                    returnStmt(
                        expressionCall(
                            expression = identifier("target", targetLambdaType, 3),
                            arguments = listOf(
                                intLiteral(21, intType),
                                intLiteral(999, intType)  // ignored
                            ),
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
    fun `reassigning lambda with parameter type coercion`() {
        // Test that a lambda with primitive param can be assigned to a variable
        // expecting boxed param type, with proper coercion applied.
        // val source: (int) => int = (x) => x + 1
        // val target: (int?) => int = source  // wrapped to accept boxed then unbox
        // return target(41)  // should return 42
        val ast = buildTypedAst {
            val intType = intType()
            val intNullable = intNullableType()
            // Source lambda type: (int) => int
            val sourceLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            // Target lambda type: (int?) => int
            val targetLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", true)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // Declare source lambda
                    varDecl("source", sourceLambdaType, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = sourceLambdaType
                    )),
                    // Assign source lambda to target variable with boxed param type
                    varDecl("target", targetLambdaType, 
                        identifier("source", sourceLambdaType, 3)
                    ),
                    // Call target with a boxed int
                    returnStmt(
                        expressionCall(
                            expression = identifier("target", targetLambdaType, 3),
                            arguments = listOf(intLiteral(41, intType)),
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
    fun `reassigning lambda with return type coercion`() {
        // Test that a lambda returning primitive can be assigned to a variable
        // expecting boxed return type, with proper coercion applied.
        // val source: () => int = () => 42
        // val target: () => int? = source  // wrapped to box return value
        // return target()  // should return boxed 42
        val ast = buildTypedAst {
            val intType = intType()
            val intNullable = intNullableType()
            // Source lambda type: () => int
            val sourceLambdaType = lambdaType(
                ClassTypeRef("builtin.int", false)
            )
            // Target lambda type: () => int?
            val targetLambdaType = lambdaType(
                ClassTypeRef("builtin.int", true)
            )
            function(
                name = "testFunction",
                returnType = intNullable,
                body = listOf(
                    // Declare source lambda
                    varDecl("source", sourceLambdaType, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(intLiteral(42, intType))
                        ),
                        lambdaTypeIndex = sourceLambdaType
                    )),
                    // Assign source lambda to target variable with boxed return type
                    varDecl("target", targetLambdaType, 
                        identifier("source", sourceLambdaType, 3)
                    ),
                    // Call target - result should be boxed
                    returnStmt(
                        expressionCall(
                            expression = identifier("target", targetLambdaType, 3),
                            arguments = emptyList(),
                            resultTypeIndex = intNullable
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `chained lambda reassignments with different types`() {
        // Test multiple chained reassignments with increasing parameter counts
        // val a: (int) => int = (x) => x
        // val b: (int, int) => int = a
        // val c: (int, int, int) => int = b
        // return c(42, 10, 20)  // only first param used, should return 42
        val ast = buildTypedAst {
            val intType = intType()
            // Lambda type with 1 param
            val type1 = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.int", false)
            )
            // Lambda type with 2 params
            val type2 = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", false),
                "b" to ClassTypeRef("builtin.int", false)
            )
            // Lambda type with 3 params
            val type3 = lambdaType(
                ClassTypeRef("builtin.int", false),
                "a" to ClassTypeRef("builtin.int", false),
                "b" to ClassTypeRef("builtin.int", false),
                "c" to ClassTypeRef("builtin.int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // val a: (int) => int = (x) => x
                    varDecl("a", type1, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(identifier("x", intType, 4))
                        ),
                        lambdaTypeIndex = type1
                    )),
                    // val b: (int, int) => int = a
                    varDecl("b", type2, 
                        identifier("a", type1, 3)
                    ),
                    // val c: (int, int, int) => int = b
                    varDecl("c", type3, 
                        identifier("b", type2, 3)
                    ),
                    // return c(42, 10, 20) - only first param used
                    returnStmt(
                        expressionCall(
                            expression = identifier("c", type3, 3),
                            arguments = listOf(
                                intLiteral(42, intType),
                                intLiteral(10, intType),
                                intLiteral(20, intType)
                            ),
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
    fun `lambda with string parameter type assignment`() {
        // Tests lambda variable assignment with specific types
        // var takesString: (string) => int = (x) => 42
        // return takesString("test")
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            
            // Lambda type: (string) => int
            val lambdaType = lambdaType(
                ClassTypeRef("builtin.int", false),
                "x" to ClassTypeRef("builtin.string", false)
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // var takesString: (string) => int = (x) => 42
                    varDecl("takesString", lambdaType, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(intLiteral(42, intType))
                        ),
                        lambdaTypeIndex = lambdaType
                    )),
                    // return takesString("test")
                    returnStmt(
                        expressionCall(
                            expression = identifier("takesString", lambdaType, 3),
                            arguments = listOf(stringLiteral("test", stringType)),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
}
