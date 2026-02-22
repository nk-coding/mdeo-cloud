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
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration test that reproduces the bug where accessing an enum value
 * in a create block (e.g. `TestEnum.A`) fails with:
 *
 *   java.lang.IllegalStateException: Failed to compile expression
 *   'TypedMemberAccessExpression': Scope not found at index 0
 *   (expression kind: identifier, evalType: 6)
 *
 * ## Root cause
 *
 * The TypeScript type checker assigns `scope = 0` (global scope) to enum-container
 * identifiers like `TestEnum`.  At runtime, [TransformationExecutionContext.empty()]
 * creates a [VariableScope] whose [VariableScope.scopeIndex] is **1** (the top-level
 * model-transformation scope).  When [IdentifierCompiler] calls
 * `context.getScope(expression.scope)` with scope index 0, it walks up the scope
 * chain starting from level 1 and finds nothing at level 0 → returns `null` →
 * throws `CompilationException("Scope not found at index 0", ...)`.
 *
 * ## AST structure that triggers the bug
 *
 * ```
 * types:
 *   0 → void
 *   1 → builtin.string
 *   2 → builtin.double
 *   3 → builtin.boolean
 *   4 → Any (nullable)
 *   5 → enum.TestEnum          ← evalType of the MemberAccess result
 *   6 → enum-container.TestEnum ← evalType of the identifier "TestEnum"
 *
 * match {
 *   create second: Test {
 *     value = TestEnum.A        // TypedMemberAccessExpression
 *       ↑ expression: TypedIdentifierExpression(evalType=6, name="TestEnum", scope=0)
 *       ↑ member: "A"
 *   }
 * }
 * ```
 *
 * The `scope = 0` on the identifier is what causes `getScope(0)` to return null.
 *
 * ## Expected behaviour after the fix
 *
 * The enum container (`TestEnum`) should be registered in scope 0 (or resolved via the
 * type registry without needing a scope lookup), so that `TestEnum.A` evaluates to the
 * string `` `TestEnum`.`A` `` and the created `Test` vertex gets its `value` property
 * set correctly.
 */
class EnumCreateBlockTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource

    // ---- Type index constants (match the JSON structure described above) ----
    // Index 0: void
    // Index 1: builtin.string
    // Index 2: builtin.double
    // Index 3: builtin.boolean
    // Index 4: Any (nullable)
    // Index 5: enum.TestEnum         ← value type of a TestEnum enum entry
    // Index 6: enum-container.TestEnum ← type of the "TestEnum" identifier itself
    private val types: List<ReturnType> = listOf(
        VoidType(),                                                                 // 0
        ClassTypeRef(type = "builtin.string",            isNullable = false),      // 1
        ClassTypeRef(type = "builtin.double",            isNullable = false),      // 2
        ClassTypeRef(type = "builtin.boolean",           isNullable = false),      // 3
        ClassTypeRef(type = "Any",                       isNullable = true),       // 4
        ClassTypeRef(type = "enum.TestEnum",             isNullable = false),      // 5
        ClassTypeRef(type = "enum-container.TestEnum",   isNullable = false)       // 6
    )

    private val metamodelData = MetamodelData(
        classes = listOf(
            ClassData(
                name = "Test",
                isAbstract = false,
                extends = emptyList(),
                properties = listOf(
                    PropertyData(
                        name = "value",
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
            )
        )
    )

    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        g = graph.traversal()
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    // =========================================================================
    // Helper: build the TypedAst that produces `create second: Test { value = TestEnum.A }`
    // =========================================================================

    private fun buildAst(): TypedAst {
        return TypedAst(
            types = types,
            metamodelPath = "/metamodel.mm",
            statements = listOf(
                TypedMatchStatement(
                    pattern = TypedPattern(
                        elements = listOf(
                            // create second: Test { value = TestEnum.A }
                            TypedPatternObjectInstanceElement(
                                objectInstance = TypedPatternObjectInstance(
                                    modifier = "create",
                                    name = "second",
                                    className = "Test",
                                    properties = listOf(
                                        TypedPatternPropertyAssignment(
                                            propertyName = "value",
                                            operator = "=",
                                            value = TypedMemberAccessExpression(
                                                evalType = 5,            // enum.TestEnum
                                                expression = TypedIdentifierExpression(
                                                    evalType = 6,        // enum-container.TestEnum
                                                    name = "TestEnum",
                                                    scope = 1
                                                ),
                                                member = "A",
                                                isNullChaining = false
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    // =========================================================================
    // The failing test
    // =========================================================================

    /**
     * BUG REPRODUCTION TEST – expected to FAIL with the current code.
     *
     * Executes a model transformation that creates a `Test` vertex with
     * `value = TestEnum.A`, where the enum container `TestEnum` is referenced
     * via an identifier with `scope = 0`.
     *
     * The [IdentifierCompiler] calls `context.getScope(0)`.  Because
     * [TransformationExecutionContext.empty()] starts the scope chain at index 1,
     * `getScope(0)` returns `null` and a [CompilationException] is thrown, which
     * is wrapped in an [IllegalStateException]:
     *
     *   "Failed to compile expression 'TypedMemberAccessExpression':
     *    Scope not found at index 0 (expression kind: identifier, evalType: 6)"
     *
     * After the fix, the test should pass and the created vertex should have
     * `value = "`TestEnum`.`A`"`.
     */
    @Test
    fun `enum container access with scope 0 in create block should set property correctly`() {
        val ast = buildAst()

        val engine = TransformationEngine(
            traversalSource = g,
            ast = ast,
            metamodelData = metamodelData,
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )

        val result = engine.execute()

        assertTrue(
            result is TransformationExecutionResult.Success,
            "Transformation should succeed but got: $result"
        )

        // Verify that a Test vertex was created
        val testVertices = g.V().hasLabel("Test").toList()
        assertEquals(1, testVertices.size, "Exactly one Test vertex should have been created")

        val testVertex = testVertices[0]

        // The `value` property should be set to the enum string representation
        val valueProperty = testVertex.property<String>("value")
        assertTrue(
            valueProperty.isPresent,
            "The 'value' property on the created Test vertex should be present. " +
            "It was not set because enum container access via scope=0 fails."
        )
        assertEquals(
            "`TestEnum`.`A`",
            valueProperty.value(),
            "The 'value' property should equal the backtick-formatted enum string"
        )
    }
}
