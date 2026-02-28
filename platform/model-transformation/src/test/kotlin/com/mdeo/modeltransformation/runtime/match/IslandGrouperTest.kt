package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.ast.patterns.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the [IslandGrouper] utility.
 *
 * An "island" is a connected component of pattern instances and links that share
 * a common modifier (forbid or require). The grouper takes a flat list of constraint
 * instances and links and partitions them into independent islands based on
 * connectivity through the links.
 *
 * The grouping is used by the match executor to build independent constraint
 * clauses — one per island — rather than treating all constraint elements as
 * a single monolithic block.
 */
class IslandGrouperTest {

    @Nested
    inner class EmptyInputTests {

        @Test
        fun `empty instances and links produce no islands`() {
            val islands = IslandGrouper.groupIntoIslands(
                instances = emptyList(),
                links = emptyList()
            )

            assertTrue(islands.isEmpty())
        }
    }

    @Nested
    inner class SingleInstanceTests {

        @Test
        fun `single instance with no links produces one island`() {
            val instance = instance("room", "Room")

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(instance),
                links = emptyList()
            )

            assertEquals(1, islands.size)
            assertEquals(1, islands[0].instances.size)
            assertEquals("room", islands[0].instances[0].objectInstance.name)
            assertTrue(islands[0].links.isEmpty())
        }
    }

    @Nested
    inner class ConnectedComponentTests {

        @Test
        fun `two instances connected by a link form one island`() {
            val a = instance("a", "A")
            val b = instance("b", "B")
            val link = link("a", "ref", "b", null)

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(a, b),
                links = listOf(link)
            )

            assertEquals(1, islands.size)
            assertEquals(2, islands[0].instances.size)
            assertEquals(1, islands[0].links.size)

            val names = islands[0].instances.map { it.objectInstance.name }.toSet()
            assertTrue(names.contains("a"))
            assertTrue(names.contains("b"))
        }

        @Test
        fun `three instances in a chain form one island`() {
            val a = instance("a", "A")
            val b = instance("b", "B")
            val c = instance("c", "C")
            val linkAB = link("a", "refB", "b", "refA")
            val linkBC = link("b", "refC", "c", "refB")

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(a, b, c),
                links = listOf(linkAB, linkBC)
            )

            assertEquals(1, islands.size)
            assertEquals(3, islands[0].instances.size)
            assertEquals(2, islands[0].links.size)
        }

        @Test
        fun `two separate pairs form two islands`() {
            val a = instance("a", "A")
            val b = instance("b", "B")
            val c = instance("c", "C")
            val d = instance("d", "D")
            val linkAB = link("a", "refB", "b", "refA")
            val linkCD = link("c", "refD", "d", "refC")

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(a, b, c, d),
                links = listOf(linkAB, linkCD)
            )

            assertEquals(2, islands.size)

            val island1Names = islands[0].instances.map { it.objectInstance.name }.toSet()
            val island2Names = islands[1].instances.map { it.objectInstance.name }.toSet()

            val allNames = island1Names + island2Names
            assertEquals(setOf("a", "b", "c", "d"), allNames)

            val abIsland = islands.first { it.instances.any { i -> i.objectInstance.name == "a" } }
            val cdIsland = islands.first { it.instances.any { i -> i.objectInstance.name == "c" } }

            assertEquals(setOf("a", "b"), abIsland.instances.map { it.objectInstance.name }.toSet())
            assertEquals(setOf("c", "d"), cdIsland.instances.map { it.objectInstance.name }.toSet())
            assertEquals(1, abIsland.links.size)
            assertEquals(1, cdIsland.links.size)
        }

        @Test
        fun `connected pair plus disconnected singleton form two islands`() {
            val a = instance("a", "A")
            val b = instance("b", "B")
            val c = instance("c", "C")
            val linkAB = link("a", "refB", "b", "refA")

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(a, b, c),
                links = listOf(linkAB)
            )

            assertEquals(2, islands.size)

            val abIsland = islands.first { it.instances.any { i -> i.objectInstance.name == "a" } }
            val cIsland = islands.first { it.instances.any { i -> i.objectInstance.name == "c" } }

            assertEquals(2, abIsland.instances.size)
            assertEquals(1, abIsland.links.size)
            assertEquals(1, cIsland.instances.size)
            assertTrue(cIsland.links.isEmpty())
        }
    }

    @Nested
    inner class LinkWithoutMatchingInstanceTests {

        @Test
        fun `link referencing external instance does not create spurious islands`() {
            val b = instance("b", "B")
            val linkExtB = link("external", "ref", "b", null)

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(b),
                links = listOf(linkExtB)
            )

            assertEquals(1, islands.size)
            assertEquals(1, islands[0].instances.size)
            assertEquals("b", islands[0].instances[0].objectInstance.name)
            assertEquals(1, islands[0].links.size)
        }
    }

    @Nested
    inner class StarTopologyTests {

        @Test
        fun `star topology with one hub forms a single island`() {
            val hub = instance("hub", "Hub")
            val s1 = instance("s1", "Spoke")
            val s2 = instance("s2", "Spoke")
            val s3 = instance("s3", "Spoke")
            val link1 = link("hub", "ref1", "s1", null)
            val link2 = link("hub", "ref2", "s2", null)
            val link3 = link("hub", "ref3", "s3", null)

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(hub, s1, s2, s3),
                links = listOf(link1, link2, link3)
            )

            assertEquals(1, islands.size)
            assertEquals(4, islands[0].instances.size)
            assertEquals(3, islands[0].links.size)
        }
    }

    @Nested
    inner class MultipleDisconnectedSingletonsTests {

        @Test
        fun `three disconnected singletons form three islands`() {
            val a = instance("a", "A")
            val b = instance("b", "B")
            val c = instance("c", "C")

            val islands = IslandGrouper.groupIntoIslands(
                instances = listOf(a, b, c),
                links = emptyList()
            )

            assertEquals(3, islands.size)
            assertTrue(islands.all { it.instances.size == 1 })
            assertTrue(islands.all { it.links.isEmpty() })
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    companion object {

        /**
         * Creates a [TypedPatternObjectInstanceElement] with the given name and class.
         * The modifier is left null; the grouper operates on topology, not modifiers.
         */
        fun instance(name: String, className: String): TypedPatternObjectInstanceElement {
            return TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = null,
                    name = name,
                    className = className,
                    properties = emptyList()
                )
            )
        }

        /**
         * Creates a [TypedPatternLinkElement] connecting two named instances.
         * The modifier is left null; the grouper operates on topology, not modifiers.
         */
        fun link(
            sourceName: String,
            sourceProperty: String?,
            targetName: String,
            targetProperty: String?
        ): TypedPatternLinkElement {
            return TypedPatternLinkElement(
                link = TypedPatternLink(
                    modifier = null,
                    source = TypedPatternLinkEnd(objectName = sourceName, propertyName = sourceProperty),
                    target = TypedPatternLinkEnd(objectName = targetName, propertyName = targetProperty)
                )
            )
        }
    }
}
