package com.mdeo.script.compiler

import com.mdeo.script.ast.types.BuiltinTypes.consumer
import com.mdeo.script.ast.types.ClassTypeRef
import kotlin.test.assertEquals

/**
 * Manual test for Set.forEach with lambda expressions.
 * This test can be compiled even if other tests have errors.
 */
fun main() {
    val helper = CompilerTestHelper()
    
    val ast = buildTypedAst {
        val setType = addType(ClassTypeRef("builtin.Set", false))
        val intTypeIndex = intType()
        val voidTypeIndex = voidType()
        
        // Create consumer lambda type: (any?) -> void
        val consumerLambdaType = addType(consumer(ClassTypeRef("builtin.any", true)))
        
        function(
            name = "testFunction",
            returnType = intTypeIndex,
            body = listOf(
                // var sum = 0
                varDecl("sum", intTypeIndex, intLiteral(0, intTypeIndex)),
                
                // val mySet = setOf(1, 2, 3)
                varDecl("mySet", setType,
                    functionCall(
                        name = "setOf",
                        overload = "",
                        arguments = listOf(
                            intLiteral(1, intTypeIndex),
                            intLiteral(2, intTypeIndex),
                            intLiteral(3, intTypeIndex)
                        ),
                        resultTypeIndex = setType
                    )
                ),
                
                // mySet.forEach((element) => { sum = sum + element })
                exprStmt(
                    memberCall(
                        expression = identifier("mySet", setType, 3),
                        member = "forEach",
                        overload = "",
                        arguments = listOf(
                            lambdaExpr(
                                parameters = listOf("element"),
                                body = listOf(
                                    assignment(
                                        identifier("sum", intTypeIndex, 3),
                                        binaryExpr(
                                            identifier("sum", intTypeIndex, 3),
                                            "+",
                                            identifier("element", intTypeIndex, 4),  // Lambda param at scope 4
                                            intTypeIndex
                                        )
                                    )
                                ),
                                lambdaTypeIndex = consumerLambdaType
                            )
                        ),
                        resultTypeIndex = voidTypeIndex
                    )
                ),
                
                // return sum (should be 1 + 2 + 3 = 6)
                returnStmt(identifier("sum", intTypeIndex, 3))
            )
        )
    }
    
    try {
        val result = helper.compileAndInvoke(ast)
        assertEquals(6, result)  // 1 + 2 + 3 = 6
        println("✅ Test PASSED: Set.forEach with lambda expression works correctly!")
        println("   Result: $result (expected: 6)")
    } catch (e: Exception) {
        println("❌ Test FAILED: ${e.message}")
        e.printStackTrace()
    }
}
