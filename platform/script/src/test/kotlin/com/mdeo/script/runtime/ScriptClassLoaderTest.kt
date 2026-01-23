package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledClass
import com.mdeo.script.compiler.CompiledProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the ScriptClassLoader class.
 */
class ScriptClassLoaderTest {
    
    @Test
    fun `loadClassForFile loads class for valid file URI`() {
        val bytecode = createSimpleBytecode("com/test/TestClass")
        val compiledClass = CompiledClass("test://test.script", "com/test/TestClass", bytecode)
        val program = CompiledProgram(mapOf("test://test.script" to compiledClass))
        
        val classLoader = ScriptClassLoader(program)
        val clazz = classLoader.loadClassForFile("test://test.script")
        
        assertNotNull(clazz)
        assertEquals("com.test.TestClass", clazz.name)
    }
    
    @Test
    fun `loadClassForFile throws for unknown file URI`() {
        val program = CompiledProgram(emptyMap())
        val classLoader = ScriptClassLoader(program)
        
        assertThrows<ClassNotFoundException> {
            classLoader.loadClassForFile("test://unknown.script")
        }
    }
    
    @Test
    fun `loadClassForFile caches loaded classes`() {
        val bytecode = createSimpleBytecode("com/test/CachedClass")
        val compiledClass = CompiledClass("test://test.script", "com/test/CachedClass", bytecode)
        val program = CompiledProgram(mapOf("test://test.script" to compiledClass))
        
        val classLoader = ScriptClassLoader(program)
        val clazz1 = classLoader.loadClassForFile("test://test.script")
        val clazz2 = classLoader.loadClassForFile("test://test.script")
        
        assert(clazz1 === clazz2)
    }
    
    @Test
    fun `findClass loads class by name`() {
        val bytecode = createSimpleBytecode("com/test/FindableClass")
        val compiledClass = CompiledClass("test://test.script", "com/test/FindableClass", bytecode)
        val program = CompiledProgram(mapOf("test://test.script" to compiledClass))
        
        val classLoader = ScriptClassLoader(program)
        val clazz = classLoader.loadClass("com.test.FindableClass")
        
        assertNotNull(clazz)
        assertEquals("com.test.FindableClass", clazz.name)
    }
    
    /**
     * Creates minimal valid bytecode for a simple class using ASM.
     */
    private fun createSimpleBytecode(className: String): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        cw.visit(
            Opcodes.V11,
            Opcodes.ACC_PUBLIC,
            className,
            null,
            "java/lang/Object",
            null
        )
        cw.visitEnd()
        return cw.toByteArray()
    }
}
