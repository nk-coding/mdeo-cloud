package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedWhileExpressionStatement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.isFailure
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WhileExpressionStatementExecutorTest {

    private lateinit var executor: WhileExpressionStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = WhileExpressionStatementExecutor()
        graph = TinkerGraph.open()
        
        val registry = ExpressionCompilerRegistry.createDefaultRegistry()
        
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
            .register(executor)
        
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            expressionCompilerRegistry = registry,
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
        fun `canExecute returns true for TypedWhileExpressionStatement`() {
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                block = emptyList()
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
    inner class FalseConditionTests {

        @Test
        fun `does not execute block when condition is initially false`() {
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                block = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "house",
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
            assertEquals(0, result.createdNodes.size)
            
            // Verify no vertex was created
            assertEquals(0, engine.traversalSource.V().count().next())
        }

        @Test
        fun `zero is falsy - loop does not execute`() {
            val statement = TypedWhileExpressionStatement(
                condition = TypedIntLiteralExpression(value = "0", evalType = 0),
                block = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "house",
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
            assertEquals(0, result.createdNodes.size)
        }
    }

    @Nested
    inner class TerminationTests {

        @Test
        fun `terminates on Failure from block`() {
            // First iteration will fail because no House exists to match
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                block = listOf(
                    TypedMatchStatement(
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
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isFailure())
        }

        @Test
        fun `terminates on Stopped from block`() {
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                block = listOf(
                    TypedStopStatement(keyword = "stop")
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("stop", result.keyword)
        }

        @Test
        fun `terminates on kill from block`() {
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                block = listOf(
                    TypedStopStatement(keyword = "kill")
                )
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("kill", result.keyword)
        }
    }

    @Nested
    inner class ContextPropagationTests {

        @Test
        fun `context is updated across iterations`() {
            // Pre-create a House
            graph.addVertex("House")
            
            // This will match once, bind 'house', then fail on second iteration
            // (because we'd need to match a different house)
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                block = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "delete",  // Delete to terminate loop after first iteration
                                        name = "house",
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
            
            // First iteration succeeds (deletes house), second iteration fails (no house)
            assertTrue(result.isFailure())
        }
    }

    @Nested
    inner class ResultAccumulationTests {

        @Test
        fun `accumulates results from successful iterations before failure`() {
            // Create two houses that will be matched/deleted
            graph.addVertex("House")
            graph.addVertex("House")
            
            val statement = TypedWhileExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                block = listOf(
                    TypedMatchStatement(
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
                        )
                    )
                )
            )
            
            // Will delete both houses then fail on third iteration
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isFailure())
            // Both houses should be deleted
            assertEquals(0, engine.traversalSource.V().count().next())
        }
    }
}
