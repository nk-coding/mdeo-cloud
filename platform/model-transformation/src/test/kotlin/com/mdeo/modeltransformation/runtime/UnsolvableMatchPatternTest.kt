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
import com.mdeo.modeltransformation.ast.statements.TypedIfMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedUntilMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertIs
import java.util.concurrent.TimeUnit

/**
 * Test that reproduces the "unsolvable match pattern" issue.
 *
 * The issue:
 * When an "until match" statement has a pattern that references a variable from an outer scope
 * (e.g., `house` from `match { house: House {} }`), and the pattern Has multiple instances
 * (room1 and room2) both connected to that outer variable via links, the Gremlin match()
 * step fails with "unsolvable match pattern".
 *
 * The error occurs because the match clauses for room1 and room2 both reference `house`
 * via edge traversals, but `house` is not included in the match clauses itself (it's pre-bound).
 * This creates disconnected components in the Gremlin match graph.
 *
 * Error message:
 * ```
 * java.lang.IllegalStateException: The provided match pattern is unsolvable: 
 * [[MatchStartStep(room2), HasStep([~label.eq(Room)]), MatchEndStep(null)], 
 *  [MatchStartStep(room2), VertexStep(IN,[`rooms`_`house`],vertex), MatchEndStep(house)], 
 *  [MatchStartStep(room1), VertexStep(IN,[`rooms`_`house`],vertex), MatchEndStep(house)]]
 * ```
 *
 * Transformation that fails:
 * ```
 * match {
 *     house: House {}
 * }
 *
 * until match {
 *     room1: Room {}
 *     room1 -- house
 *
 *     room2: Room {}
 *     room2 -- house
 * } do {
 *     match {
 *         create newRoom: Room {
 *             category = "test"
 *             value = 1200
 *         }
 *         create house -- newRoom
 *     }
 * }
 * ```
 */
class UnsolvableMatchPatternTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    // Type indices matching the formal definition from the prompt:
    // Index 0: void
    // Index 1: builtin.string (non-nullable)
    // Index 2: builtin.double (non-nullable)
    // Index 3: builtin.boolean (non-nullable)
    // Index 4: Any? (nullable)
    // Index 5: builtin.List<Room> (non-nullable)
    // Index 6: builtin.int (non-nullable)
    // Index 7: class.House (non-nullable)
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

    // MetamodelData matching the formal definition from the prompt
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
     * Test that reproduces the "unsolvable match pattern" error.
     *
     * This test creates a model with exactly ONE house (no rooms initially),
     * then executes a transformation that should create rooms until 2 rooms
     * are connected to the house.
     *
     * The transformation pattern:
     * ```
     * match {
     *     house: House {}
     * }
     *
     * until match {
     *     room1: Room {}
     *     room1 -- house
     *
     *     room2: Room {}
     *     room2 -- house
     * } do {
     *     match {
     *         create newRoom: Room {
     *             category = "test"
     *             value = 1200
     *         }
     *         create house -- newRoom
     *     }
     * }
     * ```
     *
     * Expected: Should create rooms until 2 rooms exist connected to the house
     * Actual: Fails with "unsolvable match pattern" error
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `until match with two instances both linked to outer scope variable should not produce unsolvable pattern`() {
        // Create test data: a single house (no rooms initially)
        g.addV("House").property("address", "test address").next()

        // Step 1: Match the house
        val matchHouseStatement = TypedMatchStatement(
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

        // Execute the first match to bind the house variable
        val matchResult = engine.executeStatement(matchHouseStatement, context)
        assertIs<TransformationExecutionResult.Success>(matchResult)

        // Step 2: Build the until match with the pattern that fails
        // This is the pattern: room1: Room {}, room1 -- house, room2: Room {}, room2 -- house, where room1 != room2
        val untilMatchPattern = TypedPattern(
            elements = listOf(
                // room1: Room {}
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        modifier = null,
                        name = "room1",
                        className = "Room",
                        properties = emptyList()
                    )
                ),
                // room1 -- house (room1.house -> house)
                TypedPatternLinkElement(
                    link = TypedPatternLink(
                        modifier = null,
                        // Swapped source/target: now always outgoing from house to room1
                        source = TypedPatternLinkEnd(
                            objectName = "house",
                            propertyName = "rooms"
                        ),
                        target = TypedPatternLinkEnd(
                            objectName = "room1",
                            propertyName = "house"
                        )
                    )
                ),
                // room2: Room {}
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        modifier = null,
                        name = "room2",
                        className = "Room",
                        properties = emptyList()
                    )
                ),
                // room2 -- house (room2.house -> house)
                TypedPatternLinkElement(
                    link = TypedPatternLink(
                        modifier = null,
                        // Swapped source/target: now always outgoing from house to room2
                        source = TypedPatternLinkEnd(
                            objectName = "house",
                            propertyName = "rooms"
                        ),
                        target = TypedPatternLinkEnd(
                            objectName = "room2",
                            propertyName = "house"
                        )
                    )
                ),
                // where room1 != room2 (to enforce distinct room matching)
                TypedPatternWhereClauseElement(
                    whereClause = TypedWhereClause(
                        expression = TypedBinaryExpression(
                            evalType = 3, // boolean
                            operator = "!=",
                            left = TypedIdentifierExpression(
                                evalType = 7, // Room type 
                                name = "room1",
                                scope = 1 // ModelTransformation level - TraversalBindings are added at current scope
                            ),
                            right = TypedIdentifierExpression(
                                evalType = 7, // Room type
                                name = "room2",
                                scope = 1 // ModelTransformation level - TraversalBindings are added at current scope
                            )
                        )
                    )
                )
            )
        )

        // The do block: match { create newRoom: Room { category = "test", value = 1200 }, create house -- newRoom }
        val doBlock = listOf(
            TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        // create newRoom: Room { category = "test", value = 1200 }
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "create",
                                name = "newRoom",
                                className = "Room",
                                properties = listOf(
                                    TypedPatternPropertyAssignment(
                                        propertyName = "category",
                                        operator = "=",
                                        value = TypedStringLiteralExpression(
                                            evalType = 1, // string
                                            value = "test"
                                        )
                                    ),
                                    TypedPatternPropertyAssignment(
                                        propertyName = "value",
                                        operator = "=",
                                        value = TypedIntLiteralExpression(
                                            evalType = 6, // int
                                            value = "1200"
                                        )
                                    )
                                )
                            )
                        ),
                        // create house -- newRoom
                        TypedPatternLinkElement(
                            link = TypedPatternLink(
                                modifier = "create",
                                source = TypedPatternLinkEnd(
                                    objectName = "house",
                                    propertyName = "rooms"
                                ),
                                target = TypedPatternLinkEnd(
                                    objectName = "newRoom",
                                    propertyName = "house"
                                )
                            )
                        )
                    )
                )
            )
        )

        val untilMatchStatement = TypedUntilMatchStatement(
            pattern = untilMatchPattern,
            doBlock = doBlock
        )

        // Execute the until match statement
        // This should fail with: "The provided match pattern is unsolvable"
        val result = engine.executeStatement(untilMatchStatement, context)

        // Once the bug is fixed, this should succeed
        assertIs<TransformationExecutionResult.Success>(result)

        // Verify that exactly 2 rooms were created
        val roomCount = g.V().hasLabel("Room").count().next()
        assert(roomCount == 2L) { "Expected 2 rooms, but found $roomCount" }
    }

    /**
     * This test reproduces the issue using the nested if-match structure 
     * from the formal definition in the prompt.
     * 
     * Formal definition structure:
     * ```json
     * {
     *   "kind": "ifMatch",
     *   "pattern": {
     *     "elements": [
     *       {"kind": "objectInstance", "objectInstance": {"name": "house", "className": "House", "properties": []}},
     *     ]
     *   },
     *   "thenBlock": [
     *     {
     *       "kind": "ifMatch",
     *       "pattern": {
     *         "elements": [
     *           {"kind": "objectInstance", "objectInstance": {"name": "house2", "className": "House", "properties": []}},
     *           {"kind": "whereClause", "whereClause": {"expression": {"kind": "binary", "evalType": 3, "operator": "==", "left": {"kind": "identifier", "evalType": 7, "name": "house", "scope": 2}, "right": {"kind": "identifier", "evalType": 7, "name": "house2", "scope": 4}}}}
     *         ]
     *       },
     *       "thenBlock": []
     *     }
     *   ]
     * }
     * ```
     *
     * This tests the nested scope variable resolution that underlies the unsolvable pattern issue.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `nested if-match with where clause referencing outer scope variable`() {
        // Create test data: a single house
        g.addV("House").property("address", "test address").next()

        // Build the nested if-match statement structure from the formal definition
        
        // Inner match: house2: House {} where house == house2
        val innerIfMatch = TypedIfMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // house2: House {}
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house2",
                            className = "House",
                            properties = emptyList()
                        )
                    ),
                    // where house == house2
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "==",
                                left = TypedIdentifierExpression(
                                    evalType = 7, // House type
                                    name = "house",
                                    scope = 2  // This references the outer match scope
                                ),
                                right = TypedIdentifierExpression(
                                    evalType = 7, // House type
                                    name = "house2",
                                    scope = 4  // This references the inner match scope
                                )
                            )
                        )
                    )
                )
            ),
            thenBlock = emptyList()
        )

        // Outer match: house: House {}
        val outerIfMatch = TypedIfMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // house: House {}
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
            thenBlock = listOf(innerIfMatch)
        )

        // Execute the outer match statement
        val result = engine.executeStatement(outerIfMatch, context)

        // This should succeed - both house and house2 should match the same object
        assertIs<TransformationExecutionResult.Success>(result)
    }

    /**
     * Test a disconnected match pattern where two separate House instances
     * need to be matched with a constraint that they are not the same.
     *
     * DSL pattern:
     * ```
     * using "./metamodel.mm"
     *
     * match {
     *     house : House {
     *
     *     }
     *
     *     house2 : House {
     *
     *     }
     *
     *     where house != house2
     * }
     * ```
     *
     * This is a disconnected match pattern - the two houses have no edge connection
     * between them, only a where clause constraint ensuring they are different instances.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun testDisconnectedMatchWithWhereClause_TwoHousesMustBeDifferent() {
        // Create test data: two houses with different addresses
        g.addV("House").property("address", "house address 1").next()
        g.addV("House").property("address", "house address 2").next()

        // Build the match statement for the disconnected pattern:
        // match {
        //     house: House {}
        //     house2: House {}
        //     where house != house2
        // }
        val matchStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // house: House {}
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = "House",
                            properties = emptyList()
                        )
                    ),
                    // house2: House {}
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house2",
                            className = "House",
                            properties = emptyList()
                        )
                    ),
                    // where house != house2
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "!=",
                                left = TypedIdentifierExpression(
                                    evalType = 7, // House type
                                    name = "house",
                                    scope = 1
                                ),
                                right = TypedIdentifierExpression(
                                    evalType = 7, // House type
                                    name = "house2",
                                    scope = 1
                                )
                            )
                        )
                    )
                )
            )
        )

        // Execute the match statement
        val matchResult = engine.executeStatement(matchStatement, context)

        // The match should succeed - two different houses exist
        assertIs<TransformationExecutionResult.Success>(matchResult)

        // Verify that house and house2 are bound to different instances
        val houseBinding = context.variableScope.getVariable("house") as? VariableBinding.InstanceBinding
        val house2Binding = context.variableScope.getVariable("house2") as? VariableBinding.InstanceBinding

        assert(houseBinding != null) { "Expected 'house' to be bound" }
        assert(house2Binding != null) { "Expected 'house2' to be bound" }
        assert(houseBinding!!.vertexId != house2Binding!!.vertexId) {
            "Expected 'house' and 'house2' to be different instances, but both have vertexId: ${houseBinding.vertexId}"
        }
    }
}
