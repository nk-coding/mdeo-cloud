package com.mdeo.modeltransformation.ast

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for EdgeLabelUtils utility functions.
 */
class EdgeLabelUtilsTest {

    // ========== computeEdgeLabel Tests ==========

    @Test
    fun `compute edge label with both properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel("start", "end")
        assertEquals("`start`_`end`", label)
    }

    @Test
    fun `compute edge label with null start property`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, "end")
        assertEquals("``_`end`", label)
    }

    @Test
    fun `compute edge label with null end property`() {
        val label = EdgeLabelUtils.computeEdgeLabel("start", null)
        assertEquals("`start`_``", label)
    }

    @Test
    fun `compute edge label with both null properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, null)
        assertEquals("``_``", label)
    }

    @Test
    fun `compute edge label with empty string properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel("", "")
        assertEquals("``_``", label)
    }

    // ========== parseEdgeLabel Tests ==========

    @Test
    fun `parse edge label with both properties`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`start`_`end`")
        assertEquals("start", start)
        assertEquals("end", end)
    }

    @Test
    fun `parse edge label with empty start property`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("``_`end`")
        assertNull(start)
        assertEquals("end", end)
    }

    @Test
    fun `parse edge label with empty end property`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`start`_``")
        assertEquals("start", start)
        assertNull(end)
    }

    @Test
    fun `parse edge label with both empty properties`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("``_``")
        assertNull(start)
        assertNull(end)
    }

    @Test
    fun `parse invalid edge label format`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("invalid_format")
        assertNull(start)
        assertNull(end)
    }

    @Test
    fun `parse edge label with special characters`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`my_prop`_`other_prop`")
        assertEquals("my_prop", start)
        assertEquals("other_prop", end)
    }

    // ========== Round-trip Tests ==========

    @Test
    fun `round-trip compute and parse`() {
        val label = EdgeLabelUtils.computeEdgeLabel("prop1", "prop2")
        val (start, end) = EdgeLabelUtils.parseEdgeLabel(label)
        assertEquals("prop1", start)
        assertEquals("prop2", end)
    }

    @Test
    fun `round-trip with null properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, "end")
        val (start, end) = EdgeLabelUtils.parseEdgeLabel(label)
        assertNull(start)
        assertEquals("end", end)
    }
}
