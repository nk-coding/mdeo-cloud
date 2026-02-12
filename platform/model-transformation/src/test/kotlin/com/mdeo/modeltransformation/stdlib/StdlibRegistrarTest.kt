package com.mdeo.modeltransformation.stdlib

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [StdlibRegistrar] and standard library type definitions.
 *
 * Tests cover:
 * - Type registration
 * - Type hierarchy and inheritance
 * - Method existence for pure traversal implementations
 *
 * Note: Value-based methods (string operations, type conversions, etc.) have been
 * intentionally removed as they cannot be implemented as pure Gremlin traversals.
 */
class StdlibRegistrarTest {

    private lateinit var registry: GremlinTypeRegistry

    @BeforeEach
    fun setUp() {
        registry = StdlibRegistrar.createRegistry()
    }

    @Nested
    inner class RegistrationTests {

        @Test
        fun `registers all primitive types`() {
            assertTrue(registry.hasType("builtin.any"))
            assertTrue(registry.hasType("builtin.int"))
            assertTrue(registry.hasType("builtin.long"))
            assertTrue(registry.hasType("builtin.float"))
            assertTrue(registry.hasType("builtin.double"))
            assertTrue(registry.hasType("builtin.boolean"))
            assertTrue(registry.hasType("builtin.string"))
        }

        @Test
        fun `registers all collection types`() {
            assertTrue(registry.hasType("builtin.Collection"))
            assertTrue(registry.hasType("builtin.Collection"))
            assertTrue(registry.hasType("builtin.OrderedCollection"))
            assertTrue(registry.hasType("builtin.List"))
            assertTrue(registry.hasType("builtin.Set"))
            assertTrue(registry.hasType("builtin.OrderedSet"))
            assertTrue(registry.hasType("builtin.Bag"))
        }
    }

    @Nested
    inner class IntTypeTests {

        @Test
        fun `int has abs method`() {
            val method = registry.lookupMethod("builtin.int", "abs", "")
            assertNotNull(method)
        }

        @Test
        fun `int has max method`() {
            val method = registry.lookupMethod("builtin.int", "max", "builtin.int")
            assertNotNull(method)
        }

        @Test
        fun `int has min method`() {
            val method = registry.lookupMethod("builtin.int", "min", "builtin.int")
            assertNotNull(method)
        }

        @Test
        fun `int has pow method`() {
            val method = registry.lookupMethod("builtin.int", "pow", "")
            assertNotNull(method)
        }

        @Test
        fun `int has mod method`() {
            val method = registry.lookupMethod("builtin.int", "mod", "")
            assertNotNull(method)
        }

        @Test
        fun `int has log method`() {
            val method = registry.lookupMethod("builtin.int", "log", "")
            assertNotNull(method)
        }

        @Test
        fun `int has log10 method`() {
            val method = registry.lookupMethod("builtin.int", "log10", "")
            assertNotNull(method)
        }
    }

    @Nested
    inner class LongTypeTests {

        @Test
        fun `long has abs method`() {
            val method = registry.lookupMethod("builtin.long", "abs", "")
            assertNotNull(method)
        }

        @Test
        fun `long has max method`() {
            val method = registry.lookupMethod("builtin.long", "max", "builtin.long")
            assertNotNull(method)
        }
    }

    @Nested
    inner class FloatTypeTests {

        @Test
        fun `float has abs method`() {
            val method = registry.lookupMethod("builtin.float", "abs", "")
            assertNotNull(method)
        }

        @Test
        fun `float has floor method`() {
            val method = registry.lookupMethod("builtin.float", "floor", "")
            assertNotNull(method)
        }

        @Test
        fun `float has ceiling method`() {
            val method = registry.lookupMethod("builtin.float", "ceiling", "")
            assertNotNull(method)
        }

        @Test
        fun `float has round method`() {
            val method = registry.lookupMethod("builtin.float", "round", "")
            assertNotNull(method)
        }
    }

    @Nested
    inner class DoubleTypeTests {

        @Test
        fun `double has abs method`() {
            val method = registry.lookupMethod("builtin.double", "abs", "")
            assertNotNull(method)
        }

        @Test
        fun `double has round method`() {
            val method = registry.lookupMethod("builtin.double", "round", "")
            assertNotNull(method)
        }
    }

    @Nested
    inner class CollectionTypeTests {

        @Test
        fun `Collection has size method`() {
            val method = registry.lookupMethod("builtin.Collection", "size", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has isEmpty method`() {
            val method = registry.lookupMethod("builtin.Collection", "isEmpty", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has notEmpty method`() {
            val method = registry.lookupMethod("builtin.Collection", "notEmpty", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has sum method`() {
            val method = registry.lookupMethod("builtin.Collection", "sum", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has first method`() {
            val method = registry.lookupMethod("builtin.Collection", "first", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has last method`() {
            val method = registry.lookupMethod("builtin.Collection", "last", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has filter lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "filter", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has map lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "map", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has exists lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "exists", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has all lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "all", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has none lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "none", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has one lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "one", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has find lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "find", "")
            assertNotNull(method)
        }

        @Test
        fun `Collection has reject lambda method`() {
            val method = registry.lookupMethod("builtin.Collection", "reject", "")
            assertNotNull(method)
        }
    }

    @Nested
    inner class TypeHierarchyTests {

        @Test
        fun `Collection extends any`() {
            val collectionType = registry.getType("builtin.Collection")
            assertNotNull(collectionType)
            assertTrue(collectionType.extends.contains("builtin.any"))
        }

        @Test
        fun `List extends OrderedCollection`() {
            val listType = registry.getType("builtin.List")
            assertNotNull(listType)
            assertTrue(listType.extends.contains("builtin.OrderedCollection"))
        }

        @Test
        fun `int extends any`() {
            val intType = registry.getType("builtin.int")
            assertNotNull(intType)
            assertTrue(intType.extends.contains("builtin.any"))
        }

        @Test
        fun `string extends any`() {
            val stringType = registry.getType("builtin.string")
            assertNotNull(stringType)
            assertTrue(stringType.extends.contains("builtin.any"))
        }

        @Test
        fun `OrderedCollection extends Collection`() {
            val orderedType = registry.getType("builtin.OrderedCollection")
            assertNotNull(orderedType)
            assertTrue(orderedType.extends.contains("builtin.Collection"))
        }

        @Test
        fun `OrderedSet extends OrderedCollection`() {
            val setType = registry.getType("builtin.OrderedSet")
            assertNotNull(setType)
            assertTrue(setType.extends.contains("builtin.OrderedCollection"))
        }

        @Test
        fun `Set extends Collection`() {
            val setType = registry.getType("builtin.Set")
            assertNotNull(setType)
            assertTrue(setType.extends.contains("builtin.Collection"))
        }

        @Test
        fun `Bag extends Collection`() {
            val bagType = registry.getType("builtin.Bag")
            assertNotNull(bagType)
            assertTrue(bagType.extends.contains("builtin.Collection"))
        }
    }
}
