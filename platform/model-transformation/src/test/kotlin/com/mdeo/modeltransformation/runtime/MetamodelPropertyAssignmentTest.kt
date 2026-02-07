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
 * Tests that reproduce the real-world scenario where metamodel types
 * (e.g., metamodel./metamodel.mm.House) are used with their fully-qualified names.
 *
 * This test matches the exact typed AST that the language frontend produces,
 * where eval types reference metamodel classes like "metamodel./metamodel.mm.House"
 * rather than simplified types like "__GraphNode".
 *
 * The bug: When creating a Room with `category = house.address`,
 * the MemberAccessCompiler looks up "address" on "metamodel./metamodel.mm.House"
 * in the type registry. If that metamodel type is NOT registered, the compiler
 * throws an exception which is silently caught, causing the property to be skipped.
 */
class MetamodelPropertyAssignmentTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private val executor = MatchExecutor()

    // Type indices matching the real typed AST from the user's transformation
    // Index 0: void
    // Index 1: builtin.string
    // Index 2: builtin.double
    // Index 3: builtin.boolean
    // Index 4: Any? (nullable)
    // Index 5: builtin.List<Room>
    // Index 6: builtin.int
    // Index 7: metamodel./metamodel.mm.House
    private val types: List<ReturnType> = listOf<ReturnType>(
        VoidType(),                                                             // 0
        ClassTypeRef(type = "builtin.string", isNullable = false),              // 1
        ClassTypeRef(type = "builtin.double", isNullable = false),              // 2
        ClassTypeRef(type = "builtin.boolean", isNullable = false),             // 3
        ClassTypeRef(type = "Any", isNullable = true),                          // 4
        ClassTypeRef(type = "builtin.List", isNullable = false),                // 5
        ClassTypeRef(type = "builtin.int", isNullable = false),                 // 6
        ClassTypeRef(type = "metamodel./metamodel.mm.House", isNullable = false) // 7
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

        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()

        engine = TransformationEngine(
            traversalSource = g,
            ast = TypedAst(types = emptyList(), metamodelUri = "test://model", statements = emptyList()), // Dummy AST for manual setup
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Helper to set engine types via reflection (as done in other tests).
     */
    private fun setEngineTypes() {
        val typesField = TransformationEngine::class.java.getDeclaredField("types")
        typesField.isAccessible = true
        typesField.set(engine, types)
    }

    /**
     * Helper to register metamodel classes into the GLOBAL type registry,
     * simulating what should happen during engine.execute().
     */
    private fun registerMetamodelTypes() {
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
    }

    /**
     * Reproduces the exact user scenario:
     *
     *   match {
     *       house: House {}
     *       create newRoom: Room {
     *           category = house.address
     *           value = 100 * 10
     *       }
     *       create house -- newRoom
     *   }
     *
     * The identifier `house` has evalType=7 (metamodel./metamodel.mm.House), scope=1.
     * The member access `house.address` has evalType=1 (builtin.string).
     *
     * Without registering metamodel types in the type registry, the category property
     * is silently skipped because the MemberAccessCompiler can't find "address" on
     * "metamodel./metamodel.mm.House".
     */
    @Test
    fun `category from house_address should be set on created Room with metamodel types`() {
        // Create a House vertex in the graph
        g.addV("House").property("address", "123 Main St").next()

        // Set engine types to match the real typed AST
        setEngineTypes()

        // Register metamodel types so the compiler can resolve properties
        registerMetamodelTypes()

        val pattern = TypedPattern(
            elements = listOf(
                // match house: House {}
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "house",
                        className = "House",
                        modifier = null,
                        properties = emptyList()
                    )
                ),
                // create newRoom: Room { category = house.address, value = 100 * 10 }
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        name = "newRoom",
                        className = "Room",
                        modifier = "create",
                        properties = listOf(
                            TypedPatternPropertyAssignment(
                                propertyName = "category",
                                operator = "=",
                                value = TypedMemberAccessExpression(
                                    evalType = 1, // builtin.string
                                    expression = TypedIdentifierExpression(
                                        evalType = 7, // metamodel./metamodel.mm.House
                                        name = "house",
                                        scope = 1
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                )
                            ),
                            TypedPatternPropertyAssignment(
                                propertyName = "value",
                                operator = "=",
                                value = TypedBinaryExpression(
                                    evalType = 6, // builtin.int
                                    operator = "*",
                                    left = TypedIntLiteralExpression(
                                        evalType = 6,
                                        value = "100"
                                    ),
                                    right = TypedIntLiteralExpression(
                                        evalType = 6,
                                        value = "10"
                                    )
                                )
                            )
                        )
                    )
                ),
                // create house -- newRoom  (link)
                TypedPatternLinkElement(
                    link = TypedPatternLink(
                        modifier = "create",
                        isOutgoing = true,
                        source = TypedPatternLinkEnd(objectName = "house", propertyName = "rooms"),
                        target = TypedPatternLinkEnd(objectName = "newRoom", propertyName = "house")
                    )
                )
            )
        )

        val context = TransformationExecutionContext.empty()
        val result = executor.executeMatch(pattern, context, engine)

        assertTrue(result is MatchResult.Matched, "Match should succeed")
        result as MatchResult.Matched

        val roomId = result.instanceMappings["newRoom"]
        assertNotNull(roomId, "newRoom should be mapped")

        val room = g.V(roomId).next()

        // value = 100 * 10 should work (pure constant)
        val valueProperty = room.property<Int>("value")
        assertTrue(valueProperty.isPresent, "value property should exist")
        assertEquals(1000, valueProperty.value(), "value should be 100*10=1000")

        // category = house.address MUST be set
        val categoryProperty = room.property<String>("category")
        assertTrue(categoryProperty.isPresent, "category property MUST exist - this is the bug if it fails")
        assertEquals("123 Main St", categoryProperty.value(), "category should equal house.address")
    }

    /**
     * This test demonstrates the bug WITHOUT manually registered metamodel types.
     * It shows that without type registration, the property is silently skipped.
     *
     * After the fix (TransformationEngine auto-registers metamodel types from ast.classes),
     * this test should also pass when using engine.execute() with a full TypedAst.
     */
    @Test
    fun `engine execute should auto-register metamodel types from ast classes`() {
        // Create a House vertex in the graph
        g.addV("House").property("address", "456 Oak Ave").next()

        // Build a full TypedAst with classes - this is what the real system produces
        val ast = TypedAst(
            types = types,
            metamodelUri = "./metamodel.mm",
            classes = classes,
            statements = listOf(
                TypedMatchStatement(
                    pattern = TypedPattern(
                        elements = listOf(
                            TypedPatternObjectInstanceElement(
                                objectInstance = TypedPatternObjectInstance(
                                    name = "house",
                                    className = "House",
                                    modifier = null,
                                    properties = emptyList()
                                )
                            ),
                            TypedPatternObjectInstanceElement(
                                objectInstance = TypedPatternObjectInstance(
                                    name = "newRoom",
                                    className = "Room",
                                    modifier = "create",
                                    properties = listOf(
                                        TypedPatternPropertyAssignment(
                                            propertyName = "category",
                                            operator = "=",
                                            value = TypedMemberAccessExpression(
                                                evalType = 1,
                                                expression = TypedIdentifierExpression(
                                                    evalType = 7,
                                                    name = "house",
                                                    scope = 1
                                                ),
                                                member = "address",
                                                isNullChaining = false
                                            )
                                        ),
                                        TypedPatternPropertyAssignment(
                                            propertyName = "value",
                                            operator = "=",
                                            value = TypedBinaryExpression(
                                                evalType = 6,
                                                operator = "*",
                                                left = TypedIntLiteralExpression(
                                                    evalType = 6,
                                                    value = "100"
                                                ),
                                                right = TypedIntLiteralExpression(
                                                    evalType = 6,
                                                    value = "10"
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            TypedPatternLinkElement(
                                link = TypedPatternLink(
                                    modifier = "create",
                                    isOutgoing = true,
                                    source = TypedPatternLinkEnd(objectName = "house", propertyName = "rooms"),
                                    target = TypedPatternLinkEnd(objectName = "newRoom", propertyName = "house")
                                )
                            )
                        )
                    )
                )
            )
        )

        // Execute the full AST through the engine - this should auto-register metamodel types
        val testEngine = TransformationEngine(
             traversalSource = g,
             ast = ast,
             expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
             statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )
        val result = testEngine.execute()

        assertTrue(result is TransformationExecutionResult.Success, "Transformation should succeed, got: $result")

        // Verify the Room was created with the correct properties
        val rooms = g.V().hasLabel("Room").toList()
        assertEquals(1, rooms.size, "Exactly one Room should be created")

        val room = rooms[0]

        // value = 100 * 10
        val valueProperty = room.property<Int>("value")
        assertTrue(valueProperty.isPresent, "value property should exist")
        assertEquals(1000, valueProperty.value())

        // category = house.address - THE BUG: this is missing without the fix  
        val categoryProperty = room.property<String>("category")
        assertTrue(categoryProperty.isPresent, "category property MUST exist (was silently skipped before fix)")
        assertEquals("456 Oak Ave", categoryProperty.value(), "category should equal house.address")
    }
}
