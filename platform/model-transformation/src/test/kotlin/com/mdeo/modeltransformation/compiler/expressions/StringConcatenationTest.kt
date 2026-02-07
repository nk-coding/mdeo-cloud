package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for string concatenation using the + operator.
 * 
 * This test demonstrates that string concatenation should work with the + operator,
 * similar to how numeric addition works with the + operator.
 */
@DisplayName("String Concatenation Tests")
class StringConcatenationTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        context = TraversalCompilationContext(
            types = emptyList(),
            traversalSource = g,
            typeRegistry = GremlinTypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Helper to execute a traversal.
     * For anonymous traversals, we inject a starting value.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> executeTraversal(result: TraversalCompilationResult<*, *>): T? {
        // Try to execute the traversal directly first
        val traversal = result.traversal as GraphTraversal<Any, T>
        
        // If the traversal needs a starting point, inject one
        return try {
            if (traversal.hasNext()) {
                traversal.next()
            } else {
                // If no results, try injecting a null value as starting point
                val injectedTraversal = g.inject(null as Any?).flatMap(result.traversal as GraphTraversal<Any, T>)
                if (injectedTraversal.hasNext()) injectedTraversal.next() else null
            }
        } catch (e: Exception) {
            // If direct execution fails, try with injected start
            val injectedTraversal = g.inject(null as Any?).flatMap(result.traversal as GraphTraversal<Any, T>)
            if (injectedTraversal.hasNext()) injectedTraversal.next() else null
        }
    }

    @Test
    fun `concatenates two string literals`() {
        // "Kitchen" + "wtf" should result in "Kitchenwtf"
        val left = TypedStringLiteralExpression(evalType = 0, value = "Kitchen")
        val right = TypedStringLiteralExpression(evalType = 0, value = "wtf")
        val expr = TypedBinaryExpression(evalType = 0, operator = "+", left = left, right = right)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("Kitchenwtf", value, "String concatenation should work")
    }

    @Test
    fun `concatenates empty strings`() {
        val left = TypedStringLiteralExpression(evalType = 0, value = "")
        val right = TypedStringLiteralExpression(evalType = 0, value = "test")
        val expr = TypedBinaryExpression(evalType = 0, operator = "+", left = left, right = right)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("test", value)
    }

    @Test
    fun `concatenates multiple strings`() {
        // ("Hello" + " ") + "World"
        val hello = TypedStringLiteralExpression(evalType = 0, value = "Hello")
        val space = TypedStringLiteralExpression(evalType = 0, value = " ")
        val world = TypedStringLiteralExpression(evalType = 0, value = "World")
        
        val firstConcat = TypedBinaryExpression(evalType = 0, operator = "+", left = hello, right = space)
        val expr = TypedBinaryExpression(evalType = 0, operator = "+", left = firstConcat, right = world)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("Hello World", value)
    }
}
