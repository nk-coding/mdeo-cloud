package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import com.mdeo.expression.ast.types.ClassTypeRef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests specifically targeting the refactored call compiler infrastructure.
 * 
 * These tests verify that:
 * 1. Expression calls properly use type coercion
 * 2. Lambda calls with multiple parameters work correctly
 * 3. Member calls on file-scope methods with coercion work correctly
 * 4. The shared AbstractCallCompiler methods work correctly across all subclasses
 */
class CallCompilerRefactoringTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Expression Call Type Coercion ====================
    
    @Test
    fun `expression call with int to long coercion`() {
        // val f = (x: Long) => x + 1L
        // return f(10)  // 10 (Int) should be coerced to Long
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "long", false),
                "x" to ClassTypeRef("builtin", "long", false)
            )
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", longType, 4),
                                    "+",
                                    longLiteral(1L, longType),
                                    longType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(10, intType)),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(11L, result)
    }
    
    @Test
    fun `expression call with int to double coercion`() {
        // val f = (x: Double) => x + 0.5
        // return f(10)  // 10 (Int) should be coerced to Double
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "double", false),
                "x" to ClassTypeRef("builtin", "double", false)
            )
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", doubleType, 4),
                                    "+",
                                    doubleLiteral(0.5, doubleType),
                                    doubleType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(10, intType)),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10.5, result)
    }
    
    @Test
    fun `expression call with int to nullable int boxing`() {
        // val f = (x: Int?) => x
        // return f(42)  // 42 (Int) should be boxed to Integer
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", true),
                "x" to ClassTypeRef("builtin", "int", true)
            )
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("f", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(identifier("x", intNullableType, 4))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("f", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(42, intType)),
                            resultTypeIndex = intNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    // ==================== Lambda Call with Multiple Parameters ====================
    
    @Test
    fun `lambda call with three int parameters`() {
        // val add3 = (a: Int, b: Int, c: Int) => a + b + c
        // return add3(1, 2, 3)  // should return 6
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", false),
                "a" to ClassTypeRef("builtin", "int", false),
                "b" to ClassTypeRef("builtin", "int", false),
                "c" to ClassTypeRef("builtin", "int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("add3", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("a", "b", "c"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    binaryExpr(
                                        identifier("a", intType, 4),
                                        "+",
                                        identifier("b", intType, 4),
                                        intType
                                    ),
                                    "+",
                                    identifier("c", intType, 4),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("add3", lambdaTypeIdx, 3),
                            arguments = listOf(
                                intLiteral(1, intType),
                                intLiteral(2, intType),
                                intLiteral(3, intType)
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)
    }
    
    @Test
    fun `lambda call with mixed type parameters`() {
        // val combine = (i: Int, l: Long, d: Double) => d + l.toDouble() + i.toDouble()
        // For simplicity, just return d cast to long
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val doubleType = doubleType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "long", false),
                "i" to ClassTypeRef("builtin", "int", false),
                "l" to ClassTypeRef("builtin", "long", false),
                "d" to ClassTypeRef("builtin", "double", false)
            )
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("combine", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("i", "l", "d"),
                        body = listOf(
                            // Return i + l (ignoring d for simplicity)
                            returnStmt(
                                binaryExpr(
                                    identifier("i", intType, 4),
                                    "+",
                                    identifier("l", longType, 4),
                                    longType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("combine", lambdaTypeIdx, 3),
                            arguments = listOf(
                                intLiteral(10, intType),
                                longLiteral(20L, longType),
                                doubleLiteral(30.0, doubleType)
                            ),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30L, result)
    }
    
    @Test
    fun `lambda call with multiple parameters and coercion`() {
        // val add = (a: Long, b: Long) => a + b
        // return add(10, 20)  // both ints should be coerced to Long
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "long", false),
                "a" to ClassTypeRef("builtin", "long", false),
                "b" to ClassTypeRef("builtin", "long", false)
            )
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("add", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("a", "b"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("a", longType, 4),
                                    "+",
                                    identifier("b", longType, 4),
                                    longType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("add", lambdaTypeIdx, 3),
                            arguments = listOf(
                                intLiteral(10, intType),
                                intLiteral(20, intType)
                            ),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30L, result)
    }
    
    // ==================== File-Scope Function Call with Coercion ====================
    
    @Test
    fun `file-scope function call with int to long coercion`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            
            // fun add(a: Long, b: Long): Long = a + b
            function(
                name = "add",
                returnType = longType,
                parameters = listOf(
                    TypedParameter("a", longType),
                    TypedParameter("b", longType)
                ),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("a", longType, 2),
                            "+",
                            identifier("b", longType, 2),
                            longType
                        )
                    )
                )
            )
            
            // testFunction: add(10, 20)  // both ints should be coerced to Long
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "add",
                            overload = "",
                            arguments = listOf(
                                intLiteral(10, intType),
                                intLiteral(20, intType)
                            ),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(30L, result)
    }
    
    @Test
    fun `file-scope function call with mixed nullability coercion`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            val longType = longType()
            val longNullableType = longNullableType()
            
            // fun process(a: Int, b: Long?): Long = a.toLong()
            function(
                name = "process",
                returnType = longType,
                parameters = listOf(
                    TypedParameter("a", intType),
                    TypedParameter("b", longNullableType)
                ),
                body = listOf(
                    returnStmt(identifier("a", intType, 2))
                )
            )
            
            // testFunction: process(42, 100)  // 100 (Int) should be boxed to Long?
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "process",
                            overload = "",
                            arguments = listOf(
                                intLiteral(42, intType),
                                intLiteral(100, intType)  // needs coercion to Long?
                            ),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    // ==================== Member Call Coercion Edge Cases ====================
    
    @Test
    fun `member call result used in expression call argument`() {
        // val double = (x: Int) => x * 2
        // return double("hello".length())
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", false),
                "x" to ClassTypeRef("builtin", "int", false)
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
                    returnStmt(
                        expressionCall(
                            expression = identifier("double", lambdaTypeIdx, 3),
                            arguments = listOf(
                                memberCall(
                                    expression = stringLiteral("hello", stringType),
                                    member = "length",
                                    overload = "",
                                    arguments = emptyList(),
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
        assertEquals(10, result)  // "hello".length() = 5, * 2 = 10
    }
    
    @Test
    fun `expression call result as member call target`() {
        // val getMessage = () => "hello"
        // return getMessage().length()
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "string", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("getMessage", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            returnStmt(stringLiteral("hello", stringType))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        memberCall(
                            expression = expressionCall(
                                expression = identifier("getMessage", lambdaTypeIdx, 3),
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            ),
                            member = "length",
                            overload = "",
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
    
    // ==================== Null Handling in Expression Calls ====================
    
    @Test
    fun `expression call with nullable parameter receiving null`() {
        // val identity = (x: Int?) => x
        // return identity(null)
        val ast = buildTypedAst {
            val intNullableType = intNullableType()
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", true),
                "x" to ClassTypeRef("builtin", "int", true)
            )
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("identity", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(identifier("x", intNullableType, 4))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        expressionCall(
                            expression = identifier("identity", lambdaTypeIdx, 3),
                            arguments = listOf(nullLiteral(intNullableType)),
                            resultTypeIndex = intNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    // ==================== Complex Nested Calls ====================
    
    @Test
    fun `nested expression calls with different return types`() {
        // val double = (x: Int) => x * 2
        // val triple = (x: Int) => x * 3
        // return triple(double(7))  // should return 42 (7 * 2 = 14, 14 * 3 = 42)
        val ast = buildTypedAst {
            val intType = intType()
            val intToIntLambda = lambdaType(
                ClassTypeRef("builtin", "int", false),
                "x" to ClassTypeRef("builtin", "int", false)
            )
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // val double = (x: Int) => x * 2
                    varDecl("double", intToIntLambda, lambdaExpr(
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
                        lambdaTypeIndex = intToIntLambda
                    )),
                    // val triple = (x: Int) => x * 3
                    varDecl("triple", intToIntLambda, lambdaExpr(
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
                        lambdaTypeIndex = intToIntLambda
                    )),
                    // return triple(double(7))
                    returnStmt(
                        expressionCall(
                            expression = identifier("triple", intToIntLambda, 3),
                            arguments = listOf(
                                expressionCall(
                                    expression = identifier("double", intToIntLambda, 3),
                                    arguments = listOf(intLiteral(7, intType)),
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
        assertEquals(42, result)
    }
}
