package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.TypedClass
import com.mdeo.expression.ast.types.TypedProperty
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
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
 * Test that reproduces the issue where a where clause cannot resolve
 * variables from matched pattern elements.
 *
 * The issue:
 * - A match statement binds a variable `house: House {}`
 * - A where clause tries to use `house.address == "example2"` as a condition
 * - The compiler/executor fails with "Scope not found at index 1"
 *
 * Expected behavior:
 * - The where clause should be able to access the `house` variable from the pattern
 * - It should filter matches based on the condition
 */
class MatchWhereClauseScopeTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext

    // Type indices matching the transformation structure
    // Index 0: void
    // Index 1: builtin.string
    // Index 2: builtin.double
    // Index 3: builtin.boolean
    // Index 4: Any? (nullable)
    // Index 6: builtin.int
    // Index 7: metamodel./metamodel.mm.House
    private val types: List<ReturnType> = listOf<ReturnType>(
        VoidType(),                                                                   // 0
        ClassTypeRef(type = "builtin.string", isNullable = false),                    // 1
        ClassTypeRef(type = "builtin.double", isNullable = false),                    // 2
        ClassTypeRef(type = "builtin.boolean", isNullable = false),                   // 3
        ClassTypeRef(type = "Any", isNullable = true),                                // 4
        VoidType(),                                                                   // 5 (placeholder)
        ClassTypeRef(type = "builtin.int", isNullable = false),                       // 6
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
            relations = emptyList()
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
     * Test that reproduces the exact issue from the prompt:
     *
     * match {
     *     house: House {}
     *     where house.address == "example2"
     * }
     *
     * Expected: Should match houses with address "example2"
     * Actual: Fails with "Scope not found at index 1 (expression kind: identifier, evalType: 7)"
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `where clause should access matched variable in same pattern`() {
        // Create test data: multiple houses with different addresses
        g.addV("House").property("address", "example1").next()
        g.addV("House").property("address", "example2").next()
        g.addV("House").property("address", "example3").next()

        // Create a match statement with a where clause that references the matched variable
        // The transformation is:
        // match {
        //     house: House {}
        //     where house.address == "example2"
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
                    // where house.address == "example2"
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "==",
                                left = TypedMemberAccessExpression(
                                    evalType = 1, // string
                                    expression = TypedIdentifierExpression(
                                        evalType = 7, // House type
                                        name = "house",
                                        scope = 1  // This is the key: referencing scope 1 (the matched variable)
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                ),
                                right = TypedStringLiteralExpression(
                                    evalType = 1, // string
                                    value = "example2"
                                )
                            )
                        )
                    )
                )
            )
        )

        // Execute the match statement - this should fail with "Scope not found at index 1"
        val matchResult = engine.executeStatement(matchStatement, context)

        // If the bug is fixed, this should succeed and match the house with address "example2"
        assertIs<TransformationExecutionResult.Success>(matchResult)
        
        // Verify that the matched house has the correct address
        val houseId = (context.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId
        val houseVertex = g.V(houseId).next()
        val address = houseVertex.property<String>("address").value()
        assertEquals("example2", address, "Expected to match the house with address 'example2'")
    }

    /**
     * Alternative test: where clause with different operators
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `where clause with not equals should work`() {
        // Create test data
        g.addV("House").property("address", "example1").next()
        g.addV("House").property("address", "example2").next()

        // match { house: House {} where house.address != "example1" }
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
                    ),
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "!=",
                                left = TypedMemberAccessExpression(
                                    evalType = 1, // string
                                    expression = TypedIdentifierExpression(
                                        evalType = 7, // House type
                                        name = "house",
                                        scope = 1
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                ),
                                right = TypedStringLiteralExpression(
                                    evalType = 1,
                                    value = "example1"
                                )
                            )
                        )
                    )
                )
            )
        )

        val matchResult = engine.executeStatement(matchStatement, context)

        assertIs<TransformationExecutionResult.Success>(matchResult)
        
        // Verify that the matched house does not have address "example1"
        val houseId = (context.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId
        val houseVertex = g.V(houseId).next()
        val address = houseVertex.property<String>("address").value()
        assertEquals("example2", address)
    }

    /**
     * Test with multiple where clauses referencing the same matched variable
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `multiple where clauses should access matched variable`() {
        // Create test data
        g.addV("House").property("address", "example1").next()
        g.addV("House").property("address", "example2").next()
        g.addV("House").property("address", "example3").next()

        // Test that multiple where clauses can reference the same matched variable
        // match {
        //     house: House {}
        //     where house.address == "example2"
        //     where house.address != "example1"
        // }
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
                    ),
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "==",
                                left = TypedMemberAccessExpression(
                                    evalType = 1, // string
                                    expression = TypedIdentifierExpression(
                                        evalType = 7, // House type
                                        name = "house",
                                        scope = 1
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                ),
                                right = TypedStringLiteralExpression(
                                    evalType = 1,
                                    value = "example2"
                                )
                            )
                        )
                    ),
                    TypedPatternWhereClauseElement(
                        whereClause = TypedWhereClause(
                            expression = TypedBinaryExpression(
                                evalType = 3, // boolean
                                operator = "!=",
                                left = TypedMemberAccessExpression(
                                    evalType = 1, // string
                                    expression = TypedIdentifierExpression(
                                        evalType = 7, // House type
                                        name = "house",
                                        scope = 1
                                    ),
                                    member = "address",
                                    isNullChaining = false
                                ),
                                right = TypedStringLiteralExpression(
                                    evalType = 1,
                                    value = "example1"
                                )
                            )
                        )
                    )
                )
            )
        )

        val matchResult = engine.executeStatement(matchStatement, context)

        assertIs<TransformationExecutionResult.Success>(matchResult)
        
        val houseId = (context.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId
        val houseVertex = g.V(houseId).next()
        val address = houseVertex.property<String>("address").value()
        assertEquals("example2", address)
    }
}
