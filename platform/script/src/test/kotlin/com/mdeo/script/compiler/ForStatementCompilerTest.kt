package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for for statement compilation.
 */
class ForStatementCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `for loop iterates over list summing integers`() {
        // for (item in list) { sum = sum + item }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4, 5))
        assertEquals(15, result)
    }
    
    @Test
    fun `for loop iterates over list counting elements`() {
        // for (item in list) { count = count + 1 }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("count", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
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
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(10, 20, 30))
        assertEquals(3, result)
    }
    
    @Test
    fun `for loop with break exits early`() {
        // for (item in list) { if (item > 3) break; sum = sum + item }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(identifier("item", intType, 4), ">", intLiteral(3, intType), boolType),
                                thenBlock = listOf(breakStmt())
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // [1, 2, 3, 4, 5] - break when item > 3, so sum = 1 + 2 + 3 = 6
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4, 5))
        assertEquals(6, result)
    }
    
    @Test
    fun `for loop with continue skips elements`() {
        // for (item in list) { if (item == 2) continue; sum = sum + item }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(identifier("item", intType, 4), "==", intLiteral(2, intType), boolType),
                                thenBlock = listOf(continueStmt())
                            ),
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // [1, 2, 3, 4] - skip when item == 2, so sum = 1 + 3 + 4 = 8
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4))
        assertEquals(8, result)
    }
    
    @Test
    fun `nested for loops`() {
        // for (i in list1) { for (j in list2) { sum = sum + i + j } }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("list1", listType),
                    TypedParameter("list2", listType)
                ),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "i",
                        variableType = intType,
                        iterable = identifier("list1", listType, 2),
                        body = listOf(
                            forStmt(
                                variableName = "j",
                                variableType = intType,
                                iterable = identifier("list2", listType, 2),
                                body = listOf(
                                    assignment(
                                        identifier("sum", intType, 3),
                                        binaryExpr(
                                            identifier("sum", intType, 3),
                                            "+",
                                            binaryExpr(identifier("i", intType, 4), "+", identifier("j", intType, 6), intType),
                                            intType
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // [1, 2] x [10, 20] = (1+10) + (1+20) + (2+10) + (2+20) = 11 + 21 + 12 + 22 = 66
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2), listOf(10, 20))
        assertEquals(66, result)
    }
    
    @Test
    fun `for loop modifying outer scope variable`() {
        // result = 1; for (item in list) { result = result * item }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("result", intType, intLiteral(1, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(identifier("result", intType, 3), "*", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        // [2, 3, 4] -> 1 * 2 * 3 * 4 = 24
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(2, 3, 4))
        assertEquals(24, result)
    }
    
    @Test
    fun `for loop with empty list`() {
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // Empty list - loop never executes
        val result = helper.compileAndInvoke(ast, "testFunction", emptyList<Int>())
        assertEquals(0, result)
    }
    
    @Test
    fun `for loop with long values`() {
        val ast = buildTypedAst {
            val longType = longType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = longType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", longType, longLiteral(0L, longType)),
                    forStmt(
                        variableName = "item",
                        variableType = longType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", longType, 3),
                                binaryExpr(identifier("sum", longType, 3), "+", identifier("item", longType, 4), longType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", longType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1000000000L, 2000000000L, 3000000000L))
        assertEquals(6000000000L, result)
    }
    
    @Test
    fun `for loop with string concatenation`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = stringType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("result", stringType, stringLiteral("", stringType)),
                    forStmt(
                        variableName = "item",
                        variableType = stringType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("result", stringType, 3),
                                binaryExpr(identifier("result", stringType, 3), "+", identifier("item", stringType, 4), stringType)
                            )
                        )
                    ),
                    returnStmt(identifier("result", stringType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf("a", "b", "c"))
        assertEquals("abc", result)
    }
    
    @Test
    fun `nested for loops with break in inner loop`() {
        // for (i in list1) { for (j in list2) { if (j > 1) break; sum = sum + i*j } }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("list1", listType),
                    TypedParameter("list2", listType)
                ),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "i",
                        variableType = intType,
                        iterable = identifier("list1", listType, 2),
                        body = listOf(
                            forStmt(
                                variableName = "j",
                                variableType = intType,
                                iterable = identifier("list2", listType, 2),
                                body = listOf(
                                    ifStmt(
                                        condition = binaryExpr(identifier("j", intType, 6), ">", intLiteral(1, intType), boolType),
                                        thenBlock = listOf(breakStmt())
                                    ),
                                    assignment(
                                        identifier("sum", intType, 3),
                                        binaryExpr(
                                            identifier("sum", intType, 3),
                                            "+",
                                            binaryExpr(identifier("i", intType, 4), "*", identifier("j", intType, 6), intType),
                                            intType
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // list1 = [1, 2], list2 = [1, 2, 3]
        // i=1: j=1 -> sum += 1*1 = 1, j=2 -> break
        // i=2: j=1 -> sum += 2*1 = 3, j=2 -> break
        // Total: 1 + 2 = 3
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2), listOf(1, 2, 3))
        assertEquals(3, result)
    }
    
    @Test
    fun `nested for loops with continue in inner loop`() {
        // for (i in list1) { for (j in list2) { if (j == 2) continue; sum = sum + i } }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("list1", listType),
                    TypedParameter("list2", listType)
                ),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "i",
                        variableType = intType,
                        iterable = identifier("list1", listType, 2),
                        body = listOf(
                            forStmt(
                                variableName = "j",
                                variableType = intType,
                                iterable = identifier("list2", listType, 2),
                                body = listOf(
                                    ifStmt(
                                        condition = binaryExpr(identifier("j", intType, 6), "==", intLiteral(2, intType), boolType),
                                        thenBlock = listOf(continueStmt())
                                    ),
                                    assignment(
                                        identifier("sum", intType, 3),
                                        binaryExpr(identifier("sum", intType, 3), "+", identifier("i", intType, 4), intType)
                                    )
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // list1 = [1, 2], list2 = [1, 2, 3]
        // i=1: j=1 -> sum += 1, j=2 -> continue, j=3 -> sum += 1 (sum = 2)
        // i=2: j=1 -> sum += 2, j=2 -> continue, j=3 -> sum += 2 (sum = 6)
        // Total: 6
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2), listOf(1, 2, 3))
        assertEquals(6, result)
    }
    
    @Test
    fun `for loop with multiple statements in body`() {
        // for (item in list) { sum = sum + item; product = product * item; count = count + 1 }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    varDecl("product", intType, intLiteral(1, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("item", intType, 4), intType)
                            ),
                            assignment(
                                identifier("product", intType, 3),
                                binaryExpr(identifier("product", intType, 3), "*", identifier("item", intType, 4), intType)
                            ),
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(identifier("count", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    // Return sum + product + count
                    returnStmt(
                        binaryExpr(
                            binaryExpr(identifier("sum", intType, 3), "+", identifier("product", intType, 3), intType),
                            "+",
                            identifier("count", intType, 3),
                            intType
                        )
                    )
                )
            )
        }
        
        // [2, 3, 4] -> sum = 9, product = 24, count = 3 -> 9 + 24 + 3 = 36
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(2, 3, 4))
        assertEquals(36, result)
    }
    
    @Test
    fun `for loop finding max value`() {
        // max = first element; for (item in list) { if (item > max) max = item }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("max", intType, intLiteral(-999999, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(identifier("item", intType, 4), ">", identifier("max", intType, 3), boolType),
                                thenBlock = listOf(
                                    assignment(identifier("max", intType, 3), identifier("item", intType, 4))
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("max", intType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(5, 2, 8, 1, 9, 3))
        assertEquals(9, result)
    }
    
    @Test
    fun `for loop with double values`() {
        val ast = buildTypedAst {
            val doubleType = doubleType()
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = doubleType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("sum", doubleType, doubleLiteral(0.0, doubleType)),
                    forStmt(
                        variableName = "item",
                        variableType = doubleType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", doubleType, 3),
                                binaryExpr(identifier("sum", doubleType, 3), "+", identifier("item", doubleType, 4), doubleType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", doubleType, 3))
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1.5, 2.5, 3.0))
        assertEquals(7.0, result)
    }
    
    @Test
    fun `sequential for loops with same variable name`() {
        // Two for loops in sequence using the same variable name "item"
        // for (item in list1) { sum = sum + item }
        // for (item in list2) { sum = sum + item * 10 }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("list1", listType),
                    TypedParameter("list2", listType)
                ),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    // First for loop - scope 4 for item
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list1", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    // Second for loop - also scope 4 for item (sibling scope)
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list2", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(
                                    identifier("sum", intType, 3),
                                    "+",
                                    binaryExpr(identifier("item", intType, 4), "*", intLiteral(10, intType), intType),
                                    intType
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // list1 = [1, 2, 3] -> sum = 1 + 2 + 3 = 6
        // list2 = [4, 5] -> sum = 6 + 40 + 50 = 96
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3), listOf(4, 5))
        assertEquals(96, result)
    }
    
    @Test
    fun `nested for loops with same variable name - shadowing`() {
        // for (item in list1) { for (item in list2) { innerSum += item }; outerSum += item }
        // The inner "item" should shadow the outer "item"
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("list1", listType),
                    TypedParameter("list2", listType)
                ),
                body = listOf(
                    varDecl("innerSum", intType, intLiteral(0, intType)),
                    varDecl("outerSum", intType, intLiteral(0, intType)),
                    // Outer for loop - item at scope 4
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list1", listType, 2),
                        body = listOf(
                            // Inner for loop - item at scope 6 (shadows outer)
                            forStmt(
                                variableName = "item",
                                variableType = intType,
                                iterable = identifier("list2", listType, 2),
                                body = listOf(
                                    assignment(
                                        identifier("innerSum", intType, 3),
                                        binaryExpr(identifier("innerSum", intType, 3), "+", identifier("item", intType, 6), intType)
                                    )
                                )
                            ),
                            // After inner loop, should access outer item at scope 4
                            assignment(
                                identifier("outerSum", intType, 3),
                                binaryExpr(identifier("outerSum", intType, 3), "+", identifier("item", intType, 4), intType)
                            )
                        )
                    ),
                    // Return innerSum * 1000 + outerSum to verify both values
                    returnStmt(
                        binaryExpr(
                            binaryExpr(identifier("innerSum", intType, 3), "*", intLiteral(1000, intType), intType),
                            "+",
                            identifier("outerSum", intType, 3),
                            intType
                        )
                    )
                )
            )
        }
        
        // list1 = [100, 200], list2 = [1, 2, 3]
        // Outer loop iteration 1 (item=100):
        //   Inner loop: innerSum = 1 + 2 + 3 = 6
        //   After inner: outerSum = 100
        // Outer loop iteration 2 (item=200):
        //   Inner loop: innerSum = 6 + 1 + 2 + 3 = 12
        //   After inner: outerSum = 100 + 200 = 300
        // Result: 12 * 1000 + 300 = 12300
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(100, 200), listOf(1, 2, 3))
        assertEquals(12300, result)
    }
}
