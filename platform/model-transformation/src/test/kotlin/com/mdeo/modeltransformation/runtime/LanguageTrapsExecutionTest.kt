package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.metamodel.data.ModelDataInstance
import com.mdeo.metamodel.data.ModelDataLink
import com.mdeo.metamodel.data.ModelDataPropertyValue
import com.mdeo.metamodel.data.merge
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import com.mdeo.modeltransformation.graph.ModelGraph
import com.mdeo.modeltransformation.graph.mdeo.MdeoModelGraph
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.step.LambdaHolder
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.VerificationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertIs

/**
 * The language traps the Palladio domain project's group operators ran into: two collection
 * lambdas in one expression, `isEmpty()`/`notEmpty()` as booleans, injectivity of bound objects,
 * a `where` that reads an input its pattern does not reference, `to`/`indexOf`/`includes` of the
 * script language and exponent float literals. The typed ASTs come from `TRAP_SCENARIOS` of
 * `app/packages/service-model-transformation/test/fixtures.ts` (resources `calls/`).
 *
 * Every traversal runs under [NoLambdaSteps]: the engine compiles to pure Gremlin, so that it also
 * runs against remote graph databases, and a lambda step anywhere in a traversal or its child
 * traversals fails the test.
 */
class LanguageTrapsExecutionTest {

    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }

    private fun resource(name: String): String =
        javaClass.getResource("/calls/$name")?.readText() ?: error("missing test resource calls/$name")

    private fun transformation(path: String): TypedAst =
        json.decodeFromString(TypedAst.serializer(), resource(path.trimStart('/').replace("/", "__") + ".json"))

    private val metamodelData: MetamodelData = MetamodelData.merge(
        json.decodeFromString(MetamodelData.serializer(), resource("metamodel__shop.mm.json")),
        listOf(json.decodeFromString(MetamodelData.serializer(), resource("metamodel__base.mm.json")))
    )

    private fun named(name: String, className: String) =
        ModelDataInstance(name, className, mapOf("name" to ModelDataPropertyValue.StringValue(name)))

    /** Containers `a`, `b`, `c`; the service `orders` and the component `ordersDb` on `a`, `billing` on `b`. */
    private fun shopModel() = ModelData(
        metamodelPath = "/metamodel/shop.mm",
        instances = listOf(
            named("a", "Container"),
            named("b", "Container"),
            named("c", "Container"),
            named("orders", "Service"),
            named("ordersDb", "Component"),
            named("billing", "Component")
        ),
        links = listOf(
            ModelDataLink("orders", "host", "a", null),
            ModelDataLink("ordersDb", "host", "a", null),
            ModelDataLink("billing", "host", "b", null)
        )
    )

    /**
     * Rejects every lambda step ([LambdaHolder]). TinkerPop's `LambdaRestrictionStrategy` only
     * rejects steps whose description contains "lambda", which misses Kotlin lambdas.
     */
    private class NoLambdaSteps :
        AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy>(),
        TraversalStrategy.VerificationStrategy {
        override fun apply(traversal: Traversal.Admin<*, *>) {
            for (step in traversal.steps) {
                if (step is LambdaHolder) throw VerificationException("lambda step $step", traversal)
            }
        }
    }

    /** A graph whose traversals reject lambda steps. */
    private class LambdaFreeGraph(private val inner: ModelGraph) : ModelGraph by inner {
        override fun traversal(): GraphTraversalSource = inner.traversal().withStrategies(NoLambdaSteps())
    }

    private fun graph(backend: String): ModelGraph {
        val metamodel = Metamodel.compile(metamodelData)
        val model = shopModel()
        val inner = if (backend == "tinker") TinkerModelGraph.create(model, metamodel) else MdeoModelGraph.create(model, metamodel)
        return LambdaFreeGraph(inner)
    }

    private fun run(backend: String, path: String): List<String> = graph(backend).use { graph ->
        val result = TransformationEngine.create(graph, transformation(path), deterministic = false).execute()
        assertIs<TransformationExecutionResult.Success>(result, result.toString())
        graph.toModelData().instances
            .mapNotNull { (it.properties["name"] as? ModelDataPropertyValue.StringValue)?.value }
            .filter { it !in setOf("a", "b", "c", "orders", "ordersDb", "billing") }
            .sorted()
    }

    @ParameterizedTest
    @ValueSource(strings = ["tinker", "mdeo"])
    fun `two collection lambdas in one expression`(backend: String) {
        assertEquals(
            listOf("on b billing", "only a orders", "only a ordersDb"),
            run(backend, "/optimize/scenarios/call-trap-lambdas.mt")
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["tinker", "mdeo"])
    fun `isEmpty and notEmpty are booleans`(backend: String) {
        assertEquals(listOf("empty false false"), run(backend, "/optimize/scenarios/call-trap-empty.mt"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["tinker", "mdeo"])
    fun `bound objects take part in injective matching, for objects of exactly their class`(backend: String) {
        // matesOf(seed = ordersDb): the Component mate is never ordersDb itself; matesOf(seed =
        // orders): orders is a Service, so a Component object may match it, as two pattern
        // objects of a class and its subclass may match one element (the pairs below)
        assertEquals(
            listOf("mates 1 2", "pair orders orders", "pair orders ordersDb", "svc mate orders", "svc mate ordersDb"),
            run(backend, "/optimize/scenarios/call-trap-injective.mt")
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["tinker", "mdeo"])
    fun `a where clause reads an input the pattern does not reference`(backend: String) {
        assertEquals(listOf("others 2 1 1"), run(backend, "/optimize/scenarios/call-trap-unreferenced.mt"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["tinker", "mdeo"])
    fun `to, indexOf and includes as in the script language`(backend: String) {
        assertEquals(
            listOf("includes orders", "includes ordersDb", "range 4 3 1 2 3", "step 2", "step 3", "step 4"),
            run(backend, "/optimize/scenarios/call-trap-functions.mt")
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["tinker", "mdeo"])
    fun `float literals with an exponent`(backend: String) {
        assertEquals(listOf("literals ok"), run(backend, "/optimize/scenarios/call-trap-literals.mt"))
    }

    @Test
    fun `the graph of these tests rejects lambda steps`() {
        graph("tinker").use { graph ->
            assertThrows<VerificationException> {
                graph.traversal().inject<Any>(1).map<Any> { it.get() }.toList()
            }
        }
    }
}
