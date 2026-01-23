package com.mdeo.script.stdlib.primitives

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for DoubleHelper.
 */
class DoubleHelperTest {

    // ==================== abs() tests ====================
    @Test
    fun `abs of positive number returns same`() {
        assertEquals(5.5, DoubleHelper.abs(5.5))
    }

    @Test
    fun `abs of negative number returns positive`() {
        assertEquals(5.5, DoubleHelper.abs(-5.5))
    }

    @Test
    fun `abs of zero returns zero`() {
        assertEquals(0.0, DoubleHelper.abs(0.0))
    }

    @Test
    fun `abs of negative zero returns zero`() {
        assertEquals(0.0, DoubleHelper.abs(-0.0))
    }

    @Test
    fun `abs of very small number works`() {
        assertEquals(0.0001, DoubleHelper.abs(-0.0001), 0.00001)
    }

    // ==================== ceiling() tests ====================
    @Test
    fun `ceiling of 5_3 is 6`() {
        assertEquals(6L, DoubleHelper.ceiling(5.3))
    }

    @Test
    fun `ceiling of 5_0 is 5`() {
        assertEquals(5L, DoubleHelper.ceiling(5.0))
    }

    @Test
    fun `ceiling of negative 5_7 is -5`() {
        assertEquals(-5L, DoubleHelper.ceiling(-5.7))
    }

    @Test
    fun `ceiling of 0_1 is 1`() {
        assertEquals(1L, DoubleHelper.ceiling(0.1))
    }

    @Test
    fun `ceiling of zero is zero`() {
        assertEquals(0L, DoubleHelper.ceiling(0.0))
    }

    // ==================== floor() tests ====================
    @Test
    fun `floor of 5_7 is 5`() {
        assertEquals(5L, DoubleHelper.floor(5.7))
    }

    @Test
    fun `floor of 5_0 is 5`() {
        assertEquals(5L, DoubleHelper.floor(5.0))
    }

    @Test
    fun `floor of negative 5_3 is -6`() {
        assertEquals(-6L, DoubleHelper.floor(-5.3))
    }

    @Test
    fun `floor of 0_9 is 0`() {
        assertEquals(0L, DoubleHelper.floor(0.9))
    }

    @Test
    fun `floor of zero is zero`() {
        assertEquals(0L, DoubleHelper.floor(0.0))
    }

    // ==================== log() tests ====================
    @Test
    fun `log of 1 returns 0`() {
        assertEquals(0.0, DoubleHelper.log(1.0), 0.0001)
    }

    @Test
    fun `log of e returns 1`() {
        assertEquals(1.0, DoubleHelper.log(Math.E), 0.0001)
    }

    @Test
    fun `log of 10 is approximately 2_3`() {
        val result = DoubleHelper.log(10.0)
        assertTrue(result > 2.0 && result < 2.5)
    }

    @Test
    fun `log increases with larger numbers`() {
        assertTrue(DoubleHelper.log(100.0) > DoubleHelper.log(10.0))
    }

    @Test
    fun `log of very small positive number is negative`() {
        assertTrue(DoubleHelper.log(0.001) < 0)
    }

    // ==================== log10() tests ====================
    @Test
    fun `log10 of 10 returns 1`() {
        assertEquals(1.0, DoubleHelper.log10(10.0), 0.0001)
    }

    @Test
    fun `log10 of 100 returns 2`() {
        assertEquals(2.0, DoubleHelper.log10(100.0), 0.0001)
    }

    @Test
    fun `log10 of 1 returns 0`() {
        assertEquals(0.0, DoubleHelper.log10(1.0), 0.0001)
    }

    @Test
    fun `log10 of 0_1 returns -1`() {
        assertEquals(-1.0, DoubleHelper.log10(0.1), 0.0001)
    }

    @Test
    fun `log10 increases with larger numbers`() {
        assertTrue(DoubleHelper.log10(1000.0) > DoubleHelper.log10(100.0))
    }

    // ==================== max() tests ====================
    @Test
    fun `max of two doubles returns larger`() {
        assertEquals(10.5, DoubleHelper.max(5.5, 10.5))
    }

    @Test
    fun `max with equal values returns value`() {
        assertEquals(5.5, DoubleHelper.max(5.5, 5.5))
    }

    @Test
    fun `max with negative returns larger`() {
        assertEquals(-5.5, DoubleHelper.max(-10.5, -5.5))
    }

    @Test
    fun `max with zero and positive returns positive`() {
        assertEquals(5.5, DoubleHelper.max(0.0, 5.5))
    }

    @Test
    fun `max with NaN returns NaN`() {
        assertTrue(DoubleHelper.max(Double.NaN, 5.0).isNaN())
    }

    // ==================== min() tests ====================
    @Test
    fun `min of two doubles returns smaller`() {
        assertEquals(5.5, DoubleHelper.min(5.5, 10.5))
    }

    @Test
    fun `min with equal values returns value`() {
        assertEquals(5.5, DoubleHelper.min(5.5, 5.5))
    }

    @Test
    fun `min with negative returns smaller`() {
        assertEquals(-10.5, DoubleHelper.min(-10.5, -5.5))
    }

    @Test
    fun `min with zero and negative returns negative`() {
        assertEquals(-5.5, DoubleHelper.min(0.0, -5.5))
    }

    @Test
    fun `min with NaN returns NaN`() {
        assertTrue(DoubleHelper.min(Double.NaN, 5.0).isNaN())
    }

    // ==================== pow() tests ====================
    @Test
    fun `pow of 2 to 3 is 8`() {
        assertEquals(8.0, DoubleHelper.pow(2.0, 3.0), 0.0001)
    }

    @Test
    fun `pow of any number to 0 is 1`() {
        assertEquals(1.0, DoubleHelper.pow(5.5, 0.0), 0.0001)
    }

    @Test
    fun `pow of any number to 1 is same`() {
        assertEquals(5.5, DoubleHelper.pow(5.5, 1.0), 0.0001)
    }

    @Test
    fun `pow of 0 to positive is 0`() {
        assertEquals(0.0, DoubleHelper.pow(0.0, 5.0), 0.0001)
    }

    @Test
    fun `pow of 2_5 to 2 is 6_25`() {
        assertEquals(6.25, DoubleHelper.pow(2.5, 2.0), 0.0001)
    }

    // ==================== round() tests ====================
    @Test
    fun `round of 5_4 is 5`() {
        assertEquals(5L, DoubleHelper.round(5.4))
    }

    @Test
    fun `round of 5_5 is 6`() {
        assertEquals(6L, DoubleHelper.round(5.5))
    }

    @Test
    fun `round of 5_6 is 6`() {
        assertEquals(6L, DoubleHelper.round(5.6))
    }

    @Test
    fun `round of negative 5_4 is -5`() {
        assertEquals(-5L, DoubleHelper.round(-5.4))
    }

    @Test
    fun `round of negative 5_5 is -6`() {
        // Kotlin round() rounds half away from zero
        assertEquals(-6L, DoubleHelper.round(-5.5))
    }

    // ==================== isNaN() tests ====================
    @Test
    fun `isNaN returns true for NaN`() {
        assertTrue(DoubleHelper.isNaN(Double.NaN))
    }

    @Test
    fun `isNaN returns false for normal number`() {
        assertFalse(DoubleHelper.isNaN(5.0))
    }

    @Test
    fun `isNaN returns false for zero`() {
        assertFalse(DoubleHelper.isNaN(0.0))
    }

    @Test
    fun `isNaN returns false for infinity`() {
        assertFalse(DoubleHelper.isNaN(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `isNaN returns false for negative infinity`() {
        assertFalse(DoubleHelper.isNaN(Double.NEGATIVE_INFINITY))
    }

    // ==================== isInfinite() tests ====================
    @Test
    fun `isInfinite returns true for positive infinity`() {
        assertTrue(DoubleHelper.isInfinite(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `isInfinite returns true for negative infinity`() {
        assertTrue(DoubleHelper.isInfinite(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `isInfinite returns false for normal number`() {
        assertFalse(DoubleHelper.isInfinite(5.0))
    }

    @Test
    fun `isInfinite returns false for zero`() {
        assertFalse(DoubleHelper.isInfinite(0.0))
    }

    @Test
    fun `isInfinite returns false for NaN`() {
        assertFalse(DoubleHelper.isInfinite(Double.NaN))
    }
}
