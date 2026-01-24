package com.mdeo.script.compiler.registry

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
                }
            }
            registry.registerFunction(funcDef)

            val retrieved = registry.getFunction("testFunc")
            assertNotNull(retrieved)
            assertEquals("testFunc", retrieved.name)
        }

        @Test
        fun `returns null for unregistered function`() {
            val retrieved = registry.getFunction("nonexistent")
            assertNull(retrieved)
        }

        @Test
        fun `can register multiple functions`() {
            registry.registerFunction(globalFunction("funcA") {
                staticOverload("funcA():builtin.int") {
                    descriptor = "()I"
                    owner = "test/Helper"
                    jvmMethod = "funcA"
                }
            })
            registry.registerFunction(globalFunction("funcB") {
                staticOverload("funcB():builtin.string") {
                    descriptor = "()Ljava/lang/String;"
                    owner = "test/Helper"
                    jvmMethod = "funcB"
                }
            })

            assertNotNull(registry.getFunction("funcA"))
            assertNotNull(registry.getFunction("funcB"))
        }
    }

    @Nested
    inner class OverloadLookup {

        @Test
        fun `can lookup function with overload key`() {
            val funcDef = globalFunction("println") {
                staticOverload("") {
                    descriptor = "(Ljava/lang/String;)V"
                    owner = "test/Helper"
                    jvmMethod = "println"
                }
            }
            registry.registerFunction(funcDef)

            val method = registry.lookupMethod("println", "")
            assertNotNull(method)
            assertEquals("", method.overloadKey)
        }

        @Test
        fun `can lookup function with multiple overloads`() {
            val funcDef = globalFunction("format") {
                staticOverload("builtin.string") {
                    descriptor = "(Ljava/lang/String;)Ljava/lang/String;"
                    owner = "test/Helper"
                    jvmMethod = "format1"
                }
                staticOverload("builtin.string,builtin.string") {
                    descriptor = "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
                    owner = "test/Helper"
                    jvmMethod = "format2"
                }
            }
            registry.registerFunction(funcDef)

            val method1 = registry.lookupMethod("format", "builtin.string")
            val method2 = registry.lookupMethod("format", "builtin.string,builtin.string")

            assertNotNull(method1)
            assertEquals("format1", method1.jvmMethodName)
            assertNotNull(method2)
            assertEquals("format2", method2.jvmMethodName)
        }

        @Test
        fun `returns null for nonexistent overload`() {
            val funcDef = globalFunction("println") {
                staticOverload("") {
                    descriptor = "(Ljava/lang/String;)V"
                    owner = "test/Helper"
                    jvmMethod = "println"
                }
            }
            registry.registerFunction(funcDef)

            val method = registry.lookupMethod("println", "builtin.int")
            assertNull(method)
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
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("println")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains listOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("listOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains setOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("setOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains bagOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("bagOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains orderedSetOf`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("orderedSetOf")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptyList`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("emptyList")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptySet`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("emptySet")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptyBag`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("emptyBag")
            assertNotNull(func)
        }

        @Test
        fun `global registry contains emptyOrderedSet`() {
            val func = GlobalFunctionRegistry.GLOBAL.getFunction("emptyOrderedSet")
            assertNotNull(func)
        }

        @Test
        fun `can lookup println overload`() {
            val method = GlobalFunctionRegistry.GLOBAL.lookupMethod(
                "println",
                ""
            )
            assertNotNull(method)
            assertEquals("println", method.jvmMethodName)
        }

        @Test
        fun `listOf is varargs`() {
            val method = GlobalFunctionRegistry.GLOBAL.lookupMethod(
                "listOf",
                ""
            )
            assertNotNull(method)
            assertTrue(method.isVarArgs)
        }
    }

    @Nested
    inner class GlobalFallback {

        @Test
        fun `local registry falls back to global for functions`() {
            val func = registry.getFunctionOrGlobal("println")
            assertNotNull(func)
        }

        @Test
        fun `local registry prefers local over global`() {
            val localFunc = globalFunction("println") {
                staticOverload("builtin.int") {
                    descriptor = "(I)V"
                    owner = "local/Helper"
                    jvmMethod = "localPrintln"
                }
            }
            registry.registerFunction(localFunc)

            val method = registry.lookupMethod("println", "builtin.int")
            assertNotNull(method)
            assertEquals("localPrintln", method.jvmMethodName)
        }
    }
}
