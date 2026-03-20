package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the compiled program structure produced by [ScriptCompiler].
 *
 * All functions from all script files are compiled into a single JVM class
 * ([CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME]). Functions are assigned artificial JVM
 * method names (`fn0`, `fn1`, …) tracked in [CompiledProgram.functionLookup].
 */
class CompiledProgramStructureTest {

    private val compiler = ScriptCompiler()

    @Test
    fun `functions from a file are accessible in function lookup`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }

        val input = CompilationInput(mapOf("file:///path/to/script.mdeo" to ast))
        val program = compiler.compile(input)

        val fileLookup = program.functionLookup["file:///path/to/script.mdeo"]
        assertNotNull(fileLookup)
        assertTrue(fileLookup.containsKey("getValue"), "functionLookup should contain 'getValue'")
    }

    @Test
    fun `file paths with special characters work as lookup keys`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }

        val input = CompilationInput(mapOf("file:///path/to/my script.mdeo" to ast))
        val program = compiler.compile(input)

        assertNotNull(
            program.functionLookup["file:///path/to/my script.mdeo"],
            "File with spaces in path should be present in functionLookup"
        )
    }

    @Test
    fun `functions from different files get unique jvm method names`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }

        val input = CompilationInput(mapOf(
            "file:///path/to/script1.mdeo" to ast,
            "file:///path/to/script2.mdeo" to ast
        ))
        val program = compiler.compile(input)

        val jvmName1 = program.functionLookup["file:///path/to/script1.mdeo"]!!["getValue"]!!
        val jvmName2 = program.functionLookup["file:///path/to/script2.mdeo"]!!["getValue"]!!

        assertTrue(jvmName1 != jvmName2, "Same-named functions in different files must get unique JVM names")
    }

    @Test
    fun `all functions are compiled into a single class`() {
        val ast1 = buildTypedAst {
            val intType = intType()
            function("objective", intType, body = listOf(returnStmt(intLiteral(1, intType))))
        }
        val ast2 = buildTypedAst {
            val intType = intType()
            function("constraint", intType, body = listOf(returnStmt(intLiteral(0, intType))))
        }

        val input = CompilationInput(mapOf(
            "file:///a.mdeo" to ast1,
            "file:///b.mdeo" to ast2
        ))
        val program = compiler.compile(input)

        assertEquals(
            setOf(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME),
            program.allBytecodes.keys.filter { it.startsWith("com.mdeo.script.generated") }.toSet(),
            "All functions must reside in the single ScriptProgram class"
        )
    }

    @Test
    fun `artificial jvm method names follow fn-counter pattern`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("first", intType, body = listOf(returnStmt(intLiteral(1, intType))))
            function("second", intType, body = listOf(returnStmt(intLiteral(2, intType))))
        }

        val input = CompilationInput(mapOf("file:///script.mdeo" to ast))
        val program = compiler.compile(input)

        val fileLookup = program.functionLookup["file:///script.mdeo"]!!
        val jvmNames = fileLookup.values.toSet()

        assertTrue(jvmNames.all { it.matches(Regex("fn\\d+")) }, "All JVM names should match fn<number> pattern")
        assertEquals(2, jvmNames.size, "Two functions should produce two unique JVM names")
    }
}
