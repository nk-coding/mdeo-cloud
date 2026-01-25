package com.mdeo.script.compiler.registry

import com.mdeo.script.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.function.GlobalFunctionRegistry
import com.mdeo.script.compiler.registry.function.globalFunction
import com.mdeo.script.compiler.registry.function.globalProperty
import com.mdeo.script.compiler.registry.property.GlobalPropertyRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the GlobalFunctionRegistry infrastructure.
 *
 * Tests cover:
 * - Function registration and lookup
 * - Property registration and lookup
 * - Overload resolution
 * - Global registry fallback
 */
class GlobalFunctionRegistryTest {

    private lateinit var registry: GlobalFunctionRegistry

    @BeforeEach
    fun setUp() {
        registry = GlobalFunctionRegistry()
    }

    @Nested
    inner class FunctionRegistration {

        @Test
        fun `can register and retrieve a function`() {
            val funcDef = globalFunction("testFunc") {
                staticOverload("testFunc():builtin.int") {
                    descriptor = "()I"
                    owner = "test/Helper"
                    jvmMethod = "testFunc"
                    parameterTypes = emptyList()
                    returnType = BuiltinTypes.INT
                }
            }
            registry.registerFunction(funcDef)

            val retrieved = registry.lookupFunction("testFunc")
            assertNotNull(retrieved)
            assertEquals("testFunc", retrieved.name)
        }

        @Test
        fun `returns null for unregistered function`() {
            val retrieved = registry.lookupFunction("nonexistent")
            assertNull(retrieved)
        }

        @Test
        fun `can register multiple functions`() {
            registry.registerFunction(globalFunction("funcA") {
                staticOverload("funcA():builtin.int") {
                    descriptor = "()I"
                    owner = "test/Helper"
                    jvmMethod = "funcA"
                    parameterTypes = emptyList()
                    returnType = BuiltinTypes.INT
                }
            })
            registry.registerFunction(globalFunction("funcB") {
                staticOverload("funcB():builtin.string") {
                    descriptor = "()Ljava/lang/String;"
                    owner = "test/Helper"
                    jvmMethod = "funcB"
                    parameterTypes = emptyList()
                    returnType = BuiltinTypes.STRING
                }
            })

            assertNotNull(registry.lookupFunction("funcA"))
            assertNotNull(registry.lookupFunction("funcB"))
        }
    }

    @Nested
    inner class PropertyRegistration {

        @Test
        fun `can register and retrieve a property using GlobalPropertyRegistry`() {
            val propertyRegistry = GlobalPropertyRegistry()
            val propDef = globalProperty("PI") {
                descriptor = "D"
                owner = "test/Math"
                getter = "PI"
            }
            propertyRegistry.registerProperty(propDef)

            val retrieved = propertyRegistry.getProperty("PI")
            assertNotNull(retrieved)
            assertEquals("PI", retrieved?.name)
        }

        @Test
        fun `returns null for unregistered property`() {
            val propertyRegistry = GlobalPropertyRegistry()
            val retrieved = propertyRegistry.getProperty("nonexistent")
            assertNull(retrieved)
        }
    }

    @Nested
    inner class GlobalRegistry {

        @Test
        fun `global registry contains println`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("println")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains listOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("listOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains setOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("setOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains bagOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("bagOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains orderedSetOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("orderedSetOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptyList`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("emptyList")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptySet`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("emptySet")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptyBag`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("emptyBag")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptyOrderedSet`() {
            val func = GlobalFunctionRegistry.GLOBAL.lookupFunction("emptyOrderedSet")
            assertNotNull(func)
        }

    }


}
