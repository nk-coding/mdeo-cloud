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

class IntLiteralCompilerTest {

    private lateinit var compiler: IntLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        compiler = IntLiteralCompiler()
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
        fun `returns true for TypedIntLiteralExpression`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "42")
            assertTrue(compiler.canCompile(expression))
        }

        @Test
        fun `returns false for TypedLongLiteralExpression`() {
            val expression = TypedLongLiteralExpression(evalType = 0, value = "42")
            assertFalse(compiler.canCompile(expression))
        }
    }

    @Nested
    inner class CompileTests {

        @Test
        fun `compiles positive integer to constant traversal`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "42")

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(42, result.constantValue)
        }

        @Test
        fun `compiles negative integer to constant traversal`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "-123")

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(-123, result.constantValue)
        }

        @Test
        fun `compiles zero to constant traversal`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "0")

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(0, result.constantValue)
        }

        @Test
        fun `traversal produces correct value`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "99")

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(99, value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedIntLiteralExpression(evalType = 0, value = "77")
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertTrue(result.isConstant)
            assertEquals(77, value)
        }

        @Test
        fun `compiles max int value`() {
            val expression = TypedIntLiteralExpression(
                evalType = 0,
                value = Int.MAX_VALUE.toString()
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(Int.MAX_VALUE, result.constantValue)
        }

        @Test
        fun `compiles min int value`() {
            val expression = TypedIntLiteralExpression(
                evalType = 0,
                value = Int.MIN_VALUE.toString()
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(Int.MIN_VALUE, result.constantValue)
        }
    }
}
