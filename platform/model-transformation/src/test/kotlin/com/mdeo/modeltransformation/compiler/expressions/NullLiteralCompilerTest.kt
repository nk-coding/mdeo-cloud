package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NullLiteralCompilerTest {

    private lateinit var compiler: NullLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        compiler = NullLiteralCompiler()
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
        fun `returns true for TypedNullLiteralExpression`() {
            val expression = TypedNullLiteralExpression(evalType = 0)
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
        fun `compiles null to constant traversal`() {
            val expression = TypedNullLiteralExpression(evalType = 0)

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertNull(actualValue)
        }

        @Test
        fun `traversal produces null value`() {
            val expression = TypedNullLiteralExpression(evalType = 0)

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertNull(value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedNullLiteralExpression(evalType = 0)
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertNull(value)
        }
    }
}
