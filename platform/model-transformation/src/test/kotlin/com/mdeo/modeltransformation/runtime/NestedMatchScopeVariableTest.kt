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
import com.mdeo.modeltransformation.ast.statements.TypedIfMatchStatement
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
import kotlin.test.assertIs
import java.util.concurrent.TimeUnit

/**
 * Test that reproduces the nested match scope variable resolution issue.
 *
 * The issue:
 * - An outer if-match statement binds a variable `house: House {}` in scope 2
 * - A nested if-match statement tries to reference `house` in a where clause comparing it with `house2`
 * - The compiler/executor fails with "Unresolved variable 'house' in scope 2 (expression kind: identifier, evalType: 7)"
 *
 * Expected behavior:
 * - The nested match's where clause should be able to access the `house` variable from the outer match scope
 * - It should be able to compare house == house2 in the where clause
 *
 * Transformation that fails:
 * ```
 * if match {
 *     house : House {
 *     }
 * } then {
 *     if match {
 *         house2 : House {}
 *         where house == house2
 *     } then {
 *     }
 * }
 * ```
 */
class NestedMatchScopeVariableTest {

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
     * Test that reproduces the exact issue from Prompt.md:
     *
     * if match {
     *     house : House {
     *     }
     * } then {
     *     if match {
     *         house2 : House {}
     *         where house == house2
     *     } then {
     *     }
     * }
     *
     * Expected: Should match when house and house2 are the same object
     * Actual: Fails with "Unresolved variable 'house' in scope 2 (expression kind: identifier, evalType: 7)"
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `nested match should access variable from outer match scope in where clause`() {
        // Create test data: a single house
        g.addV("House").property(graphKey("House", "address"), "test address").next()

        // Build the nested if-match statement structure
        // This mirrors the JSON structure from Prompt.md
        
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
        // This should fail with: "Unresolved variable 'house' in scope 2"
        val result = engine.executeStatement(outerIfMatch, context)

        // Currently this will fail due to the scope resolution bug
        // Once the bug is fixed, this should succeed
        assertIs<TransformationExecutionResult.Success>(result)
    }

    /**
     * Additional test: nested match trying to access outer variable in property assignment
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `nested match should access outer variable in create property assignment`() {
        // Create test data
        g.addV("House").property(graphKey("House", "address"), "outer address").next()

        // Build nested if-match with property assignment referencing outer variable
        // if match { house: House {} } then {
        //     if match {
        //         create room: Room { category = house.address }
        //     } then {}
        // }
        
        val innerIfMatch = TypedIfMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    // create room: Room { category = house.address }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = "create",
                            name = "room",
                            className = "Room",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "category",
                                    operator = "=",
                                    value = TypedMemberAccessExpression(
                                        evalType = 1, // string
                                        expression = TypedIdentifierExpression(
                                            evalType = 7, // House
                                            name = "house",
                                            scope = 2  // Referencing outer match scope
                                        ),
                                        member = "address",
                                        isNullChaining = false
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            thenBlock = emptyList()
        )

        val outerIfMatch = TypedIfMatchStatement(
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
            ),
            thenBlock = listOf(innerIfMatch)
        )

        // Execute the outer match statement
        val result = engine.executeStatement(outerIfMatch, context)

        // This should succeed once the scope bug is fixed
        assertIs<TransformationExecutionResult.Success>(result)
    }

    /**
     * Test with three levels of nesting to stress test scope resolution
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `triple nested match should access variables from all outer scopes`() {
        // Create test data
        g.addV("House").property(graphKey("House", "address"), "house1").next()
        g.addV("House").property(graphKey("House", "address"), "house2").next()
        g.addV("House").property(graphKey("House", "address"), "house3").next()

        // if match { h1: House {} } then {
        //   if match { h2: House {} } then {
        //     if match { h3: House {} where h1 == h2 && h2 == h3 } then {}
        //   }
        // }

        val innerMostIfMatch = TypedIfMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "h3",
                            className = "House",
                            properties = emptyList()
                        )
                    ),
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "&&",
                                left = TypedBinaryExpression(
                                    evalType = 3,
                                    operator = "==",
                                    left = TypedIdentifierExpression(
                                        evalType = 7,
                                        name = "h1",
                                        scope = 2  // Referencing first level scope
                                    ),
                                    right = TypedIdentifierExpression(
                                        evalType = 7,
                                        name = "h2",
                                        scope = 4  // Referencing second level scope
                                    )
                                ),
                                right = TypedBinaryExpression(
                                    evalType = 3,
                                    operator = "==",
                                    left = TypedIdentifierExpression(
                                        evalType = 7,
                                        name = "h2",
                                        scope = 4
                                    ),
                                    right = TypedIdentifierExpression(
                                        evalType = 7,
                                        name = "h3",
                                        scope = 6  // Current scope
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            thenBlock = emptyList()
        )

        val middleIfMatch = TypedIfMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "h2",
                            className = "House",
                            properties = emptyList()
                        )
                    )
                )
            ),
            thenBlock = listOf(innerMostIfMatch)
        )

        val outerIfMatch = TypedIfMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "h1",
                            className = "House",
                            properties = emptyList()
                        )
                    )
                )
            ),
            thenBlock = listOf(middleIfMatch)
        )

        // Execute the outer match statement
        val result = engine.executeStatement(outerIfMatch, context)

        // This should succeed once the scope bug is fixed
        assertIs<TransformationExecutionResult.Success>(result)
    }
}
