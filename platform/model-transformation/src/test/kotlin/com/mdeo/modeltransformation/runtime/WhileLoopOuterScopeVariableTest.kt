package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.metamodel.data.AssociationData
import com.mdeo.metamodel.data.AssociationEndData
import com.mdeo.metamodel.data.ClassData
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.MultiplicityData
import com.mdeo.metamodel.data.PropertyData
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedWhileExpressionStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph
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
        ClassTypeRef(`package` = "class", type = "House", isNullable = false)                       // 7
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

    private val metamodel = Metamodel.compile(metamodelData)

    private fun graphKey(className: String, propName: String): String =
        "prop_${metamodel.metadata.classes[className]!!.propertyFields[propName]!!.fieldIndex}"

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
            metamodelPath = "",
            statements = emptyList()
        )

        engine = TransformationEngine(
            modelGraph = TinkerModelGraph.wrap(graph, metamodel),
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
        g.addV("House").property(graphKey("House", "address"), "test address").next()

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
        val roomValues = g.V().hasLabel("Room").values<Any>(graphKey("Room", "value")).toList().map { (it as Number).toLong() }.sorted()
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), roomValues, "Room values should be 0-4")
    }
}
