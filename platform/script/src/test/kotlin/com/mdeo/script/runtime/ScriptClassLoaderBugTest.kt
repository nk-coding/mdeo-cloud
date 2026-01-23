package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledClass
import com.mdeo.script.compiler.CompiledProgram
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Tests for edge cases and potential bugs in ScriptClassLoader.
 */
class ScriptClassLoaderBugTest {

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

    /**
     * BUG: findClass does not check the cache before calling defineClass.
     * 
     * If loadClassForFile is called first, and then findClass is called 
     * (which can happen through loadClass delegation), it will attempt to 
     * define the same class twice, causing a LinkageError.
     * 
     * This test exposes the bug by calling loadClassForFile first and then
     * trying to use findClass (via loadClass).
     */
    @Test
    fun `findClass after loadClassForFile should return cached class not redefine`() {
        val className = "com/test/DuplicateTestClass"
        val bytecode = createSimpleBytecode(className)
        val compiledClass = CompiledClass("test://test.script", className, bytecode)
        val program = CompiledProgram(mapOf("test://test.script" to compiledClass))

        val classLoader = ScriptClassLoader(program)
        
        // First load via loadClassForFile - this defines the class and caches it
        val clazz1 = classLoader.loadClassForFile("test://test.script")
        assertNotNull(clazz1)
        
        // Now try to load via loadClass which calls findClass
        // This should return the cached class, not try to redefine it
        // BUG: findClass doesn't check the cache, so it tries to redefine the class
        val clazz2 = classLoader.loadClass("com.test.DuplicateTestClass")
        
        assertNotNull(clazz2)
        assertSame(clazz1, clazz2, "findClass should return the same cached class instance")
    }
    
    /**
     * BUG: Multiple calls to findClass (without loadClassForFile) will also 
     * try to define the class multiple times.
     * 
     * The findClass method doesn't cache its results, so calling loadClass
     * multiple times could potentially cause issues if the class isn't found
     * in parent classloader cache.
     */
    @Test
    fun `multiple findClass calls should not attempt duplicate class definition`() {
        val className = "com/test/MultipleLoadClass"
        val bytecode = createSimpleBytecode(className)
        val compiledClass = CompiledClass("test://test.script", className, bytecode)
        val program = CompiledProgram(mapOf("test://test.script" to compiledClass))

        val classLoader = ScriptClassLoader(program)
        
        // First call to loadClass - will call findClass which defines the class
        val clazz1 = classLoader.loadClass("com.test.MultipleLoadClass")
        assertNotNull(clazz1)
        
        // Second call to loadClass - should return cached class from parent's findLoadedClass
        // but if findClass is called again, it would try to redefine
        val clazz2 = classLoader.loadClass("com.test.MultipleLoadClass")
        assertNotNull(clazz2)
        
        assertSame(clazz1, clazz2, "Multiple loadClass calls should return the same instance")
    }
}
