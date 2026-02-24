package com.mdeo.script.compiler.registry

import com.mdeo.script.compiler.registry.type.TypeRegistry
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.mdeo.expression.ast.types.ClassTypeRef

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
            val type = globalRegistry.getType(ClassTypeRef("builtin", "ReadonlyCollection", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("ReadonlyCollection", type.typeName)
        }

        @Test
        fun `global registry contains Collection type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "Collection", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("Collection", type.typeName)
        }

        @Test
        fun `global registry contains List type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "List", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("List", type.typeName)
        }

        @Test
        fun `global registry contains Set type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "Set", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("Set", type.typeName)
        }

        @Test
        fun `global registry contains Bag type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "Bag", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("Bag", type.typeName)
        }

        @Test
        fun `global registry contains OrderedSet type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "OrderedSet", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("OrderedSet", type.typeName)
        }

        @Test
        fun `global registry contains ReadonlyBag type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "ReadonlyBag", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("ReadonlyBag", type.typeName)
        }

        @Test
        fun `global registry contains ReadonlyOrderedSet type`() {
            val type = globalRegistry.getType(ClassTypeRef("builtin", "ReadonlyOrderedSet", false))
            assertNotNull(type)
            assertEquals("builtin", type.typePackage)
            assertEquals("ReadonlyOrderedSet", type.typeName)
        }
    }

    @Nested
    inner class CollectionTypeHierarchy {

        @Test
        fun `List extends OrderedCollection`() {
            val listType = globalRegistry.getType(ClassTypeRef("builtin", "List", false))
            assertNotNull(listType)
            assertTrue(listType.extends.contains(ClassTypeRef("builtin", "OrderedCollection", false)))
        }

        @Test
        fun `Set extends Collection`() {
            val setType = globalRegistry.getType(ClassTypeRef("builtin", "Set", false))
            assertNotNull(setType)
            assertTrue(setType.extends.contains(ClassTypeRef("builtin", "Collection", false)))
        }

        @Test
        fun `Bag extends ReadonlyBag and Collection`() {
            val bagType = globalRegistry.getType(ClassTypeRef("builtin", "Bag", false))
            assertNotNull(bagType)
            assertTrue(bagType.extends.contains(ClassTypeRef("builtin", "ReadonlyBag", false)))
            assertTrue(bagType.extends.contains(ClassTypeRef("builtin", "Collection", false)))
        }

        @Test
        fun `OrderedSet extends ReadonlyOrderedSet and OrderedCollection`() {
            val orderedSetType = globalRegistry.getType(ClassTypeRef("builtin", "OrderedSet", false))
            assertNotNull(orderedSetType)
            assertTrue(orderedSetType.extends.contains(ClassTypeRef("builtin", "ReadonlyOrderedSet", false)))
            assertTrue(orderedSetType.extends.contains(ClassTypeRef("builtin", "OrderedCollection", false)))
        }

        @Test
        fun `OrderedCollection extends ReadonlyOrderedCollection and Collection`() {
            val orderedCollectionType = globalRegistry.getType(ClassTypeRef("builtin", "OrderedCollection", false))
            assertNotNull(orderedCollectionType)
            assertTrue(orderedCollectionType.extends.contains(ClassTypeRef("builtin", "ReadonlyOrderedCollection", false)))
            assertTrue(orderedCollectionType.extends.contains(ClassTypeRef("builtin", "Collection", false)))
        }

        @Test
        fun `ReadonlyOrderedCollection extends ReadonlyCollection`() {
            val readonlyOrderedCollectionType = globalRegistry.getType(ClassTypeRef("builtin", "ReadonlyOrderedCollection", false))
            assertNotNull(readonlyOrderedCollectionType)
            assertTrue(readonlyOrderedCollectionType.extends.contains(ClassTypeRef("builtin", "ReadonlyCollection", false)))
        }

        @Test
        fun `Collection extends ReadonlyCollection`() {
            val collectionType = globalRegistry.getType(ClassTypeRef("builtin", "Collection", false))
            assertNotNull(collectionType)
            assertTrue(collectionType.extends.contains(ClassTypeRef("builtin", "ReadonlyCollection", false)))
        }
    }

    @Nested
    inner class ReadonlyCollectionMethods {

        @Test
        fun `can lookup size method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyCollection", false),
                "size",
                ""
            )
            assertNotNull(method)
            assertEquals("size", method.name)
        }

        @Test
        fun `can lookup isEmpty method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyCollection", false),
                "isEmpty",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup notEmpty method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyCollection", false),
                "notEmpty",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup includes method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyCollection", false),
                "includes",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup excludes method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyCollection", false),
                "excludes",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup sum method on ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyCollection", false),
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
                ClassTypeRef("builtin", "Collection", false),
                "add",
                ""
            )
            assertNotNull(method)
            assertEquals("add", method.name)
        }

        @Test
        fun `can lookup clear method on Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Collection", false),
                "clear",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup remove method on Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Collection", false),
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
                ClassTypeRef("builtin", "ReadonlyOrderedCollection", false),
                "at",
                ""
            )
            assertNotNull(method)
            assertEquals("at", method.name)
        }

        @Test
        fun `can lookup first method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyOrderedCollection", false),
                "first",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup last method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyOrderedCollection", false),
                "last",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup indexOf method on ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "ReadonlyOrderedCollection", false),
                "indexOf",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `can lookup removeAt method on OrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "OrderedCollection", false),
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
                ClassTypeRef("builtin", "List", false),
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `List can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "List", false),
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `List can lookup at method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "List", false),
                "at",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `List can lookup removeAt method inherited from OrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "List", false),
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
                ClassTypeRef("builtin", "Set", false),
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Set can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Set", false),
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Set can lookup includes method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Set", false),
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
                ClassTypeRef("builtin", "Bag", false),
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Bag can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Bag", false),
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Bag can lookup includes method inherited from ReadonlyCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Bag", false),
                "includes",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `Bag can lookup clear method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "Bag", false),
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
                ClassTypeRef("builtin", "OrderedSet", false),
                "size",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup add method inherited from Collection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "OrderedSet", false),
                "add",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup at method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "OrderedSet", false),
                "at",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup first method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "OrderedSet", false),
                "first",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup last method inherited from ReadonlyOrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "OrderedSet", false),
                "last",
                ""
            )
            assertNotNull(method)
        }

        @Test
        fun `OrderedSet can lookup removeAt method inherited from OrderedCollection`() {
            val method = globalRegistry.lookupMethod(
                ClassTypeRef("builtin", "OrderedSet", false),
                "removeAt",
                ""
            )
            assertNotNull(method)
        }
    }
}
