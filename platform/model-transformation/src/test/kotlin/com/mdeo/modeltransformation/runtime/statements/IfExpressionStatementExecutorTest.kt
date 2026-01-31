package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedElseIfBranch
import com.mdeo.modeltransformation.ast.statements.TypedIfExpressionStatement
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

class IfExpressionStatementExecutorTest {

    private lateinit var executor: IfExpressionStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = IfExpressionStatementExecutor()
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
        fun `canExecute returns true for TypedIfExpressionStatement`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                thenBlock = emptyList(),
                elseIfBranches = emptyList()
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
    inner class TrueConditionTests {

        @Test
        fun `executes thenBlock when condition is true`() {
            graph.addVertex("House")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.matchedNodes.size)
            assertTrue(result.context.hasInstance("house"))
        }

        @Test
        fun `skips elseBlock when condition is true`() {
            graph.addVertex("House")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList(),
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
            assertTrue(result.context.hasInstance("house"))
            assertFalse(result.context.hasInstance("room"))
        }
    }

    @Nested
    inner class FalseConditionTests {

        @Test
        fun `executes elseBlock when condition is false`() {
            graph.addVertex("Room")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList(),
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
        fun `returns Success with unchanged context when no else block`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList(),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertFalse(result.context.hasInstance("house"))
        }
    }

    @Nested
    inner class ElseIfBranchTests {

        @Test
        fun `executes first matching elseIfBranch`() {
            graph.addVertex("Room")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                thenBlock = emptyList(),
                elseIfBranches = listOf(
                    TypedElseIfBranch(
                        condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                        block = listOf(
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
                ),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertTrue(result.context.hasInstance("room"))
        }

        @Test
        fun `skips elseIfBranches when main condition is true`() {
            graph.addVertex("House")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = listOf(
                    TypedElseIfBranch(
                        condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                        block = listOf(
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
                ),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertTrue(result.context.hasInstance("house"))
            assertFalse(result.context.hasInstance("room"))
        }

        @Test
        fun `checks elseIfBranches in order`() {
            graph.addVertex("House")
            graph.addVertex("Room")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                thenBlock = emptyList(),
                elseIfBranches = listOf(
                    TypedElseIfBranch(
                        condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
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
                    ),
                    TypedElseIfBranch(
                        condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                        block = listOf(
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
                ),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertFalse(result.context.hasInstance("house"))
            assertTrue(result.context.hasInstance("room"))
        }
    }

    @Nested
    inner class TruthyValueTests {

        @Test
        fun `non-zero integer is truthy`() {
            graph.addVertex("House")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedIntLiteralExpression(value = "42", evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertTrue(result.context.hasInstance("house"))
        }

        @Test
        fun `zero is falsy`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedIntLiteralExpression(value = "0", evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList(),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertFalse(result.context.hasInstance("house"))
        }

        @Test
        fun `non-empty string is truthy`() {
            graph.addVertex("House")
            
            val statement = TypedIfExpressionStatement(
                condition = TypedStringLiteralExpression(value = "hello", evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertTrue(result.context.hasInstance("house"))
        }

        @Test
        fun `empty string is falsy`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedStringLiteralExpression(value = "", evalType = 0),
                thenBlock = listOf(
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
                ),
                elseIfBranches = emptyList(),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertFalse(result.context.hasInstance("house"))
        }
    }

    @Nested
    inner class BlockResultPropagationTests {

        @Test
        fun `propagates Failure from thenBlock`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
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
                ),
                elseIfBranches = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isFailure())
        }

        @Test
        fun `propagates Stopped from thenBlock`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                thenBlock = listOf(
                    TypedStopStatement(keyword = "stop")
                ),
                elseIfBranches = emptyList()
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertEquals("stop", result.keyword)
        }

        @Test
        fun `propagates Failure from elseBlock`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                thenBlock = emptyList(),
                elseIfBranches = emptyList(),
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

        @Test
        fun `propagates Failure from elseIfBranch`() {
            val statement = TypedIfExpressionStatement(
                condition = TypedBooleanLiteralExpression(value = false, evalType = 0),
                thenBlock = emptyList(),
                elseIfBranches = listOf(
                    TypedElseIfBranch(
                        condition = TypedBooleanLiteralExpression(value = true, evalType = 0),
                        block = listOf(
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
                ),
                elseBlock = null
            )
            
            val result = executor.execute(statement, context, engine)
            
            assertTrue(result.isFailure())
        }
    }
}
