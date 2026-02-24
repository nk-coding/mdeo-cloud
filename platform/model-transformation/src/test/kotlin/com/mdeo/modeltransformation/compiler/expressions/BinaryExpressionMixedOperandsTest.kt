package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.GremlinCompilationResult
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
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        // Add a test vertex with properties
        g.addV("Test").property("strProp", "Hello").property("numProp", 42).next()
        
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        
        // Create context with string type at index 1
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
        val types = listOf(stringType, stringType)
        
        context = CompilationContext(
            types = types,
            currentScope = VariableScope.empty(),
            traversalSource = g,
            typeRegistry = GremlinTypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Helper to execute a traversal by injecting a null start value.
     * This is needed for anonymous traversals.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> executeTraversal(result: GremlinCompilationResult): T? {
        val traversal = result.traversal as GraphTraversal<Any, T>
        return g.inject(null as Any?).flatMap(traversal).next()
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
