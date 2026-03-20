package com.mdeo.metamodel

import com.mdeo.metamodel.data.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [Metamodel.loadModel] and [Model.toModelData] — verifying correct
 * construction of live models from [ModelData] and round-trip serialisation.
 */
class ModelTest {

    // ────────────────────────────────────────────────────────────────────────────
    // Shared test metamodel: House --rooms--> many Rooms, bidirectional
    // ────────────────────────────────────────────────────────────────────────────

    private val houseMetamodelData = MetamodelData(
        path = "/house.mm",
        classes = listOf(
            ClassData(
                name = "House",
                isAbstract = false,
                properties = listOf(
                    PropertyData(name = "address", primitiveType = "string", multiplicity = MultiplicityData.optional())
                )
            ),
            ClassData(
                name = "Room",
                isAbstract = false,
                properties = listOf(
                    PropertyData(name = "area", primitiveType = "double", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "floor", primitiveType = "int", multiplicity = MultiplicityData.single())
                )
            )
        ),
        associations = listOf(
            AssociationData(
                source = AssociationEndData(className = "House", name = "rooms", multiplicity = MultiplicityData.many()),
                operator = "<-->",
                target = AssociationEndData(className = "Room", name = "house", multiplicity = MultiplicityData.single())
            )
        )
    )

    private val houseMetamodel by lazy { Metamodel.compile(houseMetamodelData) }

    private fun houseModelData(numRooms: Int = 2): ModelData {
        val instances = mutableListOf<ModelDataInstance>()
        instances += ModelDataInstance(
            name = "house",
            className = "House",
            properties = mapOf("address" to ModelDataPropertyValue.StringValue("123 Main St"))
        )
        for (i in 1..numRooms) {
            instances += ModelDataInstance(
                name = "room$i",
                className = "Room",
                properties = mapOf(
                    "area" to ModelDataPropertyValue.NumberValue(20.0 + i),
                    "floor" to ModelDataPropertyValue.NumberValue(i.toDouble())
                )
            )
        }
        val links = (1..numRooms).map { i ->
            ModelDataLink(
                sourceName = "house",
                sourceProperty = "rooms",
                targetName = "room$i",
                targetProperty = "house"
            )
        }
        return ModelData(metamodelPath = "/house.mm", instances = instances, links = links)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Basic model loading
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `load empty model produces model with no instances`() {
        val mm = Metamodel.compile(MetamodelData())
        val model = mm.loadModel(ModelData(metamodelPath = "/empty.mm", instances = emptyList(), links = emptyList()))
        assertTrue(model.instancesByName.isEmpty())
    }

    @Test
    fun `load model populates instancesByName`() {
        val model = houseMetamodel.loadModel(houseModelData(2))
        assertTrue("house" in model.instancesByName)
        assertTrue("room1" in model.instancesByName)
        assertTrue("room2" in model.instancesByName)
        assertEquals(3, model.instancesByName.size)
    }

    @Test
    fun `loaded instances have correct className`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        assertEquals("House", houseMetamodel.classNameOf(model.instancesByName["house"]!!))
        assertEquals("Room", houseMetamodel.classNameOf(model.instancesByName["room1"]!!))
    }

    @Test
    fun `loaded instances are keyed by correct name`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        assertTrue("house" in model.instancesByName)
        assertTrue("room1" in model.instancesByName)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Property loading
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `string property is loaded correctly`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val house = model.instancesByName["house"]!!
        assertEquals("123 Main St", house.getPropertyByKey("address"))
    }

    @Test
    fun `double property is loaded correctly`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val room = model.instancesByName["room1"]!!
        assertEquals(21.0, room.getPropertyByKey("area") as Double, 1e-9)
    }

    @Test
    fun `int property is loaded correctly`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val room = model.instancesByName["room1"]!!
        assertEquals(1, room.getPropertyByKey("floor"))
    }

    @Test
    fun `null optional property is loaded as null`() {
        // Load house without setting the address
        val modelData = ModelData(
            metamodelPath = "/house.mm",
            instances = listOf(ModelDataInstance("house", "House", emptyMap())),
            links = emptyList()
        )
        val model = houseMetamodel.loadModel(modelData)
        val house = model.instancesByName["house"]!!
        assertNull(house.getPropertyByKey("address"))
    }

    @Test
    fun `enum property is loaded correctly`() {
        val enumMetamodel = Metamodel.compile(
            MetamodelData(
                classes = listOf(
                    ClassData(
                        name = "Task",
                        isAbstract = false,
                        properties = listOf(
                            PropertyData(name = "priority", enumType = "Priority", multiplicity = MultiplicityData.optional())
                        )
                    )
                ),
                enums = listOf(EnumData(name = "Priority", entries = listOf("LOW", "MEDIUM", "HIGH")))
            )
        )
        val modelData = ModelData(
            metamodelPath = "/test.mm",
            instances = listOf(
                ModelDataInstance(
                    "task1", "Task",
                    mapOf("priority" to ModelDataPropertyValue.EnumValue("HIGH"))
                )
            ),
            links = emptyList()
        )
        val model = enumMetamodel.loadModel(modelData)
        val task = model.instancesByName["task1"]!!
        val priority = task.getPropertyByKey("priority")
        assertNotNull(priority)
        assertEquals("HIGH", priority.toString())
        assertSame(enumMetamodel.resolveEnumValue("Priority", "HIGH"), priority)
    }

    @Test
    fun `list property is loaded as mutable list`() {
        val mm = Metamodel.compile(
            MetamodelData(
                classes = listOf(
                    ClassData(
                        name = "Tagged",
                        isAbstract = false,
                        properties = listOf(
                            PropertyData(name = "tags", primitiveType = "string", multiplicity = MultiplicityData.many())
                        )
                    )
                )
            )
        )
        val modelData = ModelData(
            metamodelPath = "/test.mm",
            instances = listOf(
                ModelDataInstance(
                    "item1", "Tagged",
                    mapOf(
                        "tags" to ModelDataPropertyValue.ListValue(
                            listOf(
                                ModelDataPropertyValue.StringValue("alpha"),
                                ModelDataPropertyValue.StringValue("beta")
                            )
                        )
                    )
                )
            ),
            links = emptyList()
        )
        val model = mm.loadModel(modelData)
        val item = model.instancesByName["item1"]!!
        @Suppress("UNCHECKED_CAST")
        val tags = item.getPropertyByKey("tags") as List<String>
        assertEquals(listOf("alpha", "beta"), tags)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Link loading
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `single-valued link is loaded correctly`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val room = model.instancesByName["room1"]!!
        val house = model.instancesByName["house"]!!
        // room.house should point to house
        val houseRef = room.getPropertyByKey("house")
        assertSame(house, houseRef)
    }

    @Test
    fun `multi-valued link is loaded correctly`() {
        val model = houseMetamodel.loadModel(houseModelData(3))
        val house = model.instancesByName["house"]!!
        @Suppress("UNCHECKED_CAST")
        val rooms = house.getPropertyByKey("rooms") as Set<ModelInstance>
        assertEquals(3, rooms.size)
        val nameByInstance = model.instancesByName.entries.associate { (k, v) -> v to k }
        assertEquals(setOf("room1", "room2", "room3"), rooms.map { nameByInstance[it]!! }.toSet())
    }

    @Test
    fun `bidirectional link both directions are set`() {
        val model = houseMetamodel.loadModel(houseModelData(2))
        val house = model.instancesByName["house"]!!
        val room1 = model.instancesByName["room1"]!!
        val room2 = model.instancesByName["room2"]!!

        @Suppress("UNCHECKED_CAST")
        val rooms = house.getPropertyByKey("rooms") as Set<ModelInstance>
        assertTrue(room1 in rooms)
        assertTrue(room2 in rooms)

        assertSame(house, room1.getPropertyByKey("house"))
        assertSame(house, room2.getPropertyByKey("house"))
    }

    // ────────────────────────────────────────────────────────────────────────────
    // getAllInstances
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `getAllInstances returns instances of given class`() {
        val model = houseMetamodel.loadModel(houseModelData(2))
        val rooms = model.getAllInstances("Room")
        assertEquals(2, rooms.size)
    }

    @Test
    fun `getAllInstances returns empty list for unknown class`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val unknown = model.getAllInstances("UnknownClass")
        assertTrue(unknown.isEmpty())
    }

    @Test
    fun `getAllInstances returns subtypes`() {
        val mm = Metamodel.compile(
            MetamodelData(
                classes = listOf(
                    ClassData(name = "Animal", isAbstract = true),
                    ClassData(name = "Dog", isAbstract = false, extends = listOf("Animal")),
                    ClassData(name = "Cat", isAbstract = false, extends = listOf("Animal"))
                )
            )
        )
        val modelData = ModelData(
            metamodelPath = "/test.mm",
            instances = listOf(
                ModelDataInstance("dog1", "Dog", emptyMap()),
                ModelDataInstance("cat1", "Cat", emptyMap()),
                ModelDataInstance("dog2", "Dog", emptyMap())
            ),
            links = emptyList()
        )
        val model = mm.loadModel(modelData)
        val animals = model.getAllInstances("Animal")
        assertEquals(3, animals.size)
        val dogs = model.getAllInstances("Dog")
        assertEquals(2, dogs.size)
        val cats = model.getAllInstances("Cat")
        assertEquals(1, cats.size)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // withModelProvider
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `withModelProvider makes class container all() work`() {
        val mm = Metamodel.compile(
            MetamodelData(
                classes = listOf(ClassData(name = "Node", isAbstract = false))
            )
        )
        val modelData = ModelData(
            metamodelPath = "/test.mm",
            instances = listOf(
                ModelDataInstance("n1", "Node", emptyMap()),
                ModelDataInstance("n2", "Node", emptyMap())
            ),
            links = emptyList()
        )
        val model = mm.loadModel(modelData)

        model.withModelProvider {
            val containerClassName = Metamodel.getClassContainerClassName("Node")
            val containerClass = mm.classLoader.loadClass(containerClassName.replace('/', '.'))
            val container = containerClass.getField("INSTANCE").get(null)
            val allMethod = containerClass.getMethod("all")
            @Suppress("UNCHECKED_CAST")
            val all = allMethod.invoke(container) as List<*>
            assertEquals(2, all.size)
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // toModelData round-trip
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `toModelData preserves metamodelPath`() {
        val modelData = houseModelData(1)
        val model = houseMetamodel.loadModel(modelData)
        val roundTripped = model.toModelData()
        assertEquals("/house.mm", roundTripped.metamodelPath)
    }

    @Test
    fun `toModelData preserves all instances`() {
        val original = houseModelData(2)
        val model = houseMetamodel.loadModel(original)
        val roundTripped = model.toModelData()

        val names = roundTripped.instances.map { it.name }.toSet()
        assertEquals(setOf("house", "room1", "room2"), names)
    }

    @Test
    fun `toModelData preserves string property`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val data = model.toModelData()
        val house = data.instances.first { it.name == "house" }
        assertEquals(ModelDataPropertyValue.StringValue("123 Main St"), house.properties["address"])
    }

    @Test
    fun `toModelData preserves numeric properties`() {
        val model = houseMetamodel.loadModel(houseModelData(1))
        val data = model.toModelData()
        val room = data.instances.first { it.name == "room1" }
        assertEquals(ModelDataPropertyValue.NumberValue(21.0), room.properties["area"])
        // int stored as double in NumberValue
        assertEquals(ModelDataPropertyValue.NumberValue(1.0), room.properties["floor"])
    }

    @Test
    fun `toModelData preserves links`() {
        val model = houseMetamodel.loadModel(houseModelData(2))
        val data = model.toModelData()

        // Should have exactly 2 links (one per room), each emitted once
        assertEquals(2, data.links.size)
        val linksByTarget = data.links.associateBy { it.targetName }
        assertTrue("room1" in linksByTarget)
        assertTrue("room2" in linksByTarget)
    }

    @Test
    fun `toModelData then loadModel produces equivalent model`() {
        val original = houseModelData(3)
        val model = houseMetamodel.loadModel(original)
        val roundTrippedData = model.toModelData()
        val reloaded = houseMetamodel.loadModel(roundTrippedData)

        // Same instance names
        assertEquals(model.instancesByName.keys, reloaded.instancesByName.keys)

        // Same property values
        val house1 = model.instancesByName["house"]!!
        val house2 = reloaded.instancesByName["house"]!!
        assertEquals(house1.getPropertyByKey("address"), house2.getPropertyByKey("address"))

        // Same link structure
        val nameByInstance1 = model.instancesByName.entries.associate { (k, v) -> v to k }
        val nameByInstance2 = reloaded.instancesByName.entries.associate { (k, v) -> v to k }
        @Suppress("UNCHECKED_CAST")
        val rooms1 = (house1.getPropertyByKey("rooms") as Set<ModelInstance>).map { nameByInstance1[it]!! }.toSet()
        @Suppress("UNCHECKED_CAST")
        val rooms2 = (house2.getPropertyByKey("rooms") as Set<ModelInstance>).map { nameByInstance2[it]!! }.toSet()
        assertEquals(rooms1, rooms2)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Instances ordering
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `instancesByName preserves insertion order`() {
        val expectedOrder = listOf("house", "room1", "room2", "room3")
        val model = houseMetamodel.loadModel(houseModelData(3))
        assertEquals(expectedOrder, model.instancesByName.keys.toList())
    }
}
