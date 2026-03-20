package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.TinkerModelGraph
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.isStopped
import com.mdeo.modeltransformation.runtime.testBindVariable
import com.mdeo.modeltransformation.runtime.testBindInstance
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
                .testBindVariable("x", 42)
                .testBindInstance("node", "test-id")
            
            val statement = TypedStopStatement(keyword = "stop")
            
            val result = executor.execute(statement, contextWithBindings, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals(42, (contextWithBindings.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
            assertEquals("test-id", (contextWithBindings.variableScope.getVariable("node") as? VariableBinding.InstanceBinding)?.vertexId)
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
                .testBindVariable("y", "value")
                .testBindInstance("other", 123L)
            
            val statement = TypedStopStatement(keyword = "kill")
            
            val result = executor.execute(statement, contextWithBindings, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("value", (contextWithBindings.variableScope.getVariable("y") as? VariableBinding.ValueBinding)?.value)
            assertEquals(123L, (contextWithBindings.variableScope.getVariable("other") as? VariableBinding.InstanceBinding)?.vertexId)
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
