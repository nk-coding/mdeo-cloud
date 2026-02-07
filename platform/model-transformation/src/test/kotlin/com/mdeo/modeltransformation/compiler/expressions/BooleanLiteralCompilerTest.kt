package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
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

class BooleanLiteralCompilerTest {

    private lateinit var compiler: BooleanLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        compiler = BooleanLiteralCompiler()
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
        fun `returns true for TypedBooleanLiteralExpression`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = true)
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
        fun `compiles true to constant traversal`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = true)

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `compiles false to constant traversal`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = false)

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(false, result.constantValue)
        }

        @Test
        fun `traversal produces true value`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = true)

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(true, value)
        }

        @Test
        fun `traversal produces false value`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = false)

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(false, value)
        }

        @Test
        fun `uses initial traversal when provided with true`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = true)
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertTrue(result.isConstant)
            assertEquals(true, value)
        }

        @Test
        fun `uses initial traversal when provided with false`() {
            val expression = TypedBooleanLiteralExpression(evalType = 0, value = false)
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertTrue(result.isConstant)
            assertEquals(false, value)
        }
    }
}
