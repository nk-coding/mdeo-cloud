package com.mdeo.modeltransformation.graph

import com.mdeo.metamodel.Metamodel
import com.mdeo.metamodel.ModelInstance
import com.mdeo.metamodel.data.*
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.graph.mdeo.MdeoGraph
import com.mdeo.modeltransformation.graph.mdeo.MdeoVertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Comprehensive tests for [MdeoModelGraph] and the underlying [MdeoGraph].
 *
 * Covers basic graph operations, comparison with [TinkerModelGraph],
 * deep copy, nondeterminism reset, ModelData round-trip, VertexRef tracking,
 * and ASM-generated property access on model instances.
 */
class MdeoModelGraphTest {

    private val metamodelData = MetamodelData(
        classes = listOf(
            ClassData(
                name = "House",
                isAbstract = false,
                extends = emptyList(),
                properties = listOf(
                    PropertyData(name = "address", primitiveType = "string", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "floors", primitiveType = "int", multiplicity = MultiplicityData.single())
                )
            ),
            ClassData(
                name = "Room",
                isAbstract = false,
                extends = emptyList(),
                properties = listOf(
                    PropertyData(name = "name", primitiveType = "string", multiplicity = MultiplicityData.single()),
                    PropertyData(name = "area", primitiveType = "double", multiplicity = MultiplicityData.single())
                )
            )
        ),
        enums = emptyList(),
        associations = listOf(
            AssociationData(
                source = AssociationEndData(className = "House", name = "rooms", multiplicity = MultiplicityData.many()),
                operator = "<>->",
                target = AssociationEndData(className = "Room", name = "house", multiplicity = MultiplicityData.single())
            )
        )
    )

    private val metamodel = Metamodel.compile(metamodelData)

    private val modelData = ModelData(
        metamodelPath = "test.mm",
        instances = listOf(
            ModelDataInstance(
                name = "house1",
                className = "House",
                properties = mapOf(
                    "address" to ModelDataPropertyValue.StringValue("123 Main St"),
                    "floors" to ModelDataPropertyValue.NumberValue(2.0)
                )
            ),
            ModelDataInstance(
                name = "room1",
                className = "Room",
                properties = mapOf(
                    "name" to ModelDataPropertyValue.StringValue("Kitchen"),
                    "area" to ModelDataPropertyValue.NumberValue(20.0)
                )
            ),
            ModelDataInstance(
                name = "room2",
                className = "Room",
                properties = mapOf(
                    "name" to ModelDataPropertyValue.StringValue("Bedroom"),
                    "area" to ModelDataPropertyValue.NumberValue(15.0)
                )
            )
        ),
        links = listOf(
            ModelDataLink(
                sourceName = "house1",
                sourceProperty = "rooms",
                targetName = "room1",
                targetProperty = "house"
            ),
            ModelDataLink(
                sourceName = "house1",
                sourceProperty = "rooms",
                targetName = "room2",
                targetProperty = "house"
            )
        )
    )

    private lateinit var mdeoGraph: MdeoModelGraph
    private lateinit var tinkerGraph: TinkerModelGraph

    @BeforeEach
    fun setUp() {
        mdeoGraph = MdeoModelGraph.create(modelData, metamodel)
        tinkerGraph = TinkerModelGraph.create(modelData, metamodel)
    }

    @AfterEach
    fun tearDown() {
        mdeoGraph.close()
        tinkerGraph.close()
    }

    /**
     * Helper to get the graph key for a property of a given class. 
     */
    private fun graphKey(className: String, propName: String): String {
        val fieldIndex = metamodel.metadata.classes[className]!!.propertyFields[propName]!!.fieldIndex
        return "prop_$fieldIndex"
    }

    // ========================================================================
    // Basic Graph Operations
    // ========================================================================

    @Test
    fun createFromModelData_producesCorrectVerticesAndEdges() {
        val g = mdeoGraph.traversal()
        val vertexCount = g.V().count().next()
        val edgeCount = g.E().count().next()

        assertEquals(3L, vertexCount)
        assertEquals(2L, edgeCount)

        // Verify labels
        val labels = g.V().label().toList().sorted()
        assertEquals(listOf("House", "Room", "Room"), labels)
    }

    @Test
    fun createEmpty_hasNoVerticesOrEdges() {
        val empty = MdeoModelGraph.create(
            ModelData(metamodelPath = "test.mm", instances = emptyList(), links = emptyList()),
            metamodel
        )
        val g = empty.traversal()

        assertEquals(0L, g.V().count().next())
        assertEquals(0L, g.E().count().next())
        empty.close()
    }

    @Test
    fun traversal_returnsWorkingGraphTraversalSource() {
        val g = mdeoGraph.traversal()
        assertNotNull(g)

        // Should be able to run a basic traversal
        val houseCount = g.V().hasLabel("House").count().next()
        assertEquals(1L, houseCount)
    }

    @Test
    fun vertexProperties_returnCorrectValues() {
        val g = mdeoGraph.traversal()
        val addressKey = graphKey("House", "address")
        val floorsKey = graphKey("House", "floors")

        val address = g.V().hasLabel("House").values<Any>(addressKey).next()
        assertEquals("123 Main St", address)

        val floors = g.V().hasLabel("House").values<Any>(floorsKey).next()
        assertEquals(2, floors)
    }

    @Test
    fun listProperties_returnAllElements() {
        // Create a metamodel with a list property
        val listMetamodelData = MetamodelData(
            classes = listOf(
                ClassData(
                    name = "Container",
                    isAbstract = false,
                    extends = emptyList(),
                    properties = listOf(
                        PropertyData(name = "tags", primitiveType = "string", multiplicity = MultiplicityData.many())
                    )
                )
            ),
            enums = emptyList(),
            associations = emptyList()
        )
        val listMetamodel = Metamodel.compile(listMetamodelData)
        val listModelData = ModelData(
            metamodelPath = "test.mm",
            instances = listOf(
                ModelDataInstance(
                    name = "c1",
                    className = "Container",
                    properties = mapOf(
                        "tags" to ModelDataPropertyValue.ListValue(
                            listOf(
                                ModelDataPropertyValue.StringValue("alpha"),
                                ModelDataPropertyValue.StringValue("beta"),
                                ModelDataPropertyValue.StringValue("gamma")
                            )
                        )
                    )
                )
            ),
            links = emptyList()
        )
        val listGraph = MdeoModelGraph.create(listModelData, listMetamodel)
        val g = listGraph.traversal()
        val tagsKey = "prop_${listMetamodel.metadata.classes["Container"]!!.propertyFields["tags"]!!.fieldIndex}"

        val tags = g.V().hasLabel("Container").values<String>(tagsKey).toList()
        assertEquals(3, tags.size)
        assertTrue(tags.containsAll(listOf("alpha", "beta", "gamma")))
        listGraph.close()
    }

    @Test
    fun addVertex_createsNewVertex() {
        val g = mdeoGraph.traversal()
        val countBefore = g.V().count().next()

        // Add a new Room vertex via traversal
        val g2 = mdeoGraph.traversal()
        g2.addV("Room").next()
        val countAfter = mdeoGraph.traversal().V().count().next()

        assertEquals(countBefore + 1, countAfter)
    }

    @Test
    fun addEdge_createsNewEdgeAndUpdatesModelAssociations() {
        // Create a fresh graph with no links initially
        val noLinksModelData = ModelData(
            metamodelPath = "test.mm",
            instances = listOf(
                ModelDataInstance(
                    name = "h1",
                    className = "House",
                    properties = mapOf(
                        "address" to ModelDataPropertyValue.StringValue("456 Oak Ave"),
                        "floors" to ModelDataPropertyValue.NumberValue(1.0)
                    )
                ),
                ModelDataInstance(
                    name = "r1",
                    className = "Room",
                    properties = mapOf(
                        "name" to ModelDataPropertyValue.StringValue("Lounge"),
                        "area" to ModelDataPropertyValue.NumberValue(30.0)
                    )
                )
            ),
            links = emptyList()
        )
        val graph = MdeoModelGraph.create(noLinksModelData, metamodel)
        val g = graph.traversal()

        assertEquals(0L, g.E().count().next())

        // Add an edge between house and room
        val house = graph.traversal().V().hasLabel("House").next()
        val room = graph.traversal().V().hasLabel("Room").next()
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel("rooms", "house")
        house.addEdge(edgeLabel, room)

        assertEquals(1L, graph.traversal().E().count().next())

        // Verify the backing model instance associations were updated
        val houseVertex = house as MdeoVertex
        val roomVertex = room as MdeoVertex
        val houseInstance = houseVertex.backingInstance
        val roomInstance = roomVertex.backingInstance

        // The "rooms" field on house should contain the room instance
        @Suppress("UNCHECKED_CAST")
        val rooms = houseInstance.getPropertyByKey("rooms") as? Set<ModelInstance>
        assertNotNull(rooms)
        assertEquals(1, rooms.size)

        // The "house" field on room should reference the house instance
        val houseRef = roomInstance.getPropertyByKey("house") as? ModelInstance
        assertNotNull(houseRef)
        assertSame(houseInstance, houseRef)

        graph.close()
    }

    @Test
    fun removeVertex_removesVertexAndEdges() {
        val g = mdeoGraph.traversal()
        assertEquals(3L, g.V().count().next())
        assertEquals(2L, g.E().count().next())

        // Remove one of the rooms
        val room = mdeoGraph.traversal().V().hasLabel("Room").next()
        room.remove()

        assertEquals(2L, mdeoGraph.traversal().V().count().next())
        // One edge should remain (the other room still linked)
        assertEquals(1L, mdeoGraph.traversal().E().count().next())
    }

    @Test
    fun removeEdge_removesEdge() {
        assertEquals(2L, mdeoGraph.traversal().E().count().next())

        val edge = mdeoGraph.traversal().E().next()
        edge.remove()

        assertEquals(1L, mdeoGraph.traversal().E().count().next())
    }

    // ========================================================================
    // Comparison with TinkerModelGraph
    // ========================================================================

    @Test
    fun matchesTinkerModelGraph_vertexCount() {
        val mdeoCount = mdeoGraph.traversal().V().count().next()
        val tinkerCount = tinkerGraph.traversal().V().count().next()
        assertEquals(tinkerCount, mdeoCount)
    }

    @Test
    fun matchesTinkerModelGraph_edgeCount() {
        val mdeoCount = mdeoGraph.traversal().E().count().next()
        val tinkerCount = tinkerGraph.traversal().E().count().next()
        assertEquals(tinkerCount, mdeoCount)
    }

    @Test
    fun matchesTinkerModelGraph_propertyValues() {
        val addressKey = graphKey("House", "address")
        val floorsKey = graphKey("House", "floors")
        val nameKey = graphKey("Room", "name")
        val areaKey = graphKey("Room", "area")

        // Compare House properties
        val mdeoAddress = mdeoGraph.traversal().V().hasLabel("House").values<Any>(addressKey).next()
        val tinkerAddress = tinkerGraph.traversal().V().hasLabel("House").values<Any>(addressKey).next()
        assertEquals(tinkerAddress, mdeoAddress)

        val mdeoFloors = mdeoGraph.traversal().V().hasLabel("House").values<Any>(floorsKey).next()
        val tinkerFloors = tinkerGraph.traversal().V().hasLabel("House").values<Any>(floorsKey).next()
        // TinkerGraph stores as Double (from NumberValue), MdeoGraph stores as Int (from metamodel type)
        assertEquals((tinkerFloors as Number).toDouble(), (mdeoFloors as Number).toDouble(), 0.001)

        // Compare Room property names (sorted for determinism)
        val mdeoNames = mdeoGraph.traversal().V().hasLabel("Room").values<Any>(nameKey).toList().map { it.toString() }.sorted()
        val tinkerNames = tinkerGraph.traversal().V().hasLabel("Room").values<Any>(nameKey).toList().map { it.toString() }.sorted()
        assertEquals(tinkerNames, mdeoNames)

        // Compare Room areas (sorted for determinism)
        val mdeoAreas = mdeoGraph.traversal().V().hasLabel("Room").values<Any>(areaKey).toList().map { (it as Number).toDouble() }.sorted()
        val tinkerAreas = tinkerGraph.traversal().V().hasLabel("Room").values<Any>(areaKey).toList().map { (it as Number).toDouble() }.sorted()
        assertEquals(tinkerAreas, mdeoAreas)
    }

    @Test
    fun matchesTinkerModelGraph_traversalResults() {
        // Compare counts by label
        val mdeoHouseCount = mdeoGraph.traversal().V().hasLabel("House").count().next()
        val tinkerHouseCount = tinkerGraph.traversal().V().hasLabel("House").count().next()
        assertEquals(tinkerHouseCount, mdeoHouseCount)

        val mdeoRoomCount = mdeoGraph.traversal().V().hasLabel("Room").count().next()
        val tinkerRoomCount = tinkerGraph.traversal().V().hasLabel("Room").count().next()
        assertEquals(tinkerRoomCount, mdeoRoomCount)

        // Both should find same number of edges with the rooms->house label
        val edgeLabel = EdgeLabelUtils.computeEdgeLabel("rooms", "house")
        val mdeoEdgeCount = mdeoGraph.traversal().E().hasLabel(edgeLabel).count().next()
        val tinkerEdgeCount = tinkerGraph.traversal().E().hasLabel(edgeLabel).count().next()
        assertEquals(tinkerEdgeCount, mdeoEdgeCount)
    }

    // ========================================================================
    // Deep Copy
    // ========================================================================

    @Test
    fun deepCopy_createsIndependentGraph() {
        val copy = mdeoGraph.deepCopy()

        // Modify original — add a vertex
        mdeoGraph.traversal().addV("Room").next()

        // Copy should be unaffected
        assertEquals(3L, copy.traversal().V().count().next())
        assertEquals(4L, mdeoGraph.traversal().V().count().next())

        copy.close()
    }

    @Test
    fun deepCopy_preservesAllData() {
        val copy = mdeoGraph.deepCopy()

        assertEquals(
            mdeoGraph.traversal().V().count().next(),
            copy.traversal().V().count().next()
        )
        assertEquals(
            mdeoGraph.traversal().E().count().next(),
            copy.traversal().E().count().next()
        )

        // Verify all labels match
        val origLabels = mdeoGraph.traversal().V().label().toList().sorted()
        val copyLabels = copy.traversal().V().label().toList().sorted()
        assertEquals(origLabels, copyLabels)

        // Verify property values match
        val addressKey = graphKey("House", "address")
        val origAddress = mdeoGraph.traversal().V().hasLabel("House").values<Any>(addressKey).next()
        val copyAddress = copy.traversal().V().hasLabel("House").values<Any>(addressKey).next()
        assertEquals(origAddress, copyAddress)

        copy.close()
    }

    // ========================================================================
    // Reset Nondeterminism
    // ========================================================================

    @Test
    fun resetNondeterminism_returnsEmptyMap() {
        // MdeoModelGraph always returns empty map since IDs don't change
        val idMap = mdeoGraph.resetNondeterminism()
        assertTrue(idMap.isEmpty())
    }

    @Test
    fun resetNondeterminism_shufflesVertexOrder() {
        // Try multiple times to detect shuffling (since shuffling could theoretically
        // produce the same order, we retry a few times)
        var foundDifferentOrder = false
        val originalOrder = mdeoGraph.traversal().V().id().toList()

        for (i in 0 until 20) {
            mdeoGraph.resetNondeterminism()
            val newOrder = mdeoGraph.traversal().V().id().toList()
            if (newOrder != originalOrder) {
                foundDifferentOrder = true
                break
            }
        }

        // With 3 vertices there are 6 permutations, so the probability of all 20
        // attempts returning the same order is (1/6)^20 ≈ 0, effectively impossible
        assertTrue(foundDifferentOrder, "Expected vertex order to change after resetNondeterminism")
    }

    // ========================================================================
    // ModelData Round-trip
    // ========================================================================

    @Test
    fun toModelData_andBack_preservesStructure() {
        val exported = mdeoGraph.toModelData()

        // Rebuild from exported data
        val rebuilt = MdeoModelGraph.create(exported, metamodel)

        assertEquals(
            mdeoGraph.traversal().V().count().next(),
            rebuilt.traversal().V().count().next()
        )
        assertEquals(
            mdeoGraph.traversal().E().count().next(),
            rebuilt.traversal().E().count().next()
        )

        // Verify instance names are preserved
        val origNames = mdeoGraph.nameRegistry.getAllMappings().values.sorted()
        val rebuiltNames = rebuilt.nameRegistry.getAllMappings().values.sorted()
        assertEquals(origNames, rebuiltNames)

        // Verify property values survive round-trip
        val addressKey = graphKey("House", "address")
        val origAddress = mdeoGraph.traversal().V().hasLabel("House").values<Any>(addressKey).next()
        val rebuiltAddress = rebuilt.traversal().V().hasLabel("House").values<Any>(addressKey).next()
        assertEquals(origAddress, rebuiltAddress)

        rebuilt.close()
    }

    // ========================================================================
    // VertexRef
    // ========================================================================

    @Test
    fun createVertexRef_tracksVertex() {
        val vertexId = mdeoGraph.traversal().V().next().id()
        val ref = mdeoGraph.createVertexRef(vertexId)

        assertNotNull(ref)
        assertEquals(vertexId, ref.rawId)
    }

    // ========================================================================
    // ASM getPropertyByKey / setPropertyByKey
    // ========================================================================

    @Test
    fun modelInstancePropertyAccess_getByKey() {
        val instance = metamodel.createInstance("House")
        // Set via ASM
        instance.setPropertyByKey("address", "Test Address")
        instance.setPropertyByKey("floors", 3)

        assertEquals("Test Address", instance.getPropertyByKey("address"))
        assertEquals(3, instance.getPropertyByKey("floors"))
    }

    @Test
    fun modelInstancePropertyAccess_setByKey() {
        val instance = metamodel.createInstance("Room")
        instance.setPropertyByKey("name", "Living Room")
        instance.setPropertyByKey("area", 25.0)

        assertEquals("Living Room", instance.getPropertyByKey("name"))
        assertEquals(25.0, instance.getPropertyByKey("area"))

        // Modify
        instance.setPropertyByKey("name", "Dining Room")
        assertEquals("Dining Room", instance.getPropertyByKey("name"))
    }
}
