package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassTypeRef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Regression tests for two bugs that occur when multiple functions in the same
 * script each contain lambda expressions.
 *
 * ## Bug 1: Duplicate lambda method names (ClassFormatError)
 *
 * Each function creates its own `LambdaCounter` starting at 0, so both lambdas
 * receive the method name `lambda$script$0`. The JVM rejects the class with:
 * ```
 * ClassFormatError: Duplicate method name "lambda$script$0" with signature "..."
 * ```
 *
 * ## Bug 2: Duplicate Lambda$0 interface with different signatures (NoSuchMethodError)
 *
 * Each function creates its own `LambdaInterfaceRegistry`. When two functions each
 * need a custom lambda interface (one not in the predefined set), they each generate
 * `Lambda$0`. The second function's bytecode overwrites the first function's entry in
 * the shared `generatedInterfaces` map, so fn0's invokedynamic instruction references
 * `Lambda$0` with the wrong method signature, resulting in:
 * ```
 * NoSuchMethodError: 'boolean Lambda$0.call(java.lang.Object)'
 * ```
 */
class LambdaMultiFunctionBugTest {

    private val helper = CompilerTestHelper()

    /**
     * Two functions each containing a lambda must not cause a ClassFormatError
     * due to duplicate `lambda$script$0` method names.
     *
     * Both lambdas use `(Any?) -> boolean`, which maps to the predefined `Predicate1`
     * interface, so Bug 2 (custom interface collision) does not apply here.
     */
    @Test
    fun `two functions with lambdas do not cause ClassFormatError`() {
        val ast = buildTypedAst {
            val doubleTypeIdx      = doubleType()      // 0
            val anyNullableTypeIdx = anyNullableType() // 1
            val booleanTypeIdx     = booleanType()     // 2

            // lambda (Any?) -> boolean  — maps to predefined Predicate1
            val lambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "boolean", false),
                "wi" to ClassTypeRef("builtin", "Any", isNullable = true)
            ) // 3

            function(
                name = "fn0",
                returnType = doubleTypeIdx,
                body = listOf(
                    varDecl(
                        "f",
                        lambdaTypeIdx,
                        lambdaExpr(
                            listOf("wi"),
                            listOf(returnStmt(booleanLiteral(true, booleanTypeIdx))),
                            lambdaTypeIdx
                        )
                    ),
                    returnStmt(doubleLiteral(0.0, doubleTypeIdx))
                )
            )

            function(
                name = "fn1",
                returnType = doubleTypeIdx,
                body = listOf(
                    varDecl(
                        "g",
                        lambdaTypeIdx,
                        lambdaExpr(
                            listOf("wi"),
                            listOf(returnStmt(booleanLiteral(false, booleanTypeIdx))),
                            lambdaTypeIdx
                        )
                    ),
                    returnStmt(doubleLiteral(0.0, doubleTypeIdx))
                )
            )
        }

        val result = helper.compileAndInvoke(ast, "fn0")
        assertEquals(0.0, result)
    }

    /**
     * Two functions that each require a *custom* lambda interface (one not in the
     * predefined set) must not cause a NoSuchMethodError due to the second function's
     * `Lambda$0` overwriting the first function's `Lambda$0` in the shared interface map.
     *
     * - fn0 lambda: `(Any /*non-nullable*/) -> boolean`  → custom `Lambda$0` with `(Object)Z`
     * - fn1 lambda: `(Any /*non-nullable*/) -> double`   → custom `Lambda$0` with `(Object)D`
     *
     * A non-nullable `Any` parameter does not match `Predicate1` (which requires
     * `Any?`/nullable), so both generate a fresh custom interface named `Lambda$0`.
     */
    @Test
    fun `two functions with different custom lambda interfaces do not cause NoSuchMethodError`() {
        val ast2 = buildTypedAst {
            val doubleTypeIdx  = doubleType()   // 0
            val booleanTypeIdx = booleanType()  // 1

            // non-nullable Any — does NOT match Predicate1 (which needs nullable Any)
            val anyNonNullableTypeIdx = addType(ClassTypeRef("builtin", "Any", isNullable = false)) // 2

            // fn0 lambda: (Any /*non-nullable*/) -> boolean  → custom Lambda$0 "(Object)Z"
            val lambdaTypeBooleanIdx = lambdaType(
                ClassTypeRef("builtin", "boolean", false),
                "x" to ClassTypeRef("builtin", "Any", isNullable = false)
            ) // 3

            // fn1 lambda: (Any /*non-nullable*/) -> double   → custom Lambda$0 "(Object)D"
            val lambdaTypeDoubleIdx = lambdaType(
                ClassTypeRef("builtin", "double", false),
                "x" to ClassTypeRef("builtin", "Any", isNullable = false)
            ) // 4

            function(
                name = "fn0",
                returnType = doubleTypeIdx,
                body = listOf(
                    varDecl(
                        "f",
                        lambdaTypeBooleanIdx,
                        lambdaExpr(
                            listOf("x"),
                            listOf(returnStmt(booleanLiteral(true, booleanTypeIdx))),
                            lambdaTypeBooleanIdx
                        )
                    ),
                    returnStmt(doubleLiteral(0.0, doubleTypeIdx))
                )
            )

            function(
                name = "fn1",
                returnType = doubleTypeIdx,
                body = listOf(
                    varDecl(
                        "g",
                        lambdaTypeDoubleIdx,
                        lambdaExpr(
                            listOf("x"),
                            listOf(returnStmt(doubleLiteral(0.0, doubleTypeIdx))),
                            lambdaTypeDoubleIdx
                        )
                    ),
                    returnStmt(doubleLiteral(0.0, doubleTypeIdx))
                )
            )
        }

        val result = helper.compileAndInvoke(ast2, "fn0")
        assertEquals(0.0, result)
    }
}
