package com.mdeo.modeltransformation.compiler.expressions.traversal

import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.expressions.ListLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.IntLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.LongLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.DoubleLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.FloatLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.StringLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.BooleanLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.NullLiteralCompiler
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListLiteralCompilerTest {

    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var compiler: ListLiteralCompiler
    private lateinit var graph: TinkerGraph
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        // Create a minimal registry for testing - only literal compilers, no identifier compiler
        registry = ExpressionCompilerRegistry().registerAll(
            IntLiteralCompiler(),
            LongLiteralCompiler(),
            DoubleLiteralCompiler(),
            FloatLiteralCompiler(),
            StringLiteralCompiler(),
            BooleanLiteralCompiler(),
            NullLiteralCompiler()
        )
        compiler = ListLiteralCompiler(registry)
        registry.register(compiler) // Add list compiler after instantiation
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
        fun `returns true for TypedListLiteralExpression`() {
            val expression = TypedListLiteralExpression(evalType = 0, elements = emptyList())
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
        fun `compiles empty list to constant traversal`() {
            val expression = TypedListLiteralExpression(evalType = 0, elements = emptyList())

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(emptyList<Any?>(), result.constantValue)
        }

        @Test
        fun `compiles list of integers to constant traversal`() {
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedIntLiteralExpression(evalType = 0, value = "1"),
                    TypedIntLiteralExpression(evalType = 0, value = "2"),
                    TypedIntLiteralExpression(evalType = 0, value = "3")
                )
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(listOf(1, 2, 3), result.constantValue)
        }

        @Test
        fun `compiles list of strings to constant traversal`() {
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedStringLiteralExpression(evalType = 0, value = "a"),
                    TypedStringLiteralExpression(evalType = 0, value = "b"),
                    TypedStringLiteralExpression(evalType = 0, value = "c")
                )
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(listOf("a", "b", "c"), result.constantValue)
        }

        @Test
        fun `compiles mixed type list to constant traversal`() {
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedIntLiteralExpression(evalType = 0, value = "42"),
                    TypedStringLiteralExpression(evalType = 0, value = "hello")
                )
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(listOf(42, "hello"), result.constantValue)
        }

        @Test
        fun `traversal produces correct list value`() {
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedIntLiteralExpression(evalType = 0, value = "10"),
                    TypedIntLiteralExpression(evalType = 0, value = "20")
                )
            )

            val result = compiler.compile(expression, context, null)
            val value = graph.traversal().inject(1).flatMap(result.traversal).next()

            assertEquals(listOf(10, 20), value)
        }

        @Test
        fun `uses initial traversal when provided`() {
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedIntLiteralExpression(evalType = 0, value = "5")
                )
            )
            val initialTraversal = graph.traversal().inject(1)

            val result = compiler.compile(expression, context, initialTraversal)
            val value = result.traversal.next()

            assertTrue(result.isConstant)
            assertEquals(listOf(5), value)
        }

        @Test
        fun `compiles nested list to constant traversal`() {
            val innerList = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedIntLiteralExpression(evalType = 0, value = "1"),
                    TypedIntLiteralExpression(evalType = 0, value = "2")
                )
            )
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(innerList)
            )

            val result = compiler.compile(expression, context, null)

            assertTrue(result.isConstant)
            assertEquals(listOf(listOf(1, 2)), result.constantValue)
        }
    }

    @Nested
    inner class ErrorHandlingTests {

        @Test
        fun `throws exception for unsupported element type`() {
            val expression = TypedListLiteralExpression(
                evalType = 0,
                elements = listOf(
                    TypedIntLiteralExpression(evalType = 0, value = "1"),
                    TypedIdentifierExpression(evalType = 0, name = "x", scope = 0)
                )
            )

            val exception = assertThrows<CompilationException> {
                compiler.compile(expression, context, null)
            }

            assertTrue(exception.message!!.contains("No compiler registered"))
        }
    }
}
