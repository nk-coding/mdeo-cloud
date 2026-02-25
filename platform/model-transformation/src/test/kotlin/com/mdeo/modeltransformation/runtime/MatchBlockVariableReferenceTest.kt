package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.AssociationData
import com.mdeo.expression.ast.types.AssociationEndData
import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.MultiplicityData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import java.util.concurrent.TimeUnit

/**
 * Test that reproduces the variable support issue from Prompt.md.
 *
 * The issue:
 * - Variables declared in match blocks cannot be referenced in the same match block
 *   when used in property comparisons.
 * - A match statement like:
 *   ```
 *   match {
 *       var x = "example"
 *       house : House { address == x }
 *   }
 *   ```
 *   fails because the variable `x` cannot be referenced in the property comparison.
 *
 * Root cause:
 * - The MatchExecutor handles variables incorrectly
 * - Variables should be evaluated in the match part as another match clause: as("_").TheActualEvaluationTraversal().as("theVariableName")
 * - Variables need to be registered in the scope beforehand using a LabelBinding
 * - After match execution, they should be replaced with ValueBinding for subsequent usage
 *
 * Expected behavior:
 * - The match should correctly find houses where address equals the variable value
 * - The variable should be evaluated and bound as a label in the Gremlin traversal
 * - The property comparison should reference the bound label
 */
class MatchBlockVariableReferenceTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    // Type indices matching the transformation structure from Prompt.md
    // Index 0: void
    // Index 1: builtin.string
    // Index 2: builtin.double
    // Index 3: builtin.boolean
    // Index 4: Any? (nullable)
    // Index 5: builtin.List<Room>
    // Index 6: builtin.int
    // Index 7: class.House
    private val types: List<ReturnType> = listOf<ReturnType>(
        VoidType(),                                                                   // 0
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false),                    // 1
        ClassTypeRef(`package` = "builtin", type = "double", isNullable = false),                    // 2
        ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false),                   // 3
        ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true),                                // 4
        ClassTypeRef(`package` = "builtin", type = "List", isNullable = false, typeArgs = mapOf(
            "T" to ClassTypeRef(`package` = "class", type = "Room", isNullable = false, typeArgs = emptyMap())
        )),                                                                          // 5
        ClassTypeRef(`package` = "builtin", type = "int", isNullable = false),                      // 6
        ClassTypeRef(`package` = "class", type = "House", isNullable = false)     // 7
    )

    private val metamodelData = MetamodelData(
        classes = listOf(
            ClassData(
                name = "House",
                isAbstract = false,
                extends = emptyList(),
                properties = listOf(
                    PropertyData(name = "address", primitiveType = "string", multiplicity = MultiplicityData.single())
                )
            ),
            ClassData(
                name = "Room",
                isAbstract = false,
                extends = emptyList(),
                properties = listOf(
                    PropertyData(name = "category", primitiveType = "string", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "value", primitiveType = "int", multiplicity = MultiplicityData.single())
                )
            )
        ),
        associations = listOf(
            AssociationData(
                source = AssociationEndData(className = "House", name = "rooms", multiplicity = MultiplicityData.many()),
                operator = "<>->",
                target = AssociationEndData(className = "Room", name = "house", multiplicity = MultiplicityData.single())
            )
        )
    )

    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()

        // Register metamodel types in the global type registry
        val typeRegistry = TypeRegistry.GLOBAL

        val houseType = gremlinType("class", "House")
            .graphProperty("address")
            .build()
        typeRegistry.register(houseType)

        val roomType = gremlinType("class", "Room")
            .graphProperty("category")
            .graphProperty("value")
            .build()
        typeRegistry.register(roomType)

        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()

        // Create AST with types
        val ast = TypedAst(
            types = types,
            metamodelPath = "./metamodel.mm",
            statements = emptyList()
        )

        engine = TransformationEngine(
            traversalSource = g,
            ast = ast,
            metamodelData = metamodelData,
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )

        context = TransformationExecutionContext.empty()
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Test that reproduces the exact issue from Prompt.md:
     *
     * ```
     * match {
     *     var x = "example"
     *     house : House {
     *         address == x
     *     }
     * }
     * ```
     *
     * Expected: Should match houses where address equals "example"
     * Actual: Fails because variable `x` cannot be referenced in the same match block
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `match block should reference variable declared in same block for property comparison`() {
        // Create test data: two houses, one matching and one non-matching
        g.addV("House").property("address", "example").next()
        g.addV("House").property("address", "different").next()

        // Build the match statement structure exactly as shown in Prompt.md
        // This mirrors the JSON AST structure from Prompt.md lines 150-176
        
        val matchStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // var x = "example"
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "x",
                            value = TypedStringLiteralExpression(
                                evalType = 1, // string
                                value = "example"
                            )
                        )
                    ),
                    // house : House { address == x }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "==",
                                    value = TypedIdentifierExpression(
                                        evalType = 1, // string (since x evaluates to a string)
                                        name = "x",
                                        scope = 1  // Same scope as the match block
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        // Execute the match statement
        // Currently this will fail because the variable cannot be referenced
        // in the same match block where it's declared
        val result = engine.executeStatement(matchStatement, context)

        // Verify that the execution succeeded
        assertIs<TransformationExecutionResult.Success>(result)
        
        // The match should have found exactly one house (the one with address="example")
        // We can verify by checking the graph
        val houses = g.V().hasLabel("House").has("address", "example").toList()
        assertEquals(1, houses.size, "Should find exactly one house matching the variable value")
    }

    /**
     * Test with a numeric variable to ensure type handling works correctly
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `match block should reference numeric variable in same block for property comparison`() {
        // Create test data: rooms with different values
        g.addV("Room").property("category", "living").property("value", 42).next()
        g.addV("Room").property("category", "bedroom").property("value", 100).next()
        g.addV("Room").property("category", "kitchen").property("value", 42).next()

        val matchStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // var targetValue = 42
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "targetValue",
                            value = TypedIntLiteralExpression(
                                evalType = 6, // int
                                value = "42"
                            )
                        )
                    ),
                    // room : Room { value == targetValue }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "room",
                            className = "Room",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "value",
                                    operator = "==",
                                    value = TypedIdentifierExpression(
                                        evalType = 6, // int
                                        name = "targetValue",
                                        scope = 1
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val result = engine.executeStatement(matchStatement, context)

        // Verify that the execution succeeded
        assertIs<TransformationExecutionResult.Success>(result)
        
        // Should find two rooms with value=42
        // Verify by checking the graph directly
        val rooms = g.V().hasLabel("Room").has("value", 42).toList()
        assertEquals(2, rooms.size, "Should find exactly two rooms with value=42")
    }

    /**
     * Test with multiple variables referenced in the same match block
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `match block should reference multiple variables declared in same block`() {
        // Create test data
        g.addV("Room").property("category", "living").property("value", 100).next()
        g.addV("Room").property("category", "bedroom").property("value", 50).next()

        val matchStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // var cat = "living"
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "cat",
                            value = TypedStringLiteralExpression(
                                evalType = 1,
                                value = "living"
                            )
                        )
                    ),
                    // var val = 100
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "val",
                            value = TypedIntLiteralExpression(
                                evalType = 6,
                                value = "100"
                            )
                        )
                    ),
                    // room : Room { category == cat, value == val }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "room",
                            className = "Room",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "category",
                                    operator = "==",
                                    value = TypedIdentifierExpression(
                                        evalType = 1,
                                        name = "cat",
                                        scope = 1
                                    )
                                ),
                                TypedPatternPropertyAssignment(
                                    propertyName = "value",
                                    operator = "==",
                                    value = TypedIdentifierExpression(
                                        evalType = 6,
                                        name = "val",
                                        scope = 1
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val result = engine.executeStatement(matchStatement, context)

        // Verify execution succeeded
        assertIs<TransformationExecutionResult.Success>(result)
        
        // Should find exactly one room matching both conditions
        // Verify by checking the graph directly
        val rooms = g.V().hasLabel("Room").has("category", "living").has("value", 100).toList()
        assertEquals(1, rooms.size, "Should find exactly one room matching both variable values")
    }

    /**
     * Test that reproduces the issue where a variable references a matched instance property.
     * 
     * From Prompt.md line 177:
     * ```
     * match {
     *     var x = house.address
     *     house : House { }
     *     create house2 : House { address = x }
     * }
     * ```
     * 
     * Expected: Should create a new house with the same address as the matched house
     * Actual: FAILS because variables are evaluated BEFORE the match executes,
     *         so `house` doesn't exist yet when evaluating `house.address`
     * 
     * The fix requires evaluating variables INSIDE the match as separate match clauses.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `match block variable should reference matched instance property in create statement`() {
        // Create test data: a house with an address
        g.addV("House").property("address", "123 Main St").next()
        
        // Build the match statement:
        // match {
        //     var x = house.address
        //     house : House { }
        //     create house2 : House { address = x }
        // }
        
        val matchStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // var x = house.address
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "x",
                            value = TypedMemberAccessExpression(
                                evalType = 1, // string
                                expression = TypedIdentifierExpression(
                                    evalType = 7, // House
                                    name = "house",
                                    scope = 1
                                ),
                                member = "address",
                                isNullChaining = false
                            )
                        )
                    ),
                    // house : House { }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = "House",
                            properties = emptyList()
                        )
                    ),
                    // create house2 : House { address = x }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = "create",
                            name = "house2",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "=",
                                    value = TypedIdentifierExpression(
                                        evalType = 1, // string
                                        name = "x",
                                        scope = 1
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val result = engine.executeStatement(matchStatement, context)
        
        // Verify execution succeeded
        assertIs<TransformationExecutionResult.Success>(result)
        
        // Should have created a new house with address = "123 Main St"
        val houses = g.V().hasLabel("House").toList()
        assertEquals(2, houses.size, "Should have 2 houses: original and newly created")
        
        // Verify the new house has the correct address
        val housesWithCorrectAddress = g.V().hasLabel("House").has("address", "123 Main St").toList()
        assertEquals(2, housesWithCorrectAddress.size, "Both houses should have address '123 Main St'")
    }
}
