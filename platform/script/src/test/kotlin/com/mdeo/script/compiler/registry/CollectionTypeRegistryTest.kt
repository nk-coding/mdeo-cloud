package com.mdeo.script.compiler.registry

import com.mdeo.script.compiler.registry.type.TypeRegistry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for collection type registrations in the TypeRegistry.
 *
 * Verifies that all collection types (List, Set, Bag, OrderedSet) and their
 * methods are properly registered and can be looked up through inheritance.
 */
class CollectionTypeRegistryTest {

    private val globalRegistry = TypeRegistry.GLOBAL

    @Nested
    inner class CollectionTypeRegistration {

        @Test
        fun `global registry contains ReadonlyCollection type`() {
            val type = globalRegistry.getType("builtin.ReadonlyCollection")
            assertNotNull(type)
            assertEquals("builtin.ReadonlyCollection", type.typeName)
        }

        @Test
        fun `global registry contains Collection type`() {
            val type = globalRegistry.getType("builtin.Collection")
            assertNotNull(type)
            assertEquals("builtin.Collection", type.typeName)
        }

        @Test
        fun `global registry contains List type`() {
            val type = globalRegistry.getType("builtin.List")
            assertNotNull(type)
            assertEquals("builtin.List", type.typeName)
        }

        @Test
        fun `global registry contains Set type`() {
            val type = globalRegistry.getType("builtin.Set")
            assertNotNull(type)
            assertEquals("builtin.Set", type.typeName)
        }

        @Test
        fun `global registry contains Bag type`() {
            val type = globalRegistry.getType("builtin.Bag")
            assertNotNull(type)
            assertEquals("builtin.Bag", type.typeName)
        }

        @Test
        fun `global registry contains OrderedSet type`() {
            val type = globalRegistry.getType("builtin.OrderedSet")
            assertNotNull(type)
            assertEquals("builtin.OrderedSet", type.typeName)
        }

        @Test
        fun `global registry contains ReadonlyBag type`() {
            val type = globalRegistry.getType("builtin.ReadonlyBag")
            assertNotNull(type)
            assertEquals("builtin.ReadonlyBag", type.typeName)
        }

        @Test
        fun `global registry contains ReadonlyOrderedSet type`() {
            val type = globalRegistry.getType("builtin.ReadonlyOrderedSet")
            assertNotNull(type)
            assertEquals("builtin.ReadonlyOrderedSet", type.typeName)
        }
    }

    @Nested
    inner class CollectionTypeHierarchy {

        @Test
        fun `List extends OrderedCollection`() {
            val listType = globalRegistry.getType("builtin.List")
            assertNotNull(listType)
            assertTrue(listType.extends.contains("builtin.OrderedCollection"))
        }

        @Test
        fun `Set extends Collection`() {
            val setType = globalRegistry.getType("builtin.Set")
            assertNotNull(setType)
            assertTrue(setType.extends.contains("builtin.Collection"))
        }

        @Test
        fun `Bag extends ReadonlyBag and Collection`() {
            val bagType = globalRegistry.getType("builtin.Bag")
            assertNotNull(bagType)
            assertTrue(bagType.extends.contains("builtin.ReadonlyBag"))
            assertTrue(bagType.extends.contains("builtin.Collection"))
        }

        @Test
        fun `OrderedSet extends ReadonlyOrderedSet and OrderedCollection`() {
            val orderedSetType = globalRegistry.getType("builtin.OrderedSet")
            assertNotNull(orderedSetType)
            assertTrue(orderedSetType.extends.contains("builtin.ReadonlyOrderedSet"))
            assertTrue(orderedSetType.extends.contains("builtin.OrderedCollection"))
        }

        @Test
        fun `OrderedCollection extends ReadonlyOrderedCollection and Collection`() {
            val orderedCollectionType = globalRegistry.getType("builtin.OrderedCollection")
            assertNotNull(orderedCollectionType)
            assertTrue(orderedCollectionType.extends.contains("builtin.ReadonlyOrderedCollection"))
            assertTrue(orderedCollectionType.extends.contains("builtin.Collection"))
        }

        @Test
        fun `ReadonlyOrderedCollection extends ReadonlyCollection`() {
            val readonlyOrderedCollectionType = globalRegistry.getType("builtin.ReadonlyOrderedCollection")
            assertNotNull(readonlyOrderedCollectionType)
            assertTrue(readonlyOrderedCollectionType.extends.contains("builtin.ReadonlyCollection"))
        }

        @Test
        fun `Collection extends ReadonlyCollection`() {
            val collectionType = globalRegistry.getType("builtin.Collection")
            assertNotNull(collectionType)
            assertTrue(collectionType.extends.contains("builtin.ReadonlyCollection"))
        }
    }

    @Nested
    inner class ReadonlyCollectionMethods {

        @Test
        fun `can lookup size method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyCollection",
                "size",
                ""
            )
            assertNotNull(method)
            assertEquals("size", method.name)
        }

        @Test
        fun `can lookup isEmpty method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyCollection",
                "isEmpty",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup notEmpty method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyCollection",
                "notEmpty",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup includes method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyCollection",
                "includes",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup excludes method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyCollection",
                "excludes",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup sum method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyCollection",
                "sum",
                ""
            )
            assertNotNull(method)
        }
    }

    @Nested
    inner class CollectionMutationMethods {

        @Test
        fun `can lookup add method on Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Collection",
                "add",
                ""
            )
            assertNotNull(method)
            assertEquals("add", method.name)
        }

        @Test
        fun `can lookup clear method on Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Collection",
                "clear",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup remove method on Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Collection",
                "remove",
                ""
            )
            assertNotNull(method)
        }
    }

    @Nested
    inner class OrderedCollectionMethods {

        @Test
        fun `can lookup at method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyOrderedCollection",
                "at",
                ""
            )
            assertNotNull(method)
            assertEquals("at", method.name)
        }

        @Test
        fun `can lookup first method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyOrderedCollection",
                "first",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup last method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyOrderedCollection",
                "last",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup indexOf method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.ReadonlyOrderedCollection",
                "indexOf",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup removeAt method on OrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedCollection",
                "removeAt",
                ""
            )
            assertNotNull(method)
        }
    }

    @Nested
    inner class ListInheritedMethods {

        @Test
        fun `List can lookup size method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.List",
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `List can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.List",
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `List can lookup at method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.List",
                "at",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `List can lookup removeAt method inherited from OrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.List",
                "removeAt",
                ""
            )
            assertNotNull(method)
        }
    }

    @Nested
    inner class SetInheritedMethods {

        @Test
        fun `Set can lookup size method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Set",
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Set can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Set",
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Set can lookup includes method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Set",
                "includes",
                ""
            )
            assertNotNull(method)
        }
    }

    @Nested
    inner class BagInheritedMethods {

        @Test
        fun `Bag can lookup size method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Bag",
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Bag can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Bag",
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Bag can lookup includes method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Bag",
                "includes",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Bag can lookup clear method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.Bag",
                "clear",
                ""
            )
            assertNotNull(method)
        }
    }

    @Nested
    inner class OrderedSetInheritedMethods {

        @Test
        fun `OrderedSet can lookup size method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedSet",
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedSet",
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup at method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedSet",
                "at",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup first method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedSet",
                "first",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup last method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedSet",
                "last",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup removeAt method inherited from OrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                "builtin.OrderedSet",
                "removeAt",
                ""
            )
            assertNotNull(method)
        }
    }
}
