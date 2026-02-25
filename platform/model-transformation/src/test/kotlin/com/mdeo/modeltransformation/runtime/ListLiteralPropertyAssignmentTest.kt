package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import com.mdeo.modeltransformation.runtime.match.MatchResult
import com.mdeo.modeltransformation.runtime.match.MatchExecutor
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * Tests for list literal property assignments in create blocks.
 * 
 * This test reproduces the bug where list literals like [10, 20]
 * create a nonsensical string value instead of a proper list of integers.
 * 
 * Bug: When using a model transformation with values = [10, 20], it creates
 * a node with a nonsensical values entry (a string) instead of a proper list
 * of integers [10, 20].
 */
class ListLiteralPropertyAssignmentTest {
    
    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private val executor = MatchExecutor()
    
    @BeforeEach
    fun setup() {
        // Configure TinkerGraph to allow multi-valued properties  
        val conf = org.apache.commons.configuration2.BaseConfiguration()
        // Allow list cardinality for the "values" property
        conf.setProperty("gremlin.tinkergraph.vertexPropertyCardinality.values", "list")
        graph = TinkerGraph.open(conf)
        g = graph.traversal()
        
        // Create a local type registry with GLOBAL as parent to avoid polluting the global registry
        // This allows us to add test-specific types without affecting other tests
        val typeRegistry = TypeRegistry(parent = TypeRegistry.GLOBAL)
        
        // Register __GraphNode with the properties that will be accessed in tests
        val graphNodeType = gremlinType("builtin", "__GraphNode")
            .graphProperty("values")
            .build()
        typeRegistry.register(graphNodeType)
        
        val nodeType = gremlinType("builtin", "Node")
            .extends("builtin", "__GraphNode")
            .graphProperty("values")
            .build()
        typeRegistry.register(nodeType)
        
        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
        
        engine = TransformationEngine(
            traversalSource = g,
            ast = TypedAst(types = emptyList(), metamodelPath = "./metamodel.mm", statements = emptyList()),
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )
        
        // Set up the types array that would normally come from a TypedAst
        // We need this for the expression compilers to resolve types
        // Based on the formal AST definition provided:
        // Type 0: void
        // Type 1: builtin.string
        // Type 2: builtin.double
        // Type 3: builtin.boolean
        // Type 4: Any (nullable)
        // Type 5: builtin.List<builtin.int>
        // Type 6: builtin.int
        val voidType = ClassTypeRef(`package` = "builtin", type = "void", isNullable = false)
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
        val doubleType = ClassTypeRef(`package` = "builtin", type = "double", isNullable = false)
        val booleanType = ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false)
        val anyType = ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true)
        val listIntType = ClassTypeRef(`package` = "builtin", type = "List", isNullable = false)
        val intType = ClassTypeRef(`package` = "builtin", type = "int", isNullable = false)
        val graphNodeTypeRef = ClassTypeRef(`package` = "builtin", type = "__GraphNode", isNullable = false)
        
        // Use reflection to set the types field since it has a private setter
        val typesField = TransformationEngine::class.java.getDeclaredField("types")
        typesField.isAccessible = true
        typesField.set(engine, listOf(voidType, stringType, doubleType, booleanType, anyType, listIntType, intType))
    }
    
    @AfterEach
    fun tearDown() {
        graph.close()
    }
    
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `list literal with integers should create proper list property not a string`() {
        // Create a pattern that creates a Node with values = [10, 20]
        // This mimics the transformation:
        // match {
        //     create node : Node {
        //         values = [10, 20]
        //     }
        // }
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "node",
                        className = "Node",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "values",
                                operator = "=",
                                value = TypedListLiteralExpression(
                                    evalType = 5, // builtin.List<builtin.int>
                                    elements = listOf(
                                        TypedIntLiteralExpression(
                                            evalType = 6, // builtin.int
                                            value = "10"
                                        ),
                                        TypedIntLiteralExpression(
                                            evalType = 6, // builtin.int
                                            value = "20"
                                        )
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
        
        // Get the created node vertex
        val nodeId = result.instanceMappings["node"]
        assertNotNull(nodeId, "node should be mapped to a vertex")
        
        val node = g.V(nodeId).next()
        
        // Verify the "values" property exists and retrieve all values using Cardinality.list
        // With Cardinality.list, multiple property values are stored separately
        val valuesIterator = node.values<Any>("values")
        val valuesList = mutableListOf<Any>()
        valuesIterator.forEachRemaining { valuesList.add(it) }
        
        // Assert we got values
        assertTrue(valuesList.isNotEmpty(), "values property should have elements")
        
        // Assert the list size
        assertEquals(2, valuesList.size, "values list should have 2 elements")
        
        // Convert to integers for comparison (handle both Int and Long)
        val intValues = valuesList.map { 
            when (it) {
                is Number -> it.toInt()
                else -> fail("List element should be a Number, but was ${it.javaClass.name}: $it")
            }
        }
        
        assertEquals(listOf(10, 20), intValues, "values list should contain [10, 20]")
    }
    
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `empty list literal should create proper empty list property`() {
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "node",
                        className = "Node",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "values",
                                operator = "=",
                                value = TypedListLiteralExpression(
                                    evalType = 5, // builtin.List<builtin.int>
                                    elements = emptyList()
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
        
        val nodeId = result.instanceMappings["node"]
        assertNotNull(nodeId, "node should be mapped to a vertex")
        
        val node = g.V(nodeId).next()
        
        // With Cardinality.list and an empty list, no properties should be added
        val valuesIterator = node.values<Any>("values")
        val valuesList = mutableListOf<Any>()
        valuesIterator.forEachRemaining { valuesList.add(it) }
        
        assertEquals(0, valuesList.size, "values list should be empty")
    }
    
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `list literal with single element should create proper single-element list property`() {
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "node",
                        className = "Node",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "values",
                                operator = "=",
                                value = TypedListLiteralExpression(
                                    evalType = 5, // builtin.List<builtin.int>
                                    elements = listOf(
                                        TypedIntLiteralExpression(
                                            evalType = 6, // builtin.int
                                            value = "42"
                                        )
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
        
        val nodeId = result.instanceMappings["node"]
        assertNotNull(nodeId, "node should be mapped to a vertex")
        
        val node = g.V(nodeId).next()
        
        // Retrieve all values using Cardinality.list semantics
        val valuesIterator = node.values<Any>("values")
        val valuesList = mutableListOf<Any>()
        valuesIterator.forEachRemaining { valuesList.add(it) }
        
        assertEquals(1, valuesList.size, "values list should have 1 element")
        
        val intValue = when (val elem = valuesList[0]) {
            is Number -> elem.toInt()
            else -> fail("List element should be a Number, but was ${elem.javaClass.name}: $elem")
        }
        
        assertEquals(42, intValue, "values list should contain [42]")
    }
    
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `list literal with computed expressions should evaluate binary operations`() {
        // Create a pattern that creates a Node with values = [100, 200, 10 * 20, 100 - 20]
        // This mimics the transformation:
        // match {
        //     create node : Node {
        //         values = [100, 200, 10 * 20, 100 - 20]
        //     }
        // }
        // Expected result: [100, 200, 200, 80]
        val pattern = TypedPattern(
            elements = listOf(
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "node",
                        className = "Node",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "values",
                                operator = "=",
                                value = TypedListLiteralExpression(
                                    evalType = 5, // builtin.List<builtin.int>
                                    elements = listOf(
                                        // Simple literal: 100
                                        TypedIntLiteralExpression(
                                            evalType = 6, // builtin.int
                                            value = "100"
                                        ),
                                        // Simple literal: 200
                                        TypedIntLiteralExpression(
                                            evalType = 6, // builtin.int
                                            value = "200"
                                        ),
                                        // Computed: 10 * 20 = 200
                                        TypedBinaryExpression(
                                            evalType = 6, // builtin.int
                                            operator = "*",
                                            left = TypedIntLiteralExpression(
                                                evalType = 6,
                                                value = "10"
                                            ),
                                            right = TypedIntLiteralExpression(
                                                evalType = 6,
                                                value = "20"
                                            )
                                        ),
                                        // Computed: 100 - 20 = 80
                                        TypedBinaryExpression(
                                            evalType = 6, // builtin.int
                                            operator = "-",
                                            left = TypedIntLiteralExpression(
                                                evalType = 6,
                                                value = "100"
                                            ),
                                            right = TypedIntLiteralExpression(
                                                evalType = 6,
                                                value = "20"
                                            )
                                        )
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
        
        // Get the created node vertex
        val nodeId = result.instanceMappings["node"]
        assertNotNull(nodeId, "node should be mapped to a vertex")
        
        val node = g.V(nodeId).next()
        
        // Retrieve all values using Cardinality.list semantics
        val valuesIterator = node.values<Any>("values")
        val valuesList = mutableListOf<Any>()
        valuesIterator.forEachRemaining { valuesList.add(it) }
        
        // Assert the list size
        assertEquals(4, valuesList.size, "values list should have 4 elements")
        
        // Convert to integers for comparison (handle both Int and Long)
        val intValues = valuesList.map { 
            when (it) {
                is Number -> it.toInt()
                else -> fail("List element should be a Number, but was ${it.javaClass.name}: $it")
            }
        }
        
        // Expected: [100, 200, 200, 80]
        // - First element: literal 100
        // - Second element: literal 200
        // - Third element: 10 * 20 = 200
        // - Fourth element: 100 - 20 = 80
        assertEquals(
            listOf(100, 200, 200, 80),
            intValues,
            "values list should contain [100, 200, 200, 80] where 3rd (10*20) and 4th (100-20) are computed"
        )
    }
}
