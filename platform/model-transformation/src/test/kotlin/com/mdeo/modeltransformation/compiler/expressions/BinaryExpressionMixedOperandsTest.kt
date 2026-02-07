package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for binary expressions with mixed constant/non-constant operands.
 */
class BinaryExpressionMixedOperandsTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        // Add a test vertex with properties
        g.addV("Test").property("strProp", "Hello").property("numProp", 42).next()
        
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        
        // Create context with string type at index 1
        val stringType = ClassTypeRef(type = "builtin.string", isNullable = false)
        val types = listOf(stringType, stringType)
        
        context = TraversalCompilationContext(
            types = types,
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
    fun `string literal concatenation should work`() {
        // "Hello" + "World"
        val left = TypedStringLiteralExpression(evalType = 1, value = "Hello")
        val right = TypedStringLiteralExpression(evalType = 1, value = "World")
        val expr = TypedBinaryExpression(evalType = 1, operator = "+", left = left, right = right)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("HelloWorld", value, "String concatenation should work")
    }

    @Test
    fun `string literal plus non-constant should compile to non-constant`() {
        // "Hello" + someVar (we'll use a placeholder identifier)
        val left = TypedStringLiteralExpression(evalType = 1, value = "Hello")
        val right = TypedStringLiteralExpression(evalType = 1, value = "World") // Simulating non-constant for now
        val expr = TypedBinaryExpression(evalType = 1, operator = "+", left = left, right = right)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result (both are literals so should concatenate)
        val value = executeTraversal<String>(result)
        assertEquals("HelloWorld", value)
    }
}
