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
 * Test that reproduces issues with reference match support.
 *
 * The model transformation system has issues when a second match statement references
 * an object instance from a previous match statement. There are two failing scenarios:
 *
 * **Problem 1: Delete reference match**
 * When you first match a House object, then try to delete it in a second match using
 * just the reference name, it fails with "no match can be found" error.
 *
 * Transformation that fails:
 * ```
 * match {
 *     house: House {}
 * }
 *
 * match {
 *     delete house
 * }
 * ```
 *
 * **Problem 2: Update reference match**
 * When you first match a House object, then try to update its properties in a second
 * match using just the reference name, it also fails.
 *
 * Transformation that fails:
 * ```
 * match {
 *     house: House {}
 * }
 *
 * match {
 *     house {
 *         address = "example"
 *     }
 * }
 * ```
 *
 * Expected behavior:
 * Both patterns should work correctly - the first should delete the house, and the
 * second should update the house address.
 *
 * Actual behavior:
 * Both fail with "no match can be found" errors because the system doesn't properly
 * resolve instance references from previous match statements.
 */
class ReferenceMatchTest {

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
        val typeRegistry = GremlinTypeRegistry.GLOBAL

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
     * Test that reproduces Problem 1: Delete reference match.
     *
     * This test creates a model with a House object, then attempts to:
     * 1. Match the House object and bind it to variable 'house'
     * 2. Delete the House object using just the reference name in a second match
     *
     * Expected: The transformation should succeed - first match finds the house,
     *           second match deletes it using the reference from the first match.
     *
     * Actual: Currently fails with "no match can be found" error because the system
     *         doesn't properly resolve the instance reference 'house' from the previous
     *         match statement.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `delete reference match should delete instance from previous match`() {
        // Create test data: a single house
        g.addV("House").property("address", "123 Main St").next()

        // Verify initial state: 1 house exists
        val initialHouseCount = g.V().hasLabel("House").count().next()
        assertEquals(1L, initialHouseCount, "Should start with 1 house")

        // Build transformation with two match statements:
        // Statement 1: Match the house
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

        // Statement 2: Delete the house using just the reference name
        // This corresponds to the formal definition where we have:
        // - modifier = "delete"
        // - name = "house"
        // - className is NOT provided (the issue is that className is missing)
        val deleteHouseStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = "delete",
                            name = "house",
                            className = null,  // No className - should use reference from previous match
                            properties = emptyList()
                        )
                    )
                )
            )
        )

        // Create AST with both statements
        val transformationAst = TypedAst(
            types = types,
            metamodelPath = "./metamodel.mm",
            statements = listOf(matchHouseStatement, deleteHouseStatement)
        )

        // Create engine with the transformation AST
        val transformationEngine = TransformationEngine(
            traversalSource = g,
            ast = transformationAst,
            metamodelData = metamodelData,
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )

        // Execute the transformation
        // Expected: Should succeed and delete the house
        // Actual: Currently fails with "no match can be found"
        val result = transformationEngine.execute()
        
        // Print failure details if it failed
        if (result is TransformationExecutionResult.Failure) {
            println("Test failed with error: ${result.reason}")
        }
        
        assertIs<TransformationExecutionResult.Success>(
            result,
            "Delete reference match should succeed using instance from previous match"
        )

        // Verify final state: 0 houses should remain
        val finalHouseCount = g.V().hasLabel("House").count().next()
        assertEquals(0L, finalHouseCount, "House should be deleted")
    }

    /**
     * Test that reproduces Problem 2: Update reference match.
     *
     * This test creates a model with a House object, then attempts to:
     * 1. Match the House object and bind it to variable 'house'
     * 2. Update the House object's properties using just the reference name in a second match
     *
     * Expected: The transformation should succeed - first match finds the house,
     *           second match updates its address property using the reference from the first match.
     *
     * Actual: Currently fails with "no match can be found" error because the system
     *         doesn't properly resolve the instance reference 'house' from the previous
     *         match statement.
     */
    @Test
    @Timeout(value = 40, unit = TimeUnit.SECONDS)
    fun `update reference match should update instance from previous match`() {
        // Create test data: a single house with an initial address
        g.addV("House").property("address", "123 Main St").next()

        // Verify initial state
        val initialAddress = g.V().hasLabel("House").values<String>("address").next()
        assertEquals("123 Main St", initialAddress, "Initial address should be '123 Main St'")

        // Build transformation with two match statements:
        // Statement 1: Match the house
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

        // Statement 2: Update the house using just the reference name
        // This corresponds to the formal definition where we have:
        // - name = "house"
        // - className is NOT provided (should use reference from previous match)
        // - properties array contains the update: address = "example"
        val updateHouseStatement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            modifier = null,
                            name = "house",
                            className = null,  // No className - should use reference from previous match
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "address",
                                    operator = "=",
                                    value = TypedStringLiteralExpression(
                                        evalType = 1,  // builtin.string (type index)
                                        value = "example"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        // Create AST with both statements
        val transformationAst = TypedAst(
            types = types,
            metamodelPath = "./metamodel.mm",
            statements = listOf(matchHouseStatement, updateHouseStatement)
        )

        // Create engine with the transformation AST
        val transformationEngine = TransformationEngine(
            traversalSource = g,
            ast = transformationAst,
            metamodelData = metamodelData,
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )

        // Execute the transformation
        // Expected: Should succeed and update the house address to "example"
        // Actual: Currently fails with "no match can be found"
        val result = transformationEngine.execute()
        
        // Print failure details if it failed
        if (result is TransformationExecutionResult.Failure) {
            println("Test failed with error: ${result.reason}")
        }
        
        assertIs<TransformationExecutionResult.Success>(
            result,
            "Update reference match should succeed using instance from previous match"
        )

        // Verify final state: address should be updated
        val finalAddress = g.V().hasLabel("House").values<String>("address").next()
        assertEquals("example", finalAddress, "Address should be updated to 'example'")
    }
}
