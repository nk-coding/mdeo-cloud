package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CompiledClass equality and hash code.
 */
class CompiledClassTest {
    
    @Test
    fun `equals returns true for same instance`() {
        val compiled = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
        )
        
        assertTrue(compiled == compiled)
    }
    
    @Test
    fun `equals returns true for identical content`() {
        val bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
        
        val compiled1 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = bytecode.copyOf()
        )
        
        val compiled2 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = bytecode.copyOf()
        )
        
        assertEquals(compiled1, compiled2)
    }
    
    @Test
    fun `equals returns false for different fileUri`() {
        val bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
        
        val compiled1 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = bytecode.copyOf()
        )
        
        val compiled2 = CompiledClass(
            fileUri = "test://file2.script",
            className = "com/test/Script",
            bytecode = bytecode.copyOf()
        )
        
        assertNotEquals(compiled1, compiled2)
    }
    
    @Test
    fun `equals returns false for different className`() {
        val bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
        
        val compiled1 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script1",
            bytecode = bytecode.copyOf()
        )
        
        val compiled2 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script2",
            bytecode = bytecode.copyOf()
        )
        
        assertNotEquals(compiled1, compiled2)
    }
    
    @Test
    fun `equals returns false for different bytecode`() {
        val compiled1 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = byteArrayOf(0x01, 0x02, 0x03)
        )
        
        val compiled2 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = byteArrayOf(0x01, 0x02, 0x04)
        )
        
        assertNotEquals(compiled1, compiled2)
    }
    
    @Test
    fun `equals returns false for non-CompiledClass`() {
        val compiled = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
        )
        
        assertFalse(compiled.equals("not a compiled class"))
        assertFalse(compiled.equals(null))
    }
    
    @Test
    fun `hashCode is consistent for equal objects`() {
        val bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
        
        val compiled1 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = bytecode.copyOf()
        )
        
        val compiled2 = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = bytecode.copyOf()
        )
        
        assertEquals(compiled1.hashCode(), compiled2.hashCode())
    }
    
    @Test
    fun `hashCode is stable`() {
        val compiled = CompiledClass(
            fileUri = "test://file1.script",
            className = "com/test/Script",
            bytecode = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
        )
        
        val hash1 = compiled.hashCode()
        val hash2 = compiled.hashCode()
        
        assertEquals(hash1, hash2)
    }
}
