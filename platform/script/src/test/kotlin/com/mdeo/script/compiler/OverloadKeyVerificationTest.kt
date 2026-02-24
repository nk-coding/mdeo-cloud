package com.mdeo.script.compiler

import com.mdeo.script.compiler.registry.type.TypeRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertEquals
import com.mdeo.expression.ast.types.ClassTypeRef

/**
 * Test to verify overload key handling is correct.
 * 
 * TypeScript frontend sends:
 * - "" for non-overloaded methods (FunctionSignature.DEFAULT_SIGNATURE)
 * - "int" for max(int), "long" for max(long), etc.
 *
 * Kotlin registry now correctly uses:
 * - "" for non-overloaded methods
 * - "int", "long", "float", "double" for overloaded methods
 */
class OverloadKeyVerificationTest {

    @Test
    fun `abs lookup with empty key should work (TypeScript sends empty string)`() {
        val registry = TypeRegistry.GLOBAL
        
        // TypeScript sends "" for non-overloaded methods like abs()
        val absWithEmptyKey = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "abs", "")
        
        // This SHOULD find the method because TypeScript sends "" for non-overloaded
        assertNotNull(absWithEmptyKey, "Should find abs with empty string key")
    }

    @Test
    fun `max lookup with type key should work (TypeScript sends int)`() {
        val registry = TypeRegistry.GLOBAL
        
        // For overloaded max(), TypeScript sends "int"
        val maxWithTypeKey = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "max", "int")
        
        // This SHOULD find the method
        assertNotNull(maxWithTypeKey, "Should find max with 'int' key")
    }

    @Test
    fun `verify registry uses correct overload keys`() {
        val registry = TypeRegistry.GLOBAL
        
        // Verify non-overloaded methods use empty string
        val absWithEmptyKey = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "abs", "")
        assertNotNull(absWithEmptyKey, "Registry should have abs with empty string key")
        
        // Verify overloaded methods use type names
        val maxWithInt = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "max", "int")
        val maxWithLong = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "max", "long")
        val maxWithFloat = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "max", "float")
        val maxWithDouble = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "max", "double")
        
        assertNotNull(maxWithInt, "Registry should have max with 'int' key")
        assertNotNull(maxWithLong, "Registry should have max with 'long' key")
        assertNotNull(maxWithFloat, "Registry should have max with 'float' key")
        assertNotNull(maxWithDouble, "Registry should have max with 'double' key")
        
        // Old full signature keys should no longer work
        val absWithFullSig = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "abs", "abs():builtin.int")
        val maxWithFullSig = registry.lookupMethod(ClassTypeRef("builtin", "int", false), "max", "max(builtin.int):builtin.int")
        
        assertNull(absWithFullSig, "Old full signature key should not work")
        assertNull(maxWithFullSig, "Old full signature key should not work")
    }
}
