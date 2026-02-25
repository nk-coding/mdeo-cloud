package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
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
 * Test that reproduces the bug where match patterns with property conditions fail to find matches.
 *
 * The issue:
 * Two semantically equivalent patterns behave differently:
 *
 * FAILING PATTERN (with inline condition):
 * ```
 * match {
 *     var x = house.address
 *     house : House {
 *         address == "exa" + "mple"
 *     }
 *     create house2 : House {
 *         address = x + "testing"
 *     }
 * }
 * ```
 *
 * WORKING PATTERN (with where clause):
 * ```
 * match {
 *     var x = house.address
 *     house : House {
 *     }
 *     create house2 : House {
 *         address = x + "testing"
 *     }
 *     where house.address == "exa" + "mple"
 * }
 * ```
 *
 * The formal JSON definitions show:
 * - First pattern has properties array with condition in the objectInstance
 * - Second pattern has empty properties array but a whereClause element
 *
 * Expected behavior:
 * Both patterns should find the house with address="example" and create house2 with address="exampletesting"
 *
 * Actual behavior:
 * The inline property condition fails to match, while the where clause works correctly.
 */
class MatchPropertyConditionTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    // Type indices:
    // Index 0: void
    // Index 1: builtin.string
    // Index 2: class.House
    private val types: List<ReturnType> = listOf<ReturnType>(
        VoidType(),                                                                   // 0
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false),                    // 1
        ClassTypeRef(`package` = "class", type = "House", isNullable = false)                        // 2
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
     * Test the FAILING pattern with inline property condition.
     *
     * This pattern should find the house with address="example" but currently fails.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `match with inline property condition should find matching house and create house2`() {
        // Create test data: a house with address="example"
        g.addV("House").property("address", "example").next()

        // Build the failing match pattern:
        // match {
        //     var x = house.address
        //     house : House {
        //         address == "exa" + "mple"
        //     }
        //     create house2 : House {
        //         address = x + "testing"
        //     }
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
                                    evalType = 2, // House
                                    name = "house",
                                    scope = 1
                                ),
                                member = "address",
                                isNullChaining = false
                            )
                        )
                    ),
                    // house : House { address == "exa" + "mple" }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "==",
                                    value = TypedBinaryExpression(
                                        evalType = 1, // string
                                        operator = "+",
                                        left = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "exa"
                                        ),
                                        right = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "mple"
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    // create house2 : House { address = x + "testing" }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = "create",
                            name = "house2",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "=",
                                    value = TypedBinaryExpression(
                                        evalType = 1, // string
                                        operator = "+",
                                        left = TypedIdentifierExpression(
                                            evalType = 1, // string (x evaluates to string)
                                            name = "x",
                                            scope = 1
                                        ),
                                        right = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "testing"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        // Execute the match statement
        val result = engine.executeStatement(matchStatement, context)

        // The match should succeed and create house2
        assertIs<TransformationExecutionResult.Success>(result, "Match should succeed with inline property condition")
        
        // Verify that house2 was created with the correct address
        val house2List = g.V().hasLabel("House").has("address", "exampletesting").toList()
        assertEquals(1, house2List.size, "Should create exactly one house2 with address='exampletesting'")
        
        // Verify original house still exists
        val originalHouse = g.V().hasLabel("House").has("address", "example").toList()
        assertEquals(1, originalHouse.size, "Original house should still exist")
    }

    /**
     * Test the WORKING pattern with where clause.
     *
     * This pattern should find the house and create house2, and currently works.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `match with where clause should find matching house and create house2`() {
        // Create test data: a house with address="example"
        g.addV("House").property("address", "example").next()

        // Build the working match pattern:
        // match {
        //     var x = house.address
        //     house : House {}
        //     create house2 : House {
        //         address = x + "testing"
        //     }
        //     where house.address == "exa" + "mple"
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
                                    evalType = 2, // House
                                    name = "house",
                                    scope = 1
                                ),
                                member = "address",
                                isNullChaining = false
                            )
                        )
                    ),
                    // house : House {}
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = "House",
                            properties = emptyList() // No inline condition
                        )
                    ),
                    // create house2 : House { address = x + "testing" }
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = "create",
                            name = "house2",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "=",
                                    value = TypedBinaryExpression(
                                        evalType = 1, // string
                                        operator = "+",
                                        left = TypedIdentifierExpression(
                                            evalType = 1, // string
                                            name = "x",
                                            scope = 1
                                        ),
                                        right = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "testing"
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    // where house.address == "exa" + "mple"
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 1, // boolean (but type index doesn't matter for where clause)
                                operator = "==",
                                left = TypedMemberAccessExpression(
                                    evalType = 1, // string
                                    expression = TypedIdentifierExpression(
                                        evalType = 2, // House
                                        name = "house",
                                        scope = 1
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                ),
                                right = TypedBinaryExpression(
                                    evalType = 1, // string
                                    operator = "+",
                                    left = TypedStringLiteralExpression(
                                        evalType = 1,
                                        value = "exa"
                                    ),
                                    right = TypedStringLiteralExpression(
                                        evalType = 1,
                                        value = "mple"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        // Execute the match statement
        val result = engine.executeStatement(matchStatement, context)

        // The match should succeed and create house2
        assertIs<TransformationExecutionResult.Success>(result, "Match should succeed with where clause")
        
        // Verify that house2 was created with the correct address
        val house2List = g.V().hasLabel("House").has("address", "exampletesting").toList()
        assertEquals(1, house2List.size, "Should create exactly one house2 with address='exampletesting'")
        
        // Verify original house still exists
        val originalHouse = g.V().hasLabel("House").has("address", "example").toList()
        assertEquals(1, originalHouse.size, "Original house should still exist")
    }

    /**
     * Test to demonstrate both patterns side-by-side for comparison.
     * 
     * This test creates two separate houses and tests both patterns
     * to clearly show the difference in behavior.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `inline property condition and where clause should behave identically`() {
        // Create two identical houses
        g.addV("House").property("address", "example").next()
        
        // First, test the where clause pattern (which works)
        val workingPatternStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "x",
                            value = TypedMemberAccessExpression(
                                evalType = 1,
                                expression = TypedIdentifierExpression(
                                    evalType = 2,
                                    name = "house",
                                    scope = 1
                                ),
                                member = "address",
                                isNullChaining = false
                            )
                        )
                    ),
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
                            modifier = "create",
                            name = "house2",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "=",
                                    value = TypedBinaryExpression(
                                        evalType = 1,
                                        operator = "+",
                                        left = TypedIdentifierExpression(
                                            evalType = 1,
                                            name = "x",
                                            scope = 1
                                        ),
                                        right = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "testing"
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    // where house.address == "exa" + "mple"
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 1,
                                operator = "==",
                                left = TypedMemberAccessExpression(
                                    evalType = 1,
                                    expression = TypedIdentifierExpression(
                                        evalType = 2,
                                        name = "house",
                                        scope = 1
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                ),
                                right = TypedBinaryExpression(
                                    evalType = 1,
                                    operator = "+",
                                    left = TypedStringLiteralExpression(
                                        evalType = 1,
                                        value = "exa"
                                    ),
                                    right = TypedStringLiteralExpression(
                                        evalType = 1,
                                        value = "mple"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val workingResult = engine.executeStatement(workingPatternStatement, context)
        assertIs<TransformationExecutionResult.Success>(workingResult, "Where clause pattern should succeed")
        
        val house2WithWhere = g.V().hasLabel("House").has("address", "exampletesting").toList()
        assertEquals(1, house2WithWhere.size, "Where clause should create house2")
        
        // Now test the inline condition pattern (currently fails)
        // First, clean up the created house2
        g.V().hasLabel("House").has("address", "exampletesting").drop().iterate()
        
        val failingPatternStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternVariableElement(
                        variable = TypedPatternVariable(
                            name = "x",
                            value = TypedMemberAccessExpression(
                                evalType = 1,
                                expression = TypedIdentifierExpression(
                                    evalType = 2,
                                    name = "house",
                                    scope = 1
                                ),
                                member = "address",
                                isNullChaining = false
                            )
                        )
                    ),
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "==",
                                    value = TypedBinaryExpression(
                                        evalType = 1,
                                        operator = "+",
                                        left = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "exa"
                                        ),
                                        right = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "mple"
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = "create",
                            name = "house2",
                            className = "House",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "=",
                                    value = TypedBinaryExpression(
                                        evalType = 1,
                                        operator = "+",
                                        left = TypedIdentifierExpression(
                                            evalType = 1,
                                            name = "x",
                                            scope = 1
                                        ),
                                        right = TypedStringLiteralExpression(
                                            evalType = 1,
                                            value = "testing"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val failingResult = engine.executeStatement(failingPatternStatement, context)
        assertIs<TransformationExecutionResult.Success>(failingResult, "Inline property condition pattern should also succeed")
        
        val house2WithInline = g.V().hasLabel("House").has("address", "exampletesting").toList()
        assertEquals(1, house2WithInline.size, "Inline property condition should create house2 just like where clause")
    }
}
