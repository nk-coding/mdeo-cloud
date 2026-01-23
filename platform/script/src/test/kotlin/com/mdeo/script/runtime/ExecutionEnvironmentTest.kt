package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompilationInput
import com.mdeo.script.compiler.ScriptCompiler
import com.mdeo.script.compiler.buildTypedAst
import com.mdeo.script.compiler.intLiteral
import com.mdeo.script.compiler.returnStmt
import com.mdeo.script.compiler.stringLiteral
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the ExecutionEnvironment class.
 */
class ExecutionEnvironmentTest {
    
    private val compiler = ScriptCompiler()
    
    @Test
    fun `invoke function from compiled program`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(100, intType))))
        }
        
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        val result = env.invoke("test://test.script", "getValue")
        assertEquals(100, result)
    }
    
    @Test
    fun `invoke different functions from same file`() {
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            
            function("getInt", intType, body = listOf(returnStmt(intLiteral(42, intType))))
            function("getString", stringType, body = listOf(returnStmt(stringLiteral("hello", stringType))))
        }
        
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        assertEquals(42, env.invoke("test://test.script", "getInt"))
        assertEquals("hello", env.invoke("test://test.script", "getString"))
    }
    
    @Test
    fun `invoke functions from different files`() {
        val ast1 = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(1, intType))))
        }
        val ast2 = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(2, intType))))
        }
        
        val input = CompilationInput(mapOf(
            "test://file1.script" to ast1,
            "test://file2.script" to ast2
        ))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        assertEquals(1, env.invoke("test://file1.script", "getValue"))
        assertEquals(2, env.invoke("test://file2.script", "getValue"))
    }
    
    @Test
    fun `invoke non-existent file throws exception`() {
        val ast = buildTypedAst { }
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        assertThrows<ClassNotFoundException> {
            env.invoke("test://nonexistent.script", "getValue")
        }
    }
    
    @Test
    fun `invoke non-existent function throws exception`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("existingFunction", intType, body = listOf(returnStmt(intLiteral(1, intType))))
        }
        
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        assertThrows<IllegalArgumentException> {
            env.invoke("test://test.script", "nonExistentFunction")
        }
    }
    
    @Test
    fun `getClass returns loaded class`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        val env = ExecutionEnvironment(program)
        
        val clazz = env.getClass("test://test.script")
        assertNotNull(clazz)
        
        val method = clazz.getMethod("getValue")
        assertNotNull(method)
        assertEquals(42, method.invoke(null))
    }
    
    @Test
    fun `custom console stream can be used`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        val input = CompilationInput(mapOf("test://test.script" to ast))
        val program = compiler.compile(input)
        
        val outputStream = ByteArrayOutputStream()
        val customConsole = PrintStream(outputStream)
        val env = ExecutionEnvironment(program, customConsole)
        
        assertNotNull(env.console)
        assertEquals(customConsole, env.console)
    }
}
