package com.mdeo.modeltransformationexecution.service

import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.EnumData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.MultiplicityData
import com.mdeo.expression.ast.types.PropertyData
import com.mdeo.modeltransformation.ast.model.ModelData
import com.mdeo.modeltransformation.ast.model.ModelDataInstance
import com.mdeo.modeltransformation.ast.model.ModelDataPropertyValue
import com.mdeo.modeltransformation.runtime.InstanceNameRegistry
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ModelDataGraphLoaderEnumTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var loader: ModelDataGraphLoader
    private lateinit var nameRegistry: InstanceNameRegistry

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        loader = ModelDataGraphLoader()
        nameRegistry = InstanceNameRegistry()
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    @Test
    fun `enum value is stored as backtick-formatted string in graph`() {
        val metamodelData = MetamodelData(
            classes = listOf(
                ClassData(
                    name = "Order",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "status",
                            enumType = "OrderStatus",
                            multiplicity = MultiplicityData.single()
                        )
                    )
                )
            ),
            enums = listOf(
                EnumData(name = "OrderStatus", entries = listOf("PENDING", "SHIPPED", "DELIVERED"))
            )
        )

        val modelData = ModelData(
            metamodelUri = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "order1",
                    className = "Order",
                    properties = mapOf("status" to ModelDataPropertyValue.EnumValue("PENDING"))
                )
            ),
            links = emptyList()
        )

        loader.load(g, modelData, nameRegistry, metamodelData)

        val statusValues = g.V().has("status").values<String>("status").toList()
        assertEquals(1, statusValues.size)
        assertEquals("`OrderStatus`.`PENDING`", statusValues.first())
    }

    @Test
    fun `enum value in list property is stored as backtick-formatted string`() {
        val metamodelData = MetamodelData(
            classes = listOf(
                ClassData(
                    name = "Item",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "tags",
                            enumType = "Tag",
                            multiplicity = MultiplicityData.many()
                        )
                    )
                )
            ),
            enums = listOf(
                EnumData(name = "Tag", entries = listOf("A", "B", "C"))
            )
        )

        val modelData = ModelData(
            metamodelUri = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "item1",
                    className = "Item",
                    properties = mapOf(
                        "tags" to ModelDataPropertyValue.ListValue(
                            listOf(
                                ModelDataPropertyValue.EnumValue("A"),
                                ModelDataPropertyValue.EnumValue("B")
                            )
                        )
                    )
                )
            ),
            links = emptyList()
        )

        loader.load(g, modelData, nameRegistry, metamodelData)

        val tagValues = g.V().has("tags").values<String>("tags").toList()
        assertEquals(2, tagValues.size)
        assertEquals(listOf("`Tag`.`A`", "`Tag`.`B`"), tagValues.sorted())
    }

    @Test
    fun `enum value is looked up from inherited parent class`() {
        val metamodelData = MetamodelData(
            classes = listOf(
                ClassData(
                    name = "BaseOrder",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "status",
                            enumType = "Status",
                            multiplicity = MultiplicityData.single()
                        )
                    )
                ),
                ClassData(
                    name = "SpecialOrder",
                    isAbstract = false,
                    extends = listOf("BaseOrder"),
                    properties = emptyList()
                )
            ),
            enums = listOf(
                EnumData(name = "Status", entries = listOf("OPEN", "CLOSED"))
            )
        )

        val modelData = ModelData(
            metamodelUri = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "special1",
                    className = "SpecialOrder",
                    properties = mapOf("status" to ModelDataPropertyValue.EnumValue("OPEN"))
                )
            ),
            links = emptyList()
        )

        loader.load(g, modelData, nameRegistry, metamodelData)

        val statusValues = g.V().has("status").values<String>("status").toList()
        assertEquals(1, statusValues.size)
        assertEquals("`Status`.`OPEN`", statusValues.first())
    }

    @Test
    fun `non-enum string value is stored unchanged`() {
        val metamodelData = MetamodelData(
            classes = listOf(
                ClassData(
                    name = "Product",
                    isAbstract = false,
                    properties = listOf(
                        PropertyData(
                            name = "label",
                            primitiveType = "String",
                            multiplicity = MultiplicityData.single()
                        )
                    )
                )
            )
        )

        val modelData = ModelData(
            metamodelUri = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "product1",
                    className = "Product",
                    properties = mapOf("label" to ModelDataPropertyValue.StringValue("hello"))
                )
            ),
            links = emptyList()
        )

        loader.load(g, modelData, nameRegistry, metamodelData)

        val labelValues = g.V().has("label").values<String>("label").toList()
        assertEquals(1, labelValues.size)
        assertEquals("hello", labelValues.first())
    }
}
