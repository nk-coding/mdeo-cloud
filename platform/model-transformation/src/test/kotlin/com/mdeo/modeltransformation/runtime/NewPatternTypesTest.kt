package com.mdeo.modeltransformation.runtime

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternPropertyAssignment
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLink
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.TinkerModelGraph
import com.mdeo.modeltransformation.runtime.statements.MatchStatementExecutor
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

/**
 * Tests for the two pattern types:
 * 1. Delete already named nodes (TypedPatternObjectInstanceElement with modifier="delete" and no className)
 * 2. Match previously matched nodes (TypedPatternObjectInstance with null className)
 */
class NewPatternTypesTest {
    
    private lateinit var executor: MatchStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext
    
    @BeforeEach
    fun setup() {
        executor = MatchStatementExecutor()
        graph = TinkerGraph.open()
        engine = TransformationEngine(
            modelGraph = TinkerModelGraph.wrap(graph),
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )
        context = TransformationExecutionContext.empty()
    }
    
    @AfterEach
    fun tearDown() {
        graph.close()
    }
    
    @Nested
    inner class DeleteAlreadyNamedNodeTests {
        
        @Test
        fun `delete already named node in same pattern with match`() {
            // Create two vertices
            val person1 = graph.addVertex("Person")
            person1.property("age", 25)
            val person1Id = person1.id()
            
            val person2 = graph.addVertex("Person")
            person2.property("age", 30)
            
            // Match one person and delete it in the same pattern
            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = "Person",
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "age",
                                        operator = "==",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "25")
                                    )
                                )
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",
                                name = "p",
                                className = null,
                                properties = emptyList()
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.deletedNodes.size)
            assertTrue(result.deletedNodes.contains(person1Id))
            
            // Verify only person1 is deleted
            val g = engine.traversalSource
            assertEquals(0, g.V().hasId(person1Id).count().next())
            assertEquals(1, g.V().hasLabel("Person").count().next())
        }
        
        @Test
        fun `delete multiple already named nodes`() {
            // Create two vertices with different properties to ensure they're matched separately
            val person1 = graph.addVertex("Person")
            person1.property("id", 1)
            val person1Id = person1.id()
            val person2 = graph.addVertex("Person")
            person2.property("id", 2)
            val person2Id = person2.id()
            
            // Match both and delete both
            // Add property constraints to ensure we match different vertices
            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p1",
                                className = "Person",
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "id",
                                        operator = "==",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "1")
                                    )
                                )
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p2",
                                className = "Person",
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "id",
                                        operator = "==",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "2")
                                    )
                                )
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",
                                name = "p1",
                                className = null,
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",
                                name = "p2",
                                className = null,
                                properties = emptyList()
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(2, result.deletedNodes.size)
            assertTrue(result.deletedNodes.contains(person1Id))
            assertTrue(result.deletedNodes.contains(person2Id))
            
            // Verify both are deleted
            val g = engine.traversalSource
            assertEquals(0, g.V().hasLabel("Person").count().next())
        }
    }
    
    @Nested
    inner class MatchPreviouslyMatchedNodeTests {
        
        @Test
        fun `match previously matched node with property assignment only`() {
            // Create a vertex
            val person = graph.addVertex("Person")
            person.property("age", 25)
            
            // First match to bind the node, then reference it without className to assign property
            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = "Person",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = null,  // No className - references previously matched node
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "name",
                                        operator = "=",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "100")
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            assertIs<TransformationExecutionResult.Success>(result)
            
            // Verify the property was assigned
            val g = engine.traversalSource
            val name = g.V().hasLabel("Person").values<Int>("name").next()
            assertEquals(100, name)
        }
        
        @Test
        fun `match previously matched node with property comparison only`() {
            // Create a vertex with a property value
            val person = graph.addVertex("Person")
            person.property("age", 25)
            person.property("score", 100)
            
            // Match and compare property on already matched node using null className
            val compareStatement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = "Person",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = null,  // No className - references previously matched node
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "score",
                                        operator = "==",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "100")
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val compareResult = executor.execute(compareStatement, context, engine)
            assertIs<TransformationExecutionResult.Success>(compareResult)
            
            // Try with wrong value - should not match
            val failStatement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = "Person",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = null,
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "score",
                                        operator = "==",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "999")
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val failResult = executor.execute(failStatement, context, engine)
            assertIs<TransformationExecutionResult.Failure>(failResult)
        }
        
        @Test
        fun `match previously matched node with mixed assignment and comparison`() {
            // Create a vertex
            val person = graph.addVertex("Person")
            person.property("age", 25)
            
            // Reference with both comparison (==) and assignment (=) on already matched node
            val mixedStatement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = "Person",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = null,  // No className
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "age",
                                        operator = "==",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "25")
                                    ),
                                    TypedPatternPropertyAssignment(
                                        propertyName = "name",
                                        operator = "=",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "42")
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val mixedResult = executor.execute(mixedStatement, context, engine)
            assertIs<TransformationExecutionResult.Success>(mixedResult)
            
            // Verify: comparison should match (age==25) and assignment should happen (name=42)
            val g = engine.traversalSource
            val vertex = g.V().hasLabel("Person").next()
            assertEquals(25, vertex.value<Int>("age"))
            assertEquals(42, vertex.value<Int>("name"))
        }
    }
    
    @Nested
    inner class CombinedPatternTests {
        
        @Test
        fun `match node update property and delete all in same pattern`() {
            // Create a vertex
            val person = graph.addVertex("Person")
            person.property("age", 25)
            val personId = person.id()
            
            // Match, update, and delete in one pattern
            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = "Person",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "p",
                                className = null,  // Reference previously matched node
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "score",
                                        operator = "=",
                                        value = TypedIntLiteralExpression(evalType = 0, value = "100")
                                    )
                                )
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",
                                name = "p",
                                className = null,
                                properties = emptyList()
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.deletedNodes.size)
            
            // Verify the vertex is deleted
            val g = engine.traversalSource
            assertEquals(0, g.V().hasId(personId).count().next())
        }
    }
}
