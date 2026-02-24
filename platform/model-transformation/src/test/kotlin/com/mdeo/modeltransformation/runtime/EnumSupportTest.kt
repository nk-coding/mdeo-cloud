package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.EnumData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.MultiplicityData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.expressions.EqualityCompilerUtil
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for enum support in model transformations.
 * 
 * Verifies that:
 * 1. Enum types are correctly registered in the type registry
 * 2. Enum container types produce constant string values with backtick format
 * 3. Equality comparison between enum and non-enum types returns constant false/true
 */
class EnumSupportTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var engine: TransformationEngine

    private val metamodelData = MetamodelData(
        classes = listOf(
            ClassData(
                name = "TestClass",
                isAbstract = false,
                extends = emptyList(),
                properties = listOf(
                    PropertyData(
                        name = "status",
                        enumType = "TestEnum",
                        multiplicity = MultiplicityData.single()
                    )
                )
            )
        ),
        enums = listOf(
            EnumData(
                name = "TestEnum",
                entries = listOf("A", "B", "C")
            ),
            EnumData(
                name = "StatusEnum",
                entries = listOf("ACTIVE", "INACTIVE", "PENDING")
            )
        )
    )

    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()

        engine = TransformationEngine(
            traversalSource = g,
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
            metamodelData = metamodelData,
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    // ==================== Type Registration Tests ====================

    @Test
    fun `enum container types are registered in the type registry`() {
        val typeRegistry = engine.typeRegistry

        // Check that enum-container types are registered
        val testEnumContainer = typeRegistry.getType(ClassTypeRef(engine.enumContainerPackage, "TestEnum", false))
        assertNotNull(testEnumContainer, "enum-container.TestEnum should be registered")

        val statusEnumContainer = typeRegistry.getType(ClassTypeRef(engine.enumContainerPackage, "StatusEnum", false))
        assertNotNull(statusEnumContainer, "enum-container.StatusEnum should be registered")
    }

    @Test
    fun `enum value types are registered in the type registry`() {
        val typeRegistry = engine.typeRegistry

        // Check that enum value types are registered
        val testEnumType = typeRegistry.getType(ClassTypeRef(engine.enumPackage, "TestEnum", false))
        assertNotNull(testEnumType, "enum.TestEnum should be registered")

        val statusEnumType = typeRegistry.getType(ClassTypeRef(engine.enumPackage, "StatusEnum", false))
        assertNotNull(statusEnumType, "enum.StatusEnum should be registered")
    }

    @Test
    fun `enum container has properties for each entry`() {
        val typeRegistry = engine.typeRegistry

        val testEnumContainer = typeRegistry.getType(ClassTypeRef(engine.enumContainerPackage, "TestEnum", false))!!

        // Check that each entry is a property
        val propertyA = testEnumContainer.getProperty("A")
        assertNotNull(propertyA, "Property 'A' should exist on enum-container.TestEnum")

        val propertyB = testEnumContainer.getProperty("B")
        assertNotNull(propertyB, "Property 'B' should exist on enum-container.TestEnum")

        val propertyC = testEnumContainer.getProperty("C")
        assertNotNull(propertyC, "Property 'C' should exist on enum-container.TestEnum")
    }

    // ==================== Enum Value String Format Tests ====================

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `enum entry produces backtick-formatted string`() {
        val typeRegistry = engine.typeRegistry

        val testEnumContainer = typeRegistry.getType(ClassTypeRef(engine.enumContainerPackage, "TestEnum", false))!!
        val propertyA = testEnumContainer.getProperty("A")!!

        // Compile the property access (the receiver is ignored for enum entries)
        val result = propertyA.compile(AnonymousTraversal.identity<Any>())

        // Execute the traversal to get the value
        val value = g.inject(1).flatMap(result.traversal as GraphTraversal<Int, Any>).next()

        assertEquals("`TestEnum`.`A`", value, "Enum entry should produce backtick-formatted string")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `different enum entries produce correct backtick-formatted strings`() {
        val typeRegistry = engine.typeRegistry

        val statusEnumContainer = typeRegistry.getType(ClassTypeRef(engine.enumContainerPackage, "StatusEnum", false))!!

        // Test ACTIVE entry
        val activeProperty = statusEnumContainer.getProperty("ACTIVE")!!
        val activeResult = activeProperty.compile(AnonymousTraversal.identity<Any>())
        val activeValue = g.inject(1).flatMap(activeResult.traversal as GraphTraversal<Int, Any>).next()
        assertEquals("`StatusEnum`.`ACTIVE`", activeValue)

        // Test INACTIVE entry
        val inactiveProperty = statusEnumContainer.getProperty("INACTIVE")!!
        val inactiveResult = inactiveProperty.compile(AnonymousTraversal.identity<Any>())
        val inactiveValue = g.inject(1).flatMap(inactiveResult.traversal as GraphTraversal<Int, Any>).next()
        assertEquals("`StatusEnum`.`INACTIVE`", inactiveValue)

        // Test PENDING entry
        val pendingProperty = statusEnumContainer.getProperty("PENDING")!!
        val pendingResult = pendingProperty.compile(AnonymousTraversal.identity<Any>())
        val pendingValue = g.inject(1).flatMap(pendingResult.traversal as GraphTraversal<Int, Any>).next()
        assertEquals("`StatusEnum`.`PENDING`", pendingValue)
    }

    // ==================== Equality Comparison Tests ====================

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `equality comparison between enum and non-enum type returns constant false`() {
        val enumType = ClassTypeRef(`package` = "enum", type = "TestEnum", isNullable = false)
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)

        val leftTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`A`") as GraphTraversal<Any, Any>
        val rightTraversal = AnonymousTraversal.constant<String>("someString") as GraphTraversal<Any, Any>

        val result = EqualityCompilerUtil.buildEqualityTraversal(
            operator = "==",
            leftTraversal = leftTraversal,
            rightTraversal = rightTraversal,
            leftType = enumType,
            rightType = stringType,
            registry = engine.typeRegistry,
            leftLabel = "left",
            rightLabel = "right"
        )

        // The result should be constant false
        val value = g.inject(1).flatMap(result as GraphTraversal<Int, Any>).next()
        assertEquals(false, value, "Comparing enum type with non-enum should return false")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `inequality comparison between enum and non-enum type returns constant true`() {
        val enumType = ClassTypeRef(`package` = "enum", type = "TestEnum", isNullable = false)
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)

        val leftTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`A`") as GraphTraversal<Any, Any>
        val rightTraversal = AnonymousTraversal.constant<String>("someString") as GraphTraversal<Any, Any>

        val result = EqualityCompilerUtil.buildEqualityTraversal(
            operator = "!=",
            leftTraversal = leftTraversal,
            rightTraversal = rightTraversal,
            leftType = enumType,
            rightType = stringType,
            registry = engine.typeRegistry,
            leftLabel = "left",
            rightLabel = "right"
        )

        // The result should be constant true
        val value = g.inject(1).flatMap(result as GraphTraversal<Int, Any>).next()
        assertEquals(true, value, "Inequality of enum type with non-enum should return true")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `equality comparison between same enum type values works normally`() {
        val enumType = ClassTypeRef(`package` = "enum", type = "TestEnum", isNullable = false)

        // Compare two equal enum values
        val leftTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`A`") as GraphTraversal<Any, Any>
        val rightTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`A`") as GraphTraversal<Any, Any>

        val result = EqualityCompilerUtil.buildEqualityTraversal(
            operator = "==",
            leftTraversal = leftTraversal,
            rightTraversal = rightTraversal,
            leftType = enumType,
            rightType = enumType,
            registry = engine.typeRegistry,
            leftLabel = "left",
            rightLabel = "right"
        )

        // The result should be true since both values are the same
        val value = g.inject(1).flatMap(result as GraphTraversal<Int, Any>).next()
        assertEquals(true, value, "Equal enum values should compare as true")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `equality comparison between different enum values of same type returns false`() {
        val enumType = ClassTypeRef(`package` = "enum", type = "TestEnum", isNullable = false)

        // Compare two different enum values
        val leftTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`A`") as GraphTraversal<Any, Any>
        val rightTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`B`") as GraphTraversal<Any, Any>

        val result = EqualityCompilerUtil.buildEqualityTraversal(
            operator = "==",
            leftTraversal = leftTraversal,
            rightTraversal = rightTraversal,
            leftType = enumType,
            rightType = enumType,
            registry = engine.typeRegistry,
            leftLabel = "left",
            rightLabel = "right"
        )

        // The result should be false since values are different
        val value = g.inject(1).flatMap(result as GraphTraversal<Int, Any>).next()
        assertEquals(false, value, "Different enum values should compare as false")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `non-enum to enum comparison also returns constant false`() {
        val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
        val enumType = ClassTypeRef(`package` = "enum", type = "TestEnum", isNullable = false)

        // Non-enum on left, enum on right
        val leftTraversal = AnonymousTraversal.constant<String>("someString") as GraphTraversal<Any, Any>
        val rightTraversal = AnonymousTraversal.constant<String>("`TestEnum`.`A`") as GraphTraversal<Any, Any>

        val result = EqualityCompilerUtil.buildEqualityTraversal(
            operator = "==",
            leftTraversal = leftTraversal,
            rightTraversal = rightTraversal,
            leftType = stringType,
            rightType = enumType,
            registry = engine.typeRegistry,
            leftLabel = "left",
            rightLabel = "right"
        )

        // The result should be constant false
        val value = g.inject(1).flatMap(result as GraphTraversal<Int, Any>).next()
        assertEquals(false, value, "Comparing non-enum with enum should return false")
    }
}
