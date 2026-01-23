package com.mdeo.script.compiler

import com.mdeo.script.runtime.ExecutionEnvironment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the ScriptCompiler class.
 */
class ScriptCompilerTest {
    
    private val compiler = ScriptCompiler()
    
    @Test
    fun `compile empty file produces valid class`() {
        val ast = buildTypedAst { }
        val input = CompilationInput(mapOf("test://empty.script" to ast))
        
        val program = compiler.compile(input)
        
        assertEquals(1, program.classes.size)
        assertNotNull(program.classes["test://empty.script"])
    }
    
    @Test
    fun `compile multiple files produces multiple classes`() {
        val ast1 = buildTypedAst {
            val intType = intType()
            function("func1", intType, body = listOf(returnStmt(intLiteral(1, intType))))
        }
        val ast2 = buildTypedAst {
            val intType = intType()
            function("func2", intType, body = listOf(returnStmt(intLiteral(2, intType))))
        }
        
        val input = CompilationInput(mapOf(
            "test://file1.script" to ast1,
            "test://file2.script" to ast2
        ))
        
        val program = compiler.compile(input)
        
        assertEquals(2, program.classes.size)
        assertNotNull(program.classes["test://file1.script"])
        assertNotNull(program.classes["test://file2.script"])
    }
    
    @Test
    fun `compiled class names are unique per file`() {
        val ast = buildTypedAst { }
        val input = CompilationInput(mapOf(
            "test://file1.script" to ast,
            "test://file2.script" to ast
        ))
        
        val program = compiler.compile(input)
        
        val class1 = program.classes["test://file1.script"]!!
        val class2 = program.classes["test://file2.script"]!!
        
        assertNotNull(class1.className)
        assertNotNull(class2.className)
        assert(class1.className != class2.className)
    }
    
    @Test
    fun `bytecode is valid and can be loaded`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        val result = env.invoke("test://test.script", "getValue")
        assertEquals(42, result)
    }
}
