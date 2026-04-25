package com.mdeo.modeltransformation.graph

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.SerializedModel
import com.mdeo.metamodel.data.*
import com.mdeo.modeltransformation.graph.mdeo.MdeoModelGraph
import com.mdeo.modeltransformation.graph.tinker.TinkerModelGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Performance comparison between [SerializedModel.AsBinary] (via [ModelBinarySerializer])
 * and [SerializedModel.AsModelData] (via [ModelData]) for round-trip serialization and
 * deserialization of [MdeoModelGraph] and [TinkerModelGraph].
 *
 * Each test verifies correctness (data round-trips without loss) and measures elapsed
 * time for both paths, printing results to stdout.
 */
class SerializedModelPerformanceTest {

    private val metamodelData = MetamodelData(
        path = "/perf.mm",
        classes = listOf(
            ClassData(
                name = "Component",
                isAbstract = false,
                properties = listOf(
                    PropertyData(name = "label", primitiveType = "string", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "weight", primitiveType = "double", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "active", primitiveType = "boolean", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "revision", primitiveType = "int", multiplicity = MultiplicityData.single())
                )
            ),
            ClassData(
                name = "Connector",
                isAbstract = false,
                properties = listOf(
                    PropertyData(name = "bandwidth", primitiveType = "double", multiplicity = MultiplicityData.single())
                )
            )
        ),
        enums = emptyList(),
        associations = listOf(
            AssociationData(
                source = AssociationEndData(className = "Component", name = "connectors", multiplicity = MultiplicityData.many()),
                operator = "<>->",
                target = AssociationEndData(className = "Connector", name = "owner", multiplicity = MultiplicityData.single())
            ),
            AssociationData(
                source = AssociationEndData(className = "Connector", name = "target", multiplicity = MultiplicityData.single()),
                operator = "-->",
                target = AssociationEndData(className = "Component", name = "incoming", multiplicity = MultiplicityData.many())
            )
        )
    )

    private val metamodel = Metamodel.compile(metamodelData)

    private val closableGraphs = mutableListOf<AutoCloseable>()

    @AfterEach
    fun tearDown() {
        closableGraphs.forEach { it.close() }
    }

    /**
     * Builds a [ModelData] instance with [componentCount] components, each owning
     * one connector that targets the next component (cyclic).
     */
    private fun buildModelData(componentCount: Int): ModelData {
        val instances = mutableListOf<ModelDataInstance>()
        val links = mutableListOf<ModelDataLink>()

        for (i in 0 until componentCount) {
            instances += ModelDataInstance(
                name = "comp$i",
                className = "Component",
                properties = mapOf(
                    "label" to ModelDataPropertyValue.StringValue("Component-$i"),
                    "weight" to ModelDataPropertyValue.NumberValue(i * 1.5),
                    "active" to ModelDataPropertyValue.BooleanValue(i % 2 == 0),
                    "revision" to ModelDataPropertyValue.NumberValue(i.toDouble())
                )
            )
            instances += ModelDataInstance(
                name = "conn$i",
                className = "Connector",
                properties = mapOf(
                    "bandwidth" to ModelDataPropertyValue.NumberValue(100.0 + i)
                )
            )
            links += ModelDataLink(
                sourceName = "comp$i",
                sourceProperty = "connectors",
                targetName = "conn$i",
                targetProperty = "owner"
            )
            val targetComp = (i + 1) % componentCount
            links += ModelDataLink(
                sourceName = "conn$i",
                sourceProperty = "target",
                targetName = "comp$targetComp",
                targetProperty = "incoming"
            )
        }

        return ModelData(metamodelPath = "perf.mm", instances = instances, links = links)
    }

    @Test
    fun mdeoModelGraph_binaryRoundTrip_preservesData() {
        val modelData = buildModelData(50)
        val graph = MdeoModelGraph.create(modelData, metamodel)
        closableGraphs += graph

        val serialized = graph.toSerializedModel()
        assertTrue(serialized is SerializedModel.AsBinary)

        val restored = MdeoModelGraph.create(serialized, metamodel)
        closableGraphs += restored

        val originalData = graph.toModelData()
        val restoredData = restored.toModelData()

        assertEquals(originalData.instances.size, restoredData.instances.size)
        assertEquals(originalData.links.size, restoredData.links.size)
    }

    @Test
    fun tinkerModelGraph_modelDataRoundTrip_preservesData() {
        val modelData = buildModelData(50)
        val graph = TinkerModelGraph.create(modelData, metamodel)
        closableGraphs += graph

        val serialized = graph.toSerializedModel()
        assertTrue(serialized is SerializedModel.AsModelData)

        val restoredModelData = serialized.toModelData(metamodel)
        val restored = TinkerModelGraph.create(restoredModelData, metamodel)
        closableGraphs += restored

        val originalData = graph.toModelData()
        val restoredData = restored.toModelData()

        assertEquals(originalData.instances.size, restoredData.instances.size)
        assertEquals(originalData.links.size, restoredData.links.size)
    }

    @Test
    fun mdeoModelGraph_binaryVsModelData_performance() {
        val componentCount = 200
        val iterations = 50
        val modelData = buildModelData(componentCount)
        val graph = MdeoModelGraph.create(modelData, metamodel)
        closableGraphs += graph

        repeat(5) {
            graph.toSerializedModel()
            graph.toModelData()
        }

        val binaryNanos = measureRepeated(iterations) {
            graph.toSerializedModel()
        }
        val modelDataNanos = measureRepeated(iterations) {
            graph.toModelData()
        }

        val binaryMs = binaryNanos / 1_000_000.0
        val modelDataMs = modelDataNanos / 1_000_000.0
        val speedup = modelDataMs / binaryMs

        println("=== MdeoModelGraph serialization ($componentCount components, $iterations iterations) ===")
        println("  Binary (toSerializedModel):  %.2f ms total, %.3f ms/iter".format(binaryMs, binaryMs / iterations))
        println("  ModelData (toModelData):     %.2f ms total, %.3f ms/iter".format(modelDataMs, modelDataMs / iterations))
        println("  Speedup: %.2fx".format(speedup))

        val binaryResult = graph.toSerializedModel() as SerializedModel.AsBinary
        println("  Binary payload size: ${binaryResult.data.size} bytes")
    }

    @Test
    fun mdeoModelGraph_binaryVsModelData_deserializationPerformance() {
        val componentCount = 200
        val iterations = 50
        val modelData = buildModelData(componentCount)
        val graph = MdeoModelGraph.create(modelData, metamodel)
        closableGraphs += graph

        val serialized = graph.toSerializedModel()
        val serializedModelData = SerializedModel.AsModelData(graph.toModelData())

        repeat(5) {
            val g1 = MdeoModelGraph.create(serialized, metamodel)
            g1.close()
            val g2 = MdeoModelGraph.create(serializedModelData, metamodel)
            g2.close()
        }

        val binaryNanos = measureRepeated(iterations) {
            val g = MdeoModelGraph.create(serialized, metamodel)
            g.close()
        }
        val modelDataNanos = measureRepeated(iterations) {
            val g = MdeoModelGraph.create(serializedModelData, metamodel)
            g.close()
        }

        val binaryMs = binaryNanos / 1_000_000.0
        val modelDataMs = modelDataNanos / 1_000_000.0
        val speedup = modelDataMs / binaryMs

        println("=== MdeoModelGraph deserialization ($componentCount components, $iterations iterations) ===")
        println("  Binary (from AsBinary):    %.2f ms total, %.3f ms/iter".format(binaryMs, binaryMs / iterations))
        println("  ModelData (from AsModelData): %.2f ms total, %.3f ms/iter".format(modelDataMs, modelDataMs / iterations))
        println("  Speedup: %.2fx".format(speedup))
    }

    @Test
    fun mdeoModelGraph_binaryPayloadSize_smallerThanModelData() {
        val modelData = buildModelData(200)
        val graph = MdeoModelGraph.create(modelData, metamodel)
        closableGraphs += graph

        val binary = graph.toSerializedModel() as SerializedModel.AsBinary
        val modelDataJson = graph.toModelData().toString()

        println("=== Payload size comparison (200 components) ===")
        println("  Binary:    ${binary.data.size} bytes")
        println("  ModelData: ${modelDataJson.length} chars (approximate)")

        assertTrue(binary.data.size < modelDataJson.length,
            "Binary format should be smaller than ModelData toString representation")
    }

    private inline fun measureRepeated(iterations: Int, block: () -> Unit): Long {
        val start = System.nanoTime()
        repeat(iterations) { block() }
        return System.nanoTime() - start
    }
}
