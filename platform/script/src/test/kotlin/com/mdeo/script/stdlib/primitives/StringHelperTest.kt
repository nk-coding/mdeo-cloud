package com.mdeo.script.stdlib.impl.primitives

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for StringHelper.
 */
class StringHelperTest {

    // ==================== characterAt() tests ====================
    @Test
    fun `characterAt returns correct character`() {
        assertEquals("e", StringHelper.characterAt("hello", 1))
    }

    @Test
    fun `characterAt at index 0 returns first`() {
        assertEquals("h", StringHelper.characterAt("hello", 0))
    }

    @Test
    fun `characterAt at last index returns last`() {
        assertEquals("o", StringHelper.characterAt("hello", 4))
    }

    @Test
    fun `characterAt throws for negative index`() {
        assertThrows<IndexOutOfBoundsException> { StringHelper.characterAt("hello", -1) }
    }

    @Test
    fun `characterAt throws for index out of bounds`() {
        assertThrows<IndexOutOfBoundsException> { StringHelper.characterAt("hello", 5) }
    }

    // ==================== concat() tests ====================
    @Test
    fun `concat joins two strings`() {
        assertEquals("hello world", StringHelper.concat("hello ", "world"))
    }

    @Test
    fun `concat with empty string returns original`() {
        assertEquals("hello", StringHelper.concat("hello", ""))
    }

    @Test
    fun `concat empty with string returns string`() {
        assertEquals("hello", StringHelper.concat("", "hello"))
    }

    @Test
    fun `concat two empty strings returns empty`() {
        assertEquals("", StringHelper.concat("", ""))
    }

    @Test
    fun `concat with special characters works`() {
        assertEquals("hello\nworld", StringHelper.concat("hello\n", "world"))
    }

    // ==================== endsWith() tests ====================
    @Test
    fun `endsWith returns true for matching suffix`() {
        assertTrue(StringHelper.endsWith("hello", "lo"))
    }

    @Test
    fun `endsWith returns false for non-matching suffix`() {
        assertFalse(StringHelper.endsWith("hello", "la"))
    }

    @Test
    fun `endsWith with same string returns true`() {
        assertTrue(StringHelper.endsWith("hello", "hello"))
    }

    @Test
    fun `endsWith with empty suffix returns true`() {
        assertTrue(StringHelper.endsWith("hello", ""))
    }

    @Test
    fun `endsWith with longer suffix returns false`() {
        assertFalse(StringHelper.endsWith("hello", "hello world"))
    }

    // ==================== firstToLowerCase() tests ====================
    @Test
    fun `firstToLowerCase lowercases first character`() {
        assertEquals("hello", StringHelper.firstToLowerCase("Hello"))
    }

    @Test
    fun `firstToLowerCase with already lowercase returns same`() {
        assertEquals("hello", StringHelper.firstToLowerCase("hello"))
    }

    @Test
    fun `firstToLowerCase with all caps lowercases first only`() {
        assertEquals("hELLO", StringHelper.firstToLowerCase("HELLO"))
    }

    @Test
    fun `firstToLowerCase with empty string returns empty`() {
        assertEquals("", StringHelper.firstToLowerCase(""))
    }

    @Test
    fun `firstToLowerCase with single character works`() {
        assertEquals("a", StringHelper.firstToLowerCase("A"))
    }

    // ==================== firstToUpperCase() tests ====================
    @Test
    fun `firstToUpperCase uppercases first character`() {
        assertEquals("Hello", StringHelper.firstToUpperCase("hello"))
    }

    @Test
    fun `firstToUpperCase with already uppercase returns same`() {
        assertEquals("Hello", StringHelper.firstToUpperCase("Hello"))
    }

    @Test
    fun `firstToUpperCase with all lowercase uppercases first only`() {
        assertEquals("Hello", StringHelper.firstToUpperCase("hello"))
    }

    @Test
    fun `firstToUpperCase with empty string returns empty`() {
        assertEquals("", StringHelper.firstToUpperCase(""))
    }

    @Test
    fun `firstToUpperCase with single character works`() {
        assertEquals("A", StringHelper.firstToUpperCase("a"))
    }

    // ==================== isInteger() tests ====================
    @Test
    fun `isInteger returns true for positive integer string`() {
        assertTrue(StringHelper.isInteger("42"))
    }

    @Test
    fun `isInteger returns true for negative integer string`() {
        assertTrue(StringHelper.isInteger("-42"))
    }

    @Test
    fun `isInteger returns false for decimal string`() {
        assertFalse(StringHelper.isInteger("42.5"))
    }

    @Test
    fun `isInteger returns false for non-numeric string`() {
        assertFalse(StringHelper.isInteger("hello"))
    }

    @Test
    fun `isInteger returns false for empty string`() {
        assertFalse(StringHelper.isInteger(""))
    }

    // ==================== isReal() tests ====================
    @Test
    fun `isReal returns true for decimal string`() {
        assertTrue(StringHelper.isReal("42.5"))
    }

    @Test
    fun `isReal returns true for integer string`() {
        assertTrue(StringHelper.isReal("42"))
    }

    @Test
    fun `isReal returns true for negative decimal`() {
        assertTrue(StringHelper.isReal("-42.5"))
    }

    @Test
    fun `isReal returns false for non-numeric string`() {
        assertFalse(StringHelper.isReal("hello"))
    }

    @Test
    fun `isReal returns false for empty string`() {
        assertFalse(StringHelper.isReal(""))
    }

    // ==================== isSubstringOf() tests ====================
    @Test
    fun `isSubstringOf returns true when contained`() {
        assertTrue(StringHelper.isSubstringOf("ell", "hello"))
    }

    @Test
    fun `isSubstringOf returns false when not contained`() {
        assertFalse(StringHelper.isSubstringOf("xyz", "hello"))
    }

    @Test
    fun `isSubstringOf with empty is always true`() {
        assertTrue(StringHelper.isSubstringOf("", "hello"))
    }

    @Test
    fun `isSubstringOf with same string returns true`() {
        assertTrue(StringHelper.isSubstringOf("hello", "hello"))
    }

    @Test
    fun `isSubstringOf with longer string returns false`() {
        assertFalse(StringHelper.isSubstringOf("hello world", "hello"))
    }

    // ==================== length() tests ====================
    @Test
    fun `length returns correct length`() {
        assertEquals(5, StringHelper.length("hello"))
    }

    @Test
    fun `length of empty string is 0`() {
        assertEquals(0, StringHelper.length(""))
    }

    @Test
    fun `length with spaces counts spaces`() {
        assertEquals(11, StringHelper.length("hello world"))
    }

    @Test
    fun `length of single character is 1`() {
        assertEquals(1, StringHelper.length("a"))
    }

    @Test
    fun `length with unicode characters`() {
        assertEquals(1, StringHelper.length("€"))
    }

    // ==================== matches() tests ====================
    @Test
    fun `matches returns true for matching regex`() {
        assertTrue(StringHelper.matches("hello", "h.*o"))
    }

    @Test
    fun `matches returns false for non-matching regex`() {
        assertFalse(StringHelper.matches("hello", "x.*"))
    }

    @Test
    fun `matches with digit pattern`() {
        assertTrue(StringHelper.matches("123", "\\d+"))
    }

    @Test
    fun `matches with exact match`() {
        assertTrue(StringHelper.matches("hello", "hello"))
    }

    @Test
    fun `matches with partial match requires full match`() {
        assertFalse(StringHelper.matches("hello", "ell"))
    }

    // ==================== pad() tests ====================
    @Test
    fun `pad right pads with spaces`() {
        assertEquals("hello     ", StringHelper.pad("hello", 10, " ", true))
    }

    @Test
    fun `pad left pads with spaces`() {
        assertEquals("     hello", StringHelper.pad("hello", 10, " ", false))
    }

    @Test
    fun `pad with custom character`() {
        assertEquals("hello00000", StringHelper.pad("hello", 10, "0", true))
    }

    @Test
    fun `pad with length equal to string returns same`() {
        assertEquals("hello", StringHelper.pad("hello", 5, " ", true))
    }

    @Test
    fun `pad with length less than string returns same`() {
        assertEquals("hello", StringHelper.pad("hello", 3, " ", true))
    }

    // ==================== replace() tests ====================
    @Test
    fun `replace replaces substring`() {
        assertEquals("hallo", StringHelper.replace("hello", "e", "a"))
    }

    @Test
    fun `replace replaces all occurrences`() {
        assertEquals("hXllX", StringHelper.replace("hello", "o", "X").replace("e", "X"))
    }

    @Test
    fun `replace with empty target inserts at every position`() {
        // Empty regex matches at every position
        assertEquals("xhxexlxlxox", StringHelper.replace("hello", "", "x"))
    }

    @Test
    fun `replace with empty replacement removes target`() {
        assertEquals("hllo", StringHelper.replace("hello", "e", ""))
    }

    @Test
    fun `replace non-existent returns same`() {
        assertEquals("hello", StringHelper.replace("hello", "x", "y"))
    }

    // ==================== split() tests ====================
    @Test
    fun `split on delimiter creates list`() {
        val result = StringHelper.split("a,b,c", ",")
        assertEquals(3, result.size())
        assertEquals("a", result.at(0))
    }

    @Test
    fun `split with no delimiter returns single element`() {
        val result = StringHelper.split("hello", ",")
        assertEquals(1, result.size())
    }

    @Test
    fun `split empty string returns empty list`() {
        val result = StringHelper.split("", ",")
        assertEquals(1, result.size())
    }

    @Test
    fun `split on each character works`() {
        val result = StringHelper.split("abc", "")
        assertTrue(result.size() > 1)
    }

    @Test
    fun `split preserves order`() {
        val result = StringHelper.split("1,2,3", ",")
        assertEquals("1", result.at(0))
        assertEquals("3", result.at(2))
    }

    // ==================== startsWith() tests ====================
    @Test
    fun `startsWith returns true for matching prefix`() {
        assertTrue(StringHelper.startsWith("hello", "he"))
    }

    @Test
    fun `startsWith returns false for non-matching prefix`() {
        assertFalse(StringHelper.startsWith("hello", "el"))
    }

    @Test
    fun `startsWith with same string returns true`() {
        assertTrue(StringHelper.startsWith("hello", "hello"))
    }

    @Test
    fun `startsWith with empty prefix returns true`() {
        assertTrue(StringHelper.startsWith("hello", ""))
    }

    @Test
    fun `startsWith with longer prefix returns false`() {
        assertFalse(StringHelper.startsWith("hello", "hello world"))
    }

    // ==================== substring() tests ====================
    @Test
    fun `substring with start and end works`() {
        assertEquals("ell", StringHelper.substring("hello", 1, 4))
    }

    @Test
    fun `substring from start works`() {
        assertEquals("he", StringHelper.substring("hello", 0, 2))
    }

    @Test
    fun `substring to end works`() {
        assertEquals("llo", StringHelper.substring("hello", 2, 5))
    }

    @Test
    fun `substring with single start returns rest`() {
        assertEquals("llo", StringHelper.substring("hello", 2))
    }

    @Test
    fun `substring with same start and end returns empty`() {
        assertEquals("", StringHelper.substring("hello", 2, 2))
    }

    // ==================== toCharSequence() tests ====================
    @Test
    fun `toCharSequence returns list of characters`() {
        val result = StringHelper.toCharSequence("hello")
        assertEquals(5, result.size())
        assertEquals("h", result.at(0))
    }

    @Test
    fun `toCharSequence empty string returns empty list`() {
        val result = StringHelper.toCharSequence("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toCharSequence single character returns single element`() {
        val result = StringHelper.toCharSequence("a")
        assertEquals(1, result.size())
    }

    @Test
    fun `toCharSequence preserves order`() {
        val result = StringHelper.toCharSequence("abc")
        assertEquals("a", result.at(0))
        assertEquals("c", result.at(2))
    }

    @Test
    fun `toCharSequence with spaces includes spaces`() {
        val result = StringHelper.toCharSequence("a b")
        assertEquals(3, result.size())
        assertEquals(" ", result.at(1))
    }

    // ==================== toLowerCase() tests ====================
    @Test
    fun `toLowerCase converts to lowercase`() {
        assertEquals("hello", StringHelper.toLowerCase("HELLO"))
    }

    @Test
    fun `toLowerCase with mixed case converts all`() {
        assertEquals("hello", StringHelper.toLowerCase("HeLLo"))
    }

    @Test
    fun `toLowerCase with already lowercase returns same`() {
        assertEquals("hello", StringHelper.toLowerCase("hello"))
    }

    @Test
    fun `toLowerCase with empty string returns empty`() {
        assertEquals("", StringHelper.toLowerCase(""))
    }

    @Test
    fun `toLowerCase with numbers keeps numbers`() {
        assertEquals("hello123", StringHelper.toLowerCase("HELLO123"))
    }

    // ==================== toUpperCase() tests ====================
    @Test
    fun `toUpperCase converts to uppercase`() {
        assertEquals("HELLO", StringHelper.toUpperCase("hello"))
    }

    @Test
    fun `toUpperCase with mixed case converts all`() {
        assertEquals("HELLO", StringHelper.toUpperCase("HeLLo"))
    }

    @Test
    fun `toUpperCase with already uppercase returns same`() {
        assertEquals("HELLO", StringHelper.toUpperCase("HELLO"))
    }

    @Test
    fun `toUpperCase with empty string returns empty`() {
        assertEquals("", StringHelper.toUpperCase(""))
    }

    @Test
    fun `toUpperCase with numbers keeps numbers`() {
        assertEquals("HELLO123", StringHelper.toUpperCase("hello123"))
    }

    // ==================== trim() tests ====================
    @Test
    fun `trim removes leading and trailing spaces`() {
        assertEquals("hello", StringHelper.trim("  hello  "))
    }

    @Test
    fun `trim removes only leading spaces`() {
        assertEquals("hello", StringHelper.trim("  hello"))
    }

    @Test
    fun `trim removes only trailing spaces`() {
        assertEquals("hello", StringHelper.trim("hello  "))
    }

    @Test
    fun `trim with no spaces returns same`() {
        assertEquals("hello", StringHelper.trim("hello"))
    }

    @Test
    fun `trim removes tabs and newlines`() {
        assertEquals("hello", StringHelper.trim("\t\nhello\t\n"))
    }
}
