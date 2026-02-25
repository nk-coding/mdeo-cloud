package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoubleLiteralCompilerTest {

    private lateinit var compiler: DoubleLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        compiler = DoubleLiteralCompiler()
        graph = TinkerGraph.open()
        context = CompilationContext(
            types = emptyList(),
            currentScope = VariableScope.empty(),
            traversalSource = graph.traversal(),
            typeRegistry = TypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    @Nested
    inner class CanCompileTests {

        @Test
        fun `returns true for TypedDoubleLiteralExpression`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "3.14")
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
        fun `compiles positive double to constant traversal`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "3.14159")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(3.14159, actualValue)
        }

        @Test
        fun `compiles negative double to constant traversal`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "-2.718")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(-2.718, actualValue)
        }

        @Test
        fun `compiles zero to constant traversal`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "0.0")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(0.0, actualValue)
        }

        @Test
        fun `traversal produces correct value`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "99.99")

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(99.99, value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "1.5")
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertEquals(1.5, value)
        }

        @Test
        fun `compiles scientific notation`() {
            val expression = TypedDoubleLiteralExpression(evalType = 0, value = "1.23e10")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(1.23e10, actualValue)
        }

        @Test
        fun `compiles max double value`() {
            val expression = TypedDoubleLiteralExpression(
                evalType = 0,
                value = Double.MAX_VALUE.toString()
            )

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(Double.MAX_VALUE, actualValue)
        }
    }
}
