package com.mdeo.script.compiler

import com.mdeo.script.runtime.ExecutionEnvironment
import com.mdeo.script.runtime.SimpleScriptContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the ScriptCompiler class.
 */
class ScriptCompilerTest {
    
    private val compiler = ScriptCompiler()
    
    @Test
    fun `compile empty file registers file in lookup`() {
        val ast = buildTypedAst { }
        val input = CompilationInput(mapOf("test://empty.script" to ast))
        
        val program = compiler.compile(input)
        
        assertEquals(1, program.functionLookup.size)
        assertNotNull(program.functionLookup["test://empty.script"])
    }
    
    @Test
    fun `compile multiple files registers all files in lookup`() {
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
        
        assertEquals(2, program.functionLookup.size)
        assertNotNull(program.functionLookup["test://file1.script"])
        assertNotNull(program.functionLookup["test://file2.script"])
    }
    
    @Test
    fun `functions in different files get unique jvm method names`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(1, intType))))
        }
        val input = CompilationInput(mapOf(
            "test://file1.script" to ast,
            "test://file2.script" to ast
        ))
        
        val program = compiler.compile(input)
        
        val jvm1 = program.functionLookup["test://file1.script"]!!["getValue"]!!
        val jvm2 = program.functionLookup["test://file2.script"]!!["getValue"]!!
        
        assertNotNull(jvm1)
        assertNotNull(jvm2)
        assert(jvm1 != jvm2) { "Same function name in different files must get different JVM names" }
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
        
        val result = env.invoke("test://test.script", "getValue", SimpleScriptContext(System.out, null))
        assertEquals(42, result)
    }

    @Test
    fun `methods with same name in different files work independently`() {
        val fileA = "test://a.script"
        val fileB = "test://b.script"

        val astA = buildTypedAst {
            val stringType = stringType()
            function(
                name = "greet",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello from A", stringType)))
            )
        }
        val astB = buildTypedAst {
            val stringType = stringType()
            function(
                name = "greet",
                returnType = stringType,
                body = listOf(returnStmt(stringLiteral("hello from B", stringType)))
            )
        }

        val program = compiler.compile(CompilationInput(mapOf(fileA to astA, fileB to astB)))
        val env = ExecutionEnvironment(program)
        val ctx = SimpleScriptContext(System.out, null)

        assertEquals("hello from A", env.invoke(fileA, "greet", ctx))
        assertEquals("hello from B", env.invoke(fileB, "greet", ctx))
    }

    @Test
    fun `all functions assigned sequential jvm method names`() {
        val ast1 = buildTypedAst {
            val intType = intType()
            function("a", intType, body = listOf(returnStmt(intLiteral(1, intType))))
            function("b", intType, body = listOf(returnStmt(intLiteral(2, intType))))
        }
        val ast2 = buildTypedAst {
            val intType = intType()
            function("c", intType, body = listOf(returnStmt(intLiteral(3, intType))))
        }

        val input = CompilationInput(mapOf("test://f1.script" to ast1, "test://f2.script" to ast2))
        val program = compiler.compile(input)

        val f1 = program.functionLookup["test://f1.script"]!!
        val f2 = program.functionLookup["test://f2.script"]!!

        assertEquals("fn0", f1["a"])
        assertEquals("fn1", f1["b"])
        assertEquals("fn2", f2["c"])
    }
}
