package com.mdeo.script.compiler

import com.mdeo.expression.ast.expressions.TypedCallArgument
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Reproduces a bug where iterating over a `listOf<double>(1, 2, 3, 4, 5)` causes a
 * ClassCastException.
 *
 * Root cause:
 *   `listOf<double>(1, 2, 3, 4, 5)` internally creates a varargs `Object[]` where each
 *   int literal is auto-boxed as `Integer.valueOf(int)` → `Integer`.
 *   When the for-loop iterates over the resulting `ScriptList<?>` with variableType=double,
 *   `ForStatementCompiler` emits `CHECKCAST java/lang/Double` + `doubleValue()`.
 *   But the objects are actually `Integer`, so `CHECKCAST java/lang/Double` throws
 *   `ClassCastException`.
 *
 * Both tests below should FAIL (ClassCastException) before the fix and pass afterwards.
 */
class GenericCollectionCoercionTest {

    private val helper = CompilerTestHelper()

    // ==================== Test 1: simple sum ====================

    /**
     * Equivalent script:
     * ```
     * fun testFunction(): double {
     *     var sum: double = 0
     *     for (entry in listOf<double>(1, 2, 3, 4, 5)) {
     *         sum = sum + entry
     *     }
     *     return sum
     * }
     * ```
     * Expected: 15.0 = 1.0 + 2.0 + 3.0 + 4.0 + 5.0
     */
    @Test
    fun `for loop over listOf double with int literals sums correctly`() {
        val ast = buildTypedAst {
            // index 0
            val doubleType = doubleType()
            // index 1
            val intType = intType()
            // index 2 – List<double>
            val listDoubleType = addType(
                ClassTypeRef(
                    `package` = "builtin",
                    type = "List",
                    isNullable = false,
                    typeArgs = mapOf("T" to ClassTypeRef("builtin", "double", false))
                )
            )

            // fun testFunction(): double
            function(
                name = "testFunction",
                returnType = doubleType,
                parameters = emptyList(),
                body = listOf(
                    // var sum: double = 0.0
                    varDecl("sum", doubleType, doubleLiteral(0.0, doubleType)),
                    // for (entry in listOf<double>(1, 2, 3, 4, 5))
                    forStmt(
                        variableName = "entry",
                        variableType = doubleType,
                        iterable = functionCallWithArgs(
                            name = "listOf",
                            overload = "",
                            arguments = listOf(
                                arg(intLiteral(1, intType), doubleType),
                                arg(intLiteral(2, intType), doubleType),
                                arg(intLiteral(3, intType), doubleType),
                                arg(intLiteral(4, intType), doubleType),
                                arg(intLiteral(5, intType), doubleType)
                            ),
                            resultTypeIndex = listDoubleType
                        ),
                        body = listOf(
                            // sum = sum + entry
                            assignment(
                                identifier("sum", doubleType, 3),
                                binaryExpr(
                                    identifier("sum", doubleType, 3),
                                    "+",
                                    identifier("entry", doubleType, 4),
                                    doubleType
                                )
                            )
                        )
                    ),
                    returnStmt(identifier("sum", doubleType, 3))
                )
            )
        }

        val result = helper.compileAndInvoke(ast, "testFunction")
        assertEquals(15.0, result)
    }

    // ==================== Test 2: stddev using listOf<double> ====================

    /**
     * Equivalent script (the exact failing user script):
     * ```
     * fun stddev(values: List<double>): double {
     *     if (values.size() == 1) {
     *         return 0
     *     }
     *     var sum: double = 0
     *     for (entry in values) {
     *         sum = sum + entry * entry
     *     }
     *     return sum / (values.size() - 1)
     * }
     *
     * fun test(): double {
     *     return stddev(listOf<double>(1, 2, 3, 4, 5))
     * }
     * ```
     * Expected: 55 / (5 - 1) = 55 / 4 = 13.75
     *   sum = 1² + 2² + 3² + 4² + 5² = 1 + 4 + 9 + 16 + 25 = 55
     *   result = 55 / (5 - 1) = 13.75
     */
    @Test
    fun `stddev function using listOf double with int literals`() {
        val ast = buildTypedAst {
            // index 0
            val voidType = voidType()
            // index 1
            val stringType = stringType()
            // index 2
            val doubleType = doubleType()
            // index 3
            val booleanType = booleanType()
            // index 4
            val anyNullableType = anyNullableType()
            // index 5 – List<double>
            val listDoubleType = addType(
                ClassTypeRef(
                    `package` = "builtin",
                    type = "List",
                    isNullable = false,
                    typeArgs = mapOf("T" to ClassTypeRef("builtin", "double", false))
                )
            )
            // index 6
            val intType = intType()

            // fun stddev(values: List<double>): double
            function(
                name = "stddev",
                returnType = doubleType,
                parameters = listOf(TypedParameter("values", listDoubleType)),
                body = listOf(
                    // if (values.size() == 1) { return 0 }
                    ifStmt(
                        condition = binaryExpr(
                            memberCall(
                                expression = identifier("values", listDoubleType, 2),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                isNullChaining = false,
                                resultTypeIndex = intType
                            ),
                            "==",
                            intLiteral(1, intType),
                            booleanType
                        ),
                        thenBlock = listOf(
                            returnStmt(doubleLiteral(0.0, doubleType))
                        )
                    ),
                    // var sum: double = 0.0
                    varDecl("sum", doubleType, doubleLiteral(0.0, doubleType)),
                    // for (entry in values) { sum = sum + entry * entry }
                    forStmt(
                        variableName = "entry",
                        variableType = doubleType,
                        iterable = identifier("values", listDoubleType, 2),
                        body = listOf(
                            // sum = sum + entry * entry
                            assignment(
                                identifier("sum", doubleType, 3),
                                binaryExpr(
                                    identifier("sum", doubleType, 3),
                                    "+",
                                    binaryExpr(
                                        identifier("entry", doubleType, 4),
                                        "*",
                                        identifier("entry", doubleType, 4),
                                        doubleType
                                    ),
                                    doubleType
                                )
                            )
                        )
                    ),
                    // return sum / (values.size() - 1)
                    returnStmt(
                        binaryExpr(
                            identifier("sum", doubleType, 3),
                            "/",
                            binaryExpr(
                                memberCall(
                                    expression = identifier("values", listDoubleType, 2),
                                    member = "size",
                                    overload = "",
                                    arguments = emptyList(),
                                    isNullChaining = false,
                                    resultTypeIndex = intType
                                ),
                                "-",
                                intLiteral(1, intType),
                                intType
                            ),
                            doubleType
                        )
                    )
                )
            )

            // fun test(): double { return stddev(listOf<double>(1, 2, 3, 4, 5)) }
            function(
                name = "test",
                returnType = doubleType,
                parameters = emptyList(),
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "stddev",
                            overload = "",
                            arguments = listOf(
                                functionCallWithArgs(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        arg(intLiteral(1, intType), doubleType),
                                        arg(intLiteral(2, intType), doubleType),
                                        arg(intLiteral(3, intType), doubleType),
                                        arg(intLiteral(4, intType), doubleType),
                                        arg(intLiteral(5, intType), doubleType)
                                    ),
                                    resultTypeIndex = listDoubleType
                                )
                            ),
                            resultTypeIndex = doubleType
                        )
                    )
                )
            )
        }

        val result = helper.compileAndInvoke(ast, "test")
        assertEquals(13.75, result)
    }
}
