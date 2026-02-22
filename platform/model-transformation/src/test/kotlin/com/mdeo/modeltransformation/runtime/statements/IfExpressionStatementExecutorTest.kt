package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedElseIfBranch
import com.mdeo.modeltransformation.ast.statements.TypedIfExpressionStatement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
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

class IfExpressionStatementExecutorTest {

    private lateinit var executor: IfExpressionStatementExecutor
    private lateinit var graph: TinkerGraph
    private lateinit var g: org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        executor = IfExpressionStatementExecutor()
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        // Set up type registry with __GraphNode, House and Room types
        val typeRegistry = GremlinTypeRegistry.GLOBAL
        val graphNodeType = gremlinType("__GraphNode")
            .graphProperty("address")
            .graphProperty("size")
            .build()
        typeRegistry.register(graphNodeType)
        val houseType = gremlinType("House")
            .extends("__GraphNode")
            .graphProperty("address")
            .graphProperty("size")
            .build()
        val roomType = gremlinType("Room")
            .extends("__GraphNode")
            .build()
        typeRegistry.register(houseType)
        typeRegistry.register(roomType)
        
        val registry = ExpressionCompilerRegistry.createDefaultRegistry()
        
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
            .register(executor)
        
        engine = TransformationEngine(
            traversalSource = g,
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
            expressionCompilerRegistry = registry,
            statementExecutorRegistry = statementRegistry
        )
        
        // Set up the types array for expression compilers to resolve types
        val stringType = ClassTypeRef(type = "builtin.string", isNullable = false)
        val intType = ClassTypeRef(type = "builtin.int", isNullable = false)
        val graphNodeTypeRef = ClassTypeRef(type = "__GraphNode", isNullable = false)
        val typesField = TransformationEngine::class.java.getDeclaredField("types")
        typesField.isAccessible = true
        typesField.set(engine, listOf(graphNodeTypeRef, stringType, graphNodeTypeRef, intType))
        
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

    @Nested
    inner class DynamicConditionTests {

        @Test
        fun `executes thenBlock when dynamic comparison condition is true`() {
            // Create a house with size > 100
            g.addV("House").property("size", 150).next()
            
            // First match the house to bind it to context
            val matchPattern = TypedPattern(
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
            
            val matchStatement = TypedMatchStatement(pattern = matchPattern)
            val matchResult = engine.executeStatement(matchStatement, context) as TransformationExecutionResult.Success
            // context is mutated in place, so it already has the house binding
            
            // Now test the if statement with dynamic condition: house.size > 100
            val statement = TypedIfExpressionStatement(
                condition = TypedBinaryExpression(
                    evalType = 0, // boolean
                    operator = ">",
                    left = TypedMemberAccessExpression(
                        evalType = 3, // int
                        expression = TypedIdentifierExpression(
                            evalType = 0, // graph node
                            name = "house",
                            scope = 1
                        ),
                        member = "size",
                        isNullChaining = false
                    ),
                    right = TypedIntLiteralExpression(
                        evalType = 3,
                        value = "100"
                    )
                ),
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "bigRoom",
                                        className = "Room",
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
            assertEquals(1, result.createdNodes.size)
        }

        @Test
        fun `executes elseBlock when dynamic comparison condition is false`() {
            // Create a house with size < 100
            g.addV("House").property("size", 50).next()
            
            // First match the house to bind it to context
            val matchPattern = TypedPattern(
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
            
            val matchStatement = TypedMatchStatement(pattern = matchPattern)
            val matchResult = engine.executeStatement(matchStatement, context) as TransformationExecutionResult.Success
            // context is mutated in place, so it already has the house binding
            
            // Test the if statement with dynamic condition: house.size > 100
            val statement = TypedIfExpressionStatement(
                condition = TypedBinaryExpression(
                    evalType = 0, // boolean
                    operator = ">",
                    left = TypedMemberAccessExpression(
                        evalType = 3, // int
                        expression = TypedIdentifierExpression(
                            evalType = 0, // graph node
                            name = "house",
                            scope = 1
                        ),
                        member = "size",
                        isNullChaining = false
                    ),
                    right = TypedIntLiteralExpression(
                        evalType = 3,
                        value = "100"
                    )
                ),
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "bigRoom",
                                        className = "Room",
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
                                        modifier = "create",
                                        name = "smallRoom",
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
            assertEquals(1, result.createdNodes.size)
        }

        @Test
        fun `handles equality comparison in condition`() {
            // Create a house with address "Main St"
            g.addV("House").property("address", "Main St").next()
            
            // First match the house
            val matchPattern = TypedPattern(
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
            
            val matchStatement = TypedMatchStatement(pattern = matchPattern)
            val matchResult = engine.executeStatement(matchStatement, context) as TransformationExecutionResult.Success
            // context is mutated in place, so it already has the house binding
            
            // Test: house.address == "Main St"
            val statement = TypedIfExpressionStatement(
                condition = TypedBinaryExpression(
                    evalType = 0, // boolean
                    operator = "==",
                    left = TypedMemberAccessExpression(
                        evalType = 1, // string
                        expression = TypedIdentifierExpression(
                            evalType = 0, // graph node
                            name = "house",
                            scope = 1
                        ),
                        member = "address",
                        isNullChaining = false
                    ),
                    right = TypedStringLiteralExpression(
                        evalType = 1,
                        value = "Main St"
                    )
                ),
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "matchedRoom",
                                        className = "Room",
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
            assertEquals(1, result.createdNodes.size)
        }

        @Test
        fun `handles logical AND in condition`() {
            // Create a house with size 150 and address "Main St"
            g.addV("House").property("size", 150).property("address", "Main St").next()
            
            // First match the house
            val matchPattern = TypedPattern(
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
            
            val matchStatement = TypedMatchStatement(pattern = matchPattern)
            val matchResult = engine.executeStatement(matchStatement, context) as TransformationExecutionResult.Success
            // context is mutated in place, so it already has the house binding
            
            // Test: house.size > 100 && house.address == "Main St"
            val statement = TypedIfExpressionStatement(
                condition = TypedBinaryExpression(
                    evalType = 0, // boolean
                    operator = "&&",
                    left = TypedBinaryExpression(
                        evalType = 0,
                        operator = ">",
                        left = TypedMemberAccessExpression(
                            evalType = 3, // int
                            expression = TypedIdentifierExpression(
                                evalType = 0,
                                name = "house",
                                scope = 1
                            ),
                            member = "size",
                            isNullChaining = false
                        ),
                        right = TypedIntLiteralExpression(
                            evalType = 3,
                            value = "100"
                        )
                    ),
                    right = TypedBinaryExpression(
                        evalType = 0,
                        operator = "==",
                        left = TypedMemberAccessExpression(
                            evalType = 1, // string
                            expression = TypedIdentifierExpression(
                                evalType = 0,
                                name = "house",
                                scope = 1
                            ),
                            member = "address",
                            isNullChaining = false
                        ),
                        right = TypedStringLiteralExpression(
                            evalType = 1,
                            value = "Main St"
                        )
                    )
                ),
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "qualifiedRoom",
                                        className = "Room",
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
            assertEquals(1, result.createdNodes.size)
        }

        @Test
        fun `handles logical AND with false result`() {
            // Create a house with size 50 and address "Main St" (size fails the > 100 check)
            g.addV("House").property("size", 50).property("address", "Main St").next()
            
            // First match the house
            val matchPattern = TypedPattern(
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
            
            val matchStatement = TypedMatchStatement(pattern = matchPattern)
            val matchResult = engine.executeStatement(matchStatement, context) as TransformationExecutionResult.Success
            // context is mutated in place, so it already has the house binding
            
            // Test: house.size > 100 && house.address == "Main St" (should be false)
            val statement = TypedIfExpressionStatement(
                condition = TypedBinaryExpression(
                    evalType = 0, // boolean
                    operator = "&&",
                    left = TypedBinaryExpression(
                        evalType = 0,
                        operator = ">",
                        left = TypedMemberAccessExpression(
                            evalType = 3, // int
                            expression = TypedIdentifierExpression(
                                evalType = 0,
                                name = "house",
                                scope = 1
                            ),
                            member = "size",
                            isNullChaining = false
                        ),
                        right = TypedIntLiteralExpression(
                            evalType = 3,
                            value = "100"
                        )
                    ),
                    right = TypedBinaryExpression(
                        evalType = 0,
                        operator = "==",
                        left = TypedMemberAccessExpression(
                            evalType = 1, // string
                            expression = TypedIdentifierExpression(
                                evalType = 0,
                                name = "house",
                                scope = 1
                            ),
                            member = "address",
                            isNullChaining = false
                        ),
                        right = TypedStringLiteralExpression(
                            evalType = 1,
                            value = "Main St"
                        )
                    )
                ),
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "qualifiedRoom",
                                        className = "Room",
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
                                        modifier = "create",
                                        name = "unqualifiedRoom",
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
            assertEquals(1, result.createdNodes.size)
        }
        
        /**
         * Tests that identifiers with scope > 1 are resolved correctly.
         * 
         * This verifies the fix for the MT scope special handling issue:
         * - Match statements can be nested inside if/while blocks
         * - When nested, the scope index increments (e.g., if { match { house: House } } means house is at scope 2)
         * - The IdentifierCompiler now resolves variables uniformly from variableScopes at all scope levels
         */
        @Test
        fun `handles nested scope identifiers (scope greater than 1)`() {
            // Create a house with size > 100
            g.addV("House").property("size", 200).next()
            
            // First match the house to bind it to context
            val matchPattern = TypedPattern(
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
            
            val matchStatement = TypedMatchStatement(pattern = matchPattern)
            val innerContext = context.enterScope()
            val matchResult = engine.executeStatement(matchStatement, innerContext) as TransformationExecutionResult.Success
            // context is mutated in place, so it already has the house binding
            
            // Test with scope = 2 (simulating a nested scope, e.g., if { match { ... } })
            // This verifies that the fix works for any scope level, not just scope 1
            val statement = TypedIfExpressionStatement(
                condition = TypedBinaryExpression(
                    evalType = 0, // boolean
                    operator = ">",
                    left = TypedMemberAccessExpression(
                        evalType = 3, // int
                        expression = TypedIdentifierExpression(
                            evalType = 0, // graph node
                            name = "house",
                            scope = 2  // Nested scope (simulating if { match { house } })
                        ),
                        member = "size",
                        isNullChaining = false
                    ),
                    right = TypedIntLiteralExpression(
                        evalType = 3,
                        value = "100"
                    )
                ),
                thenBlock = listOf(
                    TypedMatchStatement(
                        pattern = TypedPattern(
                            elements = listOf(
                                TypedPatternObjectInstanceElement(
                                    objectInstance = TypedPatternObjectInstance(
                                        modifier = "create",
                                        name = "largeRoom",
                                        className = "Room",
                                        properties = emptyList()
                                    )
                                )
                            )
                        )
                    )
                ),
                elseIfBranches = emptyList()
            )
            
            val result = executor.execute(statement, innerContext, engine)
            
            assertIs<TransformationExecutionResult.Success>(result)
            assertEquals(1, result.createdNodes.size)
        }
    }
}
