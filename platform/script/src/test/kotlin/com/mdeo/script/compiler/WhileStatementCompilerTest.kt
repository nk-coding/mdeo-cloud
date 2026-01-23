package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for while statement compilation.
 */
class WhileStatementCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `while loop counts to 5`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
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
    fun `while loop counts to 10`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
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
        assertEquals(10, result)
    }
    
    @Test
    fun `while loop never executes when condition is false`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(10, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                        body = listOf(
                            assignment(identifier("i", intType, 3), intLiteral(999, intType))
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    @Test
    fun `while loop with decrement`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(10, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), ">", intLiteral(0, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "-", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("i", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(0, result)
    }
    
    @Test
    fun `while loop calculates sum 1 to 10`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(1, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<=", intLiteral(10, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(55, result)
    }
    
    @Test
    fun `while loop calculates factorial 5`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("n", intType, intLiteral(5, intType)),
                    varDecl("result", intType, intLiteral(1, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("n", intType, 3), ">", intLiteral(1, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "*", identifier("n", intType, 3), intType)
                            ),
                            assignment(
                                identifier("n", intType, 3),
                                binaryExpr(identifier("n", intType, 3), "-", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(120, result)
    }
    
    @Test
    fun `while loop with long counter`() {
        val ast = buildTypedAst {
            val longType = longType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = longType,
                body = listOf(
                    varDecl("i", longType, longLiteral(0L, longType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", longType, 3), "<", longLiteral(100L, longType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", longType, 3),
                                binaryExpr(identifier("i", longType, 3), "+", longLiteral(1L, longType), longType)
                            )
                        )
                    ),
                    returnStmt(identifier("i", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(100L, result)
    }
    
    @Test
    fun `nested while loops`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // All these variables are declared at scope 3 (function body)
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("j", intType, intLiteral(0, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(3, intType), boolType),
                        body = listOf(
                            // j is at scope 3
                            assignment(identifier("j", intType, 3), intLiteral(0, intType)),
                            whileStmt(
                                // j is at scope 3
                                condition = binaryExpr(identifier("j", intType, 3), "<", intLiteral(4, intType), boolType),
                                body = listOf(
                                    // count is at scope 3
                                    assignment(
                                        identifier("count", intType, 3),
                                        binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                                    ),
                                    // j is at scope 3
                                    assignment(
                                        identifier("j", intType, 3),
                                        binaryExpr(identifier("j", intType, 3), "+", intLiteral(1, intType), intType)
                                    )
                                )
                            ),
                            // i is at scope 3
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("count", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(12, result)
    }
    
    @Test
    fun `three level nested while loops`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(2, intType), boolType),
                        body = listOf(
                            varDecl("j", intType, intLiteral(0, intType)),
                            whileStmt(
                                condition = binaryExpr(identifier("j", intType, 4), "<", intLiteral(2, intType), boolType),
                                body = listOf(
                                    varDecl("k", intType, intLiteral(0, intType)),
                                    whileStmt(
                                        condition = binaryExpr(identifier("k", intType, 5), "<", intLiteral(2, intType), boolType),
                                        body = listOf(
                                            // count is declared at scope 3
                                            assignment(
                                                identifier("count", intType, 3),
                                                binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                                            ),
                                            // k is declared at scope 5
                                            assignment(
                                                identifier("k", intType, 5),
                                                binaryExpr(identifier("k", intType, 5), "+", intLiteral(1, intType), intType)
                                            )
                                        )
                                    ),
                                    // j is declared at scope 4
                                    assignment(
                                        identifier("j", intType, 4),
                                        binaryExpr(identifier("j", intType, 4), "+", intLiteral(1, intType), intType)
                                    )
                                )
                            ),
                            // i is declared at scope 3
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("count", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(8, result)
    }
    
    @Test
    fun `while loop with double counter`() {
        // Use 0.25 increments to avoid floating-point precision issues
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val boolType = booleanType()
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("d", doubleType, doubleLiteral(0.0, doubleType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("d", doubleType, 3), "<", doubleLiteral(1.0, doubleType), boolType),
                        body = listOf(
                            assignment(
                                identifier("d", doubleType, 3),
                                binaryExpr(identifier("d", doubleType, 3), "+", doubleLiteral(0.25, doubleType), doubleType)
                            ),
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("count", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(4, result)  // 0.0, 0.25, 0.5, 0.75 < 1.0, so 4 iterations
    }
    
    @Test
    fun `while loop with equality check`() {
        // Using < instead of != since != is not implemented yet
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
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
    fun `while loop calculates power of 2`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("result", intType, intLiteral(1, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "*", intLiteral(2, intType), intType)
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1024, result)
    }
    
    @Test
    fun `while loop with multiple variables in body`() {
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    varDecl("product", intType, intLiteral(1, intType)),
                    whileStmt(
                        condition = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 3), intType)
                            ),
                            assignment(
                                identifier("product", intType, 3),
                                binaryExpr(identifier("product", intType, 3), "*", identifier("i", intType, 3), intType)
                            )
                        )
                    ),
                    returnStmt(binaryExpr(identifier("sum", intType, 3), "+", identifier("product", intType, 3), intType))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(135, result)
    }
    
    @Test
    fun `while loop with AND condition - both conditions checked`() {
        // Loop continues while i < 10 AND i != 5
        // Should exit when i becomes 5
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(
                            binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                            "&&",
                            binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                            boolType
                        ),
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
    fun `while loop with OR condition`() {
        // Loop continues while i < 3 OR i == 5
        // With proper short-circuit, when i < 3, the OR succeeds without checking i == 5
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(
                            binaryExpr(identifier("i", intType, 3), "<", intLiteral(3, intType), boolType),
                            "||",
                            binaryExpr(identifier("count", intType, 3), "<", intLiteral(5, intType), boolType),
                            boolType
                        ),
                        body = listOf(
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(identifier("i", intType, 3), "+", intLiteral(1, intType), intType)
                            ),
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("count", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `while loop with complex nested AND OR condition`() {
        // (i < 10 && i > 0) || i == 0
        // When i=0: i<10 is true, i>0 is false, so left AND is false
        //           i==0 is true, so OR is true
        // When i=5: i<10 is true, i>0 is true, so left AND is true
        //           OR short-circuits to true
        // When i=10: i<10 is false, so left AND is false (short-circuit)
        //            i==0 is false, so OR is false
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(
                            binaryExpr(
                                binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                                "&&",
                                binaryExpr(identifier("i", intType, 3), ">", intLiteral(0, intType), boolType),
                                boolType
                            ),
                            "||",
                            binaryExpr(identifier("i", intType, 3), "<=", intLiteral(0, intType), boolType),
                            boolType
                        ),
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
        assertEquals(10, result)
    }
    
    @Test
    fun `while loop with ternary in condition`() {
        // Condition: (flag ? i < 5 : i < 10)
        // With flag = true, loop until i >= 5
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("flag", boolType, booleanLiteral(true, boolType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = ternaryExpr(
                            condition = identifier("flag", boolType, 3),
                            trueExpr = binaryExpr(identifier("i", intType, 3), "<", intLiteral(5, intType), boolType),
                            falseExpr = binaryExpr(identifier("i", intType, 3), "<", intLiteral(10, intType), boolType),
                            resultTypeIndex = boolType
                        ),
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
}
