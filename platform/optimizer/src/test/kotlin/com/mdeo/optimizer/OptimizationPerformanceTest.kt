package com.mdeo.optimizer

import com.mdeo.expression.ast.TypedCallableBody
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.types.AssociationData
import com.mdeo.expression.ast.types.AssociationEndData
import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.MultiplicityData
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.expression.ast.statements.TypedIfStatement
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.expression.ast.statements.TypedVariableDeclarationStatement
import com.mdeo.modeltransformation.ast.TypedAst as TransformationTypedAst
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLink
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import com.mdeo.modeltransformation.service.GraphToModelDataConverter
import com.mdeo.modeltransformation.service.ModelDataGraphLoader
import com.mdeo.optimizer.config.AlgorithmParameters
import com.mdeo.optimizer.config.AlgorithmType
import com.mdeo.optimizer.config.GoalConfig
import com.mdeo.optimizer.config.MutationParameters
import com.mdeo.optimizer.config.MutationStepConfig
import com.mdeo.optimizer.config.MutationStrategy
import com.mdeo.optimizer.config.MutationsConfig
import com.mdeo.optimizer.config.ObjectiveConfig
import com.mdeo.optimizer.config.ObjectiveTendency
import com.mdeo.optimizer.config.OptimizationConfig
import com.mdeo.optimizer.config.ProblemConfig
import com.mdeo.optimizer.config.SearchConfig
import com.mdeo.optimizer.config.SolverConfig
import com.mdeo.optimizer.config.SolverProvider
import com.mdeo.optimizer.config.TerminationConfig
import com.mdeo.optimizer.config.VariationType
import com.mdeo.optimizer.graph.GraphBackend
import com.mdeo.optimizer.graph.TinkerGraphBackend
import com.mdeo.optimizer.guidance.ScriptGuidanceFunction
import com.mdeo.optimizer.guidance.ScriptModelFactory
import com.mdeo.optimizer.solution.Solution
import com.mdeo.script.ast.TypedAst as ScriptTypedAst
import com.mdeo.script.ast.TypedFunction
import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.script.runtime.model.ModelDataScriptModel
import com.mdeo.script.runtime.model.ScriptModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * End-to-end performance test for the full optimization pipeline.
 *
 * Uses the complete configuration defined by fetch_file_data.py / file_data_output.json:
 * - Metamodel: House–Room with bidirectional association
 * - Model: single House instance
 * - Transformations: addRoom.mt, deleteRoom.mt
 * - Script: constraintsAndObjective.fn (has10Rooms objective)
 * - Solver: NSGAII, population=400, evolutions=500, batches=1
 *
 * Expected wall-clock runtime: approximately 1 minute.
 * No warmup — run it and measure.
 */
class OptimizationPerformanceTest {

    /**
     * Constructs the ScriptTypedAst for constraintsAndObjective.fn directly in Kotlin.
     *
     * Equivalent script (pseudo-code):
     *   fun has10Rooms(): int {
     *     val roomCount: int = Room.all().size()
     *     if (roomCount > 10) { return roomCount - 10 }
     *     else { return 10 - roomCount }
     *   }
     *
     * Type index table (matches the original JSON):
     *   0 = VoidType
     *   1 = builtin.string
     *   2 = builtin.double
     *   3 = builtin.boolean
     *   4 = builtin.Any?
     *   5 = builtin.int
     *   6 = builtin.Collection<class/metamodel.mm.Room>
     *   7 = class-container/metamodel.mm.Room
     */
    private fun buildScriptAst(): ScriptTypedAst {
        val types: List<ReturnType> = listOf(
            VoidType(),
            ClassTypeRef(`package` = "builtin", type = "string", isNullable = false, typeArgs = emptyMap()),
            ClassTypeRef(`package` = "builtin", type = "double", isNullable = false, typeArgs = emptyMap()),
            ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false, typeArgs = emptyMap()),
            ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true),
            ClassTypeRef(`package` = "builtin", type = "int", isNullable = false, typeArgs = emptyMap()),
            ClassTypeRef(
                `package` = "builtin", type = "Collection", isNullable = false,
                typeArgs = mapOf(
                    "T" to ClassTypeRef(`package` = "class/metamodel.mm", type = "Room", isNullable = false, typeArgs = emptyMap())
                )
            ),
            ClassTypeRef(`package` = "class-container/metamodel.mm", type = "Room", isNullable = false, typeArgs = emptyMap())
        )

        // roomCount identifier (evalType=5, scope=3)
        val roomCountIdent = TypedIdentifierExpression(evalType = 5, name = "roomCount", scope = 3)

        // int literal 10 (evalType=5)
        val ten = TypedIntLiteralExpression(evalType = 5, value = "10")

        // val roomCount: int = Room.all().size()
        val roomCountDecl = TypedVariableDeclarationStatement(
            name = "roomCount",
            type = 5,
            initialValue = TypedMemberCallExpression(
                evalType = 5,
                expression = TypedMemberCallExpression(
                    evalType = 6,
                    expression = TypedIdentifierExpression(evalType = 7, name = "Room", scope = 1),
                    member = "all",
                    isNullChaining = false,
                    overload = "",
                    arguments = emptyList()
                ),
                member = "size",
                isNullChaining = false,
                overload = "",
                arguments = emptyList()
            )
        )

        // if (roomCount > 10) { return roomCount - 10 } else { return 10 - roomCount }
        val ifStatement = TypedIfStatement(
            condition = TypedBinaryExpression(
                evalType = 3,
                operator = ">",
                left = roomCountIdent,
                right = ten
            ),
            thenBlock = listOf(
                TypedReturnStatement(
                    value = TypedBinaryExpression(
                        evalType = 5,
                        operator = "-",
                        left = roomCountIdent,
                        right = ten
                    )
                )
            ),
            elseIfs = emptyList(),
            elseBlock = listOf(
                TypedReturnStatement(
                    value = TypedBinaryExpression(
                        evalType = 5,
                        operator = "-",
                        left = ten,
                        right = roomCountIdent
                    )
                )
            )
        )

        return ScriptTypedAst(
            types = types,
            metamodelPath = "/metamodel.mm",
            imports = emptyList(),
            functions = listOf(
                TypedFunction(
                    name = "has10Rooms",
                    parameters = emptyList(),
                    returnType = 5,
                    body = TypedCallableBody(body = listOf(roomCountDecl, ifStatement))
                )
            )
        )
    }

    private fun buildMetamodelData(): MetamodelData = MetamodelData(
        path = "/metamodel.mm",
        classes = listOf(
            ClassData(name = "House", isAbstract = false),
            ClassData(name = "Room", isAbstract = false)
        ),
        enums = emptyList(),
        associations = listOf(
            AssociationData(
                source = AssociationEndData(
                    className = "House",
                    multiplicity = MultiplicityData(lower = 1, upper = 1),
                    name = "rooms"
                ),
                operator = "<-->",
                target = AssociationEndData(
                    className = "Room",
                    multiplicity = MultiplicityData(lower = 1, upper = 1),
                    name = "house"
                )
            )
        ),
        importedMetamodelPaths = emptyList()
    )

    private fun buildModelData(): ModelData = ModelData(
        metamodelUri = "../metamodel.mm",
        instances = listOf(
            ModelDataInstance(
                name = "house",
                className = "House",
                properties = emptyMap()
            )
        ),
        links = emptyList()
    )

    private fun buildTypes(): List<ReturnType> = listOf(
        VoidType(),
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false, typeArgs = emptyMap()),
        ClassTypeRef(`package` = "builtin", type = "double", isNullable = false, typeArgs = emptyMap()),
        ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false, typeArgs = emptyMap()),
        ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true)
    )

    private fun buildTransformations(): Map<String, TransformationTypedAst> {
        val types = buildTypes()

        val addRoomAst = TransformationTypedAst(
            types = types,
            metamodelPath = "/metamodel.mm",
            statements = listOf(
                TypedMatchStatement(
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
                            TypedPatternObjectInstanceElement(
                                objectInstance = TypedPatternObjectInstance(
                                    modifier = "create",
                                    name = "room",
                                    className = "Room",
                                    properties = emptyList()
                                )
                            ),
                            TypedPatternLinkElement(
                                link = TypedPatternLink(
                                    modifier = "create",
                                    source = TypedPatternLinkEnd(
                                        objectName = "house",
                                        propertyName = "rooms"
                                    ),
                                    target = TypedPatternLinkEnd(
                                        objectName = "room",
                                        propertyName = "house"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val deleteRoomAst = TransformationTypedAst(
            types = types,
            metamodelPath = "/metamodel.mm",
            statements = listOf(
                TypedMatchStatement(
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
                            TypedPatternObjectInstanceElement(
                                objectInstance = TypedPatternObjectInstance(
                                    modifier = "delete",
                                    name = "room",
                                    className = "Room",
                                    properties = emptyList()
                                )
                            ),
                            TypedPatternLinkElement(
                                link = TypedPatternLink(
                                    modifier = "delete",
                                    source = TypedPatternLinkEnd(
                                        objectName = "house",
                                        propertyName = "rooms"
                                    ),
                                    target = TypedPatternLinkEnd(
                                        objectName = "room",
                                        propertyName = "house"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        return mapOf(
            "/transformation/addRoom.mt" to addRoomAst,
            "/transformation/deleteRoom.mt" to deleteRoomAst
        )
    }

    private fun buildOptimizationConfig(): OptimizationConfig = OptimizationConfig(
        problem = ProblemConfig(
            metamodelPath = "/metamodel.mm",
            modelPath = "/model/model.m"
        ),
        goal = GoalConfig(
            objectives = listOf(
                ObjectiveConfig(
                    type = ObjectiveTendency.MINIMIZE,
                    path = "/script/constraintsAndObjective.fn",
                    functionName = "has10Rooms"
                )
            ),
            constraints = emptyList()
        ),
        search = SearchConfig(
            mutations = MutationsConfig(
                usingPaths = listOf(
                    "/transformation/addRoom.mt",
                    "/transformation/deleteRoom.mt"
                )
            )
        ),
        solver = SolverConfig(
            provider = SolverProvider.MOEA,
            algorithm = AlgorithmType.NSGAII,
            parameters = AlgorithmParameters(
                population = 400,
                variation = VariationType.MUTATION,
                mutation = MutationParameters(
                    step = MutationStepConfig.Fixed(n = 1),
                    strategy = MutationStrategy.RANDOM
                )
            ),
            termination = TerminationConfig(evolutions = 500),
            batches = 1
        )
    )

    @Test
    fun `full optimization end to end runs approximately 1 minute`() {
        val metamodelData = buildMetamodelData()
        val modelData = buildModelData()
        val transformations = buildTransformations()
        val scriptAst = buildScriptAst()

        val compiledProgram = ScriptCompiler().compile(
            CompilationInput(mapOf("/script/constraintsAndObjective.fn" to scriptAst)),
            metamodelData
        )
        val environment = ExecutionEnvironment(compiledProgram)
        val graphLoader = ModelDataGraphLoader()

        val scriptModelFactory = object : ScriptModelFactory {
            override fun create(graphBackend: GraphBackend): ScriptModel {
                val g = graphBackend.traversal()
                val registry = InstanceNameRegistry()
                g.V().toList().forEach { vertex ->
                    registry.registerWithUniqueName(vertex.id(), "${vertex.label()}${vertex.id()}")
                }
                val converter = GraphToModelDataConverter(
                    metamodelData = metamodelData,
                    types = emptyList(),
                    typeRegistry = TypeRegistry.GLOBAL
                )
                val convertedModel = converter.convert(g, modelData.metamodelUri, registry)
                return ModelDataScriptModel(
                    modelData = convertedModel,
                    metamodelData = metamodelData,
                    classLoader = environment.classLoader,
                    program = environment.program
                )
            }
        }

        val objective = ScriptGuidanceFunction(
            environment,
            "/script/constraintsAndObjective.fn",
            "has10Rooms",
            scriptModelFactory
        )

        val initialSolutionProvider: () -> Solution = {
            val backend = TinkerGraphBackend()
            val g = backend.traversal()
            val nameRegistry = InstanceNameRegistry()
            graphLoader.load(g, modelData, nameRegistry, metamodelData)
            Solution(backend)
        }

        val config = buildOptimizationConfig()

        val orchestrator = OptimizationOrchestrator(
            config = config,
            objectives = listOf(objective),
            constraints = emptyList(),
            transformations = transformations,
            metamodelData = metamodelData,
            initialSolutionProvider = initialSolutionProvider
        )

        val startTime = System.currentTimeMillis()
        runBlocking {
            orchestrator.run()
        }
        val elapsedMs = System.currentTimeMillis() - startTime
        val elapsedSec = elapsedMs / 1000.0

        println(
            "Optimization completed: elapsed=${elapsedMs}ms (${String.format("%.2f", elapsedSec)}s), " +
                "population=400, evolutions=500"
        )
    }
}
