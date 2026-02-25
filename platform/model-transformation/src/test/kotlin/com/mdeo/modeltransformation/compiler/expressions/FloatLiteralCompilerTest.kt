package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedFloatLiteralExpression
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

class FloatLiteralCompilerTest {

    private lateinit var compiler: FloatLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        compiler = FloatLiteralCompiler()
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
        fun `returns true for TypedFloatLiteralExpression`() {
            val expression = TypedFloatLiteralExpression(evalType = 0, value = "3.14")
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
        fun `compiles positive float to constant traversal`() {
            val expression = TypedFloatLiteralExpression(evalType = 0, value = "3.14")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(3.14f, actualValue)
        }

        @Test
        fun `compiles negative float to constant traversal`() {
            val expression = TypedFloatLiteralExpression(evalType = 0, value = "-2.5")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(-2.5f, actualValue)
        }

        @Test
        fun `compiles zero to constant traversal`() {
            val expression = TypedFloatLiteralExpression(evalType = 0, value = "0.0")

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(0.0f, actualValue)
        }

        @Test
        fun `traversal produces correct value`() {
            val expression = TypedFloatLiteralExpression(evalType = 0, value = "99.5")

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(99.5f, value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedFloatLiteralExpression(evalType = 0, value = "1.5")
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertEquals(1.5f, value)
        }

        @Test
        fun `compiles max float value`() {
            val expression = TypedFloatLiteralExpression(
                evalType = 0,
                value = Float.MAX_VALUE.toString()
            )

            val result = compiler.compile(expression, context, null)

            val actualValue = graph.traversal().inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(Float.MAX_VALUE, actualValue)
        }
    }
}
