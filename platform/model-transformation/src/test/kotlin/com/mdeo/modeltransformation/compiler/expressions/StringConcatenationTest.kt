package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
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
    private lateinit var context: CompilationContext

    // Type constants for use in expressions
    private val STRING_TYPE_INDEX = 0

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        
        // Set up types list with string type at index 0
        val types = listOf(
            ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
        )
        
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
    fun `concatenates two string literals`() {
        // "Kitchen" + "wtf" should result in "Kitchenwtf"
        val left = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "Kitchen")
        val right = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "wtf")
        val expr = TypedBinaryExpression(evalType = STRING_TYPE_INDEX, operator = "+", left = left, right = right)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("Kitchenwtf", value, "String concatenation should work")
    }

    @Test
    fun `concatenates empty strings`() {
        val left = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "")
        val right = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "test")
        val expr = TypedBinaryExpression(evalType = STRING_TYPE_INDEX, operator = "+", left = left, right = right)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("test", value)
    }

    @Test
    fun `concatenates multiple strings`() {
        // ("Hello" + " ") + "World"
        val hello = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "Hello")
        val space = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = " ")
        val world = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "World")
        
        val firstConcat = TypedBinaryExpression(evalType = STRING_TYPE_INDEX, operator = "+", left = hello, right = space)
        val expr = TypedBinaryExpression(evalType = STRING_TYPE_INDEX, operator = "+", left = firstConcat, right = world)

        val result = registry.compile(expr, context)

        // Execute the traversal and verify the result
        val value = executeTraversal<String>(result)
        assertEquals("Hello World", value)
    }
}
