package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedIfMatchStatement
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

class IfMatchStatementExecutorTest {

    private lateinit var executor: IfMatchStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = IfMatchStatementExecutor()
        graph = TinkerGraph.open()
        
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
            .register(executor)
        
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            ast = TypedAst(types = emptyList(), metamodelUri = "test://model", statements = emptyList()), // Dummy AST
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
        fun `canExecute returns true for TypedIfMatchStatement`() {
            val statement = TypedIfMatchStatement(
                pattern = TypedPattern(elements = emptyList()),
                thenBlock = emptyList()
            )
            
            assertTrue(executor.canExecute(statement))
        }

        @Test
        fun `canExecute returns false for other statement types`() {
            val statement = TypedStopStatement(keyword = "stop")
            
            assertFalse(executor.canExecute(statement))
        }

        @Test
        fun `canExecute returns false for TypedMatchStatement`() {
            val statement = TypedMatchStatement(
                pattern = TypedPattern(elements = emptyList())
            )
            
            assertFalse(executor.canExecute(statement))
        }
    }

    @Nested
    inner class PatternMatchesTests {

        @Test
        fun `executes thenBlock when pattern matches`() {
            graph.addVertex("House")
            graph.addVertex("Room")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = null,
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
            assertTrue(result.context.hasInstance("house"))
            assertTrue(result.context.hasInstance("room"))
        }

        @Test
        fun `binds matched instances in context`() {
            val vertex = graph.addVertex("House")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(vertex.id(), result.context.lookupInstance("house"))
        }

        @Test
        fun `tracks matched nodes`() {
            graph.addVertex("House")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.matchedNodes.size)
        }

        @Test
        fun `applies create modifications on match`() {
            graph.addVertex("House")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.createdNodes.size)
            assertTrue(result.context.hasInstance("room"))
            assertEquals(1, engine.traversalSource.V().hasLabel("Room").count().next())
        }
    }

    @Nested
    inner class PatternDoesNotMatchTests {

        @Test
        fun `executes elseBlock when pattern does not match`() {
            graph.addVertex("Room")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = emptyList(),
                elseBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = null,
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
            assertFalse(result.context.hasInstance("house"))
            assertTrue(result.context.hasInstance("room"))
        }

        @Test
        fun `returns Success with unchanged context when no elseBlock`() {
            val statement = TypedIfMatchStatement(
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
                thenBlock = emptyList(),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            // IMPORTANT: This is NOT a failure - no match is OK in if-match context
            assertIs<TransformationExecutionResult.Success>(result)
            assertFalse(result.context.hasInstance("house"))
        }

        @Test
        fun `no match is NOT a failure - unlike standalone match`() {
            // Create a standalone match that would fail
            val matchStatement = TypedMatchStatement(
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
                )
            )
            
            val matchResult = engine.executeStatement(matchStatement, context)
            assertTrue(matchResult.isFailure(), "Standalone match should fail when no match")
            
            // Create an if-match with the same pattern
            val ifMatchStatement = TypedIfMatchStatement(
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
                thenBlock = emptyList(),
                elseBlock = null
            )
            
            val ifMatchResult = executor.execute(ifMatchStatement, context, engine)
            assertTrue(ifMatchResult.isSuccess(), "If-match should NOT fail when no match")
        }
    }

    @Nested
    inner class BlockResultPropagationTests {

        @Test
        fun `propagates Failure from thenBlock`() {
            graph.addVertex("House")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = listOf(
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
        fun `propagates Stopped from thenBlock`() {
            graph.addVertex("House")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = listOf(
                    TypedStopStatement(keyword = "stop")
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
        }

        @Test
        fun `propagates Failure from elseBlock`() {
            val statement = TypedIfMatchStatement(
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
                thenBlock = emptyList(),
                elseBlock = listOf(
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
    }

    @Nested
    inner class ResultMergingTests {

        @Test
        fun `merges match result with block result`() {
            graph.addVertex("House")
            graph.addVertex("Room")
            
            val statement = TypedIfMatchStatement(
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
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = null,
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
            assertEquals(2, result.matchedNodes.size)  // house + room
        }
    }
}
