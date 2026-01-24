package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Edge case tests for FunctionCallCompiler.
 * 
 * These tests focus on potential bugs in type coercion, boxing/unboxing,
 * and primitive conversions when calling functions.
 */
class FunctionCallEdgeCasesTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Boxing for Nullable Parameters ====================
    
    @Test
    fun `int literal passed to nullable Int parameter should be boxed`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            
            // fun identity(x: Int?): Int? = x
            function(
                name = "identity",
                returnType = intNullableType,
                parameters = listOf(TypedParameter("x", intNullableType)),
                body = listOf(
                    returnStmt(identifier("x", intNullableType, 2))
                )
            )
            
            // testFunction: identity(42)
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
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
    
    @Test
    fun `long literal passed to nullable Long parameter should be boxed`() {
        val ast = buildTypedAst {
            val longType = longType()
            val longNullableType = longNullableType()
            
            function(
                name = "identity",
                returnType = longNullableType,
                parameters = listOf(TypedParameter("x", longNullableType)),
                body = listOf(
                    returnStmt(identifier("x", longNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = longNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(longLiteral(9876543210L, longType)),
                            resultTypeIndex = longNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9876543210L, result)
    }
    
    @Test
    fun `double literal passed to nullable Double parameter should be boxed`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val doubleNullableType = doubleNullableType()
            
            function(
                name = "identity",
                returnType = doubleNullableType,
                parameters = listOf(TypedParameter("x", doubleNullableType)),
                body = listOf(
                    returnStmt(identifier("x", doubleNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = doubleNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(doubleLiteral(3.14159, doubleType)),
                            resultTypeIndex = doubleNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14159, result)
    }
    
    @Test
    fun `boolean literal passed to nullable Boolean parameter should be boxed`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val boolNullableType = booleanNullableType()
            
            function(
                name = "identity",
                returnType = boolNullableType,
                parameters = listOf(TypedParameter("x", boolNullableType)),
                body = listOf(
                    returnStmt(identifier("x", boolNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = boolNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(booleanLiteral(true, boolType)),
                            resultTypeIndex = boolNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `null passed to nullable Int parameter`() {
        val ast = buildTypedAst {
            val intNullableType = intNullableType()
            
            function(
                name = "identity",
                returnType = intNullableType,
                parameters = listOf(TypedParameter("x", intNullableType)),
                body = listOf(
                    returnStmt(identifier("x", intNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
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
    
    // ==================== Unboxing from Nullable Return Types ====================
    
    @Test
    fun `function returning nullable used where non-nullable expected - unboxing`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            
            // fun getValue(): Int? = 42
            function(
                name = "getValue",
                returnType = intNullableType,
                body = listOf(
                    returnStmt(intLiteral(42, intType))
                )
            )
            
            // testFunction: getValue() + 1 (where getValue returns Int?)
            // This requires unboxing
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            functionCall(
                                name = "getValue",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intNullableType
                            ),
                            "+",
                            intLiteral(1, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(43, result)
    }
    
    // ==================== Primitive Widening with Nullable Parameters ====================
    
    @Test
    fun `int passed to function expecting nullable Long should be widened and boxed`() {
        val ast = buildTypedAst {
            val intType = intType()
            val longNullableType = longNullableType()
            
            function(
                name = "identity",
                returnType = longNullableType,
                parameters = listOf(TypedParameter("x", longNullableType)),
                body = listOf(
                    returnStmt(identifier("x", longNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = longNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(intLiteral(42, intType)),
                            resultTypeIndex = longNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42L, result)
    }
    
    @Test
    fun `int passed to function expecting nullable Double should be widened and boxed`() {
        val ast = buildTypedAst {
            val intType = intType()
            val doubleNullableType = doubleNullableType()
            
            function(
                name = "identity",
                returnType = doubleNullableType,
                parameters = listOf(TypedParameter("x", doubleNullableType)),
                body = listOf(
                    returnStmt(identifier("x", doubleNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = doubleNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(intLiteral(42, intType)),
                            resultTypeIndex = doubleNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42.0, result)
    }
    
    // ==================== Any Type Parameters (Generic Boxing) ====================
    
    @Test
    fun `int passed to function expecting Any should be boxed`() {
        val ast = buildTypedAst {
            val intType = intType()
            val anyNullableType = anyNullableType()
            
            function(
                name = "identity",
                returnType = anyNullableType,
                parameters = listOf(TypedParameter("x", anyNullableType)),
                body = listOf(
                    returnStmt(identifier("x", anyNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = anyNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(intLiteral(42, intType)),
                            resultTypeIndex = anyNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `long passed to function expecting Any should be boxed`() {
        val ast = buildTypedAst {
            val longType = longType()
            val anyNullableType = anyNullableType()
            
            function(
                name = "identity",
                returnType = anyNullableType,
                parameters = listOf(TypedParameter("x", anyNullableType)),
                body = listOf(
                    returnStmt(identifier("x", anyNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = anyNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(longLiteral(9876543210L, longType)),
                            resultTypeIndex = anyNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9876543210L, result)
    }
    
    @Test
    fun `boolean passed to function expecting Any should be boxed`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val anyNullableType = anyNullableType()
            
            function(
                name = "identity",
                returnType = anyNullableType,
                parameters = listOf(TypedParameter("x", anyNullableType)),
                body = listOf(
                    returnStmt(identifier("x", anyNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = anyNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(booleanLiteral(true, boolType)),
                            resultTypeIndex = anyNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Multiple Arguments with Mixed Nullability ====================
    
    @Test
    fun `function with mixed nullable and non-nullable parameters`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            
            // fun add(a: Int, b: Int?): Int = a + (b ?: 0)
            // For simplicity, we'll just return a
            function(
                name = "add",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("a", intType),
                    TypedParameter("b", intNullableType)
                ),
                body = listOf(
                    returnStmt(identifier("a", intType, 2))
                )
            )
            
            // testFunction: add(10, 32)
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "add",
                            overload = "",
                            arguments = listOf(
                                intLiteral(10, intType),
                                intLiteral(32, intType)  // int passed to Int? - needs boxing
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
    fun `function with first nullable and second non-nullable parameters`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            
            // fun add(a: Int?, b: Int): Int = b
            function(
                name = "add",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("a", intNullableType),
                    TypedParameter("b", intType)
                ),
                body = listOf(
                    returnStmt(identifier("b", intType, 2))
                )
            )
            
            // testFunction: add(10, 32)
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "add",
                            overload = "",
                            arguments = listOf(
                                intLiteral(10, intType),  // int passed to Int? - needs boxing
                                intLiteral(32, intType)   // int passed to Int - no boxing
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(32, result)
    }
    
    // ==================== Float Type Boxing ====================
    
    @Test
    fun `float literal passed to nullable Float parameter should be boxed`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val floatNullableType = floatNullableType()
            
            function(
                name = "identity",
                returnType = floatNullableType,
                parameters = listOf(TypedParameter("x", floatNullableType)),
                body = listOf(
                    returnStmt(identifier("x", floatNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = floatNullableType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(floatLiteral(3.14f, floatType)),
                            resultTypeIndex = floatNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    // ==================== Nullable Variable passed to Nullable Parameter ====================
    
    @Test
    fun `nullable int variable passed to nullable Int parameter`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            
            function(
                name = "identity",
                returnType = intNullableType,
                parameters = listOf(TypedParameter("x", intNullableType)),
                body = listOf(
                    returnStmt(identifier("x", intNullableType, 2))
                )
            )
            
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("value", intNullableType, intLiteral(42, intType)),
                    returnStmt(
                        functionCall(
                            name = "identity",
                            overload = "",
                            arguments = listOf(identifier("value", intNullableType, 3)),
                            resultTypeIndex = intNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
}
