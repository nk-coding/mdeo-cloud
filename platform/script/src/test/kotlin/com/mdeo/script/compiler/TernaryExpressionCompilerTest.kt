package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for ternary (conditional) expression compilation.
 * 
 * These tests verify:
 * - Basic ternary expression behavior
 * - Type handling (int, long, float, double, boolean, string)
 * - Nested ternary expressions
 * - Ternary with complex conditions
 */
class TernaryExpressionCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `ternary returns true expression when condition is true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
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
    fun `ternary returns false expression when condition is false`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(false, boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
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
    fun `ternary with long type`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = longLiteral(100L, longType),
                            falseExpr = longLiteral(200L, longType),
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
    fun `ternary with float type`() {
        val ast = buildTypedAst {
            val floatType = floatType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = floatType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(false, boolType),
                            trueExpr = floatLiteral(1.5f, floatType),
                            falseExpr = floatLiteral(2.5f, floatType),
                            resultTypeIndex = floatType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.5f, result)
    }
    
    @Test
    fun `ternary with double type`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = doubleLiteral(3.14159, doubleType),
                            falseExpr = doubleLiteral(2.71828, doubleType),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14159, result)
    }
    
    @Test
    fun `ternary with boolean type`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = booleanLiteral(false, boolType),
                            falseExpr = booleanLiteral(true, boolType),
                            resultTypeIndex = boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(false, result)
    }
    
    @Test
    fun `ternary with string type`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = stringLiteral("yes", stringType),
                            falseExpr = stringLiteral("no", stringType),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("yes", result)
    }
    
    @Test
    fun `ternary with comparison condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(10, intType)),
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(identifier("x", intType, 3), ">", intLiteral(5, intType), boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
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
    fun `ternary with AND condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(booleanLiteral(true, boolType), "&&", booleanLiteral(true, boolType), boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
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
    fun `ternary with OR condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(booleanLiteral(false, boolType), "||", booleanLiteral(true, boolType), boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
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
    fun `ternary with NOT condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = unaryExpr("!", booleanLiteral(false, boolType), boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
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
    fun `nested ternary - both conditions true`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = intLiteral(1, intType),
                                falseExpr = intLiteral(2, intType),
                                resultTypeIndex = intType
                            ),
                            falseExpr = intLiteral(3, intType),
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
    fun `nested ternary - outer true inner false`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = ternaryExpr(
                                condition = booleanLiteral(false, boolType),
                                trueExpr = intLiteral(1, intType),
                                falseExpr = intLiteral(2, intType),
                                resultTypeIndex = intType
                            ),
                            falseExpr = intLiteral(3, intType),
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
    fun `nested ternary - outer false`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(false, boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = intLiteral(2, intType),
                                falseExpr = intLiteral(3, intType),
                                resultTypeIndex = intType
                            ),
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
    fun `ternary in arithmetic expression`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            intLiteral(10, intType),
                            "+",
                            ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = intLiteral(5, intType),
                                falseExpr = intLiteral(0, intType),
                                resultTypeIndex = intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(15, result)
    }
    
    @Test
    fun `ternary with variable in condition`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("flag", boolType, booleanLiteral(true, boolType)),
                    returnStmt(
                        ternaryExpr(
                            condition = identifier("flag", boolType, 3),
                            trueExpr = intLiteral(42, intType),
                            falseExpr = intLiteral(0, intType),
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
    fun `ternary with variables in expressions`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("a", intType, intLiteral(10, intType)),
                    varDecl("b", intType, intLiteral(20, intType)),
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(identifier("a", intType, 3), "<", identifier("b", intType, 3), boolType),
                            trueExpr = identifier("a", intType, 3),
                            falseExpr = identifier("b", intType, 3),
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
    fun `ternary used in variable assignment`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, 
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = intLiteral(100, intType),
                            falseExpr = intLiteral(0, intType),
                            resultTypeIndex = intType
                        )
                    ),
                    returnStmt(identifier("x", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100, result)
    }
    
    @Test
    fun `ternary in while condition context`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("limit", intType, 
                        ternaryExpr(
                            condition = booleanLiteral(true, boolType),
                            trueExpr = intLiteral(5, intType),
                            falseExpr = intLiteral(10, intType),
                            resultTypeIndex = intType
                        )
                    ),
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
    fun `deeply nested ternary expressions`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = booleanLiteral(false, boolType),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = ternaryExpr(
                                condition = booleanLiteral(false, boolType),
                                trueExpr = intLiteral(2, intType),
                                falseExpr = ternaryExpr(
                                    condition = booleanLiteral(false, boolType),
                                    trueExpr = intLiteral(3, intType),
                                    falseExpr = ternaryExpr(
                                        condition = booleanLiteral(true, boolType),
                                        trueExpr = intLiteral(4, intType),
                                        falseExpr = intLiteral(5, intType),
                                        resultTypeIndex = intType
                                    ),
                                    resultTypeIndex = intType
                                ),
                                resultTypeIndex = intType
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(4, result)
    }
    
    @Test
    fun `ternary with long comparison`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("x", longType, longLiteral(1000000000L, longType)),
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(identifier("x", longType, 3), ">", longLiteral(500000000L, longType), boolType),
                            trueExpr = longLiteral(1L, longType),
                            falseExpr = longLiteral(0L, longType),
                            resultTypeIndex = longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1L, result)
    }
    
    @Test
    fun `ternary as comparison operand`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = intLiteral(10, intType),
                                falseExpr = intLiteral(0, intType),
                                resultTypeIndex = intType
                            ),
                            ">",
                            intLiteral(5, intType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `ternary with double types - stack balance check`() {
        // Doubles are 2 slots on the stack - verify proper handling
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = doubleLiteral(1.5, doubleType),
                                falseExpr = doubleLiteral(2.5, doubleType),
                                resultTypeIndex = doubleType
                            ),
                            "+",
                            ternaryExpr(
                                condition = booleanLiteral(false, boolType),
                                trueExpr = doubleLiteral(10.0, doubleType),
                                falseExpr = doubleLiteral(20.0, doubleType),
                                resultTypeIndex = doubleType
                            ),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(21.5, result) // 1.5 + 20.0
    }
    
    @Test
    fun `ternary with long types - stack balance check`() {
        // Longs are 2 slots on the stack - verify proper handling
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = longLiteral(1000000000L, longType),
                                falseExpr = longLiteral(2000000000L, longType),
                                resultTypeIndex = longType
                            ),
                            "+",
                            ternaryExpr(
                                condition = booleanLiteral(false, boolType),
                                trueExpr = longLiteral(100L, longType),
                                falseExpr = longLiteral(200L, longType),
                                resultTypeIndex = longType
                            ),
                            longType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1000000200L, result) // 1000000000 + 200
    }
    
    @Test
    fun `ternary with short-circuit AND in condition`() {
        // Short-circuit AND: if first is false, second should NOT be evaluated
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(
                                booleanLiteral(false, boolType),
                                "&&",
                                binaryExpr(
                                    binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                    ">",
                                    intLiteral(0, intType),
                                    boolType
                                ),
                                boolType
                            ),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result) // Condition is false (short-circuited), so false branch
    }
    
    @Test
    fun `ternary with short-circuit OR in condition`() {
        // Short-circuit OR: if first is true, second should NOT be evaluated
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(
                                booleanLiteral(true, boolType),
                                "||",
                                binaryExpr(
                                    binaryExpr(intLiteral(1, intType), "/", intLiteral(0, intType), intType),
                                    ">",
                                    intLiteral(0, intType),
                                    boolType
                                ),
                                boolType
                            ),
                            trueExpr = intLiteral(1, intType),
                            falseExpr = intLiteral(2, intType),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result) // Condition is true (short-circuited), so true branch
    }
    
    @Test
    fun `ternary nested in both branches with different types`() {
        // Tests complex nesting where both branches have ternary expressions
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("x", intType, intLiteral(5, intType)),
                    returnStmt(
                        ternaryExpr(
                            condition = binaryExpr(identifier("x", intType, 3), ">", intLiteral(10, intType), boolType),
                            trueExpr = ternaryExpr(
                                condition = binaryExpr(identifier("x", intType, 3), ">", intLiteral(15, intType), boolType),
                                trueExpr = intLiteral(100, intType),
                                falseExpr = intLiteral(50, intType),
                                resultTypeIndex = intType
                            ),
                            falseExpr = ternaryExpr(
                                condition = binaryExpr(identifier("x", intType, 3), ">", intLiteral(3, intType), boolType),
                                trueExpr = intLiteral(25, intType),
                                falseExpr = intLiteral(10, intType),
                                resultTypeIndex = intType
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(25, result) // x=5: not > 10, so false branch; 5 > 3, so 25
    }
    
    @Test
    fun `multiple ternary in single expression`() {
        // Three ternary expressions used together
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                ternaryExpr(
                                    condition = booleanLiteral(true, boolType),
                                    trueExpr = intLiteral(1, intType),
                                    falseExpr = intLiteral(0, intType),
                                    resultTypeIndex = intType
                                ),
                                "+",
                                ternaryExpr(
                                    condition = booleanLiteral(false, boolType),
                                    trueExpr = intLiteral(10, intType),
                                    falseExpr = intLiteral(20, intType),
                                    resultTypeIndex = intType
                                ),
                                intType
                            ),
                            "+",
                            ternaryExpr(
                                condition = booleanLiteral(true, boolType),
                                trueExpr = intLiteral(100, intType),
                                falseExpr = intLiteral(200, intType),
                                resultTypeIndex = intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(121, result) // 1 + 20 + 100
    }
}
