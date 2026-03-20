package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.TinkerModelGraph
import com.mdeo.modeltransformation.runtime.match.MatchResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests string concatenation in property assignments during model transformation execution.
 * 
 * This test reproduces the issue where string concatenation like "Kitchen" + "wtf"
 * doesn't work correctly in property assignments at runtime.
 */
class StringConcatenationRuntimeTest {
    
    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private val executor = MatchExecutor()

    // Type constants for use in expressions
    private val STRING_TYPE_INDEX = 0
    
    // Type list with string type at index 0
    private val types = listOf(
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
    )
    
    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
        
        engine = TransformationEngine(
            modelGraph = TinkerModelGraph.wrap(graph),
            ast = TypedAst(types = types, metamodelPath = "test://model", statements = emptyList()),
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )
    }
    
    @Test
    fun `string concatenation in property assignment should work`() {
        // Create a pattern that creates a Room with category="Kitchen" + "wtf"
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "room1",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "category",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = STRING_TYPE_INDEX,
                                    operator = "+",
                                    left = TypedStringLiteralExpression(
                                        evalType = STRING_TYPE_INDEX,
                                        value = "Kitchen"
                                    ),
                                    right = TypedStringLiteralExpression(
                                        evalType = STRING_TYPE_INDEX,
                                        value = "wtf"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)
        
        assertTrue(result is MatchResult.Matched, "Match should succeed")
        result as MatchResult.Matched
        
        // Get the created vertex
        val vertexId = result.instanceMappings["room1"]?.rawId
        assertNotNull(vertexId, "room1 should be mapped to a vertex")
        
        val vertex = g.V(vertexId).next()
        
        // The vertex should have the "category" property with value "Kitchenwtf"
        val categoryProperty = vertex.property<String>("category")
        assertTrue(categoryProperty.isPresent, "category property should exist")
        assertEquals("Kitchenwtf", categoryProperty.value(), "category property should be 'Kitchenwtf'")
    }
    
    @Test
    fun `empty string concatenation should work`() {
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "room1",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "category",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = STRING_TYPE_INDEX,
                                    operator = "+",
                                    left = TypedStringLiteralExpression(
                                        evalType = STRING_TYPE_INDEX,
                                        value = ""
                                    ),
                                    right = TypedStringLiteralExpression(
                                        evalType = STRING_TYPE_INDEX,
                                        value = "test"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)
        
        assertTrue(result is MatchResult.Matched)
        result as MatchResult.Matched
        
        val vertexId = result.instanceMappings["room1"]?.rawId
        assertNotNull(vertexId)
        
        val vertex = g.V(vertexId).next()
        val categoryProperty = vertex.property<String>("category")
        assertTrue(categoryProperty.isPresent)
        assertEquals("test", categoryProperty.value())
    }
    
    @Test
    fun `multiple string concatenations should work`() {
        // ("Hello" + " ") + "World"
        val firstConcat = TypedBinaryExpression(
            evalType = STRING_TYPE_INDEX,
            operator = "+",
            left = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "Hello"),
            right = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = " ")
        )
        
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "room1",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "category",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = STRING_TYPE_INDEX,
                                    operator = "+",
                                    left = firstConcat,
                                    right = TypedStringLiteralExpression(evalType = STRING_TYPE_INDEX, value = "World")
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)
        
        assertTrue(result is MatchResult.Matched)
        result as MatchResult.Matched
        
        val vertexId = result.instanceMappings["room1"]?.rawId
        assertNotNull(vertexId)
        
        val vertex = g.V(vertexId).next()
        val categoryProperty = vertex.property<String>("category")
        assertTrue(categoryProperty.isPresent)
        assertEquals("Hello World", categoryProperty.value())
    }
}
