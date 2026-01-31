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
    fun `compute edge label with both properties outgoing`() {
        val label = EdgeLabelUtils.computeEdgeLabel("start", "end", isOutgoing = true)
        assertEquals("`start`_`end`", label)
    }

    @Test
    fun `compute edge label with both properties incoming`() {
        val label = EdgeLabelUtils.computeEdgeLabel("start", "end", isOutgoing = false)
        assertEquals("`end`_`start`", label)
    }

    @Test
    fun `compute edge label with null start property outgoing`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, "end", isOutgoing = true)
        assertEquals("``_`end`", label)
    }

    @Test
    fun `compute edge label with null end property outgoing`() {
        val label = EdgeLabelUtils.computeEdgeLabel("start", null, isOutgoing = true)
        assertEquals("`start`_``", label)
    }

    @Test
    fun `compute edge label with null start property incoming`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, "end", isOutgoing = false)
        assertEquals("`end`_``", label)
    }

    @Test
    fun `compute edge label with null end property incoming`() {
        val label = EdgeLabelUtils.computeEdgeLabel("start", null, isOutgoing = false)
        assertEquals("``_`start`", label)
    }

    @Test
    fun `compute edge label with both null properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, null, isOutgoing = true)
        assertEquals("``_``", label)
    }

    @Test
    fun `compute edge label with empty string properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel("", "", isOutgoing = true)
        assertEquals("``_``", label)
    }

    // ========== parseEdgeLabel Tests ==========

    @Test
    fun `parse edge label with both properties outgoing`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`start`_`end`", isOutgoing = true)
        assertEquals("start", start)
        assertEquals("end", end)
    }

    @Test
    fun `parse edge label with both properties incoming`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`start`_`end`", isOutgoing = false)
        assertEquals("end", start)
        assertEquals("start", end)
    }

    @Test
    fun `parse edge label with empty start property outgoing`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("``_`end`", isOutgoing = true)
        assertNull(start)
        assertEquals("end", end)
    }

    @Test
    fun `parse edge label with empty end property outgoing`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`start`_``", isOutgoing = true)
        assertEquals("start", start)
        assertNull(end)
    }

    @Test
    fun `parse edge label with both empty properties`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("``_``", isOutgoing = true)
        assertNull(start)
        assertNull(end)
    }

    @Test
    fun `parse invalid edge label format`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("invalid_format", isOutgoing = true)
        assertNull(start)
        assertNull(end)
    }

    @Test
    fun `parse edge label with special characters`() {
        val (start, end) = EdgeLabelUtils.parseEdgeLabel("`my_prop`_`other_prop`", isOutgoing = true)
        assertEquals("my_prop", start)
        assertEquals("other_prop", end)
    }

    // ========== Round-trip Tests ==========

    @Test
    fun `round-trip compute and parse outgoing`() {
        val label = EdgeLabelUtils.computeEdgeLabel("prop1", "prop2", isOutgoing = true)
        val (start, end) = EdgeLabelUtils.parseEdgeLabel(label, isOutgoing = true)
        assertEquals("prop1", start)
        assertEquals("prop2", end)
    }

    @Test
    fun `round-trip compute and parse incoming`() {
        val label = EdgeLabelUtils.computeEdgeLabel("prop1", "prop2", isOutgoing = false)
        val (start, end) = EdgeLabelUtils.parseEdgeLabel(label, isOutgoing = false)
        assertEquals("prop1", start)
        assertEquals("prop2", end)
    }

    @Test
    fun `round-trip with null properties`() {
        val label = EdgeLabelUtils.computeEdgeLabel(null, "end", isOutgoing = true)
        val (start, end) = EdgeLabelUtils.parseEdgeLabel(label, isOutgoing = true)
        assertNull(start)
        assertEquals("end", end)
    }
}
