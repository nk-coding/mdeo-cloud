package com.mdeo.script.runtime

import com.mdeo.expression.ast.statements.TypedExpressionStatement
import com.mdeo.expression.ast.types.BuiltinTypes.consumer
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.compiler.buildTypedAst
import com.mdeo.script.compiler.exprStmt
import com.mdeo.script.compiler.functionCall
import com.mdeo.script.compiler.intLiteral
import com.mdeo.script.compiler.lambdaExpr
import com.mdeo.script.compiler.memberCall
import com.mdeo.script.compiler.identifier
import com.mdeo.script.compiler.returnStmt
import com.mdeo.script.compiler.stringLiteral
import com.mdeo.script.compiler.varDecl
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for println output capture through ScriptContext.
 *
 * Verifies that println writes to the ScriptContext's printStream
 * rather than System.out, allowing output to be captured for execution summaries.
 */
class PrintlnOutputCaptureTest {

    private val compiler = ScriptCompiler()

    @Nested
    inner class SinglePrintln {

        @Test
        fun `println output is captured in execution stream`() {
            // Create a function that calls println
            val ast = buildTypedAst {
                val voidType = voidType()
                val stringType = stringType()

                function(
                    name = "testPrintln",
                    returnType = voidType,
                    body = listOf(
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Hello, World!", stringType)),
                                resultTypeIndex = voidType
                            )
                        )
                    )
                )
            }

            // Compile the program
            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            // Create a custom output stream to capture println output
            val outputStream = ByteArrayOutputStream()
            val customConsole = PrintStream(outputStream, true, Charsets.UTF_8)
            val env = ExecutionEnvironment(program)

            // Execute the function
            env.invoke("test://test.script", "testPrintln", SimpleScriptContext(customConsole, null))

            // Verify output was captured
            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertEquals("Hello, World!\n", capturedOutput)
        }

        @Test
        fun `println with empty string is captured`() {
            val ast = buildTypedAst {
                val voidType = voidType()
                val stringType = stringType()

                function(
                    name = "testEmptyPrintln",
                    returnType = voidType,
                    body = listOf(
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("", stringType)),
                                resultTypeIndex = voidType
                            )
                        )
                    )
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            val outputStream = ByteArrayOutputStream()
            val customConsole = PrintStream(outputStream, true, Charsets.UTF_8)
            val env = ExecutionEnvironment(program)

            env.invoke("test://test.script", "testEmptyPrintln", SimpleScriptContext(customConsole, null))

            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertEquals("\n", capturedOutput)
        }
    }

    @Nested
    inner class MultiplePrintlnCalls {

        @Test
        fun `multiple println calls are all captured`() {
            val ast = buildTypedAst {
                val voidType = voidType()
                val stringType = stringType()

                function(
                    name = "testMultiplePrintln",
                    returnType = voidType,
                    body = listOf(
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Line 1", stringType)),
                                resultTypeIndex = voidType
                            )
                        ),
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Line 2", stringType)),
                                resultTypeIndex = voidType
                            )
                        ),
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Line 3", stringType)),
                                resultTypeIndex = voidType
                            )
                        )
                    )
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            val outputStream = ByteArrayOutputStream()
            val customConsole = PrintStream(outputStream, true, Charsets.UTF_8)
            val env = ExecutionEnvironment(program)

            env.invoke("test://test.script", "testMultiplePrintln", SimpleScriptContext(customConsole, null))

            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertEquals("Line 1\nLine 2\nLine 3\n", capturedOutput)
        }
    }

    @Nested
    inner class PrintlnWithReturnValue {

        @Test
        fun `println in function with return value is captured`() {
            val ast = buildTypedAst {
                val intType = intType()
                val voidType = voidType()
                val stringType = stringType()

                function(
                    name = "testPrintlnWithReturn",
                    returnType = intType,
                    body = listOf(
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Computing result...", stringType)),
                                resultTypeIndex = voidType
                            )
                        ),
                        returnStmt(intLiteral(42, intType))
                    )
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            val outputStream = ByteArrayOutputStream()
            val customConsole = PrintStream(outputStream, true, Charsets.UTF_8)
            val env = ExecutionEnvironment(program)

            val result = env.invoke("test://test.script", "testPrintlnWithReturn", SimpleScriptContext(customConsole, null))

            // Both output and return value should work
            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertEquals("Computing result...\n", capturedOutput)
            assertEquals(42, result)
        }
    }

    @Nested
    inner class IsolatedExecutions {

        @Test
        fun `different executions use different output streams`() {
            val ast = buildTypedAst {
                val voidType = voidType()
                val stringType = stringType()

                function(
                    name = "testPrintln",
                    returnType = voidType,
                    body = listOf(
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Test message", stringType)),
                                resultTypeIndex = voidType
                            )
                        )
                    )
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            // First execution
            val outputStream1 = ByteArrayOutputStream()
            val console1 = PrintStream(outputStream1, true, Charsets.UTF_8)
            val env = ExecutionEnvironment(program)
            env.invoke("test://test.script", "testPrintln", SimpleScriptContext(console1, null))

            // Second execution
            val outputStream2 = ByteArrayOutputStream()
            val console2 = PrintStream(outputStream2, true, Charsets.UTF_8)
            env.invoke("test://test.script", "testPrintln", SimpleScriptContext(console2, null))

            // Both should have captured their own output independently
            val output1 = outputStream1.toString(Charsets.UTF_8)
            val output2 = outputStream2.toString(Charsets.UTF_8)

            assertEquals("Test message\n", output1)
            assertEquals("Test message\n", output2)
        }
    }

    @Nested
    inner class SpecialCharacters {

        @Test
        fun `println captures special characters correctly`() {
            val ast = buildTypedAst {
                val voidType = voidType()
                val stringType = stringType()

                function(
                    name = "testSpecialChars",
                    returnType = voidType,
                    body = listOf(
                        TypedExpressionStatement(
                            expression = functionCall(
                                name = "println",
                                overload = "",
                                arguments = listOf(stringLiteral("Tab:\there", stringType)),
                                resultTypeIndex = voidType
                            )
                        )
                    )
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            val outputStream = ByteArrayOutputStream()
            val customConsole = PrintStream(outputStream, true, Charsets.UTF_8)
            val env = ExecutionEnvironment(program)

            env.invoke("test://test.script", "testSpecialChars", SimpleScriptContext(customConsole, null))

            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertTrue(capturedOutput.contains("Tab:\there"))
        }
    }

    @Nested
    inner class LambdaPrintln {

        @Test
        fun `println works inside forEach lambda`() {
            val ast = buildTypedAst {
                val voidType = voidType()
                val stringType = stringType()
                val listType = listType()
                val consumerType = addType(consumer(ClassTypeRef("builtin", "string", false)))

                function(
                    name = "test",
                    returnType = voidType,
                    body = listOf(
                        // val items = listOf("a", "b", "c")
                        varDecl("items", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("a", stringType),
                                    stringLiteral("b", stringType),
                                    stringLiteral("c", stringType)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        // items.forEach((it) => println(it))
                        exprStmt(
                            memberCall(
                                expression = identifier("items", listType, 3),
                                member = "forEach",
                                overload = "",
                                arguments = listOf(
                                    lambdaExpr(
                                        parameters = listOf("it"),
                                        body = listOf(
                                            TypedExpressionStatement(
                                                expression = functionCall(
                                                    name = "println",
                                                    overload = "",
                                                    arguments = listOf(identifier("it", stringType, 4)),
                                                    resultTypeIndex = voidType
                                                )
                                            )
                                        ),
                                        lambdaTypeIndex = consumerType
                                    )
                                ),
                                resultTypeIndex = voidType
                            )
                        )
                    )
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            val program = compiler.compile(input)

            val outputStream = ByteArrayOutputStream()
            val ctx = SimpleScriptContext(PrintStream(outputStream, true, Charsets.UTF_8), null)
            val env = ExecutionEnvironment(program)
            env.invoke("test://test.script", "test", ctx)

            val output = outputStream.toString(Charsets.UTF_8)
            assertEquals("a\nb\nc\n", output)
        }
    }
}
