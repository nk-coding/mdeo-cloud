package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringLiteralCompilerTest {

    private lateinit var compiler: StringLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        compiler = StringLiteralCompiler()
        graph = TinkerGraph.open()
        context = CompilationContext(
            types = emptyList(),
            traversalSource = graph.traversal(),
            typeRegistry = GremlinTypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    @Nested
    inner class CanCompileTests {

        @Test
        fun `returns true for TypedStringLiteralExpression`() {
            val expression = TypedStringLiteralExpression(evalType = 0, value = "hello")
            assertTrue(compiler.canCompile(expression))
        }

        @Test
        fun `returns false for TypedIntLiteralExpression`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "42")
            assertFalse(compiler.canCompile(expression))
        }
    }

    @Nested
    inner class CompileTests {

        @Test
        fun `compiles string to constant traversal`() {
            val expression = TypedStringLiteralExpression(evalType = 0, value = "hello world")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals("hello world", actualValue)
        }

        @Test
        fun `compiles empty string to constant traversal`() {
            val expression = TypedStringLiteralExpression(evalType = 0, value = "")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals("", actualValue)
        }

        @Test
        fun `compiles string with special characters`() {
            val expression = TypedStringLiteralExpression(
                evalType = 0,
                value = "hello\nworld\ttab"
            )

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals("hello\nworld\ttab", actualValue)
        }

        @Test
        fun `traversal produces correct value`() {
            val expression = TypedStringLiteralExpression(evalType = 0, value = "test string")

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals("test string", value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedStringLiteralExpression(evalType = 0, value = "with initial")
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertEquals("with initial", value)
        }

        @Test
        fun `compiles unicode string`() {
            val expression = TypedStringLiteralExpression(evalType = 0, value = "こんにちは世界")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals("こんにちは世界", actualValue)
        }
    }
}
