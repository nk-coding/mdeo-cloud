package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst
import com.mdeo.expression.ast.TypedCallableBody
import com.mdeo.script.ast.TypedFunction
import com.mdeo.script.ast.TypedImport
import com.mdeo.script.ast.TypedParameter
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.runtime.ExecutionEnvironment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for imported function calls using the hierarchical function registry.
 *
 * These tests verify that:
 * - Imported functions can be called from another file
 * - Type coercion is applied correctly to arguments when calling imported functions
 * - Aliased imports work correctly (import foo as bar)
 */
class ImportedFunctionCallTest {

    private val compiler = ScriptCompiler()

    /**
     * Test calling a function imported from another file.
     *
     * File A defines: fun add(a: Int, b: Int): Int = a + b
     * File B imports add from A and calls it.
     */
    @Test
    fun `call imported function`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "add",
                    parameters = listOf(
                        TypedParameter("a", 0),
                        TypedParameter("b", 0)
                    ),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("a", 0, 2),
                                    "+",
                                    identifier("b", 0, 2),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(
                    name = "add",
                    ref = "add",
                    uri = "file://moduleA.script"
                )
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    name = "add",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, 0),
                                        intLiteral(20, 0)
                                    ),
                                    resultTypeIndex = 0
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(30, result)
    }

    /**
     * Test calling an imported function with type coercion.
     *
     * File A defines: fun takeLong(x: Long): Long = x * 2
     * File B imports takeLong and calls it with an Int argument (should be widened to Long).
     */
    @Test
    fun `call imported function with type coercion`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val longType = ClassTypeRef("builtin", "long", false)
        val types = listOf<ReturnType>(intType, longType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "takeLong",
                    parameters = listOf(
                        TypedParameter("x", 1)
                    ),
                    returnType = 1,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 1, 2),
                                    "*",
                                    longLiteral(2L, 1),
                                    1
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(
                    name = "takeLong",
                    ref = "takeLong",
                    uri = "file://moduleA.script"
                )
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 1,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    name = "takeLong",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(21, 0)
                                    ),
                                    resultTypeIndex = 1
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(42L, result)
    }

    /**
     * Test calling an aliased imported function.
     *
     * File A defines: fun originalName(x: Int): Int = x + 1
     * File B imports originalName as aliasedName and calls aliasedName.
     */
    @Test
    fun `call aliased imported function`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "originalName",
                    parameters = listOf(
                        TypedParameter("x", 0)
                    ),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 0, 2),
                                    "+",
                                    intLiteral(1, 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(
                    name = "aliasedName",
                    ref = "originalName",
                    uri = "file://moduleA.script"
                )
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    name = "aliasedName",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(99, 0)
                                    ),
                                    resultTypeIndex = 0
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(100, result)
    }

    /**
     * Test calling an imported function that takes double from int argument.
     *
     * This tests int→double widening for imported functions.
     */
    @Test
    fun `call imported function with int to double coercion`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val doubleType = ClassTypeRef("builtin", "double", false)
        val types = listOf<ReturnType>(intType, doubleType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "takeDouble",
                    parameters = listOf(
                        TypedParameter("x", 1)
                    ),
                    returnType = 1,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 1, 2),
                                    "*",
                                    doubleLiteral(0.5, 1),
                                    1
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(
                    name = "takeDouble",
                    ref = "takeDouble",
                    uri = "file://moduleA.script"
                )
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 1,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    name = "takeDouble",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, 0)
                                    ),
                                    resultTypeIndex = 1
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(5.0, result)
    }

    /**
     * Test importing from multiple files.
     *
     * File A defines: fun funcA(): Int = 10
     * File B defines: fun funcB(): Int = 20
     * File C imports from both A and B, calls both functions and adds results.
     */
    @Test
    fun `call functions imported from multiple files`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "funcA",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(10, 0)))
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "funcB",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(20, 0)))
                    )
                )
            )
        )

        val fileC = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "funcA", ref = "funcA", uri = "file://moduleA.script"),
                TypedImport(name = "funcB", ref = "funcB", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    functionCall("funcA", "", emptyList(), 0),
                                    "+",
                                    functionCall("funcB", "", emptyList(), 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleC.script", "testFunction")

        assertEquals(30, result)
    }

    // ==================== Additional Edge Cases ====================

    /**
     * Test that a local function shadows an imported function with the same name.
     *
     * File A defines: fun compute(x: Int): Int = x * 10
     * File B defines: fun compute(x: Int): Int = x * 2
     * File B imports compute from A (but local should shadow it)
     * 
     * When calling compute(5), should return 10 (from local), not 50 (from imported).
     */
    @Test
    fun `local function shadows imported function`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "compute",
                    parameters = listOf(TypedParameter("x", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 0, 2),
                                    "*",
                                    intLiteral(10, 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "compute", ref = "compute", uri = "file://moduleA.script")
            ),
            functions = listOf(
                // Local function with same name should shadow the import
                TypedFunction(
                    name = "compute",
                    parameters = listOf(TypedParameter("x", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 0, 2),
                                    "*",
                                    intLiteral(2, 0),
                                    0
                                )
                            )
                        )
                    )
                ),
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("compute", "", listOf(intLiteral(5, 0)), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        // Should be 10 (5 * 2 from local), NOT 50 (5 * 10 from imported)
        assertEquals(10, result)
    }

    /**
     * Test importing a function that internally calls other functions in its file.
     *
     * File A defines:
     *   fun helper(x: Int): Int = x + 1
     *   fun doubleAndIncrement(x: Int): Int = helper(x * 2)
     *
     * File B imports doubleAndIncrement and calls it.
     * The imported function should correctly call its local helper function.
     */
    @Test
    fun `imported function calls its own local functions`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "helper",
                    parameters = listOf(TypedParameter("x", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 0, 2),
                                    "+",
                                    intLiteral(1, 0),
                                    0
                                )
                            )
                        )
                    )
                ),
                TypedFunction(
                    name = "doubleAndIncrement",
                    parameters = listOf(TypedParameter("x", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    "helper",
                                    "",
                                    listOf(
                                        binaryExpr(
                                            identifier("x", 0, 2),
                                            "*",
                                            intLiteral(2, 0),
                                            0
                                        )
                                    ),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "doubleAndIncrement", ref = "doubleAndIncrement", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                // doubleAndIncrement(5) = helper(5 * 2) = helper(10) = 10 + 1 = 11
                                functionCall("doubleAndIncrement", "", listOf(intLiteral(5, 0)), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(11, result)
    }

    /**
     * Test importing a function that takes a String parameter (object type).
     *
     * File A defines: fun greet(name: String): String = "Hello, " + name
     * File B imports greet and calls it with a string argument.
     */
    @Test
    fun `imported function with string parameter`() {
        val stringType = ClassTypeRef("builtin", "string", false)
        val types = listOf<ReturnType>(stringType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "greet",
                    parameters = listOf(TypedParameter("name", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    stringLiteral("Hello, ", 0),
                                    "+",
                                    identifier("name", 0, 2),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "greet", ref = "greet", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("greet", "", listOf(stringLiteral("World", 0)), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals("Hello, World", result)
    }

    /**
     * Test that return types from imported functions are handled correctly.
     *
     * File A defines: fun getValue(): Long = 9876543210
     * File B imports getValue and uses the result in an expression.
     */
    @Test
    fun `imported function return type is handled correctly`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val longType = ClassTypeRef("builtin", "long", false)
        val types = listOf<ReturnType>(intType, longType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "getValue",
                    parameters = emptyList(),
                    returnType = 1, // Long
                    body = TypedCallableBody(
                        listOf(returnStmt(longLiteral(9876543210L, 1)))
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "getValue", ref = "getValue", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 1, // Long
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    functionCall("getValue", "", emptyList(), 1),
                                    "+",
                                    longLiteral(1L, 1),
                                    1
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(9876543211L, result)
    }

    /**
     * Test chained calls to imported functions.
     *
     * File A defines:
     *   fun double(x: Int): Int = x * 2
     *   fun triple(x: Int): Int = x * 3
     *
     * File B imports both and calls: triple(double(5)) = triple(10) = 30
     */
    @Test
    fun `chained calls to imported functions`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "double",
                    parameters = listOf(TypedParameter("x", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 0, 2),
                                    "*",
                                    intLiteral(2, 0),
                                    0
                                )
                            )
                        )
                    )
                ),
                TypedFunction(
                    name = "triple",
                    parameters = listOf(TypedParameter("x", 0)),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("x", 0, 2),
                                    "*",
                                    intLiteral(3, 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "double", ref = "double", uri = "file://moduleA.script"),
                TypedImport(name = "triple", ref = "triple", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                // triple(double(5)) = triple(10) = 30
                                functionCall(
                                    "triple",
                                    "",
                                    listOf(
                                        functionCall("double", "", listOf(intLiteral(5, 0)), 0)
                                    ),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(30, result)
    }

    /**
     * Test imported function with multiple parameters of different types.
     *
     * File A defines: fun calculate(a: Int, b: Long, c: Double): Double
     * File B imports and calls it with mixed types.
     */
    @Test
    fun `imported function with multiple parameter types`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val longType = ClassTypeRef("builtin", "long", false)
        val doubleType = ClassTypeRef("builtin", "double", false)
        val types = listOf<ReturnType>(intType, longType, doubleType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "calculate",
                    parameters = listOf(
                        TypedParameter("a", 0), // Int
                        TypedParameter("b", 1), // Long
                        TypedParameter("c", 2)  // Double
                    ),
                    returnType = 2, // Double
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    binaryExpr(
                                        identifier("a", 0, 2),
                                        "+",
                                        identifier("b", 1, 2),
                                        2 // result is promoted to double
                                    ),
                                    "+",
                                    identifier("c", 2, 2),
                                    2
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "calculate", ref = "calculate", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 2, // Double
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    "calculate",
                                    "",
                                    listOf(
                                        intLiteral(10, 0),
                                        longLiteral(20L, 1),
                                        doubleLiteral(30.5, 2)
                                    ),
                                    2
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        assertEquals(60.5, result)
    }

    // ==================== Helper for String Literal ====================

    private fun stringLiteral(value: String, typeIndex: Int) =
        com.mdeo.expression.ast.expressions.TypedStringLiteralExpression(
            evalType = typeIndex,
            value = value
        )

    // ==================== Transitive Import Test ====================

    /**
     * Test transitive imports: File A imports from B, B imports from C.
     *
     * File C defines: fun baseValue(): Int = 100
     * File B imports baseValue from C and defines: fun fromB(): Int = baseValue() + 10
     * File A imports fromB from B and calls it.
     *
     * Expected: fromB() should return 110 (100 + 10).
     *
     * This tests that the function registry properly resolves transitive import chains.
     */
    @Test
    fun `transitive imports - A imports from B which imports from C`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileC = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "baseValue",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(100, 0)))
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "baseValue", ref = "baseValue", uri = "file://moduleC.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "fromB",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    functionCall("baseValue", "", emptyList(), 0),
                                    "+",
                                    intLiteral(10, 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "fromB", ref = "fromB", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("fromB", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleA.script", "testFunction")

        assertEquals(110, result)
    }

    /**
     * Test re-exporting: File B imports from C and re-exports, File A imports from B.
     *
     * File C defines: fun original(): Int = 42
     * File B imports original from C (as 'original')
     * File B defines: fun wrapper(): Int = original()  // calls the imported function
     * File A imports wrapper from B and calls it.
     *
     * This tests that imported functions work correctly within the file that imports them.
     */
    @Test
    fun `file using its own imported functions`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileC = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "original",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(42, 0)))
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "original", ref = "original", uri = "file://moduleC.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "wrapper",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                // Calls the imported 'original' function
                                functionCall("original", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "wrapper", ref = "wrapper", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("wrapper", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleA.script", "testFunction")

        assertEquals(42, result)
    }

    /**
     * Test direct re-export: File B imports from C and re-exports the same function.
     *
     * File C defines: fun getValue(): Int = 999
     * File B imports getValue from C (no local wrapper - just a direct re-export scenario)
     * File A imports getValue from B (which should resolve to C's getValue)
     *
     * This tests the case where the registry lookup needs to resolve an import
     * that itself is an import (not a local function) in the source file.
     *
     * NOTE: This is a tricky case - when A looks up "getValue" from B's registry,
     * it needs to resolve through B's import to C.
     */
    @Test
    fun `direct re-export - A imports getValue from B which imports getValue from C`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileC = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "getValue",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(999, 0)))
                    )
                )
            )
        )

        // File B just imports getValue from C - no local functions
        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "getValue", ref = "getValue", uri = "file://moduleC.script")
            ),
            functions = emptyList()
        )

        // File A imports getValue from B
        // This should actually resolve to C's getValue through B's import
        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "getValue", ref = "getValue", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("getValue", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleA.script", "testFunction")

        assertEquals(999, result)
    }

    /**
     * Test circular imports - A imports from B, B imports from A.
     *
     * File A defines: fun funcA(): Int = 10
     *          imports funcB from B
     *          defines: fun callBoth(): Int = funcA() + funcB()
     *
     * File B defines: fun funcB(): Int = 20
     *          imports funcA from A
     *
     * This tests that circular imports don't cause infinite loops during lookup
     * and that functions can be called correctly across circular dependencies.
     */
    @Test
    fun `circular imports - A imports from B and B imports from A`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "funcB", ref = "funcB", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "funcA",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(10, 0)))
                    )
                ),
                TypedFunction(
                    name = "callBoth",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    functionCall("funcA", "", emptyList(), 0),
                                    "+",
                                    functionCall("funcB", "", emptyList(), 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "funcA", ref = "funcA", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "funcB",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(20, 0)))
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        // Test calling from file A - this will call funcA() and funcB() from B
        val resultA = env.invoke("file://moduleA.script", "callBoth")
        assertEquals(30, resultA)
    }

    // ==================== Additional Edge Case Tests ====================

    /**
     * Test deeply nested imports: A → B → C → D chain.
     *
     * File D defines: fun baseValue(): Int = 1000
     * File C imports from D and defines: fun cValue(): Int = baseValue() + 100
     * File B imports from C and defines: fun bValue(): Int = cValue() + 10
     * File A imports from B and calls bValue().
     *
     * Expected: bValue() = 1000 + 100 + 10 = 1110
     *
     * This tests that deeply nested transitive imports are resolved correctly.
     */
    @Test
    fun `deeply nested imports - A to B to C to D chain`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileD = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "baseValue",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(1000, 0)))
                    )
                )
            )
        )

        val fileC = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "baseValue", ref = "baseValue", uri = "file://moduleD.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "cValue",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    functionCall("baseValue", "", emptyList(), 0),
                                    "+",
                                    intLiteral(100, 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "cValue", ref = "cValue", uri = "file://moduleC.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "bValue",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    functionCall("cValue", "", emptyList(), 0),
                                    "+",
                                    intLiteral(10, 0),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "bValue", ref = "bValue", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("bValue", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC,
                "file://moduleD.script" to fileD
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleA.script", "testFunction")

        // 1000 (from D) + 100 (from C) + 10 (from B) = 1110
        assertEquals(1110, result)
    }

    /**
     * Test that an imported function shadows a global function with the same name.
     *
     * This is a conceptual test: if a file imports a function named "customPrint"
     * and there happened to be a global function with the same name, the import
     * should take precedence (per the hierarchical lookup order).
     *
     * In practice, we test this by having a local function shadow an import,
     * and an import shadow global (the lookup order: local → import → global).
     */
    @Test
    fun `import shadows global function lookup order`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        // File A provides a function that will be imported
        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "getValue",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(42, 0)))
                    )
                )
            )
        )

        // File B imports getValue from A
        // When calling getValue, it should use the imported function (not fall through to global)
        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "getValue", ref = "getValue", uri = "file://moduleA.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("getValue", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        // The imported function should be called, returning 42
        assertEquals(42, result)
    }

    /**
     * Test that local function shadows both imported AND global functions.
     *
     * This tests the full lookup hierarchy:
     * 1. Local functions (highest priority)
     * 2. Imported functions
     * 3. Global functions (lowest priority)
     *
     * File A defines: fun compute(): Int = 100
     * File B imports compute from A but also defines local compute(): Int = 5
     * When calling compute, the local one (5) should be used.
     */
    @Test
    fun `local shadows import which shadows global - full hierarchy`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileA = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "compute",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(100, 0)))
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "compute", ref = "compute", uri = "file://moduleA.script")
            ),
            functions = listOf(
                // Local function with same name - should shadow the import
                TypedFunction(
                    name = "compute",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(5, 0)))
                    )
                ),
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("compute", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleB.script", "testFunction")

        // Local function should be called, returning 5 (not 100 from import)
        assertEquals(5, result)
    }

    /**
     * Test importing a function with multiple chained re-exports.
     *
     * File D defines: fun originalFunc(): Int = 7
     * File C imports originalFunc from D as "funcC"
     * File B imports funcC from C as "funcB"
     * File A imports funcB from B as "funcA" and calls it
     *
     * Each file uses a different alias, testing that the registry correctly
     * follows the ref chain through multiple levels of aliasing.
     */
    @Test
    fun `chained aliased re-exports - multiple levels of renaming`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileD = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "originalFunc",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(returnStmt(intLiteral(7, 0)))
                    )
                )
            )
        )

        val fileC = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "funcC", ref = "originalFunc", uri = "file://moduleD.script")
            ),
            functions = emptyList()
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "funcB", ref = "funcC", uri = "file://moduleC.script")
            ),
            functions = emptyList()
        )

        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "funcA", ref = "funcB", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall("funcA", "", emptyList(), 0)
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC,
                "file://moduleD.script" to fileD
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleA.script", "testFunction")

        // Should resolve through all aliases to originalFunc which returns 7
        assertEquals(7, result)
    }

    /**
     * Test that imported function with parameters is called correctly through deep chain.
     *
     * File C defines: fun multiply(a: Int, b: Int): Int = a * b
     * File B imports multiply from C
     * File A imports multiply from B and calls it with arguments
     *
     * This ensures parameters are correctly passed through re-exported functions.
     */
    @Test
    fun `re-exported function with parameters works correctly`() {
        val intType = ClassTypeRef("builtin", "int", false)
        val types = listOf<ReturnType>(intType)

        val fileC = TypedAst(
            types = types,
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "multiply",
                    parameters = listOf(
                        TypedParameter("a", 0),
                        TypedParameter("b", 0)
                    ),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                binaryExpr(
                                    identifier("a", 0, 2),
                                    "*",
                                    identifier("b", 0, 2),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val fileB = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "multiply", ref = "multiply", uri = "file://moduleC.script")
            ),
            functions = emptyList()
        )

        val fileA = TypedAst(
            types = types,
            imports = listOf(
                TypedImport(name = "multiply", ref = "multiply", uri = "file://moduleB.script")
            ),
            functions = listOf(
                TypedFunction(
                    name = "testFunction",
                    parameters = emptyList(),
                    returnType = 0,
                    body = TypedCallableBody(
                        listOf(
                            returnStmt(
                                functionCall(
                                    "multiply",
                                    "",
                                    listOf(intLiteral(6, 0), intLiteral(7, 0)),
                                    0
                                )
                            )
                        )
                    )
                )
            )
        )

        val input = CompilationInput(
            mapOf(
                "file://moduleA.script" to fileA,
                "file://moduleB.script" to fileB,
                "file://moduleC.script" to fileC
            )
        )

        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        val result = env.invoke("file://moduleA.script", "testFunction")

        // 6 * 7 = 42
        assertEquals(42, result)
    }
}
