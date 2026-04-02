package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassTypeRef
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Regression test for a VerifyError caused by incorrect local variable indexing
 * when a captured variable is accessed inside a nested lambda.
 *
 * ## Bug description
 *
 * The outer lambda `(testB) => { ... }` is compiled to a JVM method with signature
 * `(ILjava/lang/String;)I`, where:
 *   - local 0: `this` (ScriptProgram)
 *   - local 1: captured int `wtf`  ← the captured variable
 *   - local 2: String parameter `testB`
 *
 * When the inner lambda `(item) => wtf` references `wtf`, the compiler must emit
 * `iload_1` (load from slot 1). The bug caused it to emit `iload_2` instead, which
 * attempted to load a `String` as an `int` and triggered:
 *
 * ```
 * java.lang.VerifyError: Bad local variable type
 *   Location: com/mdeo/script/generated/ScriptProgram.lambda$script$0(ILjava/lang/String;)I @9: iload_2
 *   Reason: Type 'java/lang/String' (current frame, locals[2]) is not assignable to integer
 * ```
 *
 * ## Equivalent script
 *
 * ```
 * fun minimiseSprintEffortDeviation(): double {
 *     var testA = listOf("test")
 *     var wtf = 0
 *     testA.map(
 *         (testB) => {
 *             listOf<int>().map((item) => wtf)
 *             return 0
 *         }
 *     )
 *     return 0
 * }
 * ```
 *
 * This test will fail with a VerifyError before the bug is fixed, and pass
 * (returning 0.0) after the fix.
 */
class LambdaNestedCaptureBugTest {

    private val helper = CompilerTestHelper()

    @Test
    fun `nested lambda capturing outer variable does not cause VerifyError`() {
        val ast = buildTypedAst {
            // Register types in the exact order matching the typed AST JSON so that
            // each type falls at the index used in the expression nodes below.
            val voidTypeIdx        = voidType()        // 0
            val stringTypeIdx      = stringType()      // 1
            val doubleTypeIdx      = doubleType()      // 2
            @Suppress("UNUSED_VARIABLE")
            val booleanTypeIdx     = booleanType()     // 3
            @Suppress("UNUSED_VARIABLE")
            val anyNullableTypeIdx = anyNullableType() // 4

            // List<string>  — type of `testA`
            val listStringTypeIdx = addType(
                ClassTypeRef(
                    `package` = "builtin",
                    type = "List",
                    isNullable = false,
                    typeArgs = mapOf("T" to ClassTypeRef("builtin", "string", false))
                )
            ) // 5

            val intTypeIdx = intType() // 6

            // lambda (item: int) => int  — inner lambda: `(item) => wtf`
            val innerLambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", false),
                "param0" to ClassTypeRef("builtin", "int", false)
            ) // 7

            // Collection<int>  — return type of List<int>.map(...)
            val collectionIntTypeIdx = addType(
                ClassTypeRef(
                    `package` = "builtin",
                    type = "Collection",
                    isNullable = false,
                    typeArgs = mapOf("T" to ClassTypeRef("builtin", "int", false))
                )
            ) // 8

            // List<int>  — result of `listOf<int>()`
            val listIntTypeIdx = addType(
                ClassTypeRef(
                    `package` = "builtin",
                    type = "List",
                    isNullable = false,
                    typeArgs = mapOf("T" to ClassTypeRef("builtin", "int", false))
                )
            ) // 9

            // lambda (testB: string) => int  — outer lambda
            val outerLambdaTypeIdx = lambdaType(
                ClassTypeRef("builtin", "int", false),
                "param0" to ClassTypeRef("builtin", "string", false)
            ) // 10

            // Inner lambda body: return wtf
            // `wtf` is declared at function scope (scope = 3) and is captured by the outer lambda.
            // This is where the bug manifests: inside the outer lambda's compiled method the
            // captured `wtf` is at local slot 1, but the compiler was emitting iload_2.
            val innerLambda = lambdaExpr(
                parameters = listOf("item"),
                body = listOf(
                    returnStmt(identifier("wtf", intTypeIdx, 3))
                ),
                lambdaTypeIndex = innerLambdaTypeIdx
            )

            // Outer lambda body:
            //   listOf<int>().map((item) => wtf)  // result discarded
            //   return 0
            val outerLambda = lambdaExpr(
                parameters = listOf("testB"),
                body = listOf(
                    exprStmt(
                        memberCall(
                            expression = functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = listIntTypeIdx
                            ),
                            member = "map",
                            overload = "",
                            arguments = listOf(innerLambda),
                            resultTypeIndex = collectionIntTypeIdx
                        )
                    ),
                    returnStmt(intLiteral(0, intTypeIdx))
                ),
                lambdaTypeIndex = outerLambdaTypeIdx
            )

            function(
                name = "minimiseSprintEffortDeviation",
                returnType = doubleTypeIdx,
                body = listOf(
                    // var testA = listOf("test")
                    varDecl(
                        "testA",
                        listStringTypeIdx,
                        functionCall(
                            name = "listOf",
                            overload = "",
                            arguments = listOf(stringLiteral("test", stringTypeIdx)),
                            resultTypeIndex = listStringTypeIdx
                        )
                    ),
                    // var wtf = 0
                    varDecl("wtf", intTypeIdx, intLiteral(0, intTypeIdx)),
                    // testA.map(outerLambda)  — result discarded
                    exprStmt(
                        memberCall(
                            expression = identifier("testA", listStringTypeIdx, 3),
                            member = "map",
                            overload = "",
                            arguments = listOf(outerLambda),
                            resultTypeIndex = collectionIntTypeIdx
                        )
                    ),
                    // return 0
                    returnStmt(intLiteral(0, intTypeIdx))
                )
            )
        }

        // Before the fix this throws VerifyError (bad local variable type).
        // After the fix the function executes and returns 0.0.
        val result = helper.compileAndInvoke(ast, "minimiseSprintEffortDeviation")
        assertEquals(0.0, result)
    }
}
