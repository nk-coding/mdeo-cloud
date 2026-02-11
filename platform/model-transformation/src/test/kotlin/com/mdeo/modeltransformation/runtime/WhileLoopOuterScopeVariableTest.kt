package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.TypedClass
import com.mdeo.expression.ast.types.TypedProperty
import com.mdeo.expression.ast.types.TypedRelation
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedWhileExpressionStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.assertEquals
import kotlin.test.assertIs
import java.util.concurrent.TimeUnit

/**
 * Test that reproduces the issue where a while loop condition cannot resolve
 * variables from outer match statements.
 *
 * The issue:
 * - A match statement binds a variable `house: House {}`
 * - A while loop tries to use `house.rooms.size() < 5` as a condition
 * - The ConditionEvaluator fails with "Unresolved variable 'house' in scope 1"
 *
 * Expected behavior:
 * - The while loop should be able to access the `house` variable from the outer scope
 * - It should execute the loop body until house.rooms.size() reaches 5
 */
class WhileLoopOuterScopeVariableTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    // Type indices matching the transformation from Prompt.md
    // Index 0: void
    // Index 1: builtin.string
    // Index 2: builtin.double
    // Index 3: builtin.boolean
    // Index 4: Any? (nullable)
    // Index 5: builtin.List<Room> 
    // Index 6: builtin.int
    // Index 7: metamodel./metamodel.mm.House
    private val types: List<ReturnType> = listOf<ReturnType>(
        VoidType(),                                                                   // 0
        ClassTypeRef(type = "builtin.string", isNullable = false),                    // 1
        ClassTypeRef(type = "builtin.double", isNullable = false),                    // 2
        ClassTypeRef(type = "builtin.boolean", isNullable = false),                   // 3
        ClassTypeRef(type = "Any", isNullable = true),                                // 4
        ClassTypeRef(type = "builtin.List", isNullable = false, typeArgs = mapOf(
            "T" to ClassTypeRef(type = "metamodel./metamodel.mm.Room", isNullable = false, typeArgs = emptyMap())
        )),                                                                          // 5
        ClassTypeRef(type = "builtin.int", isNullable = false),                      // 6
        ClassTypeRef(type = "metamodel./metamodel.mm.House", isNullable = false)     // 7
    )

    private val classes = listOf(
        TypedClass(
            name = "House",
            `package` = "metamodel./metamodel.mm",
            superClasses = emptySet(),
            properties = listOf(
                TypedProperty(name = "address", typeIndex = 1)
            ),
            relations = listOf(
                TypedRelation(
                    property = "rooms",
                    oppositeProperty = "house",
                    oppositeClassName = "metamodel./metamodel.mm.Room",
                    isOutgoing = true,
                    typeIndex = 5
                )
            )
        ),
        TypedClass(
            name = "Room",
            `package` = "metamodel./metamodel.mm",
            superClasses = emptySet(),
            properties = listOf(
                TypedProperty(name = "category", typeIndex = 1),
                TypedProperty(name = "value", typeIndex = 6)
            ),
            relations = listOf(
                TypedRelation(
                    property = "house",
                    oppositeProperty = "rooms",
                    oppositeClassName = "metamodel./metamodel.mm.House",
                    isOutgoing = false,
                    typeIndex = 7
                )
            )
        )
    )

    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()

        // Register metamodel types in the global type registry
        val typeRegistry = GremlinTypeRegistry.GLOBAL

        val houseType = gremlinType("metamodel./metamodel.mm.House")
            .graphProperty("address")
            .build()
        typeRegistry.register(houseType)

        val roomType = gremlinType("metamodel./metamodel.mm.Room")
            .graphProperty("category")
            .graphProperty("value")
            .build()
        typeRegistry.register(roomType)

        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()

        // Create AST with types
        val ast = TypedAst(
            types = types,
            metamodelUri = "./metamodel.mm",
            statements = emptyList(),
            classes = classes
        )

        engine = TransformationEngine(
            traversalSource = g,
            ast = ast,
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
     * Test that reproduces the exact issue from the Prompt.md:
     *
     * match {
     *     house: House {}
     * }
     * 
     * while (house.rooms.size() < 5) {
     *     match {
     *         create house -- newRoom
     *         create newRoom: Room {
     *             category = house.address + "test"
     *             value = house.rooms.size()
     *         }
     *     }
     * }
     *
     * Expected: Should create 5 rooms linked to the house
     * Actual: Fails with "Unresolved variable 'house' in scope 1"
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `while loop should access variable from outer match statement`() {
        // Create a House vertex with an address
        g.addV("House").property("address", "test address").next()

        // First match: match the house
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
                    )
                )
            )
        )

        // Execute the first match to bind the house variable
        val matchResult = engine.executeStatement(matchStatement, context)
        assertIs<TransformationExecutionResult.Success>(matchResult)

        // Create the while loop condition: house.rooms.size() < 5
        // This mirrors the JSON structure from Prompt.md
        val whileCondition = TypedBinaryExpression(
            evalType = 3, // boolean
            operator = "<",
            left = TypedMemberCallExpression(
                evalType = 6, // int
                expression = TypedMemberAccessExpression(
                    evalType = 5, // List<Room>
                    expression = TypedIdentifierExpression(
                        evalType = 7, // House
                        name = "house",
                        scope = 1
                    ),
                    member = "rooms",
                    isNullChaining = false
                ),
                member = "size",
                isNullChaining = false,
                arguments = emptyList(),
                overload = ""
            ),
            right = TypedIntLiteralExpression(
                evalType = 6, // int
                value = "5"
            )
        )

        // Create the while loop body: match { create house -- newRoom; create newRoom: Room {...} }
        val whileBody = listOf(
            TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
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
                        ),
                        // create newRoom: Room { category = house.address + "test", value = house.rooms.size() }
                        TypedPatternObjectInstanceElement(
                            objectInstance = TypedPatternObjectInstance(
                                modifier = "create",
                                name = "newRoom",
                                className = "Room",
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
                                                    evalType = 7, // House
                                                    name = "house",
                                                    scope = 1
                                                ),
                                                member = "address",
                                                isNullChaining = false
                                            ),
                                            right = TypedStringLiteralExpression(
                                                evalType = 1,
                                                value = "test"
                                            )
                                        )
                                    ),
                                    TypedPatternPropertyAssignment(
                                        propertyName = "value",
                                        operator = "=",
                                        value = TypedMemberCallExpression(
                                            evalType = 6, // int
                                            expression = TypedMemberAccessExpression(
                                                evalType = 5, // List<Room>
                                                expression = TypedIdentifierExpression(
                                                    evalType = 7, // House
                                                    name = "house",
                                                    scope = 1
                                                ),
                                                member = "rooms",
                                                isNullChaining = false
                                            ),
                                            member = "size",
                                            isNullChaining = false,
                                            arguments = emptyList(),
                                            overload = ""
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        // Create the while statement
        val whileStatement = TypedWhileExpressionStatement(
            condition = whileCondition,
            block = whileBody
        )

        // Execute the while loop - this should fail with "Unresolved variable 'house' in scope 1"
        val whileResult = engine.executeStatement(whileStatement, context)

        // If the bug is fixed, this should succeed and create 5 rooms
        assertIs<TransformationExecutionResult.Success>(whileResult)
        
        // Verify that 5 rooms were created
        val roomCount = g.V().hasLabel("Room").count().next()
        assertEquals(5L, roomCount, "Expected 5 rooms to be created")
        
        // Verify that all rooms are linked to the house
        val houseVertex = g.V().hasLabel("House").next()
        val linkedRoomCount = g.V(houseVertex).outE("`rooms`_`house`").count().next()
        assertEquals(5L, linkedRoomCount, "Expected 5 rooms to be linked to the house")
        
        // Verify that room values are 0, 1, 2, 3, 4 (the size of rooms at time of creation)
        val roomValues = g.V().hasLabel("Room").values<Any>("value").toList().map { (it as Number).toLong() }.sorted()
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), roomValues, "Room values should be 0-4")
    }
}
