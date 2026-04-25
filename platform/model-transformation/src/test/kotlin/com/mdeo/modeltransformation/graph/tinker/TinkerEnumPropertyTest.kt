package com.mdeo.modeltransformation.graph.tinker

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.data.ClassData
import com.mdeo.metamodel.data.EnumData
import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.MultiplicityData
import com.mdeo.metamodel.data.PropertyData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.metamodel.data.ModelDataInstance
import com.mdeo.metamodel.data.ModelDataPropertyValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests that TinkerModelGraph correctly stores enum property values as
 * backtick-formatted strings in the underlying graph.
 *
 * Uses [TinkerModelGraph.create] as the entry point (the [ModelDataGraphLoader]
 * is an internal detail of the tinker package, not part of the public API).
 */
class TinkerEnumPropertyTest {

    private val openGraphs = mutableListOf<TinkerModelGraph>()

    private fun create(modelData: ModelData, metamodel: Metamodel): TinkerModelGraph =
        TinkerModelGraph.create(modelData, metamodel).also { openGraphs.add(it) }

    @AfterEach
    fun tearDown() {
        openGraphs.forEach { it.close() }
        openGraphs.clear()
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
            metamodelPath = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "order1",
                    className = "Order",
                    properties = mapOf("status" to ModelDataPropertyValue.EnumValue("PENDING"))
                )
            ),
            links = emptyList()
        )

        val graph = create(modelData, Metamodel.compile(metamodelData))
        val g = graph.traversal()

        // Property "status" is stored under graph key "prop_0"
        val statusValues = g.V().has("prop_0").values<String>("prop_0").toList()
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
            metamodelPath = "test",
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

        val graph = create(modelData, Metamodel.compile(metamodelData))
        val g = graph.traversal()

        // Property "tags" is stored under graph key "prop_0"
        val tagValues = g.V().has("prop_0").values<String>("prop_0").toList()
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
            metamodelPath = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "special1",
                    className = "SpecialOrder",
                    properties = mapOf("status" to ModelDataPropertyValue.EnumValue("OPEN"))
                )
            ),
            links = emptyList()
        )

        val graph = create(modelData, Metamodel.compile(metamodelData))
        val g = graph.traversal()

        // Property "status" is stored under graph key "prop_0" (inherited from BaseOrder)
        val statusValues = g.V().has("prop_0").values<String>("prop_0").toList()
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
            metamodelPath = "test",
            instances = listOf(
                ModelDataInstance(
                    name = "product1",
                    className = "Product",
                    properties = mapOf("label" to ModelDataPropertyValue.StringValue("hello"))
                )
            ),
            links = emptyList()
        )

        val graph = create(modelData, Metamodel.compile(metamodelData))
        val g = graph.traversal()

        // Property "label" is stored under graph key "prop_0"
        val labelValues = g.V().has("prop_0").values<String>("prop_0").toList()
        assertEquals(1, labelValues.size)
        assertEquals("hello", labelValues.first())
    }
}
