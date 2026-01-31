package com.mdeo.script.stdlib.impl.primitives

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for AnyHelper.
 *
 * Note: The following methods and their tests have been removed:
 * - asBag, asList, asSet, asOrderedSet
 */
class AnyHelperTest {

    // ==================== asBoolean() tests ====================
    @Test
    fun `asBoolean converts true string`() {
        assertTrue(AnyHelper.asBoolean("true"))
    }

    @Test
    fun `asBoolean converts false string`() {
        assertFalse(AnyHelper.asBoolean("false"))
    }

    @Test
    fun `asBoolean converts TRUE string`() {
        assertTrue(AnyHelper.asBoolean("TRUE"))
    }

    @Test
    fun `asBoolean converts non-zero number to true`() {
        assertTrue(AnyHelper.asBoolean(1))
    }

    @Test
    fun `asBoolean converts zero to false`() {
        assertFalse(AnyHelper.asBoolean(0))
    }

    // ==================== asInteger() tests ====================
    @Test
    fun `asInteger converts string to int`() {
        assertEquals(42, AnyHelper.asInteger("42"))
    }

    @Test
    fun `asInteger converts double to int`() {
        assertEquals(42, AnyHelper.asInteger(42.7))
    }

    @Test
    fun `asInteger converts true to 1`() {
        assertEquals(1, AnyHelper.asInteger(true))
    }

    @Test
    fun `asInteger converts false to 0`() {
        assertEquals(0, AnyHelper.asInteger(false))
    }

    @Test
    fun `asInteger returns int as is`() {
        assertEquals(42, AnyHelper.asInteger(42))
    }

    // ==================== asDouble() tests ====================
    @Test
    fun `asDouble converts string to double`() {
        assertEquals(42.5, AnyHelper.asDouble("42.5"))
    }

    @Test
    fun `asDouble converts int to double`() {
        assertEquals(42.0, AnyHelper.asDouble(42))
    }

    @Test
    fun `asDouble returns double as is`() {
        assertEquals(42.5, AnyHelper.asDouble(42.5))
    }

    @Test
    fun `asDouble converts float to double`() {
        assertEquals(42.5, AnyHelper.asDouble(42.5f), 0.001)
    }

    @Test
    fun `asDouble converts long to double`() {
        assertEquals(42.0, AnyHelper.asDouble(42L))
    }

    // ==================== asFloat() tests ====================
    @Test
    fun `asFloat converts string to float`() {
        assertEquals(42.5f, AnyHelper.asFloat("42.5"), 0.001f)
    }

    @Test
    fun `asFloat converts int to float`() {
        assertEquals(42.0f, AnyHelper.asFloat(42))
    }

    @Test
    fun `asFloat converts double to float`() {
        assertEquals(42.5f, AnyHelper.asFloat(42.5), 0.001f)
    }

    @Test
    fun `asFloat returns float as is`() {
        assertEquals(42.5f, AnyHelper.asFloat(42.5f))
    }

    @Test
    fun `asFloat converts long to float`() {
        assertEquals(42.0f, AnyHelper.asFloat(42L))
    }

    // ==================== toString() tests ====================
    @Test
    fun `toString converts int to string`() {
        assertEquals("42", AnyHelper.toString(42))
    }

    @Test
    fun `toString converts double to string`() {
        assertTrue(AnyHelper.toString(42.5).startsWith("42.5"))
    }

    @Test
    fun `toString converts boolean to string`() {
        assertEquals("true", AnyHelper.toString(true))
    }

    @Test
    fun `toString returns string as is`() {
        assertEquals("hello", AnyHelper.toString("hello"))
    }

    @Test
    fun `toString converts null to null string`() {
        assertEquals("null", AnyHelper.toString(null))
    }

    // ==================== format() tests ====================
    @Test
    fun `format integer with pattern`() {
        val result = AnyHelper.format(42, "{0,number,00000}")
        assertEquals("00042", result)
    }

    @Test
    fun `format double with pattern`() {
        val result = AnyHelper.format(42.5, "{0,number,0.00}")
        assertEquals("42.50", result)
    }

    @Test
    fun `format string with pattern`() {
        val result = AnyHelper.format("hello", "{0}")
        assertEquals("hello", result)
    }

    @Test
    fun `format with simple pattern`() {
        val result = AnyHelper.format("test", "{0}")
        assertEquals("test", result)
    }

    @Test
    fun `format with message`() {
        val result = AnyHelper.format("hello", "Message: {0}")
        assertEquals("Message: hello", result)
    }

    // ==================== type() tests ====================
    @Test
    fun `type returns Integer for int`() {
        val result = AnyHelper.type(42)
        assertTrue(result?.simpleName?.contains("Int") == true || result?.simpleName?.contains("Integer") == true)
    }

    @Test
    fun `type returns String for string`() {
        val result = AnyHelper.type("hello")
        assertTrue(result?.simpleName?.contains("String") == true)
    }

    @Test
    fun `type returns Boolean for boolean`() {
        val result = AnyHelper.type(true)
        assertTrue(result?.simpleName?.contains("Boolean") == true)
    }

    @Test
    fun `type returns Double for double`() {
        val result = AnyHelper.type(42.5)
        assertTrue(result?.simpleName?.contains("Double") == true)
    }

    @Test
    fun `type returns null for null`() {
        val result = AnyHelper.type(null)
        assertTrue(result == null)
    }

    // ==================== isTypeOf() tests ====================
    @Test
    fun `isTypeOf returns true for matching type`() {
        assertTrue(AnyHelper.isTypeOf(42, Int::class.javaObjectType))
    }

    @Test
    fun `isTypeOf returns false for non-matching type`() {
        assertFalse(AnyHelper.isTypeOf(42, String::class.java))
    }

    @Test
    fun `isTypeOf with string class`() {
        assertTrue(AnyHelper.isTypeOf("hello", String::class.java))
    }

    @Test
    fun `isTypeOf with boolean class`() {
        assertTrue(AnyHelper.isTypeOf(true, Boolean::class.javaObjectType))
    }

    @Test
    fun `isTypeOf with subclass returns false for superclass`() {
        // isTypeOf checks exact type, so String is not exactly Any
        assertFalse(AnyHelper.isTypeOf("hello", Any::class.java))
    }

    // ==================== instanceOf() tests ====================
    @Test
    fun `instanceOf returns true for matching type`() {
        assertTrue(AnyHelper.instanceOf(42, Int::class.javaObjectType))
    }

    @Test
    fun `instanceOf returns true for superclass`() {
        assertTrue(AnyHelper.instanceOf("hello", Any::class.java))
    }

    @Test
    fun `instanceOf returns false for non-matching type`() {
        assertFalse(AnyHelper.instanceOf(42, String::class.java))
    }

    @Test
    fun `instanceOf with interface`() {
        assertTrue(AnyHelper.instanceOf(listOf(1, 2), Iterable::class.java))
    }

    @Test
    fun `instanceOf returns false for null`() {
        assertFalse(AnyHelper.instanceOf(null, String::class.java))
    }

    // ==================== hasProperty() tests ====================
    @Test
    fun `hasProperty returns true for existing property`() {
        // Using map which has containsKey check
        val obj = mapOf("name" to "John")
        assertTrue(AnyHelper.hasProperty(obj, "name"))
    }

    @Test
    fun `hasProperty returns false for non-existing property`() {
        val obj = mapOf("name" to "John")
        assertFalse(AnyHelper.hasProperty(obj, "age"))
    }

    @Test
    fun `hasProperty with map and existing key`() {
        val map = mapOf("name" to "John")
        assertTrue(AnyHelper.hasProperty(map, "name"))
    }

    @Test
    fun `hasProperty with map and non-existing key`() {
        val map = mapOf("name" to "John")
        assertFalse(AnyHelper.hasProperty(map, "age"))
    }

    @Test
    fun `hasProperty returns false for null`() {
        assertFalse(AnyHelper.hasProperty(null, "property"))
    }
}
