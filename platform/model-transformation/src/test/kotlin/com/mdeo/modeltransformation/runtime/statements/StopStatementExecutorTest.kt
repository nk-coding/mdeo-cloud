package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.isStopped
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StopStatementExecutorTest {

    private lateinit var executor: StopStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = StopStatementExecutor()
        graph = TinkerGraph.open()
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            ast = TypedAst(types = emptyList(), metamodelUri = "test://model", statements = emptyList()),
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
    inner class CanExecuteTests {

        @Test
        fun `canExecute returns true for TypedStopStatement`() {
            val statement = TypedStopStatement(keyword = "stop")
            
            assertTrue(executor.canExecute(statement))
        }

        @Test
        fun `canExecute returns false for other statement types`() {
            val statement = TypedMatchStatement(
                pattern = TypedPattern(elements = emptyList())
            )
            
            assertFalse(executor.canExecute(statement))
        }
    }

    @Nested
    inner class StopExecutionTests {

        @Test
        fun `stop keyword returns Stopped result`() {
            val statement = TypedStopStatement(keyword = "stop")
            
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isStopped())
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("stop", result.keyword)
        }

        @Test
        fun `stop result isNormalStop is true`() {
            val statement = TypedStopStatement(keyword = "stop")
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertTrue(result.isNormalStop)
            assertFalse(result.isKill)
        }

        @Test
        fun `stop preserves execution context`() {
            val contextWithBindings = context
                .bindVariable("x", 42)
                .bindInstance("node", "test-id")
            
            val statement = TypedStopStatement(keyword = "stop")
            
            val result = executor.execute(statement, contextWithBindings, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals(42, result.context.lookupVariable("x"))
            assertEquals("test-id", result.context.lookupInstance("node"))
        }
    }

    @Nested
    inner class KillExecutionTests {

        @Test
        fun `kill keyword returns Stopped result`() {
            val statement = TypedStopStatement(keyword = "kill")
            
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isStopped())
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("kill", result.keyword)
        }

        @Test
        fun `kill result isKill is true`() {
            val statement = TypedStopStatement(keyword = "kill")
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertTrue(result.isKill)
            assertFalse(result.isNormalStop)
        }

        @Test
        fun `kill preserves execution context`() {
            val contextWithBindings = context
                .bindVariable("y", "value")
                .bindInstance("other", 123L)
            
            val statement = TypedStopStatement(keyword = "kill")
            
            val result = executor.execute(statement, contextWithBindings, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("value", result.context.lookupVariable("y"))
            assertEquals(123L, result.context.lookupInstance("other"))
        }
    }

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `handles unknown keyword gracefully`() {
            val statement = TypedStopStatement(keyword = "unknown")
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("unknown", result.keyword)
            assertFalse(result.isNormalStop)
            assertFalse(result.isKill)
        }

        @Test
        fun `handles empty context`() {
            val statement = TypedStopStatement(keyword = "stop")
            
            val result = executor.execute(statement, TransformationExecutionContext.empty(), engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
        }
    }
}
