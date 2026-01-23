package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for class name generation from file URIs.
 * File URIs with special characters need to be sanitized to create valid JVM class names.
 */
class ClassNameGenerationTest {
    
    private val helper = CompilerTestHelper()
    private val compiler = ScriptCompiler()
    
    @Test
    fun `file URI with colons in scheme is converted correctly`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        val input = CompilationInput(mapOf("file:///path/to/script.mdeo" to ast))
        val program = compiler.compile(input)
        
        assertNotNull(program.classes["file:///path/to/script.mdeo"])
        
        // The class name should not contain characters that are invalid in JVM class names
        val className = program.classes["file:///path/to/script.mdeo"]!!.className
        assertNotNull(className)
        assert(!className.contains(":")) { "Class name should not contain colons: $className" }
        assert(!className.contains(" ")) { "Class name should not contain spaces: $className" }
    }
    
    @Test
    fun `file URI with spaces is converted correctly`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        val input = CompilationInput(mapOf("file:///path/to/my script.mdeo" to ast))
        val program = compiler.compile(input)
        
        assertNotNull(program.classes["file:///path/to/my script.mdeo"])
        
        val className = program.classes["file:///path/to/my script.mdeo"]!!.className
        assert(!className.contains(" ")) { "Class name should not contain spaces: $className" }
    }
    
    @Test
    fun `different file URIs produce different class names`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        val input = CompilationInput(mapOf(
            "file:///path/to/script1.mdeo" to ast,
            "file:///path/to/script2.mdeo" to ast
        ))
        val program = compiler.compile(input)
        
        val class1 = program.classes["file:///path/to/script1.mdeo"]!!.className
        val class2 = program.classes["file:///path/to/script2.mdeo"]!!.className
        
        assert(class1 != class2) { "Different files should produce different class names" }
    }
    
    /**
     * Potential BUG: File URIs that differ only in characters that get sanitized
     * to the same character could produce the same class name.
     * 
     * For example: "file.script" and "file_script" might both become "file_script"
     */
    @Test
    fun `file URIs differing only in sanitized characters produce unique class names`() {
        val ast = buildTypedAst {
            val intType = intType()
            function("getValue", intType, body = listOf(returnStmt(intLiteral(42, intType))))
        }
        
        // These URIs differ only in dot vs underscore
        val input = CompilationInput(mapOf(
            "test://file.name" to ast,
            "test://file_name" to ast
        ))
        val program = compiler.compile(input)
        
        val class1 = program.classes["test://file.name"]!!.className
        val class2 = program.classes["test://file_name"]!!.className
        
        // BUG: Both will be sanitized to have underscores, producing the same class name
        // This assertion should fail if the bug exists
        assert(class1 != class2) { 
            "Different URIs should produce different class names even after sanitization. " +
            "Got: class1=$class1, class2=$class2" 
        }
    }
}
