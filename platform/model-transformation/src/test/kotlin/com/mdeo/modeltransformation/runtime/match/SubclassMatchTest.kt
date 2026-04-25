package com.mdeo.modeltransformation.runtime.match

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.AssociationData
import com.mdeo.metamodel.data.AssociationEndData
import com.mdeo.metamodel.data.ClassData
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.metamodel.data.ModelDataInstance
import com.mdeo.metamodel.data.ModelDataLink
import com.mdeo.metamodel.data.MultiplicityData
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.graph.mdeo.MdeoModelGraph
import com.mdeo.modeltransformation.graph.ModelGraph
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph
import com.mdeo.modeltransformation.runtime.StatementExecutorRegistry
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertIs

/**
 * Tests for subclass/parent-class matching in [MatchExecutor].
 *
 * A pattern variable typed as `Room` should match vertices labelled `LargeRoom`
 * when `LargeRoom extends Room` in the metamodel.  Both [TinkerModelGraph] and
 * [MdeoModelGraph] backends are exercised for every scenario.
 */
class SubclassMatchTest {

    // ========================================================================
    // Metamodel helpers
    // ========================================================================

    /**
     * Metamodel with a two-level hierarchy:
     *   Vehicle (abstract)
     *     └── Car
     *     └── Truck
     *
     * and a standalone class Owner linked to Vehicle.
     */
    private fun buildHierarchyMetamodel(): MetamodelData = MetamodelData(
        path = "/metamodel.mm",
        classes = listOf(
            ClassData(name = "Vehicle", isAbstract = true),
            ClassData(name = "Car", isAbstract = false, extends = listOf("Vehicle")),
            ClassData(name = "Truck", isAbstract = false, extends = listOf("Vehicle")),
            ClassData(name = "Owner", isAbstract = false)
        ),
        enums = emptyList(),
        associations = listOf(
            AssociationData(
                source = AssociationEndData(
                    className = "Owner",
                    multiplicity = MultiplicityData(lower = 0, upper = -1),
                    name = "vehicles"
                ),
                operator = "<-->",
                target = AssociationEndData(
                    className = "Vehicle",
                    multiplicity = MultiplicityData(lower = 0, upper = 1),
                    name = "owner"
                )
            )
        ),
        importedMetamodelPaths = emptyList()
    )

    /**
     * A deeper three-level hierarchy:
     *   Room
     *     └── LargeRoom
     *           └── BallRoom
     */
    private fun buildDeepHierarchyMetamodel(): MetamodelData = MetamodelData(
        path = "/metamodel.mm",
        classes = listOf(
            ClassData(name = "Room", isAbstract = false),
            ClassData(name = "LargeRoom", isAbstract = false, extends = listOf("Room")),
            ClassData(name = "BallRoom", isAbstract = false, extends = listOf("LargeRoom"))
        ),
        enums = emptyList(),
        associations = emptyList(),
        importedMetamodelPaths = emptyList()
    )

    // ========================================================================
    // Graph backends
    // ========================================================================

    private val openGraphs = mutableListOf<ModelGraph>()

    @AfterEach
    fun tearDown() {
        openGraphs.forEach { it.close() }
        openGraphs.clear()
    }

    private fun tinkerGraph(modelData: ModelData, metamodel: Metamodel): TinkerModelGraph =
        TinkerModelGraph.create(modelData, metamodel).also { openGraphs.add(it) }

    private fun mdeoGraph(modelData: ModelData, metamodel: Metamodel): MdeoModelGraph =
        MdeoModelGraph.create(modelData, metamodel).also { openGraphs.add(it) }

    private fun createEngine(modelGraph: ModelGraph, metamodelData: MetamodelData): TransformationEngine {
        val metamodel = Metamodel.compile(metamodelData)
        return TransformationEngine(
            modelGraph = modelGraph,
            ast = TypedAst(types = emptyList(), metamodelPath = metamodelData.path, statements = emptyList()),
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )
    }

    private fun matchInstance(name: String, className: String): TypedPatternObjectInstanceElement =
        TypedPatternObjectInstanceElement(
            objectInstance = TypedPatternObjectInstance(
                modifier = null,
                name = name,
                className = className,
                properties = emptyList()
            )
        )

    // ========================================================================
    // 1. Matching by parent class should find subclass instances
    // ========================================================================

    @Nested
    inner class ParentClassMatchesSubclassInstances {

        @Test
        fun `tinker - match Room finds LargeRoom vertex`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "lr1", className = "LargeRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(matchInstance("r", "Room"))
                )
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result, "Matching a 'Room' pattern variable should find a 'LargeRoom' vertex (LargeRoom extends Room)")
        }

        @Test
        fun `mdeo - match Room finds LargeRoom vertex`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "lr1", className = "LargeRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(matchInstance("r", "Room"))
                )
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result, "Matching a 'Room' pattern variable should find a 'LargeRoom' vertex (LargeRoom extends Room)")
        }

        @Test
        fun `tinker - match Vehicle finds Car vertex (abstract parent)`() {
            val metamodelData = buildHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "car1", className = "Car", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(matchInstance("v", "Vehicle"))
                )
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result, "Matching abstract 'Vehicle' should find 'Car' vertex (Car extends Vehicle)")
        }

        @Test
        fun `mdeo - match Vehicle finds Car vertex (abstract parent)`() {
            val metamodelData = buildHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "car1", className = "Car", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(matchInstance("v", "Vehicle"))
                )
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result, "Matching abstract 'Vehicle' should find 'Car' vertex (Car extends Vehicle)")
        }
    }

    // ========================================================================
    // 2. Matching by concrete leaf class still works with exact label
    // ========================================================================

    @Nested
    inner class LeafClassMatchExact {

        @Test
        fun `tinker - match LargeRoom finds LargeRoom vertex`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "lr1", className = "LargeRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(matchInstance("r", "LargeRoom"))
                )
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `mdeo - match LargeRoom finds LargeRoom vertex`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "lr1", className = "LargeRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(matchInstance("r", "LargeRoom"))
                )
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result)
        }
    }

    // ========================================================================
    // 3. Deep hierarchy: top-level ancestor matches leaf instances
    // ========================================================================

    @Nested
    inner class DeepHierarchyAncestorMatch {

        @Test
        fun `tinker - match Room finds BallRoom (2 levels deep)`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "br1", className = "BallRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(elements = listOf(matchInstance("r", "Room")))
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result, "Matching 'Room' should find 'BallRoom' vertex (BallRoom extends LargeRoom extends Room)")
        }

        @Test
        fun `mdeo - match Room finds BallRoom (2 levels deep)`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "br1", className = "BallRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(elements = listOf(matchInstance("r", "Room")))
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Success>(result, "Matching 'Room' should find 'BallRoom' vertex (BallRoom extends LargeRoom extends Room)")
        }

        @Test
        fun `tinker - match Room finds both LargeRoom and BallRoom vertices`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "lr1", className = "LargeRoom", properties = emptyMap()),
                    ModelDataInstance(name = "br1", className = "BallRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val context = TransformationExecutionContext.empty()
            val matchExecutor = MatchExecutor()
            val result = matchExecutor.executeMatch(
                TypedPattern(elements = listOf(matchInstance("r", "Room"))),
                context, engine
            )
            assertIs<MatchResult.Matched>(result, "Matching 'Room' should find one of [LargeRoom, BallRoom]")
        }

        @Test
        fun `mdeo - match Room finds both LargeRoom and BallRoom vertices`() {
            val metamodelData = buildDeepHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "lr1", className = "LargeRoom", properties = emptyMap()),
                    ModelDataInstance(name = "br1", className = "BallRoom", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val context = TransformationExecutionContext.empty()
            val matchExecutor = MatchExecutor()
            val result = matchExecutor.executeMatch(
                TypedPattern(elements = listOf(matchInstance("r", "Room"))),
                context, engine
            )
            assertIs<MatchResult.Matched>(result, "Matching 'Room' should find one of [LargeRoom, BallRoom]")
        }
    }

    // ========================================================================
    // 4. Edge traversal: walk to a vertex matched by parent class
    // ========================================================================

    @Nested
    inner class EdgeWalkToSubclassVertex {

        @Test
        fun `tinker - edge walk to Vehicle from Owner finds Car vertex`() {
            val metamodelData = buildHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "owner1", className = "Owner", properties = emptyMap()),
                    ModelDataInstance(name = "car1", className = "Car", properties = emptyMap())
                ),
                links = listOf(
                    ModelDataLink(
                        sourceName = "owner1", sourceProperty = "vehicles",
                        targetName = "car1", targetProperty = "owner"
                    )
                )
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            // Build a pattern: match owner --vehicles--> vehicle(Vehicle)
            val ownerInst = matchInstance("owner", "Owner")
            val vehicleInst = matchInstance("v", "Vehicle")
            val link = com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement(
                link = com.mdeo.modeltransformation.ast.patterns.TypedPatternLink(
                    source = com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd(
                        objectName = "owner", propertyName = "vehicles"
                    ),
                    target = com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd(
                        objectName = "v", propertyName = "owner"
                    )
                )
            )

            val context = TransformationExecutionContext.empty()
            val matchExecutor = MatchExecutor()
            val result = matchExecutor.executeMatch(
                TypedPattern(elements = listOf(ownerInst, vehicleInst, link)),
                context, engine
            )
            assertIs<MatchResult.Matched>(result, "Following edge from Owner to Vehicle should find Car (Car extends Vehicle)")
        }

        @Test
        fun `mdeo - edge walk to Vehicle from Owner finds Car vertex`() {
            val metamodelData = buildHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "owner1", className = "Owner", properties = emptyMap()),
                    ModelDataInstance(name = "car1", className = "Car", properties = emptyMap())
                ),
                links = listOf(
                    ModelDataLink(
                        sourceName = "owner1", sourceProperty = "vehicles",
                        targetName = "car1", targetProperty = "owner"
                    )
                )
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val ownerInst = matchInstance("owner", "Owner")
            val vehicleInst = matchInstance("v", "Vehicle")
            val link = com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement(
                link = com.mdeo.modeltransformation.ast.patterns.TypedPatternLink(
                    source = com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd(
                        objectName = "owner", propertyName = "vehicles"
                    ),
                    target = com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd(
                        objectName = "v", propertyName = "owner"
                    )
                )
            )

            val context = TransformationExecutionContext.empty()
            val matchExecutor = MatchExecutor()
            val result = matchExecutor.executeMatch(
                TypedPattern(elements = listOf(ownerInst, vehicleInst, link)),
                context, engine
            )
            assertIs<MatchResult.Matched>(result, "Following edge from Owner to Vehicle should find Car (Car extends Vehicle)")
        }
    }

    // ========================================================================
    // 5. Parent class match does NOT find unrelated class instances
    // ========================================================================

    @Nested
    inner class ParentClassDoesNotMatchUnrelated {

        @Test
        fun `tinker - match Vehicle fails when only Owner vertices exist`() {
            val metamodelData = buildHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "owner1", className = "Owner", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(elements = listOf(matchInstance("v", "Vehicle")))
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `mdeo - match Vehicle fails when only Owner vertices exist`() {
            val metamodelData = buildHierarchyMetamodel()
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(
                    ModelDataInstance(name = "owner1", className = "Owner", properties = emptyMap())
                ),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(elements = listOf(matchInstance("v", "Vehicle")))
            )
            val result = engine.executeStatement(statement, TransformationExecutionContext.empty())
            assertIs<TransformationExecutionResult.Failure>(result)
        }
    }

    // ========================================================================
    // 6. No hierarchy (simple metamodel) - existing behavior unchanged
    // ========================================================================

    @Nested
    inner class NoHierarchyBehaviorUnchanged {

        @Test
        fun `tinker - match Room still works without hierarchy`() {
            val metamodelData = MetamodelData(
                path = "/metamodel.mm",
                classes = listOf(ClassData(name = "Room", isAbstract = false)),
                enums = emptyList(), associations = emptyList(),
                importedMetamodelPaths = emptyList()
            )
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(ModelDataInstance(name = "r1", className = "Room", properties = emptyMap())),
                links = emptyList()
            )
            val engine = createEngine(tinkerGraph(modelData, metamodel), metamodelData)

            val result = engine.executeStatement(
                TypedMatchStatement(pattern = TypedPattern(listOf(matchInstance("r", "Room")))),
                TransformationExecutionContext.empty()
            )
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `mdeo - match Room still works without hierarchy`() {
            val metamodelData = MetamodelData(
                path = "/metamodel.mm",
                classes = listOf(ClassData(name = "Room", isAbstract = false)),
                enums = emptyList(), associations = emptyList(),
                importedMetamodelPaths = emptyList()
            )
            val metamodel = Metamodel.compile(metamodelData)
            val modelData = ModelData(
                metamodelPath = metamodelData.path,
                instances = listOf(ModelDataInstance(name = "r1", className = "Room", properties = emptyMap())),
                links = emptyList()
            )
            val engine = createEngine(mdeoGraph(modelData, metamodel), metamodelData)

            val result = engine.executeStatement(
                TypedMatchStatement(pattern = TypedPattern(listOf(matchInstance("r", "Room")))),
                TransformationExecutionContext.empty()
            )
            assertIs<TransformationExecutionResult.Success>(result)
        }
    }
}
