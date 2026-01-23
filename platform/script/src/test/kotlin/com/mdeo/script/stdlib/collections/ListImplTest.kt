package com.mdeo.script.stdlib.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.function.Predicate
import java.util.function.Function
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/**
 * Comprehensive tests for ListImpl covering all methods with at least 5 tests each.
 */
class ListImplTest {

    // ==================== size() tests ====================
    @Test
    fun `size returns 0 for empty list`() {
        val list = ListImpl<Int>()
        assertEquals(0, list.size())
    }

    @Test
    fun `size returns correct count for single element`() {
        val list = ListImpl.of(1)
        assertEquals(1, list.size())
    }

    @Test
    fun `size returns correct count for multiple elements`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertEquals(5, list.size())
    }

    @Test
    fun `size updates after adding elements`() {
        val list = ListImpl<Int>()
        list.add(1)
        list.add(2)
        assertEquals(2, list.size())
    }

    @Test
    fun `size updates after removing elements`() {
        val list = ListImpl.of(1, 2, 3)
        list.remove(2)
        assertEquals(2, list.size())
    }

    @Test
    fun `size returns correct count with duplicates`() {
        val list = ListImpl.of(1, 1, 1, 1, 1)
        assertEquals(5, list.size())
    }

    // ==================== isEmpty() tests ====================
    @Test
    fun `isEmpty returns true for empty list`() {
        val list = ListImpl<Int>()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `isEmpty returns false for non-empty list`() {
        val list = ListImpl.of(1)
        assertFalse(list.isEmpty())
    }

    @Test
    fun `isEmpty returns true after clearing`() {
        val list = ListImpl.of(1, 2, 3)
        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `isEmpty returns false after adding to empty list`() {
        val list = ListImpl<Int>()
        list.add(1)
        assertFalse(list.isEmpty())
    }

    @Test
    fun `isEmpty returns true after removing all elements`() {
        val list = ListImpl.of(1)
        list.remove(1)
        assertTrue(list.isEmpty())
    }

    // ==================== notEmpty() tests ====================
    @Test
    fun `notEmpty returns false for empty list`() {
        val list = ListImpl<Int>()
        assertFalse(list.notEmpty())
    }

    @Test
    fun `notEmpty returns true for non-empty list`() {
        val list = ListImpl.of(1)
        assertTrue(list.notEmpty())
    }

    @Test
    fun `notEmpty returns false after clearing`() {
        val list = ListImpl.of(1, 2, 3)
        list.clear()
        assertFalse(list.notEmpty())
    }

    @Test
    fun `notEmpty returns true after adding to empty list`() {
        val list = ListImpl<Int>()
        list.add(1)
        assertTrue(list.notEmpty())
    }

    @Test
    fun `notEmpty returns false after removing all elements`() {
        val list = ListImpl.of(1)
        list.remove(1)
        assertFalse(list.notEmpty())
    }

    // ==================== includes() tests ====================
    @Test
    fun `includes returns true when element exists`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.includes(2))
    }

    @Test
    fun `includes returns false when element does not exist`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.includes(5))
    }

    @Test
    fun `includes returns false for empty list`() {
        val list = ListImpl<Int>()
        assertFalse(list.includes(1))
    }

    @Test
    fun `includes handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        assertTrue(list.includes(null))
    }

    @Test
    fun `includes returns true for first element`() {
        val list = ListImpl.of("a", "b", "c")
        assertTrue(list.includes("a"))
    }

    @Test
    fun `includes returns true for last element`() {
        val list = ListImpl.of("a", "b", "c")
        assertTrue(list.includes("c"))
    }

    // ==================== excludes() tests ====================
    @Test
    fun `excludes returns false when element exists`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.excludes(2))
    }

    @Test
    fun `excludes returns true when element does not exist`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.excludes(5))
    }

    @Test
    fun `excludes returns true for empty list`() {
        val list = ListImpl<Int>()
        assertTrue(list.excludes(1))
    }

    @Test
    fun `excludes handles null elements`() {
        val list = ListImpl<String?>()
        list.add("a")
        assertTrue(list.excludes(null))
    }

    @Test
    fun `excludes returns false when null is present and checking null`() {
        val list = ListImpl<String?>()
        list.add(null)
        assertFalse(list.excludes(null))
    }

    // ==================== includesAll() tests ====================
    @Test
    fun `includesAll returns true when all elements exist`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val subset = ListImpl.of(2, 3, 4)
        assertTrue(list.includesAll(subset))
    }

    @Test
    fun `includesAll returns false when some elements missing`() {
        val list = ListImpl.of(1, 2, 3)
        val other = ListImpl.of(2, 3, 4)
        assertFalse(list.includesAll(other))
    }

    @Test
    fun `includesAll returns true for empty collection`() {
        val list = ListImpl.of(1, 2, 3)
        val empty = ListImpl<Int>()
        assertTrue(list.includesAll(empty))
    }

    @Test
    fun `includesAll returns true for same collection`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.includesAll(list))
    }

    @Test
    fun `includesAll returns false when list is empty`() {
        val list = ListImpl<Int>()
        val other = ListImpl.of(1)
        assertFalse(list.includesAll(other))
    }

    // ==================== excludesAll() tests ====================
    @Test
    fun `excludesAll returns true when no elements exist`() {
        val list = ListImpl.of(1, 2, 3)
        val other = ListImpl.of(4, 5, 6)
        assertTrue(list.excludesAll(other))
    }

    @Test
    fun `excludesAll returns false when some elements exist`() {
        val list = ListImpl.of(1, 2, 3)
        val other = ListImpl.of(3, 4, 5)
        assertFalse(list.excludesAll(other))
    }

    @Test
    fun `excludesAll returns true for empty collection`() {
        val list = ListImpl.of(1, 2, 3)
        val empty = ListImpl<Int>()
        assertTrue(list.excludesAll(empty))
    }

    @Test
    fun `excludesAll returns true for empty list`() {
        val list = ListImpl<Int>()
        val other = ListImpl.of(1, 2, 3)
        assertTrue(list.excludesAll(other))
    }

    @Test
    fun `excludesAll returns false for same collection`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.excludesAll(list))
    }

    // ==================== count(item) tests ====================
    @Test
    fun `count returns 0 when element not found`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(0, list.count(5))
    }

    @Test
    fun `count returns 1 for unique element`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(1, list.count(2))
    }

    @Test
    fun `count returns correct count for duplicates`() {
        val list = ListImpl.of(1, 2, 2, 2, 3)
        assertEquals(3, list.count(2))
    }

    @Test
    fun `count returns 0 for empty list`() {
        val list = ListImpl<Int>()
        assertEquals(0, list.count(1))
    }

    @Test
    fun `count handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add(null)
        list.add("a")
        assertEquals(2, list.count(null))
    }

    // ==================== count(predicate) tests ====================
    @Test
    fun `count with predicate returns 0 when no match`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(0, list.count(Predicate { it > 10 }))
    }

    @Test
    fun `count with predicate returns correct count`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertEquals(3, list.count(Predicate { it > 2 }))
    }

    @Test
    fun `count with predicate returns size when all match`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(3, list.count(Predicate { it > 0 }))
    }

    @Test
    fun `count with predicate returns 0 for empty list`() {
        val list = ListImpl<Int>()
        assertEquals(0, list.count(Predicate { true }))
    }

    @Test
    fun `count with predicate works with complex predicate`() {
        val list = ListImpl.of("apple", "banana", "cherry", "apricot")
        assertEquals(2, list.count(Predicate { it.startsWith("a") }))
    }

    // ==================== add() tests ====================
    @Test
    fun `add adds element to empty list`() {
        val list = ListImpl<Int>()
        list.add(1)
        assertEquals(1, list.size())
        assertTrue(list.includes(1))
    }

    @Test
    fun `add adds element to end of list`() {
        val list = ListImpl.of(1, 2)
        list.add(3)
        assertEquals(3, list.last())
    }

    @Test
    fun `add allows duplicates`() {
        val list = ListImpl.of(1)
        list.add(1)
        assertEquals(2, list.size())
    }

    @Test
    fun `add allows null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        assertTrue(list.includes(null))
    }

    @Test
    fun `add returns true`() {
        val list = ListImpl<Int>()
        assertTrue(list.add(1))
    }

    // ==================== addAll() tests ====================
    @Test
    fun `addAll adds all elements from collection`() {
        val list = ListImpl.of(1, 2)
        val other = ListImpl.of(3, 4, 5)
        list.addAll(other)
        assertEquals(5, list.size())
    }

    @Test
    fun `addAll returns true when elements added`() {
        val list = ListImpl<Int>()
        val other = ListImpl.of(1, 2)
        assertTrue(list.addAll(other))
    }

    @Test
    fun `addAll returns false for empty collection`() {
        val list = ListImpl.of(1)
        val empty = ListImpl<Int>()
        assertFalse(list.addAll(empty))
    }

    @Test
    fun `addAll preserves order`() {
        val list = ListImpl.of(1, 2)
        val other = ListImpl.of(3, 4)
        list.addAll(other)
        assertEquals(3, list.at(2))
        assertEquals(4, list.at(3))
    }

    @Test
    fun `addAll allows duplicates from other collection`() {
        val list = ListImpl.of(1, 2)
        val other = ListImpl.of(1, 2)
        list.addAll(other)
        assertEquals(4, list.size())
    }

    // ==================== remove() tests ====================
    @Test
    fun `remove removes element from list`() {
        val list = ListImpl.of(1, 2, 3)
        list.remove(2)
        assertFalse(list.includes(2))
    }

    @Test
    fun `remove returns true when element found`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.remove(2))
    }

    @Test
    fun `remove returns false when element not found`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.remove(5))
    }

    @Test
    fun `remove only removes first occurrence`() {
        val list = ListImpl.of(1, 2, 2, 3)
        list.remove(2)
        assertEquals(3, list.size())
        assertTrue(list.includes(2))
    }

    @Test
    fun `remove handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        assertTrue(list.remove(null))
        assertEquals(1, list.size())
    }

    // ==================== removeAll() tests ====================
    @Test
    fun `removeAll removes all matching elements`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val toRemove = ListImpl.of(2, 4)
        list.removeAll(toRemove)
        assertEquals(3, list.size())
        assertFalse(list.includes(2))
        assertFalse(list.includes(4))
    }

    @Test
    fun `removeAll returns true when elements removed`() {
        val list = ListImpl.of(1, 2, 3)
        val toRemove = ListImpl.of(2)
        assertTrue(list.removeAll(toRemove))
    }

    @Test
    fun `removeAll returns false when no elements removed`() {
        val list = ListImpl.of(1, 2, 3)
        val toRemove = ListImpl.of(4, 5)
        assertFalse(list.removeAll(toRemove))
    }

    @Test
    fun `removeAll handles empty collection`() {
        val list = ListImpl.of(1, 2, 3)
        val empty = ListImpl<Int>()
        assertFalse(list.removeAll(empty))
        assertEquals(3, list.size())
    }

    @Test
    fun `removeAll only removes first occurrence of each`() {
        val list = ListImpl.of(1, 2, 2, 3, 3, 3)
        val toRemove = ListImpl.of(2, 3)
        list.removeAll(toRemove)
        assertEquals(4, list.size())
    }

    // ==================== clear() tests ====================
    @Test
    fun `clear removes all elements`() {
        val list = ListImpl.of(1, 2, 3)
        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `clear on empty list is safe`() {
        val list = ListImpl<Int>()
        list.clear()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `clear sets size to 0`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        list.clear()
        assertEquals(0, list.size())
    }

    @Test
    fun `clear allows adding elements after`() {
        val list = ListImpl.of(1, 2, 3)
        list.clear()
        list.add(4)
        assertEquals(1, list.size())
        assertEquals(4, list.first())
    }

    @Test
    fun `clear works with null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        list.clear()
        assertTrue(list.isEmpty())
    }

    // ==================== at() tests ====================
    @Test
    fun `at returns element at index`() {
        val list = ListImpl.of("a", "b", "c")
        assertEquals("b", list.at(1))
    }

    @Test
    fun `at returns first element at index 0`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(1, list.at(0))
    }

    @Test
    fun `at returns last element`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(3, list.at(2))
    }

    @Test
    fun `at throws for negative index`() {
        val list = ListImpl.of(1, 2, 3)
        assertThrows<IndexOutOfBoundsException> { list.at(-1) }
    }

    @Test
    fun `at throws for index out of bounds`() {
        val list = ListImpl.of(1, 2, 3)
        assertThrows<IndexOutOfBoundsException> { list.at(3) }
    }

    @Test
    fun `at throws for empty list`() {
        val list = ListImpl<Int>()
        assertThrows<IndexOutOfBoundsException> { list.at(0) }
    }

    // ==================== first() tests ====================
    @Test
    fun `first returns first element`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(1, list.first())
    }

    @Test
    fun `first returns only element in single-element list`() {
        val list = ListImpl.of(42)
        assertEquals(42, list.first())
    }

    @Test
    fun `first throws for empty list`() {
        val list = ListImpl<Int>()
        assertThrows<NoSuchElementException> { list.first() }
    }

    @Test
    fun `first returns null when first element is null`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        assertNull(list.first())
    }

    @Test
    fun `first reflects updates after add`() {
        val list = ListImpl<Int>()
        list.add(1)
        assertEquals(1, list.first())
        list.add(2)
        assertEquals(1, list.first())
    }

    // ==================== last() tests ====================
    @Test
    fun `last returns last element`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(3, list.last())
    }

    @Test
    fun `last returns only element in single-element list`() {
        val list = ListImpl.of(42)
        assertEquals(42, list.last())
    }

    @Test
    fun `last throws for empty list`() {
        val list = ListImpl<Int>()
        assertThrows<NoSuchElementException> { list.last() }
    }

    @Test
    fun `last returns null when last element is null`() {
        val list = ListImpl<String?>()
        list.add("a")
        list.add(null)
        assertNull(list.last())
    }

    @Test
    fun `last reflects updates after add`() {
        val list = ListImpl<Int>()
        list.add(1)
        assertEquals(1, list.last())
        list.add(2)
        assertEquals(2, list.last())
    }

    // ==================== indexOf() tests ====================
    @Test
    fun `indexOf returns correct index`() {
        val list = ListImpl.of("a", "b", "c")
        assertEquals(1, list.indexOf("b"))
    }

    @Test
    fun `indexOf returns 0 for first element`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(0, list.indexOf(1))
    }

    @Test
    fun `indexOf returns -1 for missing element`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(-1, list.indexOf(5))
    }

    @Test
    fun `indexOf returns first occurrence index`() {
        val list = ListImpl.of(1, 2, 2, 3)
        assertEquals(1, list.indexOf(2))
    }

    @Test
    fun `indexOf returns -1 for empty list`() {
        val list = ListImpl<Int>()
        assertEquals(-1, list.indexOf(1))
    }

    // ==================== invert() tests ====================
    @Test
    fun `invert reverses list order`() {
        val list = ListImpl.of(1, 2, 3)
        val inverted = list.invert()
        assertEquals(3, inverted.at(0))
        assertEquals(2, inverted.at(1))
        assertEquals(1, inverted.at(2))
    }

    @Test
    fun `invert returns empty for empty list`() {
        val list = ListImpl<Int>()
        val inverted = list.invert()
        assertTrue(inverted.isEmpty())
    }

    @Test
    fun `invert on single element returns same`() {
        val list = ListImpl.of(1)
        val inverted = list.invert()
        assertEquals(1, inverted.first())
    }

    @Test
    fun `invert creates new collection`() {
        val list = ListImpl.of(1, 2, 3)
        val inverted = list.invert()
        list.add(4)
        assertEquals(3, inverted.size())
    }

    @Test
    fun `invert preserves all elements`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val inverted = list.invert()
        assertEquals(5, inverted.size())
        assertTrue(inverted.includes(1))
        assertTrue(inverted.includes(5))
    }

    // ==================== removeAt() tests ====================
    @Test
    fun `removeAt removes element at index`() {
        val list = ListImpl.of(1, 2, 3)
        val removed = list.removeAt(1)
        assertEquals(2, removed)
        assertEquals(2, list.size())
    }

    @Test
    fun `removeAt throws for negative index`() {
        val list = ListImpl.of(1, 2, 3)
        assertThrows<IndexOutOfBoundsException> { list.removeAt(-1) }
    }

    @Test
    fun `removeAt throws for index out of bounds`() {
        val list = ListImpl.of(1, 2, 3)
        assertThrows<IndexOutOfBoundsException> { list.removeAt(3) }
    }

    @Test
    fun `removeAt removes first element correctly`() {
        val list = ListImpl.of(1, 2, 3)
        val removed = list.removeAt(0)
        assertEquals(1, removed)
        assertEquals(2, list.first())
    }

    @Test
    fun `removeAt removes last element correctly`() {
        val list = ListImpl.of(1, 2, 3)
        val removed = list.removeAt(2)
        assertEquals(3, removed)
        assertEquals(2, list.last())
    }

    // ==================== sortBy() tests ====================
    @Test
    fun `sortBy sorts in place`() {
        val list = ListImpl.of(3, 1, 2)
        list.sortBy(Function { it })
        assertEquals(1, list.at(0))
        assertEquals(2, list.at(1))
        assertEquals(3, list.at(2))
    }

    @Test
    fun `sortBy returns same list`() {
        val list = ListImpl.of(3, 1, 2)
        val sorted = list.sortBy(Function { it })
        assertTrue(sorted === list)
    }

    @Test
    fun `sortBy with custom key extractor`() {
        val list = ListImpl.of("apple", "cat", "banana")
        list.sortBy(Function { it.length })
        assertEquals("cat", list.first())
        assertEquals("banana", list.last())
    }

    @Test
    fun `sortBy handles empty list`() {
        val list = ListImpl<Int>()
        list.sortBy(Function { it })
        assertTrue(list.isEmpty())
    }

    @Test
    fun `sortBy handles single element`() {
        val list = ListImpl.of(1)
        list.sortBy(Function { it })
        assertEquals(1, list.first())
    }

    // ==================== sortedBy() tests ====================
    @Test
    fun `sortedBy returns new sorted collection`() {
        val list = ListImpl.of(3, 1, 2)
        val sorted = list.sortedBy(Function { it })
        assertEquals(1, sorted.at(0))
        assertEquals(3, list.at(0)) // Original unchanged
    }

    @Test
    fun `sortedBy does not modify original`() {
        val list = ListImpl.of(3, 1, 2)
        list.sortedBy(Function { it })
        assertEquals(3, list.first())
    }

    @Test
    fun `sortedBy with custom key extractor`() {
        val list = ListImpl.of("apple", "cat", "banana")
        val sorted = list.sortedBy(Function { it.length })
        assertEquals("cat", sorted.first())
    }

    @Test
    fun `sortedBy handles empty list`() {
        val list = ListImpl<Int>()
        val sorted = list.sortedBy(Function { it })
        assertTrue(sorted.isEmpty())
    }

    @Test
    fun `sortedBy handles duplicates`() {
        val list = ListImpl.of(2, 1, 2, 1)
        val sorted = list.sortedBy(Function { it })
        assertEquals(4, sorted.size())
        assertEquals(1, sorted.at(0))
        assertEquals(1, sorted.at(1))
    }

    // ==================== excluding() tests ====================
    @Test
    fun `excluding removes first occurrence`() {
        val list = ListImpl.of(1, 2, 2, 3)
        val result = list.excluding(2)
        assertEquals(3, result.size())
        assertTrue(result.includes(2))
    }

    @Test
    fun `excluding returns new collection`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.excluding(2)
        assertTrue(list.includes(2))
    }

    @Test
    fun `excluding element not in list returns same elements`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.excluding(5)
        assertEquals(3, result.size())
    }

    @Test
    fun `excluding from empty list returns empty`() {
        val list = ListImpl<Int>()
        val result = list.excluding(1)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `excluding handles null`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        val result = list.excluding(null)
        assertEquals(1, result.size())
    }

    // ==================== excludingAll() tests ====================
    @Test
    fun `excludingAll removes all matching elements`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val toExclude = ListImpl.of(2, 4)
        val result = list.excludingAll(toExclude)
        assertEquals(3, result.size())
        assertFalse(result.includes(2))
        assertFalse(result.includes(4))
    }

    @Test
    fun `excludingAll returns new collection`() {
        val list = ListImpl.of(1, 2, 3)
        val toExclude = ListImpl.of(2)
        val result = list.excludingAll(toExclude)
        assertTrue(list.includes(2))
    }

    @Test
    fun `excludingAll with empty collection returns all elements`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.excludingAll(ListImpl())
        assertEquals(3, result.size())
    }

    @Test
    fun `excludingAll from empty list returns empty`() {
        val list = ListImpl<Int>()
        val result = list.excludingAll(ListImpl.of(1, 2))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `excludingAll removes all occurrences of matching elements`() {
        val list = ListImpl.of(1, 2, 2, 3, 3, 3)
        val toExclude = ListImpl.of(2, 3)
        val result = list.excludingAll(toExclude)
        assertEquals(1, result.size())
    }

    // ==================== including() tests ====================
    @Test
    fun `including adds element`() {
        val list = ListImpl.of(1, 2)
        val result = list.including(3)
        assertEquals(3, result.size())
        assertTrue(result.includes(3))
    }

    @Test
    fun `including returns new collection`() {
        val list = ListImpl.of(1, 2)
        val result = list.including(3)
        assertEquals(2, list.size())
    }

    @Test
    fun `including allows duplicates`() {
        val list = ListImpl.of(1, 2)
        val result = list.including(2)
        assertEquals(3, result.size())
    }

    @Test
    fun `including to empty list works`() {
        val list = ListImpl<Int>()
        val result = list.including(1)
        assertEquals(1, result.size())
    }

    @Test
    fun `including handles null`() {
        val list = ListImpl<String?>()
        list.add("a")
        val result = list.including(null)
        assertEquals(2, result.size())
    }

    // ==================== includingAll() tests ====================
    @Test
    fun `includingAll adds all elements`() {
        val list = ListImpl.of(1, 2)
        val toAdd = ListImpl.of(3, 4)
        val result = list.includingAll(toAdd)
        assertEquals(4, result.size())
    }

    @Test
    fun `includingAll returns new collection`() {
        val list = ListImpl.of(1, 2)
        val toAdd = ListImpl.of(3, 4)
        val result = list.includingAll(toAdd)
        assertEquals(2, list.size())
    }

    @Test
    fun `includingAll with empty collection returns same elements`() {
        val list = ListImpl.of(1, 2)
        val result = list.includingAll(ListImpl())
        assertEquals(2, result.size())
    }

    @Test
    fun `includingAll to empty list works`() {
        val list = ListImpl<Int>()
        val result = list.includingAll(ListImpl.of(1, 2))
        assertEquals(2, result.size())
    }

    @Test
    fun `includingAll allows duplicates`() {
        val list = ListImpl.of(1, 2)
        val toAdd = ListImpl.of(1, 2)
        val result = list.includingAll(toAdd)
        assertEquals(4, result.size())
    }

    // ==================== random() tests ====================
    @Test
    fun `random returns element from list`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val random = list.random()
        assertTrue(list.includes(random))
    }

    @Test
    fun `random returns only element from single-element list`() {
        val list = ListImpl.of(42)
        assertEquals(42, list.random())
    }

    @Test
    fun `random throws for empty list`() {
        val list = ListImpl<Int>()
        assertThrows<NoSuchElementException> { list.random() }
    }

    @Test
    fun `random can return any element`() {
        val list = ListImpl.of(1, 2, 3)
        val results = mutableSetOf<Int>()
        repeat(100) {
            results.add(list.random())
        }
        // With 100 tries, we should get at least 2 different results
        assertTrue(results.size >= 2)
    }

    @Test
    fun `random works with null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        assertNull(list.random())
    }

    // ==================== sum() tests ====================
    @Test
    fun `sum returns 0 for empty list`() {
        val list = ListImpl<Int>()
        assertEquals(0.0, list.sum())
    }

    @Test
    fun `sum returns sum of integers`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertEquals(15.0, list.sum())
    }

    @Test
    fun `sum returns sum of doubles`() {
        val list = ListImpl.of(1.5, 2.5, 3.0)
        assertEquals(7.0, list.sum())
    }

    @Test
    fun `sum handles mixed numeric types`() {
        val list = ListImpl<Number>()
        list.add(1)
        list.add(2.5)
        list.add(3L)
        assertEquals(6.5, list.sum())
    }

    @Test
    fun `sum throws for non-numeric elements`() {
        val list = ListImpl.of("a", "b")
        assertThrows<IllegalArgumentException> { list.sum() }
    }

    // ==================== concat() tests ====================
    @Test
    fun `concat without separator joins elements`() {
        val list = ListImpl.of("a", "b", "c")
        assertEquals("abc", list.concat())
    }

    @Test
    fun `concat with separator joins elements`() {
        val list = ListImpl.of("a", "b", "c")
        assertEquals("a, b, c", list.concat(", "))
    }

    @Test
    fun `concat returns empty string for empty list`() {
        val list = ListImpl<String>()
        assertEquals("", list.concat())
    }

    @Test
    fun `concat handles null elements`() {
        val list = ListImpl<String?>()
        list.add("a")
        list.add(null)
        list.add("b")
        assertEquals("a, null, b", list.concat(", "))
    }

    @Test
    fun `concat converts numbers to strings`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals("1-2-3", list.concat("-"))
    }

    // ==================== flatten() tests ====================
    @Test
    fun `flatten flattens nested collections`() {
        val inner1 = ListImpl.of(1, 2)
        val inner2 = ListImpl.of(3, 4)
        val outer = ListImpl<Any>()
        outer.add(inner1)
        outer.add(inner2)
        val flattened = outer.flatten()
        assertEquals(4, flattened.size())
    }

    @Test
    fun `flatten returns same for flat collection`() {
        val list = ListImpl.of(1, 2, 3)
        val flattened = list.flatten()
        assertEquals(3, flattened.size())
    }

    @Test
    fun `flatten handles deeply nested collections`() {
        val inner = ListImpl.of(1)
        val middle = ListImpl<Any>()
        middle.add(inner)
        val outer = ListImpl<Any>()
        outer.add(middle)
        val flattened = outer.flatten()
        assertEquals(1, flattened.size())
        assertEquals(1, flattened.first())
    }

    @Test
    fun `flatten returns empty for empty list`() {
        val list = ListImpl<Any>()
        val flattened = list.flatten()
        assertTrue(flattened.isEmpty())
    }

    @Test
    fun `flatten handles mixed elements and collections`() {
        val inner = ListImpl.of(2, 3)
        val outer = ListImpl<Any>()
        outer.add(1)
        outer.add(inner)
        outer.add(4)
        val flattened = outer.flatten()
        assertEquals(4, flattened.size())
    }

    // ==================== asBag() tests ====================
    @Test
    fun `asBag converts to bag`() {
        val list = ListImpl.of(1, 2, 2, 3)
        val bag = list.asBag()
        assertNotNull(bag)
        assertEquals(4, bag.size())
    }

    @Test
    fun `asBag preserves duplicates`() {
        val list = ListImpl.of(1, 1, 1)
        val bag = list.asBag()
        assertEquals(3, bag.count(1))
    }

    @Test
    fun `asBag handles empty list`() {
        val list = ListImpl<Int>()
        val bag = list.asBag()
        assertTrue(bag.isEmpty())
    }

    @Test
    fun `asBag creates new collection`() {
        val list = ListImpl.of(1, 2)
        val bag = list.asBag()
        list.add(3)
        assertEquals(2, bag.size())
    }

    @Test
    fun `asBag preserves null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        val bag = list.asBag()
        assertTrue(bag.includes(null))
    }

    // ==================== asOrderedSet() tests ====================
    @Test
    fun `asOrderedSet removes duplicates`() {
        val list = ListImpl.of(1, 2, 2, 3)
        val orderedSet = list.asOrderedSet()
        assertEquals(3, orderedSet.size())
    }

    @Test
    fun `asOrderedSet preserves order`() {
        val list = ListImpl.of(3, 1, 2)
        val orderedSet = list.asOrderedSet()
        assertEquals(3, orderedSet.first())
        assertEquals(2, orderedSet.last())
    }

    @Test
    fun `asOrderedSet handles empty list`() {
        val list = ListImpl<Int>()
        val orderedSet = list.asOrderedSet()
        assertTrue(orderedSet.isEmpty())
    }

    @Test
    fun `asOrderedSet creates new collection`() {
        val list = ListImpl.of(1, 2)
        val orderedSet = list.asOrderedSet()
        list.add(3)
        assertEquals(2, orderedSet.size())
    }

    @Test
    fun `asOrderedSet handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add(null)
        val orderedSet = list.asOrderedSet()
        assertEquals(1, orderedSet.size())
    }

    // ==================== asList() tests ====================
    @Test
    fun `asList creates new list`() {
        val list = ListImpl.of(1, 2, 3)
        val newList = list.asList()
        assertEquals(3, newList.size())
    }

    @Test
    fun `asList preserves order`() {
        val list = ListImpl.of(3, 1, 2)
        val newList = list.asList()
        assertEquals(3, newList.first())
    }

    @Test
    fun `asList preserves duplicates`() {
        val list = ListImpl.of(1, 1, 1)
        val newList = list.asList()
        assertEquals(3, newList.size())
    }

    @Test
    fun `asList handles empty list`() {
        val list = ListImpl<Int>()
        val newList = list.asList()
        assertTrue(newList.isEmpty())
    }

    @Test
    fun `asList creates independent copy`() {
        val list = ListImpl.of(1, 2)
        val newList = list.asList()
        list.add(3)
        assertEquals(2, newList.size())
    }

    // ==================== asSet() tests ====================
    @Test
    fun `asSet removes duplicates`() {
        val list = ListImpl.of(1, 2, 2, 3)
        val set = list.asSet()
        assertEquals(3, set.size())
    }

    @Test
    fun `asSet handles empty list`() {
        val list = ListImpl<Int>()
        val set = list.asSet()
        assertTrue(set.isEmpty())
    }

    @Test
    fun `asSet creates new collection`() {
        val list = ListImpl.of(1, 2)
        val set = list.asSet()
        list.add(3)
        assertEquals(2, set.size())
    }

    @Test
    fun `asSet preserves all unique elements`() {
        val list = ListImpl.of(1, 2, 3)
        val set = list.asSet()
        assertTrue(set.includes(1))
        assertTrue(set.includes(2))
        assertTrue(set.includes(3))
    }

    @Test
    fun `asSet handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add(null)
        val set = list.asSet()
        assertEquals(1, set.size())
    }

    // ==================== clone() tests ====================
    @Test
    fun `clone creates copy with same elements`() {
        val list = ListImpl.of(1, 2, 3)
        val cloned = list.clone()
        assertEquals(3, cloned.size())
    }

    @Test
    fun `clone creates independent copy`() {
        val list = ListImpl.of(1, 2, 3)
        val cloned = list.clone()
        list.add(4)
        assertEquals(3, cloned.size())
    }

    @Test
    fun `clone preserves order`() {
        val list = ListImpl.of(3, 1, 2)
        val cloned = list.clone()
        assertEquals(3, cloned.first())
    }

    @Test
    fun `clone handles empty list`() {
        val list = ListImpl<Int>()
        val cloned = list.clone()
        assertTrue(cloned.isEmpty())
    }

    @Test
    fun `clone preserves duplicates`() {
        val list = ListImpl.of(1, 1, 2, 2)
        val cloned = list.clone()
        assertEquals(4, cloned.size())
    }

    // ==================== atLeastNMatch() tests ====================
    @Test
    fun `atLeastNMatch returns true when n elements match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.atLeastNMatch(Predicate { it > 2 }, 3))
    }

    @Test
    fun `atLeastNMatch returns true when more than n elements match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.atLeastNMatch(Predicate { it > 2 }, 2))
    }

    @Test
    fun `atLeastNMatch returns false when fewer than n match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertFalse(list.atLeastNMatch(Predicate { it > 4 }, 3))
    }

    @Test
    fun `atLeastNMatch with n equals 0 always returns true`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.atLeastNMatch(Predicate { false }, 0))
    }

    @Test
    fun `atLeastNMatch on empty list returns false for n greater than 0`() {
        val list = ListImpl<Int>()
        assertFalse(list.atLeastNMatch(Predicate { true }, 1))
    }

    // ==================== atMostNMatch() tests ====================
    @Test
    fun `atMostNMatch returns true when n elements match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.atMostNMatch(Predicate { it > 3 }, 2))
    }

    @Test
    fun `atMostNMatch returns true when fewer than n match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.atMostNMatch(Predicate { it > 4 }, 2))
    }

    @Test
    fun `atMostNMatch returns false when more than n match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertFalse(list.atMostNMatch(Predicate { it > 2 }, 2))
    }

    @Test
    fun `atMostNMatch with n equals 0 returns true when none match`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.atMostNMatch(Predicate { it > 10 }, 0))
    }

    @Test
    fun `atMostNMatch on empty list always returns true`() {
        val list = ListImpl<Int>()
        assertTrue(list.atMostNMatch(Predicate { true }, 0))
    }

    // ==================== aggregate() tests ====================
    @Test
    fun `aggregate groups by key`() {
        val list = ListImpl.of("apple", "banana", "apricot", "cherry")
        val grouped = list.aggregate(Function { it[0].toString() })
        assertEquals(3, grouped.size())
    }

    @Test
    fun `aggregate returns correct groups`() {
        val list = ListImpl.of(1, 2, 3, 4, 5, 6)
        val grouped = list.aggregate(Function { it % 2 })
        assertEquals(2, grouped.size())
        assertEquals(3, grouped.get(0)?.size())
        assertEquals(3, grouped.get(1)?.size())
    }

    @Test
    fun `aggregate on empty list returns empty map`() {
        val list = ListImpl<Int>()
        val grouped = list.aggregate(Function { it })
        assertTrue(grouped.isEmpty())
    }

    @Test
    fun `aggregate handles null keys`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        val grouped = list.aggregate(Function { it })
        assertEquals(2, grouped.size())
    }

    @Test
    fun `aggregate preserves all elements`() {
        val list = ListImpl.of(1, 2, 3)
        val grouped = list.aggregate(Function { "all" })
        assertEquals(3, grouped.get("all")?.size())
    }

    // ==================== map() tests ====================
    @Test
    fun `map transforms elements`() {
        val list = ListImpl.of(1, 2, 3)
        val mapped = list.map(Function { it * 2 })
        assertEquals(2, mapped.first())
        assertEquals(6, mapped.last())
    }

    @Test
    fun `map preserves order`() {
        val list = ListImpl.of("a", "bb", "ccc")
        val mapped = list.map(Function { it.length })
        assertEquals(1, mapped.first())
        assertEquals(3, mapped.last())
    }

    @Test
    fun `map on empty list returns empty`() {
        val list = ListImpl<Int>()
        val mapped = list.map(Function { it * 2 })
        assertTrue(mapped.isEmpty())
    }

    @Test
    fun `map preserves duplicates`() {
        val list = ListImpl.of(1, 1, 2)
        val mapped = list.map(Function { it * 2 })
        assertEquals(3, mapped.size())
    }

    @Test
    fun `map can change element type`() {
        val list = ListImpl.of(1, 2, 3)
        val mapped = list.map(Function { it.toString() })
        assertEquals("1", mapped.first())
    }

    // ==================== exists() tests ====================
    @Test
    fun `exists returns true when element matches`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.exists(Predicate { it > 3 }))
    }

    @Test
    fun `exists returns false when no element matches`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.exists(Predicate { it > 10 }))
    }

    @Test
    fun `exists returns false for empty list`() {
        val list = ListImpl<Int>()
        assertFalse(list.exists(Predicate { true }))
    }

    @Test
    fun `exists short-circuits on first match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        var count = 0
        list.exists(Predicate { count++; it == 2 })
        assertEquals(2, count) // Should stop after finding 2
    }

    @Test
    fun `exists handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        assertTrue(list.exists(Predicate { it == null }))
    }

    // ==================== forAll() tests ====================
    @Test
    fun `forAll returns true when all match`() {
        val list = ListImpl.of(2, 4, 6, 8)
        assertTrue(list.forAll(Predicate { it % 2 == 0 }))
    }

    @Test
    fun `forAll returns false when one doesnt match`() {
        val list = ListImpl.of(2, 4, 5, 8)
        assertFalse(list.forAll(Predicate { it % 2 == 0 }))
    }

    @Test
    fun `forAll returns true for empty list`() {
        val list = ListImpl<Int>()
        assertTrue(list.forAll(Predicate { false }))
    }

    @Test
    fun `forAll short-circuits on first non-match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        var count = 0
        list.forAll(Predicate { count++; it < 3 })
        assertEquals(3, count) // Should stop at 3
    }

    @Test
    fun `forAll handles single element`() {
        val list = ListImpl.of(5)
        assertTrue(list.forAll(Predicate { it == 5 }))
    }

    // ==================== associate() tests ====================
    @Test
    fun `associate creates map from elements to values`() {
        val list = ListImpl.of("a", "bb", "ccc")
        val map = list.associate(Function { it.length })
        assertEquals(1, map.get("a"))
        assertEquals(3, map.get("ccc"))
    }

    @Test
    fun `associate on empty list returns empty map`() {
        val list = ListImpl<String>()
        val map = list.associate(Function { it.length })
        assertTrue(map.isEmpty())
    }

    @Test
    fun `associate handles duplicate keys by keeping last`() {
        val list = ListImpl.of("a", "b", "c")
        val map = list.associate(Function { it.uppercase() })
        assertEquals(3, map.size())
    }

    @Test
    fun `associate with null values`() {
        val list = ListImpl.of(1, 2)
        val map = list.associate(Function { if (it == 1) null else "value" })
        assertNull(map.get(1))
    }

    @Test
    fun `associate preserves element as key`() {
        val list = ListImpl.of(1, 2, 3)
        val map = list.associate(Function { it * 10 })
        assertEquals(10, map.get(1))
        assertEquals(30, map.get(3))
    }

    // ==================== nMatch() tests ====================
    @Test
    fun `nMatch returns true when exactly n match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.nMatch(Predicate { it > 3 }, 2))
    }

    @Test
    fun `nMatch returns false when more than n match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertFalse(list.nMatch(Predicate { it > 2 }, 2))
    }

    @Test
    fun `nMatch returns false when fewer than n match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertFalse(list.nMatch(Predicate { it > 4 }, 2))
    }

    @Test
    fun `nMatch with n equals 0 returns true when none match`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.nMatch(Predicate { it > 10 }, 0))
    }

    @Test
    fun `nMatch on empty list returns true for n equals 0`() {
        val list = ListImpl<Int>()
        assertTrue(list.nMatch(Predicate { true }, 0))
    }

    // ==================== none() tests ====================
    @Test
    fun `none returns true when no elements match`() {
        val list = ListImpl.of(1, 2, 3)
        assertTrue(list.none(Predicate { it > 10 }))
    }

    @Test
    fun `none returns false when some elements match`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.none(Predicate { it > 2 }))
    }

    @Test
    fun `none returns true for empty list`() {
        val list = ListImpl<Int>()
        assertTrue(list.none(Predicate { true }))
    }

    @Test
    fun `none short-circuits on first match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        var count = 0
        list.none(Predicate { count++; it == 2 })
        assertEquals(2, count)
    }

    @Test
    fun `none returns false when all match`() {
        val list = ListImpl.of(2, 4, 6)
        assertFalse(list.none(Predicate { it % 2 == 0 }))
    }

    // ==================== one() tests ====================
    @Test
    fun `one returns true when exactly one matches`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertTrue(list.one(Predicate { it == 3 }))
    }

    @Test
    fun `one returns false when none match`() {
        val list = ListImpl.of(1, 2, 3)
        assertFalse(list.one(Predicate { it > 10 }))
    }

    @Test
    fun `one returns false when multiple match`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertFalse(list.one(Predicate { it > 3 }))
    }

    @Test
    fun `one returns false for empty list`() {
        val list = ListImpl<Int>()
        assertFalse(list.one(Predicate { true }))
    }

    @Test
    fun `one short-circuits on second match`() {
        val list = ListImpl.of(1, 2, 3, 2, 5)
        var count = 0
        list.one(Predicate { count++; it == 2 })
        assertEquals(4, count) // Should stop at second 2
    }

    // ==================== reject() tests ====================
    @Test
    fun `reject removes matching elements`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val result = list.reject(Predicate { it > 3 })
        assertEquals(3, result.size())
    }

    @Test
    fun `reject returns new collection`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.reject(Predicate { it > 2 })
        assertEquals(3, list.size())
    }

    @Test
    fun `reject returns empty when all match`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.reject(Predicate { it > 0 })
        assertTrue(result.isEmpty())
    }

    @Test
    fun `reject returns all when none match`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.reject(Predicate { it > 10 })
        assertEquals(3, result.size())
    }

    @Test
    fun `reject on empty list returns empty`() {
        val list = ListImpl<Int>()
        val result = list.reject(Predicate { true })
        assertTrue(result.isEmpty())
    }

    // ==================== rejectOne() tests ====================
    @Test
    fun `rejectOne removes first matching element`() {
        val list = ListImpl.of(1, 2, 3, 2, 5)
        val result = list.rejectOne(Predicate { it == 2 })
        assertEquals(4, result.size())
        assertTrue(result.includes(2))
    }

    @Test
    fun `rejectOne returns new collection`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.rejectOne(Predicate { it == 2 })
        assertEquals(3, list.size())
    }

    @Test
    fun `rejectOne returns all when none match`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.rejectOne(Predicate { it > 10 })
        assertEquals(3, result.size())
    }

    @Test
    fun `rejectOne on empty list returns empty`() {
        val list = ListImpl<Int>()
        val result = list.rejectOne(Predicate { true })
        assertTrue(result.isEmpty())
    }

    @Test
    fun `rejectOne only removes one occurrence`() {
        val list = ListImpl.of(2, 2, 2)
        val result = list.rejectOne(Predicate { it == 2 })
        assertEquals(2, result.size())
    }

    // ==================== filter() tests ====================
    @Test
    fun `filter keeps matching elements`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        val result = list.filter(Predicate { it > 3 })
        assertEquals(2, result.size())
    }

    @Test
    fun `filter returns new collection`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.filter(Predicate { it > 1 })
        assertEquals(3, list.size())
    }

    @Test
    fun `filter returns all when all match`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.filter(Predicate { it > 0 })
        assertEquals(3, result.size())
    }

    @Test
    fun `filter returns empty when none match`() {
        val list = ListImpl.of(1, 2, 3)
        val result = list.filter(Predicate { it > 10 })
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter on empty list returns empty`() {
        val list = ListImpl<Int>()
        val result = list.filter(Predicate { true })
        assertTrue(result.isEmpty())
    }

    // ==================== find() tests ====================
    @Test
    fun `find returns first matching element`() {
        val list = ListImpl.of(1, 2, 3, 4, 5)
        assertEquals(3, list.find(Predicate { it > 2 }))
    }

    @Test
    fun `find returns null when no match`() {
        val list = ListImpl.of(1, 2, 3)
        assertNull(list.find(Predicate { it > 10 }))
    }

    @Test
    fun `find returns null for empty list`() {
        val list = ListImpl<Int>()
        assertNull(list.find(Predicate { true }))
    }

    @Test
    fun `find returns first element when all match`() {
        val list = ListImpl.of(1, 2, 3)
        assertEquals(1, list.find(Predicate { it > 0 }))
    }

    @Test
    fun `find can find null element`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        assertNull(list.find(Predicate { it == null }))
    }

    // ==================== iterator tests ====================
    @Test
    fun `iterator iterates in order`() {
        val list = ListImpl.of(1, 2, 3)
        val result = mutableListOf<Int>()
        for (element in list) {
            result.add(element)
        }
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `iterator on empty list produces no elements`() {
        val list = ListImpl<Int>()
        var count = 0
        for (element in list) {
            count++
        }
        assertEquals(0, count)
    }

    @Test
    fun `iterator handles duplicates`() {
        val list = ListImpl.of(1, 1, 2, 2)
        val result = mutableListOf<Int>()
        for (element in list) {
            result.add(element)
        }
        assertEquals(4, result.size)
    }

    @Test
    fun `iterator handles null elements`() {
        val list = ListImpl<String?>()
        list.add(null)
        list.add("a")
        var nullCount = 0
        for (element in list) {
            if (element == null) nullCount++
        }
        assertEquals(1, nullCount)
    }

    @Test
    fun `iterator reflects current state`() {
        val list = ListImpl.of(1, 2, 3)
        list.add(4)
        var count = 0
        for (element in list) {
            count++
        }
        assertEquals(4, count)
    }
}
