package com.mdeo.optimizer.rulegen

import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.metamodel.data.ModelData
import com.mdeo.optimizer.config.RefinementConfig
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import kotlin.random.Random

/**
 * Applies every generated operator to random consistent models and checks that the result is
 * consistent too, and that every operator can apply to some consistent model.
 */
class RuleConsistencyTest {

    private val bounds = listOf("*", "?", "+", "1", "2..3", "2..*", "1..2", "2")

    /** Every pair of end multiplicities, as in MDEOptimiser's rule generation tests. */
    @TestFactory
    fun `operators for every pair of multiplicities keep models consistent`(): List<DynamicTest> =
        bounds.flatMap { e -> bounds.map { f -> e to f } }.map { (e, f) ->
            DynamicTest.dynamicTest("A.e[$e] <--> B.f[$f]") {
                val metamodel = mm("Context", "A", "B") {
                    assoc("Context.as[*]", "*-->", "A.ctx")
                    assoc("Context.bs[*]", "*-->", "B.ctx")
                    assoc("A.e[$e]", "<-->", "B.f[$f]")
                }
                check(metamodel, listOf(spec("mutate A"), spec("mutate B")))
            }
        }

    @TestFactory
    fun `operators for a unidirectional end keep models consistent`(): List<DynamicTest> =
        bounds.map { e ->
            DynamicTest.dynamicTest("A.e[$e] --> B") {
                val metamodel = mm("Context", "A", "B") {
                    assoc("Context.as[*]", "*-->", "A.ctx")
                    assoc("Context.bs[*]", "*-->", "B.ctx")
                    assoc("A.e[$e]", "-->", "B")
                }
                check(metamodel, listOf(spec("mutate A"), spec("mutate B")))
            }
        }

    @TestFactory
    fun `operators for compositions keep models consistent`(): List<DynamicTest> = listOf(
        "rooms need a house" to mm("House", "Room") { assoc("House.rooms[*]", "*-->", "Room.house") },
        "a house needs rooms" to mm("House", "Room") { assoc("House.rooms[+]", "*-->", "Room.house[?]") },
        "whole on the right" to mm("Parent", "Child") { assoc("Child.parent", "<--*", "Parent.children[*]") },
        "unnamed whole end" to mm("Whole", "Part") { assoc("Part.whole", "--*", "Whole") },
        "two possible containers" to mm("Box", "Shelf", "Item") {
            assoc("Box.items[*]", "*-->", "Item.box[?]")
            assoc("Shelf.items[0..2]", "*-->", "Item.shelf[?]")
        },
        "folders need files" to mm("Folder", "File") { assoc("Folder.files[1..*]", "*-->", "File.folder") },
        "recursive" to mm("Node") { assoc("Node.children[*]", "*-->", "Node.parent[?]") }
    ).map { (name, metamodel) ->
        DynamicTest.dynamicTest(name) {
            check(metamodel, metamodel.classes.map { spec("mutate ${it.name}") })
        }
    }

    @TestFactory
    fun `operators on metamodels with several associations keep models consistent`(): List<DynamicTest> = listOf(
        "two associations between the same classes" to mm("A", "B") {
            assoc("A.x[*]", "<-->", "B.xb")
            assoc("A.y[*]", "<-->", "B.yb[?]")
        },
        "scrum planning" to mm("Plan", "Sprint", "WorkItem", "Stakeholder") {
            assoc("Plan.sprints[*]", "*-->", "Sprint.plan")
            assoc("Plan.backlog[*]", "*-->", "WorkItem.plan")
            assoc("Plan.stakeholders[*]", "*-->", "Stakeholder.plan")
            assoc("Sprint.committedItems[+]", "<-->", "WorkItem.isPlannedFor[?]")
            assoc("WorkItem.stakeholder", "<-->", "Stakeholder.workItems[*]")
        },
        "inheritance" to MetamodelBuilder("/test.mm").apply {
            cls("Root")
            cls("abstract Shape")
            cls("Circle : Shape")
            cls("Square : Shape")
            cls("Tag")
            cls("Corner")
            assoc("Root.shapes[*]", "*-->", "Shape.root")
            assoc("Root.tags[*]", "*-->", "Tag.root")
            assoc("Root.corners[*]", "*-->", "Corner.root")
            assoc("Shape.tags[0..2]", "<-->", "Tag.shapes[*]")
            assoc("Square.corner", "<-->", "Corner.square[?]")
        }.build()
    ).map { (name, metamodel) ->
        DynamicTest.dynamicTest(name) {
            check(metamodel, metamodel.classes.map { spec("mutate ${it.name}") })
        }
    }

    @TestFactory
    fun `operators keep refined multiplicities`(): List<DynamicTest> = listOf(
        Triple(
            "palladio allocation",
            mm("ResourceEnvironment", "ResourceContainer", "Allocation", "AllocationContext") {
                assoc("ResourceEnvironment.resourceContainer[*]", "*-->", "ResourceContainer.resourceEnvironment[?]")
                assoc("Allocation.allocationContexts[*]", "*-->", "AllocationContext.allocation[?]")
                assoc("AllocationContext.resourceContainer[?]", "-->", "ResourceContainer")
            },
            Pair(listOf(spec("mutate AllocationContext.resourceContainer")), listOf(refine("AllocationContext.resourceContainer[1]")))
        ),
        Triple(
            "scrum with planned work items",
            mm("Plan", "Sprint", "WorkItem") {
                assoc("Plan.sprints[*]", "*-->", "Sprint.plan")
                assoc("Plan.backlog[*]", "*-->", "WorkItem.plan")
                assoc("Sprint.committedItems[+]", "<-->", "WorkItem.isPlannedFor[?]")
            },
            Pair(listOf(spec("mutate Sprint")), listOf(refine("WorkItem.isPlannedFor[1]")))
        )
    ).map { (name, metamodel, specsAndRefinements) ->
        DynamicTest.dynamicTest(name) {
            check(metamodel, specsAndRefinements.first, specsAndRefinements.second)
        }
    }

    // -------------------------------------------------------------------------

    private fun check(metamodel: MetamodelData, specs: List<MutationRuleSpec>, refinements: List<RefinementConfig> = emptyList()) {
        val generated = MutationRuleGenerator.generate(metamodel, specs, refinements)
        val info = MetamodelInfo(metamodel, refinements.map { MultiplicityOverride(it.className, it.fieldName, it.lower, it.upper) })
        val randomModels = RandomModels(info, metamodel, seed = 7)
        val models = (1..MODELS).mapNotNull { randomModels.next() }
        assertTrue(models.size == MODELS, "could not build $MODELS consistent models")

        val runner = RuleRunner(metamodel)
        val applied = mutableMapOf<String, Int>()
        val problems = mutableListOf<String>()

        fun step(mutation: GeneratedMutation, before: ModelData): ModelData? {
            val after = try {
                runner.apply(mutation, before)
            } catch (e: Exception) {
                problems += "${mutation.name} threw ${e::class.simpleName}: ${e.message}"
                return null
            }
            if (after !is Applied.Yes) return null
            applied.merge(mutation.name, 1, Int::plus)
            val violations = MultiplicityChecker.violations(after.model, info)
            if (violations.isNotEmpty()) {
                problems += "${mutation.name} made ${violations.joinToString()}\n  before: ${before.render()}\n  after:  ${after.model.render()}"
                return null
            }
            return after.model
        }

        for (mutation in generated.mutations) {
            for (model in models) {
                var current = model
                repeat(REPEATS) { current = step(mutation, current) ?: return@repeat }
            }
        }
        val random = Random(11)
        for (model in models) {
            var current = model
            repeat(WALK) {
                val mutation = generated.mutations.random(random)
                current = step(mutation, current) ?: current
            }
        }

        if (problems.isNotEmpty()) fail<Unit>(problems.distinct().take(5).joinToString("\n"))
        val dead = generated.mutations.map { it.name }.filter { it !in applied }
        assertTrue(dead.isEmpty(), "never applied: $dead\nwarnings: ${generated.warnings}")
    }

    private companion object {
        const val MODELS = 12
        const val REPEATS = 4
        const val WALK = 40
    }
}
