package com.mdeo.script.runtime

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
    fun `loadClassForFile loads script program class for a known file`() {
        val bytecode = createSimpleBytecode(CompiledProgram.SCRIPT_PROGRAM_INTERNAL_NAME)
        val program = CompiledProgram(
            allBytecodes = mapOf(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME to bytecode),
            functionLookup = mapOf("test://test.script" to mapOf("fn" to "fn0"))
        )

        val classLoader = ScriptClassLoader(program, ScriptClassLoaderTest::class.java.classLoader)
        val clazz = classLoader.loadClassForFile("test://test.script")

        assertNotNull(clazz)
        assertEquals(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME, clazz.name)
    }

    @Test
    fun `loadClassForFile throws for unknown file URI`() {
        val program = CompiledProgram(emptyMap())
        val classLoader = ScriptClassLoader(program, ScriptClassLoaderTest::class.java.classLoader)

        assertThrows<ClassNotFoundException> {
            classLoader.loadClassForFile("test://unknown.script")
        }
    }

    @Test
    fun `loadClassForFile caches loaded class`() {
        val bytecode = createSimpleBytecode(CompiledProgram.SCRIPT_PROGRAM_INTERNAL_NAME)
        val program = CompiledProgram(
            allBytecodes = mapOf(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME to bytecode),
            functionLookup = mapOf("test://test.script" to mapOf("fn" to "fn0"))
        )

        val classLoader = ScriptClassLoader(program, ScriptClassLoaderTest::class.java.classLoader)
        val clazz1 = classLoader.loadClassForFile("test://test.script")
        val clazz2 = classLoader.loadClassForFile("test://test.script")

        assert(clazz1 === clazz2)
    }

    @Test
    fun `findClass loads class by binary name`() {
        val bytecode = createSimpleBytecode(CompiledProgram.SCRIPT_PROGRAM_INTERNAL_NAME)
        val program = CompiledProgram(
            allBytecodes = mapOf(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME to bytecode)
        )

        val classLoader = ScriptClassLoader(program, ScriptClassLoaderTest::class.java.classLoader)
        val clazz = classLoader.loadClass(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME)

        assertNotNull(clazz)
        assertEquals(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME, clazz.name)
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
