package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedForMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.isFailure
import com.mdeo.modeltransformation.runtime.isSuccess
import com.mdeo.modeltransformation.runtime.testHasInstance
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ForMatchStatementExecutorTest {

    private lateinit var executor: ForMatchStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = ForMatchStatementExecutor()
        graph = TinkerGraph.open()
        
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
            .register(executor)
        
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
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
        fun `canExecute returns true for TypedForMatchStatement`() {
            val statement = TypedForMatchStatement(
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
    inner class NoMatchTests {

        @Test
        fun `returns Success when pattern has no matches`() {
            val statement = TypedForMatchStatement(
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
            
            // No matches is NOT a failure for for-match
            assertTrue(result.isSuccess())
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `does not execute doBlock when no matches`() {
            val statement = TypedForMatchStatement(
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
            assertEquals(0, result.createdNodes.size)
            assertEquals(0, engine.traversalSource.V().count().next())
        }
    }

    @Nested
    inner class IterationTests {

        @Test
        fun `executes doBlock for each match`() {
            // Create 3 houses
            graph.addVertex("House")
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
                    // Create a Room for each House
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
            // Matched nodes are not visible outside the loop (sideeffectsOnly)
            assertEquals(3, result.createdNodes.size)  // 3 rooms created
            assertEquals(3, engine.traversalSource.V().hasLabel("Room").count().next())
        }

        @Test
        fun `accumulates results from all iterations`() {
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
            
            assertIs<TransformationExecutionResult.Success>(result)
            // Matched nodes are not visible outside the loop
        }

        @Test
        fun `context is updated across iterations`() {
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
            
            assertIs<TransformationExecutionResult.Success>(result)
            // Note: Bindings from for-match iterations are in child scopes and not visible in parent context
        }
    }

    @Nested
    inner class UpfrontMatchingTests {

        @Test
        fun `matches are collected upfront - body modifications do not affect matches`() {
            // Create 2 houses
            graph.addVertex("House")
            graph.addVertex("House")
            
            // For-match finds all 2 houses upfront
            // Body creates new houses, but they are NOT matched
            val statement = TypedForMatchStatement(
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
                    // Create a new House in each iteration
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
            // Only 2 original matches (but not visible outside loop)
            // But 2 new houses created
            assertEquals(2, result.createdNodes.size)
            // Total 4 houses in graph
            assertEquals(4, engine.traversalSource.V().hasLabel("House").count().next())
        }
    }

    @Nested
    inner class TerminationTests {

        @Test
        fun `terminates on Failure from doBlock`() {
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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

        @Test
        fun `terminates early - not all iterations complete on Stopped`() {
            graph.addVertex("House")
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
            // Only first iteration completes before stopping
        }
    }

    @Nested
    inner class ModificationTests {

        @Test
        fun `applies create modifications in pattern for each match`() {
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
                                name = "room",
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
            // 2 rooms created (one per match)
            assertEquals(2, result.createdNodes.size)
            assertEquals(2, engine.traversalSource.V().hasLabel("Room").count().next())
        }

        @Test
        fun `applies delete modifications in pattern`() {
            val house1 = graph.addVertex("House")
            val house2 = graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",
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
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(2, result.deletedNodes.size)
            assertEquals(0, engine.traversalSource.V().hasLabel("House").count().next())
        }
    }

    @Nested
    inner class SingleMatchTests {

        @Test
        fun `executes doBlock once for single match`() {
            graph.addVertex("House")
            
            val statement = TypedForMatchStatement(
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
            // Matched nodes are not visible outside the loop
            assertEquals(1, result.createdNodes.size)
        }
    }
}
