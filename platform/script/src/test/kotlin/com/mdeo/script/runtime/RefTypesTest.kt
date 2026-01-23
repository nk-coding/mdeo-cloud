package com.mdeo.script.runtime

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for the Ref wrapper types.
 */
class RefTypesTest {
    
    @Test
    fun `IntRef default constructor initializes to 0`() {
        val ref = IntRef()
        assertEquals(0, ref.value)
    }
    
    @Test
    fun `IntRef constructor with value`() {
        val ref = IntRef(42)
        assertEquals(42, ref.value)
    }
    
    @Test
    fun `IntRef value can be modified`() {
        val ref = IntRef(10)
        ref.value = 20
        assertEquals(20, ref.value)
    }
    
    @Test
    fun `IntRef with negative value`() {
        val ref = IntRef(-100)
        assertEquals(-100, ref.value)
    }
    
    @Test
    fun `IntRef with max value`() {
        val ref = IntRef(Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, ref.value)
    }
    
    @Test
    fun `IntRef with min value`() {
        val ref = IntRef(Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, ref.value)
    }
    
    @Test
    fun `LongRef default constructor initializes to 0`() {
        val ref = LongRef()
        assertEquals(0L, ref.value)
    }
    
    @Test
    fun `LongRef constructor with value`() {
        val ref = LongRef(9876543210L)
        assertEquals(9876543210L, ref.value)
    }
    
    @Test
    fun `LongRef value can be modified`() {
        val ref = LongRef(10L)
        ref.value = 20L
        assertEquals(20L, ref.value)
    }
    
    @Test
    fun `LongRef with negative value`() {
        val ref = LongRef(-9876543210L)
        assertEquals(-9876543210L, ref.value)
    }
    
    @Test
    fun `LongRef with max value`() {
        val ref = LongRef(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, ref.value)
    }
    
    @Test
    fun `LongRef with min value`() {
        val ref = LongRef(Long.MIN_VALUE)
        assertEquals(Long.MIN_VALUE, ref.value)
    }
    
    @Test
    fun `FloatRef default constructor initializes to 0`() {
        val ref = FloatRef()
        assertEquals(0.0f, ref.value)
    }
    
    @Test
    fun `FloatRef constructor with value`() {
        val ref = FloatRef(3.14f)
        assertEquals(3.14f, ref.value)
    }
    
    @Test
    fun `FloatRef value can be modified`() {
        val ref = FloatRef(1.0f)
        ref.value = 2.5f
        assertEquals(2.5f, ref.value)
    }
    
    @Test
    fun `FloatRef with negative value`() {
        val ref = FloatRef(-3.14f)
        assertEquals(-3.14f, ref.value)
    }
    
    @Test
    fun `FloatRef with max value`() {
        val ref = FloatRef(Float.MAX_VALUE)
        assertEquals(Float.MAX_VALUE, ref.value)
    }
    
    @Test
    fun `FloatRef with min value`() {
        val ref = FloatRef(Float.MIN_VALUE)
        assertEquals(Float.MIN_VALUE, ref.value)
    }
    
    @Test
    fun `FloatRef with positive infinity`() {
        val ref = FloatRef(Float.POSITIVE_INFINITY)
        assertEquals(Float.POSITIVE_INFINITY, ref.value)
    }
    
    @Test
    fun `FloatRef with NaN`() {
        val ref = FloatRef(Float.NaN)
        assertEquals(true, ref.value.isNaN())
    }
    
    @Test
    fun `DoubleRef default constructor initializes to 0`() {
        val ref = DoubleRef()
        assertEquals(0.0, ref.value)
    }
    
    @Test
    fun `DoubleRef constructor with value`() {
        val ref = DoubleRef(2.71828)
        assertEquals(2.71828, ref.value)
    }
    
    @Test
    fun `DoubleRef value can be modified`() {
        val ref = DoubleRef(1.0)
        ref.value = 2.5
        assertEquals(2.5, ref.value)
    }
    
    @Test
    fun `DoubleRef with negative value`() {
        val ref = DoubleRef(-2.71828)
        assertEquals(-2.71828, ref.value)
    }
    
    @Test
    fun `DoubleRef with max value`() {
        val ref = DoubleRef(Double.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, ref.value)
    }
    
    @Test
    fun `DoubleRef with min value`() {
        val ref = DoubleRef(Double.MIN_VALUE)
        assertEquals(Double.MIN_VALUE, ref.value)
    }
    
    @Test
    fun `DoubleRef with positive infinity`() {
        val ref = DoubleRef(Double.POSITIVE_INFINITY)
        assertEquals(Double.POSITIVE_INFINITY, ref.value)
    }
    
    @Test
    fun `DoubleRef with NaN`() {
        val ref = DoubleRef(Double.NaN)
        assertEquals(true, ref.value.isNaN())
    }
    
    @Test
    fun `ObjectRef default constructor initializes to null`() {
        val ref = ObjectRef<String>()
        assertNull(ref.value)
    }
    
    @Test
    fun `ObjectRef constructor with value`() {
        val ref = ObjectRef("hello")
        assertEquals("hello", ref.value)
    }
    
    @Test
    fun `ObjectRef value can be modified`() {
        val ref = ObjectRef("initial")
        ref.value = "updated"
        assertEquals("updated", ref.value)
    }
    
    @Test
    fun `ObjectRef can hold different types`() {
        val stringRef = ObjectRef("string")
        val intRef = ObjectRef(42)
        
        assertEquals("string", stringRef.value)
        assertEquals(42, intRef.value)
    }
    
    @Test
    fun `ObjectRef can be set to null`() {
        val ref = ObjectRef("initial")
        ref.value = null
        assertNull(ref.value)
    }
    
    @Test
    fun `ObjectRef with complex object`() {
        data class Person(val name: String, val age: Int)
        val ref = ObjectRef(Person("John", 30))
        
        assertEquals("John", ref.value?.name)
        assertEquals(30, ref.value?.age)
    }
    
    @Test
    fun `IntRef multiple modifications`() {
        val ref = IntRef(0)
        for (i in 1..10) {
            ref.value = ref.value + 1
        }
        assertEquals(10, ref.value)
    }
    
    @Test
    fun `LongRef multiple modifications`() {
        val ref = LongRef(0L)
        for (i in 1..10) {
            ref.value = ref.value + 1L
        }
        assertEquals(10L, ref.value)
    }
    
    @Test
    fun `FloatRef multiple modifications`() {
        val ref = FloatRef(0.0f)
        for (i in 1..10) {
            ref.value = ref.value + 0.1f
        }
        assertEquals(1.0f, ref.value, 0.0001f)
    }
    
    @Test
    fun `DoubleRef multiple modifications`() {
        val ref = DoubleRef(0.0)
        for (i in 1..10) {
            ref.value = ref.value + 0.1
        }
        assertEquals(1.0, ref.value, 0.0001)
    }
}
