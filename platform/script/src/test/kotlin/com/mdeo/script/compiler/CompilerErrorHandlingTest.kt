package com.mdeo.script.compiler

import com.mdeo.expression.ast.expressions.TypedExpression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

/**
 * Tests for error handling and edge cases in the compiler.
 */
class CompilerErrorHandlingTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `unsupported expression kind throws CompilationException`() {
        val ast = buildTypedAst {
            val intType = intType()
            
            // Create a custom expression that is not supported
            val unsupportedExpr = object : TypedExpression {
                override val kind = "unsupportedKind" // This kind doesn't exist
                override val evalType = intType
            }
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(returnStmt(unsupportedExpr))
            )
        }
        
        val exception = assertThrows<CompilationException> {
            helper.compileAndInvoke(ast)
        }
        
        assertTrue(exception.message?.contains("unsupportedKind") == true,
            "Exception should mention the unsupported expression kind")
    }
    
    @Test
    fun `empty function body with void return type works`() {
        val ast = buildTypedAst {
            val voidType = voidType()
            function(
                name = "testFunction",
                returnType = voidType,
                body = emptyList()
            )
        }
        
        // Should not throw - void functions with no explicit return are valid
        val result = helper.compileAndInvoke(ast)
        // Void functions return null via reflection
        assertTrue(result == null || result == Unit)
    }
    
    /**
     * Potential BUG: Non-void function without return statement.
     * 
     * If a function has a non-void return type but the body doesn't end with
     * a return statement, the bytecode might be invalid. The ensureReturn method
     * in ScriptCompiler only adds RETURN for void functions.
     */
    @Test
    fun `non-void function without return statement should fail at runtime`() {
        val ast = buildTypedAst {
            val intType = intType()
            function(
                name = "testFunction",
                returnType = intType,
                body = emptyList() // No return statement!
            )
        }
        
        // This should fail either at compile time (preferred) or at class load/verification time
        // Currently, with COMPUTE_FRAMES, this will fail at class verification with a VerifyError
        assertThrows<Throwable> {
            helper.compileAndInvoke(ast)
        }
    }
}
