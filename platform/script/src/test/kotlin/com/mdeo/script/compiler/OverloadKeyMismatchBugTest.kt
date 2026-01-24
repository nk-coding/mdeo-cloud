package com.mdeo.script.compiler

import com.mdeo.script.compiler.registry.function.GlobalFunctionRegistry
import com.mdeo.script.compiler.registry.function.globalFunction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests to verify overload key handling with the new convention.
 *
 * The convention for overload keys is:
 * - Empty string "" for non-overloaded functions (default signature)
 * - Type-based key like "builtin.int" for overloaded functions
 *
 * This matches the TypeScript side where FunctionSignature.DEFAULT_SIGNATURE = ""
 */
class OverloadKeyMismatchBugTest {

    private val helper = CompilerTestHelper()

    /**
     * Tests that empty overload key (default signature) works correctly.
     * This is the expected behavior when TypeScript sends DEFAULT_SIGNATURE = "".
     */
    @Test
    fun `empty overload key works for default signature`() {
        val ast = buildTypedAst {
            val voidTypeIdx = voidType()
            val stringTypeIdx = stringType()

            function(
                name = "testFunction",
                returnType = voidTypeIdx,
                body = listOf(
                    exprStmt(
                        functionCall(
                            name = "println",
                            overload = "",  // DEFAULT_SIGNATURE from TypeScript
                            arguments = listOf(stringLiteral("hello", stringTypeIdx)),
                            resultTypeIndex = voidTypeIdx
                        )
                    ),
                    returnVoid()
                )
            )
        }

        // This should work correctly with empty overload key
        helper.compileAndInvoke(ast)
    }

    /**
     * Tests that the global registry properly handles the empty overload key.
     */
    @Test
    fun `global registry lookup with empty key succeeds`() {
        val registry = GlobalFunctionRegistry.GLOBAL
        
        // println is registered with empty string overload key
        val method = registry.lookupMethod("println", "")
        kotlin.test.assertNotNull(method)
        assertEquals("println", method.jvmMethodName)
    }
}
