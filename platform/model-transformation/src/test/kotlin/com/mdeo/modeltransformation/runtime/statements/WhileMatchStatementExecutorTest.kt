package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedWhileMatchStatement
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

class WhileMatchStatementExecutorTest {

    private lateinit var executor: WhileMatchStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = WhileMatchStatementExecutor()
        graph = TinkerGraph.open()
        
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
            .register(executor)
        
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()), // Dummy AST
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
        fun `canExecute returns true for TypedWhileMatchStatement`() {
            val statement = TypedWhileMatchStatement(
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
        fun `returns Success when pattern does not match initially`() {
            val statement = TypedWhileMatchStatement(
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
            
            // IMPORTANT: No match is NOT a failure - unlike standalone match
            assertTrue(result.isSuccess())
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `no match is NOT a failure - terminates loop normally`() {
            val statement = TypedWhileMatchStatement(
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
        }
    }

    @Nested
    inner class IterationTests {

        @Test
        fun `executes doBlock while pattern matches`() {
            // Create 3 houses
            graph.addVertex("House")
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedWhileMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",  // Delete to ensure termination
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
            assertEquals(3, result.deletedNodes.size)
            assertEquals(0, engine.traversalSource.V().count().next())
        }

        @Test
        fun `accumulates results from multiple iterations`() {
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedWhileMatchStatement(
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
                doBlock = listOf(
                    // Create a Room for each House deleted
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
            assertEquals(2, result.deletedNodes.size)
            assertEquals(2, result.createdNodes.size)
            assertEquals(2, engine.traversalSource.V().hasLabel("Room").count().next())
        }

    }

    @Nested
    inner class TerminationTests {

        @Test
        fun `terminates on Failure from doBlock`() {
            graph.addVertex("House")
            
            val statement = TypedWhileMatchStatement(
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
            
            val statement = TypedWhileMatchStatement(
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
        fun `terminates on kill from doBlock`() {
            graph.addVertex("House")
            
            val statement = TypedWhileMatchStatement(
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
                    TypedStopStatement(keyword = "kill")
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("kill", result.keyword)
        }
    }

    @Nested
    inner class ModificationTests {

        @Test
        fun `applies create modifications in pattern`() {
            graph.addVertex("House")
            
            val statement = TypedWhileMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "delete",  // Delete to terminate loop
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
            assertEquals(1, result.createdNodes.size)
        }
    }
}
