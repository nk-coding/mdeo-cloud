package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.VoidType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Integration tests that exercise the compiler with realistic, complex scenarios
 * combining multiple features together.
 * 
 * These tests validate that different compiler components work correctly
 * in combination, catching integration bugs that unit tests might miss.
 */
class IntegrationTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== 1. Recursive Function Calls ====================
    
    @Test
    fun `recursive factorial function`() {
        // fun factorial(n: Int): Int {
        //     if (n <= 1) return 1
        //     return n * factorial(n - 1)
        // }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "factorial",
                returnType = intType,
                parameters = listOf(TypedParameter("n", intType)),
                body = listOf(
                    ifStmt(
                        condition = binaryExpr(
                            identifier("n", intType, 2),
                            "<=",
                            intLiteral(1, intType),
                            boolType
                        ),
                        thenBlock = listOf(returnStmt(intLiteral(1, intType)))
                    ),
                    returnStmt(
                        binaryExpr(
                            identifier("n", intType, 2),
                            "*",
                            functionCall(
                                name = "factorial",
                                overload = "",
                                arguments = listOf(
                                    binaryExpr(
                                        identifier("n", intType, 2),
                                        "-",
                                        intLiteral(1, intType),
                                        intType
                                    )
                                ),
                                resultTypeIndex = intType
                            ),
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
                            name = "factorial",
                            overload = "",
                            arguments = listOf(intLiteral(5, intType)),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(120, result)  // 5! = 120
    }
    
    @Test
    fun `recursive fibonacci function`() {
        // fun fib(n: Int): Int {
        //     if (n <= 1) return n
        //     return fib(n - 1) + fib(n - 2)
        // }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "fib",
                returnType = intType,
                parameters = listOf(TypedParameter("n", intType)),
                body = listOf(
                    ifStmt(
                        condition = binaryExpr(
                            identifier("n", intType, 2),
                            "<=",
                            intLiteral(1, intType),
                            boolType
                        ),
                        thenBlock = listOf(returnStmt(identifier("n", intType, 2)))
                    ),
                    returnStmt(
                        binaryExpr(
                            functionCall(
                                name = "fib",
                                overload = "",
                                arguments = listOf(
                                    binaryExpr(
                                        identifier("n", intType, 2),
                                        "-",
                                        intLiteral(1, intType),
                                        intType
                                    )
                                ),
                                resultTypeIndex = intType
                            ),
                            "+",
                            functionCall(
                                name = "fib",
                                overload = "",
                                arguments = listOf(
                                    binaryExpr(
                                        identifier("n", intType, 2),
                                        "-",
                                        intLiteral(2, intType),
                                        intType
                                    )
                                ),
                                resultTypeIndex = intType
                            ),
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
                            name = "fib",
                            overload = "",
                            arguments = listOf(intLiteral(10, intType)),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(55, result)  // fib(10) = 55
    }
    
    // ==================== 2. Higher-Order Functions (Lambda Composition) ====================
    
    @Test
    fun `lambda calls another lambda - chained increments`() {
        // val increment = (x) => x + 1
        // Call increment twice manually
        // increment(increment(5)) -> 7
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
                    varDecl("increment", lambdaTypeIdx, lambdaExpr(
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
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Nested call: increment(increment(5))
                    returnStmt(
                        expressionCall(
                            expression = identifier("increment", lambdaTypeIdx, 3),
                            arguments = listOf(
                                expressionCall(
                                    expression = identifier("increment", lambdaTypeIdx, 3),
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
        assertEquals(7, result)  // 5 + 1 + 1 = 7
    }
    
    @Test
    fun `lambda composition - sequential transformations`() {
        // val doubler = (x) => x * 2
        // val addThree = (x) => x + 3
        // val result = doubler(addThree(5))  -> (5 + 3) * 2 = 16
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
                    varDecl("doubler", lambdaTypeIdx, lambdaExpr(
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
                    varDecl("addThree", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", intType, 4),
                                    "+",
                                    intLiteral(3, intType),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    // Nested call: doubler(addThree(5))
                    returnStmt(
                        expressionCall(
                            expression = identifier("doubler", lambdaTypeIdx, 3),
                            arguments = listOf(
                                expressionCall(
                                    expression = identifier("addThree", lambdaTypeIdx, 3),
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
        assertEquals(16, result)  // (5 + 3) * 2 = 16
    }
    
    // ==================== 3. Complex Control Flow ====================
    
    @Test
    fun `nested if-else with for loops and break`() {
        // Process list, skip negatives, break on zero, accumulate positives
        // result = 0
        // for item in list {
        //     if (item < 0) { continue }
        //     else if (item == 0) { break }
        //     else { result = result + item }
        // }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("item", intType, 4),
                                    "<",
                                    intLiteral(0, intType),
                                    boolType
                                ),
                                thenBlock = listOf(continueStmt()),
                                elseIfs = listOf(
                                    elseIfClause(
                                        condition = binaryExpr(
                                            identifier("item", intType, 4),
                                            "==",
                                            intLiteral(0, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(breakStmt())
                                    )
                                ),
                                elseBlock = listOf(
                                    assignment(
                                        identifier("result", intType, 3),
                                        binaryExpr(
                                            identifier("result", intType, 3),
                                            "+",
                                            identifier("item", intType, 4),
                                            intType
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        // List: [1, -2, 3, -4, 5, 0, 7, 8]
        // Should process: 1 (add), -2 (skip), 3 (add), -4 (skip), 5 (add), 0 (break)
        // Result: 1 + 3 + 5 = 9
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, -2, 3, -4, 5, 0, 7, 8))
        assertEquals(9, result)
    }
    
    @Test
    fun `nested if statements with multiple conditions`() {
        // Categorize a number and return a score
        // if (n > 100) {
        //     if (n > 200) return 4
        //     else return 3
        // } else if (n > 50) {
        //     return 2
        // } else if (n > 0) {
        //     return 1
        // } else {
        //     return 0
        // }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "categorize",
                returnType = intType,
                parameters = listOf(TypedParameter("n", intType)),
                body = listOf(
                    ifStmt(
                        condition = binaryExpr(
                            identifier("n", intType, 2),
                            ">",
                            intLiteral(100, intType),
                            boolType
                        ),
                        thenBlock = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("n", intType, 2),
                                    ">",
                                    intLiteral(200, intType),
                                    boolType
                                ),
                                thenBlock = listOf(returnStmt(intLiteral(4, intType))),
                                elseBlock = listOf(returnStmt(intLiteral(3, intType)))
                            )
                        ),
                        elseIfs = listOf(
                            elseIfClause(
                                condition = binaryExpr(
                                    identifier("n", intType, 2),
                                    ">",
                                    intLiteral(50, intType),
                                    boolType
                                ),
                                thenBlock = listOf(returnStmt(intLiteral(2, intType)))
                            ),
                            elseIfClause(
                                condition = binaryExpr(
                                    identifier("n", intType, 2),
                                    ">",
                                    intLiteral(0, intType),
                                    boolType
                                ),
                                thenBlock = listOf(returnStmt(intLiteral(1, intType)))
                            )
                        ),
                        elseBlock = listOf(returnStmt(intLiteral(0, intType)))
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    // Test multiple inputs and sum results
                    varDecl("sum", intType, intLiteral(0, intType)),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(
                            identifier("sum", intType, 3),
                            "+",
                            functionCall("categorize", "", 
                                listOf(intLiteral(250, intType)), intType),
                            intType
                        )
                    ),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(
                            identifier("sum", intType, 3),
                            "+",
                            functionCall("categorize", "",
                                listOf(intLiteral(150, intType)), intType),
                            intType
                        )
                    ),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(
                            identifier("sum", intType, 3),
                            "+",
                            functionCall("categorize", "",
                                listOf(intLiteral(75, intType)), intType),
                            intType
                        )
                    ),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(
                            identifier("sum", intType, 3),
                            "+",
                            functionCall("categorize", "",
                                listOf(intLiteral(25, intType)), intType),
                            intType
                        )
                    ),
                    assignment(
                        identifier("sum", intType, 3),
                        binaryExpr(
                            identifier("sum", intType, 3),
                            "+",
                            functionCall("categorize", "",
                                listOf(intLiteral(-5, intType)), intType),
                            intType
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // 250 -> 4, 150 -> 3, 75 -> 2, 25 -> 1, -5 -> 0
        // Sum = 4 + 3 + 2 + 1 + 0 = 10
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    // ==================== 4. Variable Capture Chains ====================
    
    @Test
    fun `multiple lambdas capturing and modifying the same variable`() {
        // var counter = 0
        // val addOne = () => { counter = counter + 1; return counter }
        // val addTen = () => { counter = counter + 10; return counter }
        // addOne() + addTen() + addOne() -> 1 + 11 + 12 = 24
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("counter", intType, intLiteral(0, intType)),
                    varDecl("addOne", lambdaTypeIdx, lambdaExpr(
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
                    varDecl("addTen", lambdaTypeIdx, lambdaExpr(
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
                            ),
                            returnStmt(identifier("counter", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                expressionCall(
                                    expression = identifier("addOne", lambdaTypeIdx, 3),
                                    arguments = emptyList(),
                                    resultTypeIndex = intType
                                ),
                                "+",
                                expressionCall(
                                    expression = identifier("addTen", lambdaTypeIdx, 3),
                                    arguments = emptyList(),
                                    resultTypeIndex = intType
                                ),
                                intType
                            ),
                            "+",
                            expressionCall(
                                expression = identifier("addOne", lambdaTypeIdx, 3),
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(24, result)  // 1 + 11 + 12 = 24
    }
    
    // ==================== 5. Type Coercion Through Call Chain ====================
    
    @Test
    fun `int value passed through functions with different parameter types`() {
        // Test type coercion at function call site
        // fun addLong(x: Long): Long = x + 1L
        // fun addDouble(x: Double): Double = x + 0.5
        // result = addDouble(addLong(5))  -> 5 coerced to long, +1 = 6, coerced to double, +0.5 = 6.5
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val doubleType = doubleType()
            
            function(
                name = "addLong",
                returnType = longType,
                parameters = listOf(TypedParameter("x", longType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", longType, 2),
                            "+",
                            longLiteral(1L, longType),
                            longType
                        )
                    )
                )
            )
            
            function(
                name = "addDouble",
                returnType = doubleType,
                parameters = listOf(TypedParameter("x", doubleType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", doubleType, 2),
                            "+",
                            doubleLiteral(0.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "addDouble",
                            overload = "",
                            arguments = listOf(
                                functionCall(
                                    name = "addLong",
                                    overload = "",
                                    // int 5 coerced to long at call site
                                    arguments = listOf(intLiteral(5, intType)),
                                    resultTypeIndex = longType
                                )
                            ),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6.5, result)
    }
    
    // ==================== 6. Collection Processing with Lambdas ====================
    
    @Test
    fun `for loop with filter-like logic using captured threshold`() {
        // Filter elements > threshold and sum them
        // var threshold = 5
        // var sum = 0
        // for item in list { if (item > threshold) { sum = sum + item } }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("threshold", intType, intLiteral(5, intType)),
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    identifier("item", intType, 4),
                                    ">",
                                    identifier("threshold", intType, 3),
                                    boolType
                                ),
                                thenBlock = listOf(
                                    assignment(
                                        identifier("sum", intType, 3),
                                        binaryExpr(
                                            identifier("sum", intType, 3),
                                            "+",
                                            identifier("item", intType, 4),
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
        
        // List: [1, 3, 7, 2, 9, 4, 6]
        // Filter > 5: [7, 9, 6]
        // Sum: 7 + 9 + 6 = 22
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 3, 7, 2, 9, 4, 6))
        assertEquals(22, result)
    }
    
    // ==================== 7. String Manipulation Pipeline ====================
    
    @Test
    fun `chained string method calls`() {
        // "  hello world  ".trim().toUpperCase().length()
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = memberCall(
                                expression = memberCall(
                                    expression = stringLiteral("  hello world  ", stringType),
                                    member = "trim",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = stringType
                                ),
                                member = "toUpperCase",
                                overload = "",
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
        assertEquals(11, result)  // "hello world".length()
    }
    
    @Test
    fun `string manipulation with conditional processing`() {
        // if (input.length() > 5) {
        //     return input.toUpperCase()
        // } else {
        //     return input.toLowerCase()
        // }
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "processString",
                returnType = stringType,
                parameters = listOf(TypedParameter("input", stringType)),
                body = listOf(
                    ifStmt(
                        condition = binaryExpr(
                            memberCall(
                                expression = identifier("input", stringType, 2),
                                member = "length",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            ),
                            ">",
                            intLiteral(5, intType),
                            boolType
                        ),
                        thenBlock = listOf(
                            returnStmt(
                                memberCall(
                                    expression = identifier("input", stringType, 2),
                                    member = "toUpperCase",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = stringType
                                )
                            )
                        ),
                        elseBlock = listOf(
                            returnStmt(
                                memberCall(
                                    expression = identifier("input", stringType, 2),
                                    member = "toLowerCase",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = stringType
                                )
                            )
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        // Concatenate results of both paths
                        binaryExpr(
                            functionCall(
                                name = "processString",
                                overload = "",
                                arguments = listOf(stringLiteral("LongWord", stringType)),
                                resultTypeIndex = stringType
                            ),
                            "+",
                            functionCall(
                                name = "processString",
                                overload = "",
                                arguments = listOf(stringLiteral("Hi", stringType)),
                                resultTypeIndex = stringType
                            ),
                            stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("LONGWORDhi", result)
    }
    
    // ==================== 8. Nested For Loops with Early Exit ====================
    
    @Test
    fun `nested for loops with break in inner loop`() {
        // Process 2D-like data: find first pair where sum > 10
        // for i in list1 {
        //     for j in list2 {
        //         if (i + j > 10) {
        //             result = i * 100 + j
        //             break  // breaks inner loop only
        //         }
        //     }
        // }
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
                    varDecl("result", intType, intLiteral(0, intType)),
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
                                        condition = binaryExpr(
                                            binaryExpr(
                                                identifier("i", intType, 4),
                                                "+",
                                                identifier("j", intType, 6),
                                                intType
                                            ),
                                            ">",
                                            intLiteral(10, intType),
                                            boolType
                                        ),
                                        thenBlock = listOf(
                                            assignment(
                                                identifier("result", intType, 3),
                                                binaryExpr(
                                                    binaryExpr(
                                                        identifier("i", intType, 4),
                                                        "*",
                                                        intLiteral(100, intType),
                                                        intType
                                                    ),
                                                    "+",
                                                    identifier("j", intType, 6),
                                                    intType
                                                )
                                            ),
                                            breakStmt()
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        // list1 = [3, 5, 7], list2 = [2, 8, 4]
        // i=3: j=2 (3+2=5, no), j=8 (3+8=11 > 10, break) -> result = 308
        // i=5: j=2 (5+2=7, no), j=8 (5+8=13 > 10, break) -> result = 508
        // i=7: j=2 (7+2=9, no), j=8 (7+8=15 > 10, break) -> result = 708
        // Final result: 708 (last assignment wins since we break inner but continue outer)
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(3, 5, 7), listOf(2, 8, 4))
        assertEquals(708, result)
    }
    
    // ==================== 9. State Accumulation with Lambdas ====================
    
    @Test
    fun `lambda that modifies multiple captured variables`() {
        // var sum = 0
        // var count = 0
        // var product = 1
        // val accumulate = (x) => { sum = sum + x; count = count + 1; product = product * x }
        // Call accumulate(2), accumulate(3), accumulate(4)
        // Return sum * 1000 + count * 100 + product
        val ast = buildTypedAst {
            val intType = intType()
            val voidType = voidType()
            val lambdaTypeIdx = lambdaType(
                VoidType(),
                "x" to ClassTypeRef("builtin.int", false)
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    varDecl("count", intType, intLiteral(0, intType)),
                    varDecl("product", intType, intLiteral(1, intType)),
                    varDecl("accumulate", lambdaTypeIdx, lambdaExpr(
                        parameters = listOf("x"),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(
                                    identifier("sum", intType, 3),
                                    "+",
                                    identifier("x", intType, 4),
                                    intType
                                )
                            ),
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(
                                    identifier("count", intType, 3),
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            ),
                            assignment(
                                identifier("product", intType, 3),
                                binaryExpr(
                                    identifier("product", intType, 3),
                                    "*",
                                    identifier("x", intType, 4),
                                    intType
                                )
                            )
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    exprStmt(
                        expressionCall(
                            expression = identifier("accumulate", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(2, intType)),
                            resultTypeIndex = voidType
                        )
                    ),
                    exprStmt(
                        expressionCall(
                            expression = identifier("accumulate", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(3, intType)),
                            resultTypeIndex = voidType
                        )
                    ),
                    exprStmt(
                        expressionCall(
                            expression = identifier("accumulate", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(4, intType)),
                            resultTypeIndex = voidType
                        )
                    ),
                    // Return sum * 1000 + count * 100 + product
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    identifier("sum", intType, 3),
                                    "*",
                                    intLiteral(1000, intType),
                                    intType
                                ),
                                "+",
                                binaryExpr(
                                    identifier("count", intType, 3),
                                    "*",
                                    intLiteral(100, intType),
                                    intType
                                ),
                                intType
                            ),
                            "+",
                            identifier("product", intType, 3),
                            intType
                        )
                    )
                )
            )
        }
        
        // sum = 2 + 3 + 4 = 9
        // count = 3
        // product = 2 * 3 * 4 = 24
        // result = 9000 + 300 + 24 = 9324
        val result = helper.compileAndInvoke(ast)
        assertEquals(9324, result)
    }
    
    // ==================== 10. Complex Arithmetic Expressions ====================
    
    @Test
    fun `nested arithmetic with type promotion`() {
        // Expression: (10 + 5L) * 2.0 / (3.5 - 1.5) + 0.5
        // = 15L * 2.0 / 2.0 + 0.5
        // = 30.0 / 2.0 + 0.5
        // = 15.0 + 0.5
        // = 15.5
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val doubleType = doubleType()
            
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    binaryExpr(
                                        intLiteral(10, intType),
                                        "+",
                                        longLiteral(5L, longType),
                                        longType
                                    ),
                                    "*",
                                    doubleLiteral(2.0, doubleType),
                                    doubleType
                                ),
                                "/",
                                binaryExpr(
                                    doubleLiteral(3.5, doubleType),
                                    "-",
                                    doubleLiteral(1.5, doubleType),
                                    doubleType
                                ),
                                doubleType
                            ),
                            "+",
                            doubleLiteral(0.5, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(15.5, result)
    }
    
    @Test
    fun `complex arithmetic with parentheses priority`() {
        // ((a + b) * (c - d)) / ((e + f) * g)
        // a=10, b=2, c=8, d=3, e=1, f=2, g=3
        // = (12 * 5) / (3 * 3) = 60 / 9 = 6 (integer division)
        val ast = buildTypedAst {
            val intType = intType()
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("a", intType, intLiteral(10, intType)),
                    varDecl("b", intType, intLiteral(2, intType)),
                    varDecl("c", intType, intLiteral(8, intType)),
                    varDecl("d", intType, intLiteral(3, intType)),
                    varDecl("e", intType, intLiteral(1, intType)),
                    varDecl("f", intType, intLiteral(2, intType)),
                    varDecl("g", intType, intLiteral(3, intType)),
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    identifier("a", intType, 3),
                                    "+",
                                    identifier("b", intType, 3),
                                    intType
                                ),
                                "*",
                                binaryExpr(
                                    identifier("c", intType, 3),
                                    "-",
                                    identifier("d", intType, 3),
                                    intType
                                ),
                                intType
                            ),
                            "/",
                            binaryExpr(
                                binaryExpr(
                                    identifier("e", intType, 3),
                                    "+",
                                    identifier("f", intType, 3),
                                    intType
                                ),
                                "*",
                                identifier("g", intType, 3),
                                intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)  // 60 / 9 = 6 (integer division)
    }
    
    // ==================== 11. Ternary Expressions with Side Effects ====================
    
    @Test
    fun `ternary where both branches call functions`() {
        // fun inc(x: Int): Int = x + 1
        // fun dec(x: Int): Int = x - 1
        // cond ? inc(5) : dec(5)
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "inc",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(identifier("x", intType, 2), "+", intLiteral(1, intType), intType)
                    )
                )
            )
            
            function(
                name = "dec",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(identifier("x", intType, 2), "-", intLiteral(1, intType), intType)
                    )
                )
            )
            
            function(
                name = "process",
                returnType = intType,
                parameters = listOf(TypedParameter("cond", boolType)),
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            condition = identifier("cond", boolType, 2),
                            trueExpr = functionCall(
                                name = "inc",
                                overload = "",
                                arguments = listOf(intLiteral(5, intType)),
                                resultTypeIndex = intType
                            ),
                            falseExpr = functionCall(
                                name = "dec",
                                overload = "",
                                arguments = listOf(intLiteral(5, intType)),
                                resultTypeIndex = intType
                            ),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            functionCall(
                                name = "process",
                                overload = "",
                                arguments = listOf(booleanLiteral(true, boolType)),
                                resultTypeIndex = intType
                            ),
                            "+",
                            functionCall(
                                name = "process",
                                overload = "",
                                arguments = listOf(booleanLiteral(false, boolType)),
                                resultTypeIndex = intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        // process(true) = inc(5) = 6
        // process(false) = dec(5) = 4
        // result = 6 + 4 = 10
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    // ==================== 12. Null-Safe Chaining ====================
    
    @Test
    fun `null-safe chaining with multiple levels`() {
        // s?.trim()?.toUpperCase()?.length()
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            function(
                name = "testFunction",
                returnType = intNullableType,
                parameters = listOf(TypedParameter("s", stringNullableType)),
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = memberCall(
                                expression = memberCall(
                                    expression = identifier("s", stringNullableType, 2),
                                    member = "trim",
                                    overload = "",
                                    arguments = emptyList(),
                                    isNullChaining = true,
                                    resultTypeIndex = stringNullableType
                                ),
                                member = "toUpperCase",
                                overload = "",
                                arguments = emptyList(),
                                isNullChaining = true,
                                resultTypeIndex = stringNullableType
                            ),
                            member = "length",
                            overload = "",
                            arguments = emptyList(),
                            isNullChaining = true,
                            resultTypeIndex = intNullableType
                        )
                    )
                )
            )
        }
        
        // Test with non-null value
        val resultNonNull = helper.compileAndInvoke(ast, "testFunction", "  hello  ")
        assertEquals(5, resultNonNull)  // "hello".length()
        
        // Test with null value
        val resultNull = helper.compileAndInvoke(ast, "testFunction", null)
        assertNull(resultNull)
    }
    
    // ==================== 13. Function Call as Iterable in For Loop ====================
    
    @Test
    fun `for loop iterating over function call result`() {
        // fun getNumbers(): List = [1, 2, 3, 4, 5]
        // for n in getNumbers() { sum = sum + n }
        val ast = buildTypedAst {
            val intType = intType()
            val listType = listType()
            
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("numbers", listType)),
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    forStmt(
                        variableName = "n",
                        variableType = intType,
                        iterable = identifier("numbers", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(
                                    identifier("sum", intType, 3),
                                    "+",
                                    identifier("n", intType, 4),
                                    intType
                                )
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
    
    // ==================== 14. Lambda Reassignment ====================
    
    @Test
    fun `using two different lambdas with same signature`() {
        // val add1 = (x) => x + 1
        // val mult2 = (x) => x * 2
        // val result1 = add1(5)   // 6
        // val result2 = mult2(5)  // 10
        // return result1 + result2  // 16
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
                    varDecl("add1", lambdaTypeIdx, lambdaExpr(
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
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    varDecl("mult2", lambdaTypeIdx, lambdaExpr(
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
                    varDecl("result1", intType,
                        expressionCall(
                            expression = identifier("add1", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(5, intType)),
                            resultTypeIndex = intType
                        )
                    ),
                    varDecl("result2", intType,
                        expressionCall(
                            expression = identifier("mult2", lambdaTypeIdx, 3),
                            arguments = listOf(intLiteral(5, intType)),
                            resultTypeIndex = intType
                        )
                    ),
                    returnStmt(
                        binaryExpr(
                            identifier("result1", intType, 3),
                            "+",
                            identifier("result2", intType, 3),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(16, result)  // 6 + 10 = 16
    }
    
    // ==================== 15. Mixed Scenario: Counter with Multiple Operations ====================
    
    @Test
    fun `stateful counter with increment, decrement, and reset lambdas`() {
        // var count = 0
        // val inc = () => { count = count + 1; return count }
        // val dec = () => { count = count - 1; return count }
        // val reset = () => { count = 0; return count }
        // inc() + inc() + inc() + dec() + reset() + inc()
        // = 1 + 2 + 3 + 2 + 0 + 1 = 9
        val ast = buildTypedAst {
            val intType = intType()
            val lambdaTypeIdx = lambdaType(ClassTypeRef("builtin.int", false))
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("count", intType, intLiteral(0, intType)),
                    varDecl("inc", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(
                                    identifier("count", intType, 3),
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            ),
                            returnStmt(identifier("count", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    varDecl("dec", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            assignment(
                                identifier("count", intType, 3),
                                binaryExpr(
                                    identifier("count", intType, 3),
                                    "-",
                                    intLiteral(1, intType),
                                    intType
                                )
                            ),
                            returnStmt(identifier("count", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    varDecl("reset", lambdaTypeIdx, lambdaExpr(
                        parameters = emptyList(),
                        body = listOf(
                            assignment(
                                identifier("count", intType, 3),
                                intLiteral(0, intType)
                            ),
                            returnStmt(identifier("count", intType, 3))
                        ),
                        lambdaTypeIndex = lambdaTypeIdx
                    )),
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    binaryExpr(
                                        binaryExpr(
                                            expressionCall(
                                                expression = identifier("inc", lambdaTypeIdx, 3),
                                                arguments = emptyList(),
                                                resultTypeIndex = intType
                                            ),
                                            "+",
                                            expressionCall(
                                                expression = identifier("inc", lambdaTypeIdx, 3),
                                                arguments = emptyList(),
                                                resultTypeIndex = intType
                                            ),
                                            intType
                                        ),
                                        "+",
                                        expressionCall(
                                            expression = identifier("inc", lambdaTypeIdx, 3),
                                            arguments = emptyList(),
                                            resultTypeIndex = intType
                                        ),
                                        intType
                                    ),
                                    "+",
                                    expressionCall(
                                        expression = identifier("dec", lambdaTypeIdx, 3),
                                        arguments = emptyList(),
                                        resultTypeIndex = intType
                                    ),
                                    intType
                                ),
                                "+",
                                expressionCall(
                                    expression = identifier("reset", lambdaTypeIdx, 3),
                                    arguments = emptyList(),
                                    resultTypeIndex = intType
                                ),
                                intType
                            ),
                            "+",
                            expressionCall(
                                expression = identifier("inc", lambdaTypeIdx, 3),
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(9, result)  // 1 + 2 + 3 + 2 + 0 + 1 = 9
    }
    
    // ==================== 16. While Loop with Complex Termination ====================
    
    @Test
    fun `while loop with multiple termination conditions`() {
        // Find first number in list that is divisible by both 3 and 5
        // while (i < list.size && !found) { ... }
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            val listType = listType()
            
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("list", listType)),
                body = listOf(
                    varDecl("result", intType, intLiteral(-1, intType)),
                    forStmt(
                        variableName = "item",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            ifStmt(
                                condition = binaryExpr(
                                    binaryExpr(
                                        binaryExpr(
                                            identifier("item", intType, 4),
                                            "%",
                                            intLiteral(3, intType),
                                            intType
                                        ),
                                        "==",
                                        intLiteral(0, intType),
                                        boolType
                                    ),
                                    "&&",
                                    binaryExpr(
                                        binaryExpr(
                                            identifier("item", intType, 4),
                                            "%",
                                            intLiteral(5, intType),
                                            intType
                                        ),
                                        "==",
                                        intLiteral(0, intType),
                                        boolType
                                    ),
                                    boolType
                                ),
                                thenBlock = listOf(
                                    assignment(
                                        identifier("result", intType, 3),
                                        identifier("item", intType, 4)
                                    ),
                                    breakStmt()
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
        }
        
        // List: [1, 3, 5, 10, 15, 30]
        // First number divisible by both 3 and 5 is 15
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 3, 5, 10, 15, 30))
        assertEquals(15, result)
    }
    
    // ==================== 17. Lambda with Captured Function Parameter ====================
    
    @Test
    fun `lambda captures function parameter`() {
        // fun addNTimes(n: Int, times: Int): Int {
        //     var result = 0
        //     var i = 0
        //     while (i < times) {
        //         result = result + n
        //         i = i + 1
        //     }
        //     return result
        // }
        // return addNTimes(3, 4)  // 3+3+3+3 = 12
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "addNTimes",
                returnType = intType,
                parameters = listOf(
                    TypedParameter("n", intType),
                    TypedParameter("times", intType)
                ),
                body = listOf(
                    varDecl("result", intType, intLiteral(0, intType)),
                    varDecl("i", intType, intLiteral(0, intType)),
                    whileStmt(
                        condition = binaryExpr(
                            identifier("i", intType, 3),
                            "<",
                            identifier("times", intType, 2),
                            boolType
                        ),
                        body = listOf(
                            assignment(
                                identifier("result", intType, 3),
                                binaryExpr(
                                    identifier("result", intType, 3),
                                    "+",
                                    identifier("n", intType, 2),
                                    intType
                                )
                            ),
                            assignment(
                                identifier("i", intType, 3),
                                binaryExpr(
                                    identifier("i", intType, 3),
                                    "+",
                                    intLiteral(1, intType),
                                    intType
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("result", intType, 3))
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "addNTimes",
                            overload = "",
                            arguments = listOf(intLiteral(3, intType), intLiteral(4, intType)),
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(12, result)  // 3 * 4 = 12
    }
    
    // ==================== 18. Complex Boolean Logic ====================
    
    @Test
    fun `complex boolean expressions with short-circuit evaluation`() {
        // (a > 5 && b < 10) || (c == 0 && d != 0)
        val ast = buildTypedAst {
            val intType = intType()
            val boolType = booleanType()
            
            function(
                name = "evaluate",
                returnType = boolType,
                parameters = listOf(
                    TypedParameter("a", intType),
                    TypedParameter("b", intType),
                    TypedParameter("c", intType),
                    TypedParameter("d", intType)
                ),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    identifier("a", intType, 2),
                                    ">",
                                    intLiteral(5, intType),
                                    boolType
                                ),
                                "&&",
                                binaryExpr(
                                    identifier("b", intType, 2),
                                    "<",
                                    intLiteral(10, intType),
                                    boolType
                                ),
                                boolType
                            ),
                            "||",
                            binaryExpr(
                                binaryExpr(
                                    identifier("c", intType, 2),
                                    "==",
                                    intLiteral(0, intType),
                                    boolType
                                ),
                                "&&",
                                binaryExpr(
                                    identifier("d", intType, 2),
                                    "!=",
                                    intLiteral(0, intType),
                                    boolType
                                ),
                                boolType
                            ),
                            boolType
                        )
                    )
                )
            )
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("sum", intType, intLiteral(0, intType)),
                    // Test case 1: (6 > 5 && 3 < 10) = true
                    ifStmt(
                        condition = functionCall(
                            "evaluate",
                            "",
                            listOf(intLiteral(6, intType), intLiteral(3, intType), intLiteral(1, intType), intLiteral(0, intType)),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    // Test case 2: (3 > 5 = false) || (0 == 0 && 1 != 0 = true) = true
                    ifStmt(
                        condition = functionCall(
                            "evaluate",
                            "",
                            listOf(intLiteral(3, intType), intLiteral(3, intType), intLiteral(0, intType), intLiteral(1, intType)),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    // Test case 3: (3 > 5 = false) || (1 == 0 = false) = false
                    ifStmt(
                        condition = functionCall(
                            "evaluate",
                            "",
                            listOf(intLiteral(3, intType), intLiteral(3, intType), intLiteral(1, intType), intLiteral(1, intType)),
                            boolType
                        ),
                        thenBlock = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(identifier("sum", intType, 3), "+", intLiteral(1, intType), intType)
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // Two tests pass (true), one fails (false)
        val result = helper.compileAndInvoke(ast)
        assertEquals(2, result)
    }
    
    // ==================== 19. Accumulator Pattern with For Loop ====================
    
    @Test
    fun `reduce-like pattern summing squares`() {
        // var sum = 0
        // for n in list { sum = sum + n * n }
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
                        variableName = "n",
                        variableType = intType,
                        iterable = identifier("list", listType, 2),
                        body = listOf(
                            assignment(
                                identifier("sum", intType, 3),
                                binaryExpr(
                                    identifier("sum", intType, 3),
                                    "+",
                                    binaryExpr(
                                        identifier("n", intType, 4),
                                        "*",
                                        identifier("n", intType, 4),
                                        intType
                                    ),
                                    intType
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("sum", intType, 3))
                )
            )
        }
        
        // List: [1, 2, 3, 4, 5]
        // Sum of squares: 1 + 4 + 9 + 16 + 25 = 55
        val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4, 5))
        assertEquals(55, result)
    }
    
    // ==================== 20. Mixed Types in Arithmetic Chain ====================
    
    @Test
    fun `arithmetic chain with int long float double`() {
        // 10 + 5L + 2.5f + 1.0 = 18.5
        val ast = buildTypedAst {
            val intType = intType()
            val longType = longType()
            val floatType = floatType()
            val doubleType = doubleType()
            
            function(
                name = "testFunction",
                returnType = doubleType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            binaryExpr(
                                binaryExpr(
                                    intLiteral(10, intType),
                                    "+",
                                    longLiteral(5L, longType),
                                    longType
                                ),
                                "+",
                                floatLiteral(2.5f, floatType),
                                floatType
                            ),
                            "+",
                            doubleLiteral(1.0, doubleType),
                            doubleType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(18.5, result)
    }
}
