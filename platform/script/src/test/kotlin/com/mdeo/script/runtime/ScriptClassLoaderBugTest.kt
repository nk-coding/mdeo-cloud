package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledProgram
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Tests for edge cases and regression coverage in ScriptClassLoader.
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
     * Regression: loadClassForFile followed by loadClass (which routes through findClass)
     * must return the same cached class instance without attempting to redefine it.
     */
    @Test
    fun `findClass after loadClassForFile should return cached class not redefine`() {
        val bytecode = createSimpleBytecode(CompiledProgram.SCRIPT_PROGRAM_INTERNAL_NAME)
        val binaryName = CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME
        val program = CompiledProgram(
            allBytecodes = mapOf(binaryName to bytecode),
            functionLookup = mapOf("test://test.script" to emptyMap())
        )

        val classLoader = ScriptClassLoader(program, ScriptClassLoaderBugTest::class.java.classLoader)

        // First load via loadClassForFile - this defines the class and caches it
        val clazz1 = classLoader.loadClassForFile("test://test.script")
        assertNotNull(clazz1)

        // loadClass routes through findClass; the unified cache ensures no double-define
        val clazz2 = classLoader.loadClass(binaryName)

        assertNotNull(clazz2)
        assertSame(clazz1, clazz2, "findClass should return the same cached class instance")
    }

    /**
     * Regression: multiple loadClass calls for the same name must return the same instance
     * and never attempt to define the class twice.
     */
    @Test
    fun `multiple findClass calls should not attempt duplicate class definition`() {
        val bytecode = createSimpleBytecode(CompiledProgram.SCRIPT_PROGRAM_INTERNAL_NAME)
        val binaryName = CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME
        val program = CompiledProgram(
            allBytecodes = mapOf(binaryName to bytecode)
        )

        val classLoader = ScriptClassLoader(program, ScriptClassLoaderBugTest::class.java.classLoader)

        // First call to loadClass - defines and caches the class
        val clazz1 = classLoader.loadClass(binaryName)
        assertNotNull(clazz1)

        // Second call must return the cached instance, not try to redefine
        val clazz2 = classLoader.loadClass(binaryName)
        assertNotNull(clazz2)

        assertSame(clazz1, clazz2, "Multiple loadClass calls should return the same instance")
    }
}
