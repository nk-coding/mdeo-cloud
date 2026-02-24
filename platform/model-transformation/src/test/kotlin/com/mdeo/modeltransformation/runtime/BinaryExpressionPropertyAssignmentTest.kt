package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import com.mdeo.modeltransformation.runtime.match.MatchResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for binary expression property assignments in create blocks.
 * 
 * This test reproduces the bug where binary expressions like "house.address + 'test'"
 * don't work correctly in property assignments.
 */
class BinaryExpressionPropertyAssignmentTest {
    
    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private val executor = MatchExecutor()
    
    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        // Set up a type registry with __GraphNode, House and Room types
        val typeRegistry = GremlinTypeRegistry.GLOBAL
        
        // Register __GraphNode with the properties that will be accessed in tests
        val graphNodeType = gremlinType("builtin", "__GraphNode")
            .graphProperty("address")
            .graphProperty("value")
            .build()
        typeRegistry.register(graphNodeType)
        
        val houseType = gremlinType("builtin", "House")
            .extends("builtin", "__GraphNode")
            .graphProperty("address")
            .graphProperty("value")
            .build()
        val roomType = gremlinType("builtin", "Room")
            .extends("builtin", "__GraphNode")
            .graphProperty("category")
            .graphProperty("value")
            .build()
        typeRegistry.register(houseType)
        typeRegistry.register(roomType)
        
        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
        
        engine = TransformationEngine(
            traversalSource = g,
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()), // Dummy AST
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )
        
        // Set up the types array that would normally come from a TypedAst
        // We need this for the expression compilers to resolve types
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
        val intType = ClassTypeRef(`package` = "builtin", type = "int", isNullable = false)
        val graphNodeTypeRef = ClassTypeRef(`package` = "builtin", type = "__GraphNode", isNullable = false)
        
        // Use reflection to set the types field since it has a private setter
        val typesField = TransformationEngine::class.java.getDeclaredField("types")
        typesField.isAccessible = true
        typesField.set(engine, listOf(graphNodeTypeRef, stringType, graphNodeTypeRef, intType))
    }
    
    @AfterEach
    fun tearDown() {
        graph.close()
    }
    
    @Test
    fun `string concatenation with member access and literal should work in property assignment`() {
        // Create a House with an address property
        g.addV("House").property("address", "123 Main St").next()
        
        // Create a pattern that matches the house and creates a room with category = house.address + "test"
        val pattern = TypedPattern(
            elements = listOf(
                // Match: house: House {}
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "house",
                        className = "House",
                        modifier = null,
                        properties = emptyList()
                    )
                ),
                // Create: newRoom: Room { category = house.address + "test" }
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "newRoom",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "category",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = 1, // string
                                    operator = "+",
                                    left = TypedMemberAccessExpression(
                                        evalType = 1, // string
                                        expression = TypedIdentifierExpression(
                                            evalType = 0, // graph node
                                            name = "house",
                                            scope = 1 // MT scope level (matches real typed ASTs)
                                        ),
                                        member = "address",
                                        isNullChaining = false
                                    ),
                                    right = TypedStringLiteralExpression(
                                        evalType = 1,
                                        value = "test"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)
        
        assertTrue(result is MatchResult.Matched, "Match should succeed")
        result as MatchResult.Matched
        
        // Get the created room vertex
        val roomId = result.instanceMappings["newRoom"]
        assertNotNull(roomId, "newRoom should be mapped to a vertex")
        
        val room = g.V(roomId).next()
        
        // The room should have the "category" property set to house.address + "test"
        val categoryProperty = room.property<String>("category")
        assertTrue(categoryProperty.isPresent, "category property should exist")
        assertEquals("123 Main Sttest", categoryProperty.value(), "category should be set to house.address + 'test'")
    }
    
    @Test
    fun `arithmetic binary expression with member access should work in property assignment`() {
        // Create a House with a value property
        g.addV("House").property("value", 100).next()
        
        // Create a pattern that matches the house and creates a room with value = house.value * 10
        val pattern = TypedPattern(
            elements = listOf(
                // Match: house: House {}
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "house",
                        className = "House",
                        modifier = null,
                        properties = emptyList()
                    )
                ),
                // Create: newRoom: Room { value = house.value * 10 }
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "newRoom",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "value",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = 3, // int
                                    operator = "*",
                                    left = TypedMemberAccessExpression(
                                        evalType = 3, // int
                                        expression = TypedIdentifierExpression(
                                            evalType = 0, // graph node
                                            name = "house",
                                            scope = 1 // MT scope level (matches real typed ASTs)
                                        ),
                                        member = "value",
                                        isNullChaining = false
                                    ),
                                    right = TypedIntLiteralExpression(
                                        evalType = 3,
                                        value = "10"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)
        
        assertTrue(result is MatchResult.Matched, "Match should succeed")
        result as MatchResult.Matched
        
        // Get the created room vertex
        val roomId = result.instanceMappings["newRoom"]
        assertNotNull(roomId, "newRoom should be mapped to a vertex")
        
        val room = g.V(roomId).next()
        
        // The room should have the "value" property set to house.value * 10
        val valueProperty = room.property<Int>("value")
        assertTrue(valueProperty.isPresent, "value property should exist")
        assertEquals(1000, valueProperty.value(), "value should be set to house.value * 10")
    }
    
    @Test
    fun `addition binary expression with two literals should work in property assignment`() {
        // Test the simple case: room.value = 100 * 10 (two literals)
        val pattern = TypedPattern(
            elements = listOf(
                // Create: newRoom: Room { value = 100 * 10 }
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "newRoom",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "value",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = 3, // int
                                    operator = "*",
                                    left = TypedIntLiteralExpression(
                                        evalType = 3,
                                        value = "100"
                                    ),
                                    right = TypedIntLiteralExpression(
                                        evalType = 3,
                                        value = "10"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)
        
        assertTrue(result is MatchResult.Matched, "Match should succeed")
        result as MatchResult.Matched
        
        // Get the created room vertex
        val roomId = result.instanceMappings["newRoom"]
        assertNotNull(roomId, "newRoom should be mapped to a vertex")
        
        val room = g.V(roomId).next()
        
        // The room should have the "value" property set to 1000
        val valueProperty = room.property<Int>("value")
        assertTrue(valueProperty.isPresent, "value property should exist")
        assertEquals(1000, valueProperty.value(), "value should be set to 100 * 10")
    }
}
