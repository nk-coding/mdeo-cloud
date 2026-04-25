package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLink
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import com.mdeo.modeltransformation.runtime.isFailure
import com.mdeo.modeltransformation.runtime.isSuccess
import com.mdeo.modeltransformation.runtime.isStopped
import com.mdeo.modeltransformation.runtime.testBindInstance
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for statement executors working together with the registry and engine.
 */
class StatementExecutorIntegrationTest {

    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var registry: StatementExecutorRegistry

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        registry = StatementExecutorRegistry.createDefaultRegistry()
        
        engine = TransformationEngine(
            modelGraph = TinkerModelGraph.wrap(graph),
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = registry
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    @Nested
    inner class MatchThenStopTests {

        @Test
        fun `match followed by stop executes correctly`() {
            graph.addVertex("House")
            
            val ast = TypedAst(
                types = emptyList(),
                metamodelPath = "test://metamodel",
                statements = listOf(
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
                    ),
                    TypedStopStatement(keyword = "stop")
                )
            )
            
            engine = TransformationEngine(
                modelGraph = TinkerModelGraph.wrap(graph),
                ast = ast,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = registry
            )
            
            val result = engine.execute()
            
            assertTrue(result.isStopped())
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertTrue(result.isNormalStop)
        }

        @Test
        fun `failed match prevents stop execution`() {
            // No vertices in graph
            
            val ast = TypedAst(
                types = emptyList(),
                metamodelPath = "test://metamodel",
                statements = listOf(
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
                    ),
                    TypedStopStatement(keyword = "stop")
                )
            )
            
            engine = TransformationEngine(
                modelGraph = TinkerModelGraph.wrap(graph),
                ast = ast,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = registry
            )
            
            val result = engine.execute()
            
            assertTrue(result.isFailure())
        }
    }

    @Nested
    inner class SequentialMatchTests {

        @Test
        fun `sequential matches accumulate bindings`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge("`rooms`_``", room)
            
            val ast = TypedAst(
                types = emptyList(),
                metamodelPath = "test://metamodel",
                statements = listOf(
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
                    ),
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
                                ),
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = null,
                                        name = "room",
                                        className = "Room",
                                        properties = emptyList()
                                    )
                                ),
                                TypedPatternLinkElement(
                                    link = TypedPatternLink(
                                        modifier = null,
                                source = TypedPatternLinkEnd(objectName = "house", propertyName = "rooms"),
                                        target = TypedPatternLinkEnd(objectName = "room")
                                    )
                                )
                            )
                        )
                    )
                )
            )
            
            engine = TransformationEngine(
                modelGraph = TinkerModelGraph.wrap(graph),
                ast = ast,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = registry
            )
            
            val result = engine.execute()
            
            assertTrue(result.isSuccess())
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `uses pre-bound instance from first match in second match`() {
            val house1 = graph.addVertex("House")
            val house2 = graph.addVertex("House")
            val room1 = graph.addVertex("Room")
            val room2 = graph.addVertex("Room")
            house1.addEdge("`rooms`_``", room1)
            house2.addEdge("`rooms`_``", room2)
            
            val context = TransformationExecutionContext.empty()
                .testBindInstance("house", house2.id())
            
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
                        ),
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = null,
                                name = "room",
                                className = "Room",
                                properties = emptyList()
                            )
                        ),
                        TypedPatternLinkElement(
                            link = TypedPatternLink(
                                modifier = null,
                                source = TypedPatternLinkEnd(objectName = "house", propertyName = "rooms"),
                                target = TypedPatternLinkEnd(objectName = "room")
                            )
                        )
                    )
                )
            )
            
            val result = engine.executeStatement(matchStatement, context)
            
            assertTrue(result.isSuccess())
            assertIs<TransformationExecutionResult.Success>(result)
            // Should match house2 -> room2 due to pre-bound house
            assertEquals(room2.id(), (context.variableScope.getVariable("room") as? VariableBinding.InstanceBinding)?.vertexId)
        }
    }

    @Nested
    inner class KillTests {

        @Test
        fun `kill terminates execution immediately`() {
            val ast = TypedAst(
                types = emptyList(),
                metamodelPath = "test://metamodel",
                statements = listOf(
                    TypedStopStatement(keyword = "kill")
                )
            )
            
            engine = TransformationEngine(
                modelGraph = TinkerModelGraph.wrap(graph),
                ast = ast,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = registry
            )
            
            val result = engine.execute()
            
            assertTrue(result.isStopped())
            assertIs<TransformationExecutionResult.Stopped>(result)
            assertTrue(result.isKill)
        }
    }

    @Nested
    inner class EmptyAstTests {

        @Test
        fun `empty ast returns success`() {
            val ast = TypedAst(
                types = emptyList(),
                metamodelPath = "test://metamodel",
                statements = emptyList()
            )
            
            engine = TransformationEngine(
                modelGraph = TinkerModelGraph.wrap(graph),
                ast = ast,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = registry
            )
            
            val result = engine.execute()
            
            assertTrue(result.isSuccess())
        }
    }
}
