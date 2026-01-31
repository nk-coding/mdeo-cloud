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
            assertTrue(registry.hasType("collection.readonly"))
            assertTrue(registry.hasType("collection.collection"))
            assertTrue(registry.hasType("collection.readonly-ordered"))
            assertTrue(registry.hasType("collection.list"))
            assertTrue(registry.hasType("collection.set"))
            assertTrue(registry.hasType("collection.ordered-set"))
            assertTrue(registry.hasType("collection.bag"))
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
        fun `ReadonlyCollection has size property`() {
            val property = registry.lookupProperty("collection.readonly", "size")
            assertNotNull(property)
        }

        @Test
        fun `ReadonlyCollection has isEmpty method`() {
            val method = registry.lookupMethod("collection.readonly", "isEmpty", "")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has notEmpty method`() {
            val method = registry.lookupMethod("collection.readonly", "notEmpty", "")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has sum method`() {
            val method = registry.lookupMethod("collection.readonly", "sum", "")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has first method`() {
            val method = registry.lookupMethod("collection.readonly", "first", "")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has last method`() {
            val method = registry.lookupMethod("collection.readonly", "last", "")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has filter lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "filter", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has map lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "map", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has exists lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "exists", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has all lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "all", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has none lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "none", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has one lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "one", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has find lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "find", "lambda")
            assertNotNull(method)
        }

        @Test
        fun `ReadonlyCollection has reject lambda method`() {
            val method = registry.lookupMethod("collection.readonly", "reject", "lambda")
            assertNotNull(method)
        }
    }

    @Nested
    inner class TypeHierarchyTests {

        @Test
        fun `Collection extends ReadonlyCollection`() {
            val collectionType = registry.getType("collection.collection")
            assertNotNull(collectionType)
            assertTrue(collectionType.extends.contains("collection.readonly"))
        }

        @Test
        fun `List extends ReadonlyOrderedCollection`() {
            val listType = registry.getType("collection.list")
            assertNotNull(listType)
            assertTrue(listType.extends.contains("collection.readonly-ordered"))
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
        fun `ReadonlyOrderedCollection extends ReadonlyCollection`() {
            val orderedType = registry.getType("collection.readonly-ordered")
            assertNotNull(orderedType)
            assertTrue(orderedType.extends.contains("collection.readonly"))
        }

        @Test
        fun `OrderedSet extends ReadonlyOrderedCollection`() {
            val setType = registry.getType("collection.ordered-set")
            assertNotNull(setType)
            assertTrue(setType.extends.contains("collection.readonly-ordered"))
        }

        @Test
        fun `Set extends ReadonlyCollection`() {
            val setType = registry.getType("collection.set")
            assertNotNull(setType)
            assertTrue(setType.extends.contains("collection.readonly"))
        }

        @Test
        fun `Bag extends ReadonlyCollection`() {
            val bagType = registry.getType("collection.bag")
            assertNotNull(bagType)
            assertTrue(bagType.extends.contains("collection.readonly"))
        }
    }
}
