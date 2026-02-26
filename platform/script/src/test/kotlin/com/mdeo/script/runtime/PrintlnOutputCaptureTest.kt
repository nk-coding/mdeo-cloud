package com.mdeo.script.runtime

import com.mdeo.expression.ast.statements.TypedExpressionStatement
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.compiler.buildTypedAst
import com.mdeo.script.compiler.functionCall
import com.mdeo.script.compiler.intLiteral
import com.mdeo.script.compiler.returnStmt
import com.mdeo.script.compiler.stringLiteral
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for println output capture through ExecutionContext.
 *
 * Verifies that println writes to the execution environment's console stream
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
            ExecutionContext.withContext(customConsole, null) {
                env.invoke("test://test.script", "testPrintln")
            }

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

            ExecutionContext.withContext(customConsole, null) {
                env.invoke("test://test.script", "testEmptyPrintln")
            }

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

            ExecutionContext.withContext(customConsole, null) {
                env.invoke("test://test.script", "testMultiplePrintln")
            }

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

            val result = ExecutionContext.withContext(customConsole, null) {
                env.invoke("test://test.script", "testPrintlnWithReturn")
            }

            // Both output and return value should work
            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertEquals("Computing result...\n", capturedOutput)
            assertEquals(42, result)
        }
    }

    @Nested
    inner class DefaultConsoleStream {

        @Test
        fun `default console uses System_out when no context is set`() {
            val ast = buildTypedAst {
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(returnStmt(intLiteral(1, intType)))
                )
            }

            val input = CompilationInput(mapOf("test://test.script" to ast))
            compiler.compile(input)

            // When no context is set, getConsole() should fall back to System.out
            ExecutionContext.clearConsole()
            assertEquals(System.out, ExecutionContext.getConsole())
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
            ExecutionContext.withContext(console1, null) {
                env.invoke("test://test.script", "testPrintln")
            }

            // Second execution
            val outputStream2 = ByteArrayOutputStream()
            val console2 = PrintStream(outputStream2, true, Charsets.UTF_8)
            ExecutionContext.withContext(console2, null) {
                env.invoke("test://test.script", "testPrintln")
            }

            // Both should have captured their own output independently
            val output1 = outputStream1.toString(Charsets.UTF_8)
            val output2 = outputStream2.toString(Charsets.UTF_8)

            assertEquals("Test message\n", output1)
            assertEquals("Test message\n", output2)
        }
    }

    @Nested
    inner class ExecutionContextBehavior {

        @Test
        fun `ExecutionContext_withContext properly restores previous context`() {
            val stream1 = PrintStream(ByteArrayOutputStream())
            val stream2 = PrintStream(ByteArrayOutputStream())

            ExecutionContext.setConsole(stream1)
            assertEquals(stream1, ExecutionContext.getConsole())

            ExecutionContext.withContext(stream2, null) {
                assertEquals(stream2, ExecutionContext.getConsole())
            }

            // Should restore to stream1
            assertEquals(stream1, ExecutionContext.getConsole())

            // Clean up
            ExecutionContext.clearConsole()
        }

        @Test
        fun `ExecutionContext returns System_out when not set`() {
            ExecutionContext.clearConsole()
            assertEquals(System.out, ExecutionContext.getConsole())
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

            ExecutionContext.withContext(customConsole, null) {
                env.invoke("test://test.script", "testSpecialChars")
            }

            val capturedOutput = outputStream.toString(Charsets.UTF_8)
            assertTrue(capturedOutput.contains("Tab:\there"))
        }
    }
}
