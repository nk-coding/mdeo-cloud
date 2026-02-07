package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedLongLiteralExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongLiteralCompilerTest {

    private lateinit var compiler: LongLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        compiler = LongLiteralCompiler()
        graph = TinkerGraph.open()
        context = TraversalCompilationContext(
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
        fun `returns true for TypedLongLiteralExpression`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "42")
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
        fun `compiles positive long to constant traversal`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "42")

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(42L, result.constantValue)
        }

        @Test
        fun `compiles negative long to constant traversal`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "-123")

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(-123L, result.constantValue)
        }

        @Test
        fun `compiles zero to constant traversal`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "0")

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(0L, result.constantValue)
        }

        @Test
        fun `traversal produces correct value`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "9999999999")

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(9999999999L, value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "77")
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertTrue(result.isConstant)
            assertEquals(77L, value)
        }

        @Test
        fun `compiles max long value`() {
            val expression = TypedLongLiteralExpression(
                evalType = 0,
                value = Long.MAX_VALUE.toString()
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(Long.MAX_VALUE, result.constantValue)
        }

        @Test
        fun `compiles min long value`() {
            val expression = TypedLongLiteralExpression(
                evalType = 0,
                value = Long.MIN_VALUE.toString()
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(Long.MIN_VALUE, result.constantValue)
        }
    }
}
