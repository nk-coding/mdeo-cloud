package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph
import com.mdeo.modeltransformation.runtime.match.MatchResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests the handling of the "name" property in metamodel classes.
 * 
 * This test demonstrates and verifies the fix for the issue where model elements
 * with a "name" property in their metamodel get that property overridden by the
 * internal name tracking mechanism.
 */
class NamePropertyConflictTest {
    
    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private val executor = MatchExecutor()
    
    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
        
        engine = TransformationEngine(
            modelGraph = TinkerModelGraph.wrap(graph),
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()), // Dummy AST
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )
        
        // Set up the types array that would normally come from a TypedAst
        // Type 0: builtin.string (used in expressions)
        // Type 1: builtin.int (used in expressions)
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
        val intType = ClassTypeRef(`package` = "builtin", type = "int", isNullable = false)
        
        // Use reflection to set the types field since it has a private setter
        val typesField = TransformationEngine::class.java.getDeclaredField("types")
        typesField.isAccessible = true
        typesField.set(engine, listOf(stringType, intType))
    }
    
    @Test
    fun `created node with name property should preserve the property value in result`() {
        // Create a pattern that creates a Person with name="Alice"
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "person1",
                        className = "Person",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "name",
                                operator = "=",
                                value = TypedStringLiteralExpression(
                                    evalType = 0,
                                    value = "Alice"
                                )
                            ),
                            TypedPatternPropertyAssignment(
                                propertyName = "age",
                                operator = "=",
                                value = TypedIntLiteralExpression(
                                    evalType = 0,
                                    value = "30"
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
        val vertexId = result.instanceMappings["person1"]?.rawId
        assertNotNull(vertexId, "person1 should be mapped to a vertex")
        
        val vertex = g.V(vertexId).next()
        
        // The vertex should have the "name" property with value "Alice"
        val nameProperty = vertex.property<String>("name")
        assertTrue(nameProperty.isPresent, "name property should exist")
        assertEquals("Alice", nameProperty.value(), "name property should be 'Alice'")
        
        // The vertex should also have the age property
        val ageProperty = vertex.property<Int>("age")
        assertTrue(ageProperty.isPresent, "age property should exist")
        assertEquals(30, ageProperty.value(), "age property should be 30")
    }
    
    @Test
    fun `matched node from input model should preserve name property when modified`() {
        // Setup: Create a person in the input model with name="Bob"
        val inputVertex = g.addV("Person")
            .property("name", "Bob")
            .property("age", 25)
            .next()
        
        // Pattern: Match the person and update age
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "personX",
                        className = "Person",
                        modifier = null,
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "age",
                                operator = "=",
                                value = TypedIntLiteralExpression(
                                    evalType = 0,
                                    value = "26"
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
        
        // Verify the name property is still "Bob"
        val vertex = g.V(inputVertex).next()
        val nameProperty = vertex.property<String>("name")
        assertTrue(nameProperty.isPresent, "name property should exist")
        assertEquals("Bob", nameProperty.value(), "name property should still be 'Bob'")
        
        val ageProperty = vertex.property<Int>("age")
        assertEquals(26, ageProperty.value(), "age property should be updated to 26")
    }
    
    @Test
    fun `multiple nodes with same class should get unique instance names`() {
        // Create a pattern that creates two persons
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "p1",
                        className = "Person",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "name",
                                operator = "=",
                                value = TypedStringLiteralExpression(
                                    evalType = 0,
                                    value = "Charlie"
                                )
                            )
                        )
                    )
                ),
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "p2",
                        className = "Person",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "name",
                                operator = "=",
                                value = TypedStringLiteralExpression(
                                    evalType = 0,
                                    value = "Charlie"
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
        
        // Both persons should have "Charlie" as their name property
        val p1Id = result.instanceMappings["p1"]?.rawId
        val p2Id = result.instanceMappings["p2"]?.rawId
        
        assertNotNull(p1Id)
        assertNotNull(p2Id)
        assertNotEquals(p1Id, p2Id, "p1 and p2 should be different vertices")
        
        val v1 = g.V(p1Id).next()
        val v2 = g.V(p2Id).next()
        
        assertEquals("Charlie", v1.property<String>("name").value())
        assertEquals("Charlie", v2.property<String>("name").value())
    }
}
