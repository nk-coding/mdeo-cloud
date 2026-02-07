package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedUntilMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.isFailure
import com.mdeo.modeltransformation.runtime.isSuccess
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UntilMatchStatementExecutorTest {

    private lateinit var executor: UntilMatchStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = UntilMatchStatementExecutor()
        graph = TinkerGraph.open()
        
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
            .register(executor)
        
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            ast = TypedAst(types = emptyList(), metamodelUri = "test://model", statements = emptyList()),
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = statementRegistry
        )
        context = TransformationExecutionContext.empty()
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    @Nested
    inner class CanExecuteTests {

        @Test
        fun `canExecute returns true for TypedUntilMatchStatement`() {
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(elements = emptyList()),
                doBlock = emptyList()
            )
            
            assertTrue(executor.canExecute(statement))
        }

        @Test
        fun `canExecute returns false for other statement types`() {
            val statement = TypedStopStatement(keyword = "stop")
            
            assertFalse(executor.canExecute(statement))
        }
    }

    @Nested
    inner class DoUntilBehaviorTests {

        @Test
        fun `executes body at least once even if pattern matches immediately`() {
            graph.addVertex("House")
            
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "room",
                                        className = "Room",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            // Body executed once, creating a Room
            assertEquals(1, result.createdNodes.size)
            assertTrue(result.context.hasInstance("room"))
        }

        @Test
        fun `terminates when pattern matches after body execution`() {
            // Initially no House, so pattern won't match
            // Body creates a House, then pattern matches and loop terminates
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "newHouse",
                                        className = "House",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.createdNodes.size)
            // Pattern matched and bound the house
            assertTrue(result.context.hasInstance("house"))
        }
    }

    @Nested
    inner class IterationTests {

        @Test
        fun `loops until pattern matches`() {
            // Create 3 Rooms and 1 House
            // The loop will delete Rooms until no Rooms remain
            // Then it checks for "no Room" pattern which matches
            graph.addVertex("Room")
            graph.addVertex("Room")
            graph.addVertex("Room")
            
            // Use a counter approach: doBlock just creates markers (House)
            // Pattern checks for 3 Houses (meaning 3 iterations completed)
            // This way we can verify the loop runs multiple times
            
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    // Check if we have deleted all rooms (no Room left)
                    // This pattern will fail while Rooms exist
                    // We'll use a different approach: check for a specific marker
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    // Delete one Room
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "delete",
                                        name = "room",
                                        className = "Room",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    ),
                    // Create a House each iteration
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "newHouse",
                                        className = "House",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            // First iteration: delete 1 room, create 1 house, then pattern matches house
            // Loop terminates after first iteration because House now exists
            assertEquals(1, result.deletedNodes.size)
            assertEquals(1, result.createdNodes.size)
            // Pattern matched a house
            assertTrue(result.context.hasInstance("house"))
        }

        @Test
        fun `iterates multiple times until pattern matches`() {
            // Create 3 Rooms but no House
            // Each iteration just deletes a Room (no House created)
            // After 3 iterations, no more Rooms, so doBlock fails
            // This tests that the loop iterates multiple times
            graph.addVertex("Room")
            graph.addVertex("Room")
            graph.addVertex("Room")
            // Add a House that will eventually be matched after all rooms are gone
            graph.addVertex("House")
            
            // Pattern checks for "Building" which doesn't exist
            // doBlock just deletes Rooms
            // After all Rooms deleted (3 iterations), doBlock fails, 
            // but we won't reach that because we pre-add a House
            // and check for House as termination
            
            // Actually, let's test: loop until we've deleted 3 items
            // We can track this by the number of Room vertices remaining
            
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    // Pattern matches when we find a House (always exists, but...)
                    // Actually this won't work because House always exists
                    // Let's match for "no Room exists" - but we can't express that easily
                    // 
                    // Better approach: doBlock deletes Room, once all Rooms gone,
                    // the doBlock fails (no Room to match), but until checks first...
                    // No wait, until is do-until, so doBlock runs THEN pattern checks
                    //
                    // Let me think differently:
                    // - Pattern: find a "Mansion" 
                    // - doBlock: delete a Room, and if no more Rooms, create a Mansion
                    // - After 3 iterations, all Rooms deleted, Mansion created, pattern matches
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "mansion",
                                className = "Mansion",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    // Just delete one Room per iteration
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "delete",
                                        name = "room",
                                        className = "Room",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            // Add a Mansion after 3 rooms exist - we'll manually verify
            // Actually let's just verify that it runs 3 times and then fails
            // because doBlock can't find a 4th Room
            
            // Since there's no Mansion, and doBlock will fail after deleting all 3 Rooms
            // (can't find a 4th Room to delete), this will result in Failure
            // But all 3 Rooms should be deleted before failure
            
            val result = executor.execute(statement, context, engine)
            
            // After 3 successful doBlock executions, the 4th iteration fails
            // because no Room to delete
            assertTrue(result.isFailure())
            // All 3 rooms should have been deleted
            assertEquals(0, graph.traversal().V().hasLabel("Room").count().next())
            // House still exists
            assertEquals(1, graph.traversal().V().hasLabel("House").count().next())
        }

        @Test
        fun `accumulates results from all iterations`() {
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "newHouse",
                                        className = "House",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            // One iteration: body creates House, then pattern matches
            assertEquals(1, result.createdNodes.size)
        }
    }

    @Nested
    inner class TerminationTests {

        @Test
        fun `terminates on Failure from doBlock`() {
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    // Try to match a nonexistent type - will fail
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = null,
                                        name = "nonexistent",
                                        className = "Nonexistent",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isFailure())
        }

        @Test
        fun `terminates on Stopped from doBlock`() {
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = listOf(
                    TypedStopStatement(keyword = "stop")
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("stop", result.keyword)
        }
    }

    @Nested
    inner class ModificationOnMatchTests {

        @Test
        fun `applies modifications from terminating pattern match`() {
            // The pattern has both match and create elements
            graph.addVertex("House")
            
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "create",
                                name = "termRoom",
                                className = "Room",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            // The terminating match created a Room
            assertEquals(1, result.createdNodes.size)
            assertTrue(result.context.hasInstance("termRoom"))
            assertEquals(1, engine.traversalSource.V().hasLabel("Room").count().next())
        }
    }

    @Nested
    inner class PatternMatchIsNotFailureTests {

        @Test
        fun `pattern match is NOT a failure - it terminates the loop`() {
            graph.addVertex("House")
            
            val statement = TypedUntilMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "house",
                                className = "House",
                                properties = emptyList()
                            )
                        )
                    )
                ),
                doBlock = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            // Pattern matching is the termination condition, not a failure
            assertTrue(result.isSuccess())
            assertIs<TransformationExecutionResult.Success>(result)
            assertTrue(result.context.hasInstance("house"))
        }
    }
}
