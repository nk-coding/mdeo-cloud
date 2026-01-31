package com.mdeo.script.stdlib.impl.collections

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Comprehensive tests for MapImpl.
 */
class MapImplTest {

    // ==================== size() tests ====================
    @Test
    fun `size returns 0 for empty map`() {
        val map = MapImpl<String, Int>()
        assertEquals(0, map.size())
    }

    @Test
    fun `size returns correct count`() {
        val map = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        assertEquals(3, map.size())
    }

    @Test
    fun `size updates after adding entries`() {
        val map = MapImpl<String, Int>()
        map.put("a", 1)
        map.put("b", 2)
        assertEquals(2, map.size())
    }

    @Test
    fun `size updates after removing entries`() {
        val map = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        map.remove("b")
        assertEquals(2, map.size())
    }

    @Test
    fun `size does not increase for duplicate key`() {
        val map = MapImpl.of("a" to 1)
        map.put("a", 2)
        assertEquals(1, map.size())
    }

    // ==================== isEmpty() tests ====================
    @Test
    fun `isEmpty returns true for empty map`() {
        val map = MapImpl<String, Int>()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `isEmpty returns false for non-empty map`() {
        val map = MapImpl.of("a" to 1)
        assertFalse(map.isEmpty())
    }

    @Test
    fun `isEmpty returns true after clearing`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `isEmpty returns false after adding to empty map`() {
        val map = MapImpl<String, Int>()
        map.put("a", 1)
        assertFalse(map.isEmpty())
    }

    @Test
    fun `isEmpty returns true after removing all entries`() {
        val map = MapImpl.of("a" to 1)
        map.remove("a")
        assertTrue(map.isEmpty())
    }

    // ==================== containsKey() tests ====================
    @Test
    fun `containsKey returns true when key exists`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        assertTrue(map.containsKey("a"))
    }

    @Test
    fun `containsKey returns false when key does not exist`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        assertFalse(map.containsKey("c"))
    }

    @Test
    fun `containsKey returns false for empty map`() {
        val map = MapImpl<String, Int>()
        assertFalse(map.containsKey("a"))
    }

    @Test
    fun `containsKey handles null key`() {
        val map = MapImpl<String?, Int>()
        map.put(null, 1)
        assertTrue(map.containsKey(null))
    }

    @Test
    fun `containsKey returns false after removing key`() {
        val map = MapImpl.of("a" to 1)
        map.remove("a")
        assertFalse(map.containsKey("a"))
    }

    // ==================== containsValue() tests ====================
    @Test
    fun `containsValue returns true when value exists`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        assertTrue(map.containsValue(1))
    }

    @Test
    fun `containsValue returns false when value does not exist`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        assertFalse(map.containsValue(5))
    }

    @Test
    fun `containsValue returns false for empty map`() {
        val map = MapImpl<String, Int>()
        assertFalse(map.containsValue(1))
    }

    @Test
    fun `containsValue handles null value`() {
        val map = MapImpl<String, Int?>()
        map.put("a", null)
        assertTrue(map.containsValue(null))
    }

    @Test
    fun `containsValue returns true for duplicate value`() {
        val map = MapImpl.of("a" to 1, "b" to 1)
        assertTrue(map.containsValue(1))
    }

    // ==================== get() tests ====================
    @Test
    fun `get returns value for existing key`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        assertEquals(1, map.get("a"))
    }

    @Test
    fun `get returns null for non-existing key`() {
        val map = MapImpl.of("a" to 1)
        assertNull(map.get("b"))
    }

    @Test
    fun `get returns null for empty map`() {
        val map = MapImpl<String, Int>()
        assertNull(map.get("a"))
    }

    @Test
    fun `get returns null value correctly`() {
        val map = MapImpl<String, Int?>()
        map.put("a", null)
        assertNull(map.get("a"))
    }

    @Test
    fun `get with null key works`() {
        val map = MapImpl<String?, Int>()
        map.put(null, 42)
        assertEquals(42, map.get(null))
    }

    // ==================== put() tests ====================
    @Test
    fun `put adds new entry`() {
        val map = MapImpl<String, Int>()
        map.put("a", 1)
        assertEquals(1, map.get("a"))
    }

    @Test
    fun `put overwrites existing entry`() {
        val map = MapImpl.of("a" to 1)
        map.put("a", 2)
        assertEquals(2, map.get("a"))
    }

    @Test
    fun `put with null key works`() {
        val map = MapImpl<String?, Int>()
        map.put(null, 1)
        assertEquals(1, map.get(null))
    }

    @Test
    fun `put with null value works`() {
        val map = MapImpl<String, Int?>()
        map.put("a", null)
        assertTrue(map.containsKey("a"))
    }

    @Test
    fun `put increases size for new key`() {
        val map = MapImpl.of("a" to 1)
        map.put("b", 2)
        assertEquals(2, map.size())
    }

    // ==================== putAll() tests ====================
    @Test
    fun `putAll adds all entries`() {
        val map1 = MapImpl.of("a" to 1)
        val map2 = MapImpl.of("b" to 2, "c" to 3)
        map1.putAll(map2)
        assertEquals(3, map1.size())
    }

    @Test
    fun `putAll overwrites existing entries`() {
        val map1 = MapImpl.of("a" to 1, "b" to 2)
        val map2 = MapImpl.of("a" to 10)
        map1.putAll(map2)
        assertEquals(10, map1.get("a"))
    }

    @Test
    fun `putAll with empty map does nothing`() {
        val map1 = MapImpl.of("a" to 1)
        val map2 = MapImpl<String, Int>()
        map1.putAll(map2)
        assertEquals(1, map1.size())
    }

    @Test
    fun `putAll to empty map works`() {
        val map1 = MapImpl<String, Int>()
        val map2 = MapImpl.of("a" to 1, "b" to 2)
        map1.putAll(map2)
        assertEquals(2, map1.size())
    }

    @Test
    fun `putAll handles multiple overlapping keys`() {
        val map1 = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        val map2 = MapImpl.of("b" to 20, "c" to 30, "d" to 40)
        map1.putAll(map2)
        assertEquals(4, map1.size())
        assertEquals(20, map1.get("b"))
    }

    @Test
    fun `putAll copies null values from source map`() {
        // BUG: putAll skips null values - it should copy them
        val map1 = MapImpl<String, Int?>()
        map1.put("a", 1)
        val map2 = MapImpl<String, Int?>()
        map2.put("b", null)
        map2.put("c", 3)
        map1.putAll(map2)
        assertEquals(3, map1.size())
        assertTrue(map1.containsKey("b"))
        assertNull(map1.get("b"))
    }

    @Test
    fun `putAll overwrites with null value from source map`() {
        // BUG: putAll skips null values - it should overwrite existing values with null
        val map1 = MapImpl<String, Int?>()
        map1.put("a", 1)
        val map2 = MapImpl<String, Int?>()
        map2.put("a", null)
        map1.putAll(map2)
        assertTrue(map1.containsKey("a"))
        assertNull(map1.get("a"))
    }

    // ==================== remove() tests ====================
    @Test
    fun `remove removes entry and returns value`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        val removed = map.remove("a")
        assertEquals(1, removed)
        assertFalse(map.containsKey("a"))
    }

    @Test
    fun `remove returns null for non-existing key`() {
        val map = MapImpl.of("a" to 1)
        val removed = map.remove("b")
        assertNull(removed)
    }

    @Test
    fun `remove from empty map returns null`() {
        val map = MapImpl<String, Int>()
        val removed = map.remove("a")
        assertNull(removed)
    }

    @Test
    fun `remove decreases size`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        map.remove("a")
        assertEquals(1, map.size())
    }

    @Test
    fun `remove with null key works`() {
        val map = MapImpl<String?, Int>()
        map.put(null, 1)
        val removed = map.remove(null)
        assertEquals(1, removed)
    }

    // ==================== clear() tests ====================
    @Test
    fun `clear removes all entries`() {
        val map = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `clear on empty map is safe`() {
        val map = MapImpl<String, Int>()
        map.clear()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `clear sets size to 0`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        map.clear()
        assertEquals(0, map.size())
    }

    @Test
    fun `clear allows adding entries after`() {
        val map = MapImpl.of("a" to 1)
        map.clear()
        map.put("b", 2)
        assertEquals(1, map.size())
    }

    @Test
    fun `clear removes null entries`() {
        val map = MapImpl<String?, Int?>()
        map.put(null, null)
        map.clear()
        assertFalse(map.containsKey(null))
    }

    // ==================== keySet() tests ====================
    @Test
    fun `keySet returns all keys`() {
        val map = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        val keys = map.keySet()
        assertEquals(3, keys.size())
        assertTrue(keys.includes("a"))
        assertTrue(keys.includes("b"))
        assertTrue(keys.includes("c"))
    }

    @Test
    fun `keySet returns empty for empty map`() {
        val map = MapImpl<String, Int>()
        assertTrue(map.keySet().isEmpty())
    }

    @Test
    fun `keySet is readonly set`() {
        val map = MapImpl.of("a" to 1)
        val keys = map.keySet()
        @Suppress("USELESS_IS_CHECK")
        assertTrue(keys is ReadonlySet)
    }

    @Test
    fun `keySet contains null key`() {
        val map = MapImpl<String?, Int>()
        map.put(null, 1)
        assertTrue(map.keySet().includes(null))
    }

    @Test
    fun `keySet reflects map state`() {
        val map = MapImpl.of("a" to 1)
        map.put("b", 2)
        // Note: keySet is a snapshot, so it may not reflect the new state
        val keys = map.keySet()
        assertEquals(2, keys.size())
    }

    // ==================== values() tests ====================
    @Test
    fun `values returns all values`() {
        val map = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        val values = map.values()
        assertEquals(3, values.size())
    }

    @Test
    fun `values returns empty for empty map`() {
        val map = MapImpl<String, Int>()
        assertTrue(map.values().isEmpty())
    }

    @Test
    fun `values returns readonly bag`() {
        val map = MapImpl.of("a" to 1)
        val values = map.values()
        @Suppress("USELESS_IS_CHECK")
        assertTrue(values is ReadonlyBag)
    }

    @Test
    fun `values contains duplicate values`() {
        val map = MapImpl.of("a" to 1, "b" to 1, "c" to 1)
        val values = map.values()
        assertEquals(3, values.size())
    }

    @Test
    fun `values contains null value`() {
        val map = MapImpl<String, Int?>()
        map.put("a", null)
        assertTrue(map.values().includes(null))
    }

    // ==================== equals() and hashCode() tests ====================
    @Test
    fun `equals returns true for same content`() {
        val map1 = MapImpl.of("a" to 1, "b" to 2)
        val map2 = MapImpl.of("a" to 1, "b" to 2)
        assertEquals(map1, map2)
    }

    @Test
    fun `equals returns false for different content`() {
        val map1 = MapImpl.of("a" to 1, "b" to 2)
        val map2 = MapImpl.of("a" to 1, "b" to 3)
        assertFalse(map1.equals(map2))
    }

    @Test
    fun `equals returns false for different sizes`() {
        val map1 = MapImpl.of("a" to 1)
        val map2 = MapImpl.of("a" to 1, "b" to 2)
        assertFalse(map1.equals(map2))
    }

    @Test
    fun `equals with same reference returns true`() {
        val map = MapImpl.of("a" to 1)
        assertTrue(map.equals(map))
    }

    @Test
    fun `hashCode is consistent`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        val hash1 = map.hashCode()
        val hash2 = map.hashCode()
        assertEquals(hash1, hash2)
    }

    // ==================== toString() tests ====================
    @Test
    fun `toString returns readable format`() {
        val map = MapImpl.of("a" to 1)
        val str = map.toString()
        assertTrue(str.contains("a"))
        assertTrue(str.contains("1"))
    }

    @Test
    fun `toString for empty map`() {
        val map = MapImpl<String, Int>()
        assertNotNull(map.toString())
    }

    @Test
    fun `toString with null key and value`() {
        val map = MapImpl<String?, Int?>()
        map.put(null, null)
        assertNotNull(map.toString())
    }

    @Test
    fun `toString with multiple entries`() {
        val map = MapImpl.of("a" to 1, "b" to 2, "c" to 3)
        val str = map.toString()
        assertTrue(str.contains("a"))
        assertTrue(str.contains("b"))
        assertTrue(str.contains("c"))
    }

    @Test
    fun `toString is not empty for non-empty map`() {
        val map = MapImpl.of("key" to "value")
        assertTrue(map.toString().isNotEmpty())
    }

    // ==================== factory method tests ====================
    @Test
    fun `of creates map with entries`() {
        val map = MapImpl.of("a" to 1, "b" to 2)
        assertEquals(2, map.size())
        assertEquals(1, map.get("a"))
    }

    @Test
    fun `of with no arguments creates empty map`() {
        val map = MapImpl.of<String, Int>()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `empty creates empty map`() {
        val map = MapImpl.empty<String, Int>()
        assertTrue(map.isEmpty())
    }

    @Test
    fun `of with single entry works`() {
        val map = MapImpl.of("a" to 1)
        assertEquals(1, map.size())
    }

    @Test
    fun `of with duplicate keys keeps last value`() {
        val map = MapImpl.of("a" to 1, "a" to 2)
        assertEquals(2, map.get("a"))
        assertEquals(1, map.size())
    }
}
