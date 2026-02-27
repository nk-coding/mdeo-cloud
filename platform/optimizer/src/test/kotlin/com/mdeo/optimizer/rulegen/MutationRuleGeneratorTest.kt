package com.mdeo.optimizer.rulegen

import com.mdeo.expression.ast.types.AssociationData
import com.mdeo.expression.ast.types.AssociationEndData
import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.MultiplicityData
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Nested

/**
 * Unit tests for [MutationRuleGenerator] and its collaborators.
 *
 * Uses a simple in-memory metamodel:
 *
 *   House <>-> Room (1..*) -- (rooms / house bidirectional)
 *   Room  --> Window (0..*) -- windows (unidirectional)
 *
 * This covers the key multiplicity and bidirectionality combinations used
 * in [SpecsGenerator] decision logic.
 */
class MutationRuleGeneratorTest {

    // -------------------------------------------------------------------------
    // Shared metamodel fixture
    // -------------------------------------------------------------------------

    private val houseClass = ClassData(name = "House", isAbstract = false)
    private val roomClass  = ClassData(name = "Room",  isAbstract = false)
    private val windowClass = ClassData(name = "Window", isAbstract = false)

    /** House <>-> Room (containment, bidirectional) */
    private val houseRoomsAssoc = AssociationData(
        source = AssociationEndData(
            className = "House",
            name = "rooms",
            multiplicity = MultiplicityData.many()     // 0..*
        ),
        operator = "<>->",
        target = AssociationEndData(
            className = "Room",
            name = "house",
            multiplicity = MultiplicityData.single()   // 1..1
        )
    )

    /** Room --> Window (non-containment, unidirectional 0..*) */
    private val roomWindowsAssoc = AssociationData(
        source = AssociationEndData(
            className = "Room",
            name = "windows",
            multiplicity = MultiplicityData.many()     // 0..*
        ),
        operator = "-->",
        target = AssociationEndData(
            className = "Window",
            name = null,                               // unidirectional
            multiplicity = MultiplicityData.optional() // not relevant
        )
    )

    private val metamodelData = MetamodelData(
        path = "/project/home.mm",
        classes = listOf(houseClass, roomClass, windowClass),
        associations = listOf(houseRoomsAssoc, roomWindowsAssoc)
    )

    // -------------------------------------------------------------------------
    // MutationRuleNameGenerator
    // -------------------------------------------------------------------------

    @Nested
    inner class NamingTests {

        @Test
        fun `forNode produces ACTION_ClassName`() {
            assertEquals("CREATE_Foo", MutationRuleNameGenerator.forNode("CREATE", "Foo"))
            assertEquals("DELETE_Foo", MutationRuleNameGenerator.forNode("DELETE", "Foo"))
        }

        @Test
        fun `forEdge produces ACTION_ClassName_refName`() {
            assertEquals("ADD_Foo_bar", MutationRuleNameGenerator.forEdge("ADD", "Foo", "bar"))
            assertEquals("REMOVE_Foo_bar", MutationRuleNameGenerator.forEdge("REMOVE", "Foo", "bar"))
        }

        @Test
        fun `forNodeCreate produces contextual name`() {
            assertEquals(
                "CREATE_Room_in_House_via_rooms",
                MutationRuleNameGenerator.forNodeCreate("Room", "House", "rooms")
            )
        }

        @Test
        fun `fromRepairSpec with LB-repair appends suffix`() {
            val s = RepairSpec("Foo", "bar", RepairSpecType.CREATE_LB_REPAIR)
            assertTrue(MutationRuleNameGenerator.fromRepairSpec(s).endsWith("_LBREPAIR"))
        }

        @Test
        fun `fromRepairSpec with node-only spec omits edge`() {
            val s = RepairSpec("Foo", null, RepairSpecType.DELETE)
            assertEquals("DELETE_Foo", MutationRuleNameGenerator.fromRepairSpec(s))
        }
    }

    // -------------------------------------------------------------------------
    // MetamodelInfo
    // -------------------------------------------------------------------------

    @Nested
    inner class MetamodelInfoTests {

        private val info = MetamodelInfo(metamodelData)

        @Test
        fun `classNames returns only non-abstract classes`() {
            val names = info.classNames()
            assertEquals(setOf("House", "Room", "Window"), names.toSet())
        }

        @Test
        fun `referencesForNode House returns rooms`() {
            val refs = info.referencesForNode("House")
            assertEquals(1, refs.size)
            val r = refs[0]
            assertEquals("rooms", r.refName)
            assertEquals("Room", r.targetClass)
            assertTrue(r.isContainment)
            assertNotNull(r.opposite)
            assertEquals(1, r.opposite!!.lower)
            assertEquals(1, r.opposite!!.upper)
        }

        @Test
        fun `referencesForNode Room returns windows`() {
            val refs = info.referencesForNode("Room")
            assertEquals(1, refs.size)
            val r = refs[0]
            assertEquals("windows", r.refName)
            assertEquals("Window", r.targetClass)
            assertFalse(r.isContainment)
            assertEquals(null, r.opposite)
        }

        @Test
        fun `containmentContextsFor Room returns House via rooms`() {
            val contexts = info.containmentContextsFor("Room")
            assertEquals(1, contexts.size)
            assertEquals("House" to "rooms", contexts[0])
        }

        @Test
        fun `containmentContextsFor House returns empty`() {
            assertTrue(info.containmentContextsFor("House").isEmpty())
        }
    }

    // -------------------------------------------------------------------------
    // SpecsGenerator
    // -------------------------------------------------------------------------

    @Nested
    inner class SpecsGeneratorTests {

        private val info = MetamodelInfo(metamodelData)
        private val generator = SpecsGenerator()

        @Test
        fun `ALL action on Room produces ADD and REMOVE for windows`() {
            val spec = MutationRuleSpec("Room", action = MutationAction.ALL)
            val result = generator.getRepairsForRuleSpec(spec, info)

            // Should have CREATE, DELETE, ADD, REMOVE keys
            assertTrue(result.containsKey("CREATE"))
            assertTrue(result.containsKey("DELETE"))
            assertTrue(result.containsKey("ADD"))
            assertTrue(result.containsKey("REMOVE"))
        }

        @Test
        fun `reference with lower=1 upper=3 and bounded opposite produces CREATE_LB_REPAIR`() {
            // Build a local metamodel with a reference that has lower!=upper and bounded opposite
            val garageClass = ClassData(name = "Garage", isAbstract = false)
            val garagesAssoc = AssociationData(
                source = AssociationEndData(
                    className = "House",
                    name = "garages",
                    multiplicity = MultiplicityData(lower = 1, upper = 3)
                ),
                operator = "-->",
                target = AssociationEndData(
                    className = "Garage",
                    name = "owner",
                    multiplicity = MultiplicityData(lower = 0, upper = 2)
                )
            )
            val localMeta = MetamodelData(
                path = "/project/garage.mm",
                classes = listOf(houseClass, garageClass),
                associations = listOf(garagesAssoc)
            )
            val localInfo = MetamodelInfo(localMeta)
            val spec = MutationRuleSpec("House", edge = "garages", action = MutationAction.CREATE)
            val result = generator.getRepairsForRuleSpec(spec, localInfo)

            val createSpecs = result["CREATE"] ?: emptyList()
            val types = createSpecs.map { it.type }.toSet()
            assertTrue(
                types.contains(RepairSpecType.CREATE_LB_REPAIR),
                "Expected CREATE_LB_REPAIR for ref with lower=1, upper=3 and bounded opposite; got: $types"
            )
        }

        @Test
        fun `reference with lower=upper and no opposite produces SWAP not ADD`() {
            // Build a local metamodel with a reference that has lower==upper
            val garageClass = ClassData(name = "Garage", isAbstract = false)
            val mainGarageAssoc = AssociationData(
                source = AssociationEndData(
                    className = "House",
                    name = "mainGarage",
                    multiplicity = MultiplicityData(lower = 1, upper = 1)
                ),
                operator = "-->",
                target = AssociationEndData(
                    className = "Garage",
                    name = null,           // unidirectional
                    multiplicity = MultiplicityData(lower = 0, upper = 1)
                )
            )
            val localMeta = MetamodelData(
                path = "/project/garage.mm",
                classes = listOf(houseClass, garageClass),
                associations = listOf(mainGarageAssoc)
            )
            val localInfo = MetamodelInfo(localMeta)
            val spec = MutationRuleSpec("House", edge = "mainGarage", action = MutationAction.ADD)
            val result = generator.getRepairsForRuleSpec(spec, localInfo)

            val addSpecs = result["ADD"] ?: emptyList()
            val types = addSpecs.map { it.type }.toSet()
            assertTrue(
                types.contains(RepairSpecType.SWAP),
                "Expected SWAP for ref with lower==upper==1 and no opposite; got: $types"
            )
            assertFalse(
                types.contains(RepairSpecType.ADD),
                "Expected no plain ADD when lower==upper; got: $types"
            )
        }

        @Test
        fun `ADD action on windows ref produces ADD repair spec`() {
            val spec = MutationRuleSpec("Room", edge = "windows", action = MutationAction.ADD)
            val result = generator.getRepairsForRuleSpec(spec, info)

            val addSpecs = result["ADD"] ?: emptyList()
            assertTrue(addSpecs.isNotEmpty(), "Expected ADD specs for Room.windows")
            val types = addSpecs.map { it.type }.toSet()
            assertTrue(
                types.any { it == RepairSpecType.ADD || it == RepairSpecType.SWAP },
                "Expected ADD or SWAP but got: $types"
            )
        }

        @Test
        fun `REMOVE action only produces REMOVE key`() {
            val spec = MutationRuleSpec("Room", edge = "windows", action = MutationAction.REMOVE)
            val result = generator.getRepairsForRuleSpec(spec, info)

            assertTrue(result.containsKey("REMOVE"))
            assertFalse(result.containsKey("ADD"))
            assertFalse(result.containsKey("CREATE"))
            assertFalse(result.containsKey("DELETE"))
        }

        @Test
        fun `edge spec filters to specified reference only`() {
            val spec = MutationRuleSpec("Room", edge = "windows", action = MutationAction.ADD)
            val result = generator.getRepairsForRuleSpec(spec, info)

            result.values.flatten().forEach { r ->
                assertEquals("windows", r.edgeName, "Only 'windows' ref should appear in specs")
            }
        }
    }

    // -------------------------------------------------------------------------
    // MutationAstBuilder
    // -------------------------------------------------------------------------

    @Nested
    inner class MutationAstBuilderTests {

        private val info = MetamodelInfo(metamodelData)
        private val mmPath = "/project/home.mm"

        @Test
        fun `ADD rule produces match with forbid and create link`() {
            val spec = RepairSpec("Room", "windows", RepairSpecType.ADD)
            val ast = MutationAstBuilder.build("ADD_Room_windows", spec, mmPath, info)

            assertNotNull(ast)
            assertEquals(mmPath, ast.metamodelPath)
            assertEquals(1, ast.statements.size)

            val stmt = ast.statements[0] as TypedMatchStatement
            val links = stmt.pattern.elements.filterIsInstance<TypedPatternLinkElement>()
            val modifiers = links.map { it.link.modifier }.toSet()
            assertTrue(modifiers.contains("forbid"), "Expected forbid NAC")
            assertTrue(modifiers.contains("create"), "Expected create link")
        }

        @Test
        fun `REMOVE rule produces match with delete link only`() {
            val spec = RepairSpec("Room", "windows", RepairSpecType.REMOVE)
            val ast = MutationAstBuilder.build("REMOVE_Room_windows", spec, mmPath, info)

            assertNotNull(ast)
            val stmt = ast.statements[0] as TypedMatchStatement
            val links = stmt.pattern.elements.filterIsInstance<TypedPatternLinkElement>()
            val modifiers = links.map { it.link.modifier }.toSet()
            assertEquals(setOf("delete"), modifiers, "REMOVE should have exactly one delete link (no redundant match link)")
        }

        @Test
        fun `CHANGE rule produces match with delete and create links`() {
            val spec = RepairSpec("Room", "windows", RepairSpecType.CHANGE)
            val ast = MutationAstBuilder.build("CHANGE_Room_windows", spec, mmPath, info)

            assertNotNull(ast)
            val stmt = ast.statements[0] as TypedMatchStatement
            val links = stmt.pattern.elements.filterIsInstance<TypedPatternLinkElement>()
            val modifiers = links.map { it.link.modifier }.toSet()
            assertTrue(modifiers.contains("delete"))
            assertTrue(modifiers.contains("create"))
            assertTrue(modifiers.contains("forbid"))
        }

        @Test
        fun `CREATE Room produces containment rule with House container`() {
            val spec = RepairSpec("Room", null, RepairSpecType.CREATE)
            val ast = MutationAstBuilder.build(
                "CREATE_Room_in_House_via_rooms", spec, mmPath, info,
                createContext = "House" to "rooms"
            )

            assertNotNull(ast)
            val stmt = ast.statements[0] as TypedMatchStatement
            val instances = stmt.pattern.elements.filterIsInstance<TypedPatternObjectInstanceElement>()
            val created = instances.filter { it.objectInstance.modifier == "create" }
            assertEquals(1, created.size)
            assertEquals("Room", created[0].objectInstance.className)
        }

        @Test
        fun `CREATE standalone returns create-only pattern`() {
            val spec = RepairSpec("House", null, RepairSpecType.CREATE)
            val ast = MutationAstBuilder.build("CREATE_House", spec, mmPath, info, createContext = null)

            assertNotNull(ast)
            val stmt = ast.statements[0] as TypedMatchStatement
            val instances = stmt.pattern.elements.filterIsInstance<TypedPatternObjectInstanceElement>()
            assertEquals(1, instances.size)
            assertEquals("create", instances[0].objectInstance.modifier)
            assertEquals("House", instances[0].objectInstance.className)
        }

        @Test
        fun `DELETE rule produces match then delete elements`() {
            val spec = RepairSpec("Room", null, RepairSpecType.DELETE)
            val ast = MutationAstBuilder.build("DELETE_Room", spec, mmPath, info)

            assertNotNull(ast)
            val stmt = ast.statements[0] as TypedMatchStatement
            val instances = stmt.pattern.elements.filterIsInstance<TypedPatternObjectInstanceElement>()
            val matchable = instances.filter { it.objectInstance.modifier == null }
            val deleted   = instances.filter { it.objectInstance.modifier == "delete" }
            assertEquals(1, matchable.size,  "Expected one matchable instance")
            assertEquals(1, deleted.size,    "Expected one delete instance")
            assertEquals("Room", matchable[0].objectInstance.className)
            assertEquals(null, deleted[0].objectInstance.className)
        }
    }

    // -------------------------------------------------------------------------
    // MutationRuleGenerator (end-to-end)
    // -------------------------------------------------------------------------

    @Nested
    inner class MutationRuleGeneratorTests {

        @Test
        fun `generate returns non-empty list for simple spec`() {
            val specs = listOf(MutationRuleSpec("Room", action = MutationAction.ADD))
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            assertTrue(mutations.isNotEmpty(), "Expected at least one mutation rule")
        }

        @Test
        fun `generate CREATE for node with no outgoing references produces standalone CREATE rule`() {
            // Window has no outgoing references in the fixture
            val specs = listOf(MutationRuleSpec("Window", action = MutationAction.CREATE))
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            val names = mutations.map { it.name }.toSet()
            assertTrue(
                names.contains("CREATE_Window"),
                "Expected standalone CREATE_Window for class with no outgoing refs; got: $names"
            )
        }

        @Test
        fun `generate with ALL produces multiple rule types`() {
            val specs = listOf(MutationRuleSpec("Room", action = MutationAction.ALL))
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            val names = mutations.map { it.name }.toSet()
            assertTrue(names.isNotEmpty())
            // Should have ADD/REMOVE/SWAP rules for windows
            assertTrue(names.any { it.startsWith("ADD") || it.startsWith("REMOVE") || it.startsWith("SWAP") },
                "Expected ADD/REMOVE/SWAP rule; got: $names")
            // Should have CREATE rules (Room has containment context via House.rooms)
            assertTrue(names.any { it.startsWith("CREATE") },
                "Expected CREATE rule; got: $names")
            // Should have DELETE rules for windows (unidirectional, no mandatory opposite)
            assertTrue(names.any { it.startsWith("DELETE") },
                "Expected DELETE rule; got: $names")
        }

        @Test
        fun `generate deduplicates rules with same name`() {
            // Two identical specs for the same node/edge
            val specs = listOf(
                MutationRuleSpec("Room", edge = "windows", action = MutationAction.ADD),
                MutationRuleSpec("Room", edge = "windows", action = MutationAction.ADD)
            )
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            val names = mutations.map { it.name }
            assertEquals(names.distinct().size, names.size, "Expected no duplicate names")
        }

        @Test
        fun `generate skips unknown class with warning`() {
            val specs = listOf(MutationRuleSpec("NonExistent", action = MutationAction.ALL))
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            assertTrue(mutations.isEmpty(), "Expected empty result for unknown class")
        }

        @Test
        fun `generate with empty specs returns empty list`() {
            val mutations = MutationRuleGenerator.generate(metamodelData, emptyList())
            assertTrue(mutations.isEmpty())
        }

        @Test
        fun `generate CREATE Room produces containment-context rules`() {
            val specs = listOf(MutationRuleSpec("Room", action = MutationAction.CREATE))
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            val names = mutations.map { it.name }
            assertTrue(
                names.any { it.contains("House") && it.contains("rooms") },
                "Expected a CREATE rule for Room in House via rooms, got: $names"
            )
        }

        @Test
        fun `each generated mutation has valid TypedAst with one statement`() {
            val specs = listOf(MutationRuleSpec("Room", edge = "windows", action = MutationAction.ADD))
            val mutations = MutationRuleGenerator.generate(metamodelData, specs)
            for (m in mutations) {
                assertNotNull(m.typedAst)
                assertTrue(m.typedAst.statements.isNotEmpty(), "Rule '${m.name}' has no statements")
                assertTrue(m.typedAst.statements.all { it is TypedMatchStatement })
            }
        }
    }
}
