package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals

/**
 * Edge case tests for type coercion operations.
 * 
 * These tests verify the correct behavior of combined operations:
 * - Unboxing + widening (e.g., Integer → double)
 * - Widening + boxing (e.g., int → Double?)
 * - Unboxing + widening + boxing (e.g., Integer? → Double?)
 * 
 * These scenarios are particularly important for:
 * - Return statements where expression type differs from function return type
 * - Function call arguments where argument type differs from parameter type
 * - Variable assignments with type coercion
 */
class CoercionEdgeCasesTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Unboxing + Widening Tests ====================
    
    @Nested
    inner class UnboxingPlusWidening {
        
        @Test
        fun `Integer variable to double return - unbox then widen`() {
            // Scenario: nullable Int? variable returned as double
            // Requires: unbox Integer to int, then widen int to double
            val ast = buildTypedAst {
                val intNullableType = intNullableType()
                val doubleType = doubleType()
                val intType = intType()
                
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(42, intType)),
                        returnStmt(identifier("x", intNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }
        
        @Test
        fun `Integer variable to long return - unbox then widen`() {
            val ast = buildTypedAst {
                val intNullableType = intNullableType()
                val longType = longType()
                val intType = intType()
                
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(100, intType)),
                        returnStmt(identifier("x", intNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(100L, result)
        }
        
        @Test
        fun `Integer variable to float return - unbox then widen`() {
            val ast = buildTypedAst {
                val intNullableType = intNullableType()
                val floatType = floatType()
                val intType = intType()
                
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(55, intType)),
                        returnStmt(identifier("x", intNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(55.0f, result)
        }
        
        @Test
        fun `Long variable to double return - unbox then widen`() {
            val ast = buildTypedAst {
                val longNullableType = longNullableType()
                val doubleType = doubleType()
                val longType = longType()
                
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", longNullableType, longLiteral(9876543210L, longType)),
                        returnStmt(identifier("x", longNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(9876543210.0, result)
        }
        
        @Test
        fun `Float variable to double return - unbox then widen`() {
            val ast = buildTypedAst {
                val floatNullableType = floatNullableType()
                val doubleType = doubleType()
                val floatType = floatType()
                
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", floatNullableType, floatLiteral(3.14f, floatType)),
                        returnStmt(identifier("x", floatNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14f.toDouble(), result)
        }
    }
    
    // ==================== Widening + Boxing Tests ====================
    
    @Nested
    inner class WideningPlusBoxing {
        
        @Test
        fun `int literal to nullable Double return - widen then box`() {
            // Scenario: int literal returned as Double?
            // Requires: widen int to double, then box to Double
            val ast = buildTypedAst {
                val intType = intType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        returnStmt(intLiteral(42, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }
        
        @Test
        fun `int literal to nullable Long return - widen then box`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longNullableType = longNullableType()
                
                function(
                    name = "testFunction",
                    returnType = longNullableType,
                    body = listOf(
                        returnStmt(intLiteral(123, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(123L, result)
        }
        
        @Test
        fun `int literal to nullable Float return - widen then box`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatNullableType = floatNullableType()
                
                function(
                    name = "testFunction",
                    returnType = floatNullableType,
                    body = listOf(
                        returnStmt(intLiteral(77, intType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(77.0f, result)
        }
        
        @Test
        fun `long literal to nullable Double return - widen then box`() {
            val ast = buildTypedAst {
                val longType = longType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        returnStmt(longLiteral(1234567890123L, longType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1234567890123.0, result)
        }
        
        @Test
        fun `float literal to nullable Double return - widen then box`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        returnStmt(floatLiteral(2.718f, floatType))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.718f.toDouble(), result)
        }
        
        @Test
        fun `int variable to nullable Double return - widen then box`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        varDecl("x", intType, intLiteral(99, intType)),
                        returnStmt(identifier("x", intType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(99.0, result)
        }
    }
    
    // ==================== Unboxing + Widening + Boxing Tests ====================
    
    @Nested
    inner class UnboxingWideningBoxing {
        
        @Test
        fun `Integer variable to nullable Double return - unbox, widen, box`() {
            // Scenario: Integer? returned as Double?
            // Requires: unbox to int, widen to double, box to Double
            val ast = buildTypedAst {
                val intType = intType()
                val intNullableType = intNullableType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(42, intType)),
                        returnStmt(identifier("x", intNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }
        
        @Test
        fun `Integer variable to nullable Long return - unbox, widen, box`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullableType = intNullableType()
                val longNullableType = longNullableType()
                
                function(
                    name = "testFunction",
                    returnType = longNullableType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(88, intType)),
                        returnStmt(identifier("x", intNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(88L, result)
        }
        
        @Test
        fun `Long variable to nullable Double return - unbox, widen, box`() {
            val ast = buildTypedAst {
                val longType = longType()
                val longNullableType = longNullableType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        varDecl("x", longNullableType, longLiteral(5555555555L, longType)),
                        returnStmt(identifier("x", longNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(5555555555.0, result)
        }
        
        @Test
        fun `Float variable to nullable Double return - unbox, widen, box`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val floatNullableType = floatNullableType()
                val doubleNullableType = doubleNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleNullableType,
                    body = listOf(
                        varDecl("x", floatNullableType, floatLiteral(1.5f, floatType)),
                        returnStmt(identifier("x", floatNullableType, 3))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(1.5f.toDouble(), result)
        }
    }
    
    // ==================== Function Argument Coercion Tests ====================
    
    @Nested
    inner class FunctionArgumentCoercion {
        
        @Test
        fun `int argument to double parameter`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                
                function(
                    name = "square",
                    returnType = doubleType,
                    parameters = listOf(TypedParameter("x", doubleType)),
                    body = listOf(
                        returnStmt(binaryExpr(
                            identifier("x", doubleType, 2),
                            "*",
                            identifier("x", doubleType, 2),
                            doubleType
                        ))
                    )
                )
                
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "square",
                                overload = "square(builtin.double):builtin.double",
                                arguments = listOf(intLiteral(5, intType)),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(25.0, result)
        }
        
        @Test
        fun `int argument to nullable Double parameter - widen and box`() {
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
                                overload = "identity(builtin.double?):builtin.double?",
                                arguments = listOf(intLiteral(7, intType)),
                                resultTypeIndex = doubleNullableType
                            )
                        )
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(7.0, result)
        }
    }
    
    // ==================== Arithmetic with Mixed Nullable Types ====================
    
    @Nested
    inner class ArithmeticMixedNullable {
        
        @Test
        fun `Integer plus int results in int`() {
            val ast = buildTypedAst {
                val intType = intType()
                val intNullableType = intNullableType()
                
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(10, intType)),
                        returnStmt(binaryExpr(
                            identifier("x", intNullableType, 3),
                            "+",
                            intLiteral(5, intType),
                            intType
                        ))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(15, result)
        }
        
        @Test
        fun `Integer plus Long results in Long`() {
            val ast = buildTypedAst {
                val intType = intType()
                val longType = longType()
                val intNullableType = intNullableType()
                
                function(
                    name = "testFunction",
                    returnType = longType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(10, intType)),
                        returnStmt(binaryExpr(
                            identifier("x", intNullableType, 3),
                            "+",
                            longLiteral(20L, longType),
                            longType
                        ))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(30L, result)
        }
        
        @Test
        fun `Integer plus Double results in Double`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                val intNullableType = intNullableType()
                
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        varDecl("x", intNullableType, intLiteral(10, intType)),
                        returnStmt(binaryExpr(
                            identifier("x", intNullableType, 3),
                            "+",
                            doubleLiteral(2.5, doubleType),
                            doubleType
                        ))
                    )
                )
            }
            
            val result = helper.compileAndInvoke(ast)
            assertEquals(12.5, result)
        }
    }
}
