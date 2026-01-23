package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for FunctionCallCompiler.
 * 
 * Tests verify that function calls are correctly compiled and executed,
 * including parameter passing, type coercion, and return value handling.
 */
class FunctionCallCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Basic Function Calls ====================
    
    @Test
    fun `call function with no parameters returning int`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            // Define the function: fun getValue(): Int = 42
            function(
                name = "getValue",
                returnType = intType,
                body = listOf(returnStmt(intLiteral(42, intType)))
            )
            
            // Define the test function that calls getValue
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "getValue",
                            overload = "getValue():builtin.int",
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
    fun `call function with no parameters returning long`() {
        val ast = buildTypedAst {
            val longType = longType()
            
            function(
                name = "getValue",
                returnType = longType,
                body = listOf(returnStmt(longLiteral(9876543210L, longType)))
            )
            
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "getValue",
                            overload = "getValue():builtin.long",
                            arguments = emptyList(),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9876543210L, result)
    }
    
    @Test
    fun `call function with no parameters returning float`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            
            function(
                name = "getValue",
                returnType = floatType,
                body = listOf(returnStmt(floatLiteral(3.14f, floatType)))
            )
            
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "getValue",
                            overload = "getValue():builtin.float",
                            arguments = emptyList(),
                            resultTypeIndex = floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    @Test
    fun `call function with no parameters returning double`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            
            function(
                name = "getValue",
                returnType = doubleType,
                body = listOf(returnStmt(doubleLiteral(2.71828, doubleType)))
            )
            
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "getValue",
                            overload = "getValue():builtin.double",
                            arguments = emptyList(),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.71828, result)
    }
    
    @Test
    fun `call function with no parameters returning boolean`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            
            function(
                name = "getValue",
                returnType = boolType,
                body = listOf(returnStmt(booleanLiteral(true, boolType)))
            )
            
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "getValue",
                            overload = "getValue():builtin.boolean",
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
    fun `call function with no parameters returning string`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "getValue",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello world", stringType)))
            )
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "getValue",
                            overload = "getValue():builtin.string",
                            arguments = emptyList(),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello world", result)
    }
    
    // ==================== Functions with Single Parameter ====================
    
    @Test
    fun `call function with single int parameter`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            // fun double(x: Int): Int = x + x
            function(
                name = "double",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            "+",
                            identifier("x", intType, 2),
                            intType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "double",
                            overload = "double(builtin.int):builtin.int",
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
    fun `call function with single long parameter`() {
        val ast = buildTypedAst {
            val longType = longType()
            
            function(
                name = "double",
                returnType = longType,
                parameters = listOf(TypedParameter("x", longType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", longType, 2),
                            "+",
                            identifier("x", longType, 2),
                            longType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "double",
                            overload = "double(builtin.long):builtin.long",
                            arguments = listOf(longLiteral(5000000000L, longType)),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10000000000L, result)
    }
    
    @Test
    fun `call function with single string parameter`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            // fun greet(name: String): String = "Hello, " + name
            function(
                name = "greet",
                returnType = stringType,
                parameters = listOf(TypedParameter("name", stringType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            stringLiteral("Hello, ", stringType),
                            "+",
                            identifier("name", stringType, 2),
                            stringType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "greet",
                            overload = "greet(builtin.string):builtin.string",
                            arguments = listOf(stringLiteral("World", stringType)),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("Hello, World", result)
    }
    
    // ==================== Functions with Multiple Parameters ====================
    
    @Test
    fun `call function with two int parameters`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            // fun add(a: Int, b: Int): Int = a + b
            function(
                name = "add",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("a", intType),
                    TypedParameter("b", intType)
                ),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("a", intType, 2),
                            "+",
                            identifier("b", intType, 2),
                            intType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "add",
                            overload = "add(builtin.int,builtin.int):builtin.int",
                            arguments = listOf(
                                intLiteral(10, intType),
                                intLiteral(32, intType)
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
    fun `call function with three parameters of different types`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            val stringType = stringType()
            
            // fun format(prefix: String, value: Double, multiplier: Int): String
            // returns prefix + (value * multiplier)
            function(
                name = "format",
                returnType = stringType,
                parameters = listOf(
                    TypedParameter("prefix", stringType),
                    TypedParameter("value", doubleType),
                    TypedParameter("multiplier", intType)
                ),
                body = listOf(
                    // Just return the prefix for this test
                    returnStmt(identifier("prefix", stringType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "format",
                            overload = "format(builtin.string,builtin.double,builtin.int):builtin.string",
                            arguments = listOf(
                                stringLiteral("Result: ", stringType),
                                doubleLiteral(3.14, doubleType),
                                intLiteral(2, intType)
                            ),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("Result: ", result)
    }
    
    // ==================== Type Coercion in Arguments ====================
    
    @Test
    fun `int argument coerced to long parameter`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            
            // fun processLong(value: Long): Long = value + 1
            function(
                name = "processLong",
                returnType = longType,
                parameters = listOf(TypedParameter("value", longType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("value", longType, 2),
                            "+",
                            longLiteral(1L, longType),
                            longType
                        )
                    )
                )
            )
            
            // testFunction calls processLong with an int
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "processLong",
                            overload = "processLong(builtin.long):builtin.long",
                            arguments = listOf(intLiteral(41, intType)),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    @Test
    fun `int argument coerced to double parameter`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleType = doubleType()
            
            // fun processDouble(value: Double): Double = value * 2
            function(
                name = "processDouble",
                returnType = doubleType,
                parameters = listOf(TypedParameter("value", doubleType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("value", doubleType, 2),
                            "*",
                            doubleLiteral(2.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
            
            // testFunction calls processDouble with an int
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "processDouble",
                            overload = "processDouble(builtin.double):builtin.double",
                            arguments = listOf(intLiteral(21, intType)),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0, result)
    }
    
    @Test
    fun `float argument coerced to double parameter`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val doubleType = doubleType()
            
            function(
                name = "processDouble",
                returnType = doubleType,
                parameters = listOf(TypedParameter("value", doubleType)),
                body = listOf(
                    returnStmt(identifier("value", doubleType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "processDouble",
                            overload = "processDouble(builtin.double):builtin.double",
                            arguments = listOf(floatLiteral(3.14f, floatType)),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        // Float to double conversion may have precision differences
        assertEquals(3.14f.toDouble(), result as Double, 0.001)
    }
    
    @Test
    fun `long argument coerced to double parameter`() {
        val ast = buildTypedAst {
            val longType = longType()
            val doubleType = doubleType()
            
            function(
                name = "processDouble",
                returnType = doubleType,
                parameters = listOf(TypedParameter("value", doubleType)),
                body = listOf(
                    returnStmt(identifier("value", doubleType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "processDouble",
                            overload = "processDouble(builtin.double):builtin.double",
                            arguments = listOf(longLiteral(100L, longType)),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100.0, result)
    }
    
    // ==================== Nested Function Calls ====================
    
    @Test
    fun `nested function calls`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            // fun increment(x: Int): Int = x + 1
            function(
                name = "increment",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            "+",
                            intLiteral(1, intType),
                            intType
                        )
                    )
                )
            )
            
            // testFunction calls increment(increment(increment(0)))
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "increment",
                            overload = "increment(builtin.int):builtin.int",
                            arguments = listOf(
                                functionCall(
                                    name = "increment",
                                    overload = "increment(builtin.int):builtin.int",
                                    arguments = listOf(
                                        functionCall(
                                            name = "increment",
                                            overload = "increment(builtin.int):builtin.int",
                                            arguments = listOf(intLiteral(0, intType)),
                                            resultTypeIndex = intType
                                        )
                                    ),
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
        assertEquals(3, result)
    }
    
    @Test
    fun `function call as argument to another function`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            // fun square(x: Int): Int = x * x
            function(
                name = "square",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            "*",
                            identifier("x", intType, 2),
                            intType
                        )
                    )
                )
            )
            
            // fun add(a: Int, b: Int): Int = a + b
            function(
                name = "add",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("a", intType),
                    TypedParameter("b", intType)
                ),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("a", intType, 2),
                            "+",
                            identifier("b", intType, 2),
                            intType
                        )
                    )
                )
            )
            
            // testFunction: add(square(2), square(3)) = 4 + 9 = 13
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "add",
                            overload = "add(builtin.int,builtin.int):builtin.int",
                            arguments = listOf(
                                functionCall(
                                    name = "square",
                                    overload = "square(builtin.int):builtin.int",
                                    arguments = listOf(intLiteral(2, intType)),
                                    resultTypeIndex = intType
                                ),
                                functionCall(
                                    name = "square",
                                    overload = "square(builtin.int):builtin.int",
                                    arguments = listOf(intLiteral(3, intType)),
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
        assertEquals(13, result)
    }
    
    // ==================== Function Calls with Variable Arguments ====================
    
    @Test
    fun `call function with variable as argument`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            function(
                name = "double",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            "*",
                            intLiteral(2, intType),
                            intType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("myVar", intType, intLiteral(21, intType)),
                    returnStmt(
                        functionCall(
                            name = "double",
                            overload = "double(builtin.int):builtin.int",
                            arguments = listOf(identifier("myVar", intType, 3)),
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
    fun `call function with expression as argument`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            function(
                name = "square",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            "*",
                            identifier("x", intType, 2),
                            intType
                        )
                    )
                )
            )
            
            // testFunction: square(3 + 2) = square(5) = 25
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "square",
                            overload = "square(builtin.int):builtin.int",
                            arguments = listOf(
                                binaryExpr(
                                    intLiteral(3, intType),
                                    "+",
                                    intLiteral(2, intType),
                                    intType
                                )
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(25, result)
    }
    
    // ==================== Boolean Return Values ====================
    
    @Test
    fun `call function returning boolean in condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            // fun isPositive(x: Int): Boolean = x > 0
            function(
                name = "isPositive",
                returnType = boolType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            ">",
                            intLiteral(0, intType),
                            boolType
                        )
                    )
                )
            )
            
            // testFunction: if (isPositive(5)) 1 else 0
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            functionCall(
                                name = "isPositive",
                                overload = "isPositive(builtin.int):builtin.boolean",
                                arguments = listOf(intLiteral(5, intType)),
                                resultTypeIndex = boolType
                            ),
                            intLiteral(1, intType),
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
}
