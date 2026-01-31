package com.mdeo.script.stdlib.impl.primitives

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for BooleanHelper.
 */
class BooleanHelperTest {

    // ==================== toString() tests ====================
    @Test
    fun `toString converts true to true string`() {
        assertEquals("true", BooleanHelper.toString(true))
    }

    @Test
    fun `toString converts false to false string`() {
        assertEquals("false", BooleanHelper.toString(false))
    }

    @Test
    fun `toString result is lowercase`() {
        val result = BooleanHelper.toString(true)
        assertEquals(result, result.lowercase())
    }

    @Test
    fun `toString returns consistent result for true`() {
        assertEquals(BooleanHelper.toString(true), BooleanHelper.toString(true))
    }

    @Test
    fun `toString returns consistent result for false`() {
        assertEquals(BooleanHelper.toString(false), BooleanHelper.toString(false))
    }

    // ==================== asInteger() tests ====================
    @Test
    fun `asInteger converts true to 1`() {
        assertEquals(1, BooleanHelper.asInteger(true))
    }

    @Test
    fun `asInteger converts false to 0`() {
        assertEquals(0, BooleanHelper.asInteger(false))
    }

    @Test
    fun `asInteger true is not zero`() {
        assertTrue(BooleanHelper.asInteger(true) != 0)
    }

    @Test
    fun `asInteger false is zero`() {
        assertTrue(BooleanHelper.asInteger(false) == 0)
    }

    @Test
    fun `asInteger is consistent`() {
        assertEquals(BooleanHelper.asInteger(true), BooleanHelper.asInteger(true))
    }

    // ==================== asLong() tests ====================
    @Test
    fun `asLong converts true to 1L`() {
        assertEquals(1L, BooleanHelper.asLong(true))
    }

    @Test
    fun `asLong converts false to 0L`() {
        assertEquals(0L, BooleanHelper.asLong(false))
    }

    @Test
    fun `asLong true is not zero`() {
        assertTrue(BooleanHelper.asLong(true) != 0L)
    }

    @Test
    fun `asLong false is zero`() {
        assertTrue(BooleanHelper.asLong(false) == 0L)
    }

    @Test
    fun `asLong returns Long type`() {
        val result: Long = BooleanHelper.asLong(true)
        assertEquals(1L, result)
    }

    // ==================== asDouble() tests ====================
    @Test
    fun `asDouble converts true to 1_0`() {
        assertEquals(1.0, BooleanHelper.asDouble(true))
    }

    @Test
    fun `asDouble converts false to 0_0`() {
        assertEquals(0.0, BooleanHelper.asDouble(false))
    }

    @Test
    fun `asDouble true is not zero`() {
        assertTrue(BooleanHelper.asDouble(true) != 0.0)
    }

    @Test
    fun `asDouble false is zero`() {
        assertTrue(BooleanHelper.asDouble(false) == 0.0)
    }

    @Test
    fun `asDouble returns Double type`() {
        val result: Double = BooleanHelper.asDouble(true)
        assertEquals(1.0, result)
    }

    // ==================== asFloat() tests ====================
    @Test
    fun `asFloat converts true to 1_0f`() {
        assertEquals(1.0f, BooleanHelper.asFloat(true))
    }

    @Test
    fun `asFloat converts false to 0_0f`() {
        assertEquals(0.0f, BooleanHelper.asFloat(false))
    }

    @Test
    fun `asFloat true is not zero`() {
        assertTrue(BooleanHelper.asFloat(true) != 0.0f)
    }

    @Test
    fun `asFloat false is zero`() {
        assertTrue(BooleanHelper.asFloat(false) == 0.0f)
    }

    @Test
    fun `asFloat returns Float type`() {
        val result: Float = BooleanHelper.asFloat(true)
        assertEquals(1.0f, result)
    }

    // ==================== not() tests ====================
    @Test
    fun `not of true is false`() {
        assertFalse(BooleanHelper.not(true))
    }

    @Test
    fun `not of false is true`() {
        assertTrue(BooleanHelper.not(false))
    }

    @Test
    fun `not of not is identity`() {
        assertTrue(BooleanHelper.not(BooleanHelper.not(true)))
        assertFalse(BooleanHelper.not(BooleanHelper.not(false)))
    }

    @Test
    fun `not is consistent`() {
        assertEquals(BooleanHelper.not(true), BooleanHelper.not(true))
    }

    @Test
    fun `not double negation`() {
        val original = true
        assertEquals(original, BooleanHelper.not(BooleanHelper.not(original)))
    }

    // ==================== and() tests ====================
    @Test
    fun `and true true returns true`() {
        assertTrue(BooleanHelper.and(true, true))
    }

    @Test
    fun `and true false returns false`() {
        assertFalse(BooleanHelper.and(true, false))
    }

    @Test
    fun `and false true returns false`() {
        assertFalse(BooleanHelper.and(false, true))
    }

    @Test
    fun `and false false returns false`() {
        assertFalse(BooleanHelper.and(false, false))
    }

    @Test
    fun `and is commutative`() {
        assertEquals(BooleanHelper.and(true, false), BooleanHelper.and(false, true))
    }

    // ==================== or() tests ====================
    @Test
    fun `or true true returns true`() {
        assertTrue(BooleanHelper.or(true, true))
    }

    @Test
    fun `or true false returns true`() {
        assertTrue(BooleanHelper.or(true, false))
    }

    @Test
    fun `or false true returns true`() {
        assertTrue(BooleanHelper.or(false, true))
    }

    @Test
    fun `or false false returns false`() {
        assertFalse(BooleanHelper.or(false, false))
    }

    @Test
    fun `or is commutative`() {
        assertEquals(BooleanHelper.or(true, false), BooleanHelper.or(false, true))
    }

    // ==================== xor() tests ====================
    @Test
    fun `xor true true returns false`() {
        assertFalse(BooleanHelper.xor(true, true))
    }

    @Test
    fun `xor true false returns true`() {
        assertTrue(BooleanHelper.xor(true, false))
    }

    @Test
    fun `xor false true returns true`() {
        assertTrue(BooleanHelper.xor(false, true))
    }

    @Test
    fun `xor false false returns false`() {
        assertFalse(BooleanHelper.xor(false, false))
    }

    @Test
    fun `xor is commutative`() {
        assertEquals(BooleanHelper.xor(true, false), BooleanHelper.xor(false, true))
    }

    // ==================== implies() tests ====================
    @Test
    fun `implies true true returns true`() {
        assertTrue(BooleanHelper.implies(true, true))
    }

    @Test
    fun `implies true false returns false`() {
        assertFalse(BooleanHelper.implies(true, false))
    }

    @Test
    fun `implies false true returns true`() {
        assertTrue(BooleanHelper.implies(false, true))
    }

    @Test
    fun `implies false false returns true`() {
        assertTrue(BooleanHelper.implies(false, false))
    }

    @Test
    fun `implies with antecedent false always true`() {
        assertTrue(BooleanHelper.implies(false, true))
        assertTrue(BooleanHelper.implies(false, false))
    }
}
