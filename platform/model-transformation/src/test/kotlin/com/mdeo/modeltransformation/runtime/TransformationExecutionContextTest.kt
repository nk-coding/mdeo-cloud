package com.mdeo.modeltransformation.runtime

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransformationExecutionContextTest {

    private lateinit var context: TransformationExecutionContext

    @BeforeEach
    fun setUp() {
        context = TransformationExecutionContext.empty()
    }

    @Nested
    inner class EmptyContextTests {

        @Test
        fun `empty context has no variables`() {
            assertFalse(context.hasVariable("x"))
            assertNull(context.lookupVariable("x"))
        }

        @Test
        fun `empty context has no instances`() {
            assertFalse(context.hasInstance("node1"))
            assertNull(context.lookupInstance("node1"))
        }

        @Test
        fun `empty context has depth 0`() {
            assertEquals(0, context.getScopeDepth())
        }

        @Test
        fun `getAllVariables returns empty map`() {
            assertTrue(context.getAllVariables().isEmpty())
        }

        @Test
        fun `getAllInstances returns empty map`() {
            assertTrue(context.getAllInstances().isEmpty())
        }
    }

    @Nested
    inner class VariableBindingTests {

        @Test
        fun `bindVariable creates new context with variable`() {
            val newContext = context.bindVariable("x", 42)
            
            assertTrue(newContext.hasVariable("x"))
            assertEquals(42, newContext.lookupVariable("x"))
        }

        @Test
        fun `original context is unchanged after binding`() {
            context.bindVariable("x", 42)
            
            assertFalse(context.hasVariable("x"))
        }

        @Test
        fun `bindVariable with null value`() {
            val newContext = context.bindVariable("x", null)
            
            assertTrue(newContext.hasVariable("x"))
            assertNull(newContext.lookupVariable("x"))
        }

        @Test
        fun `bindVariable overwrites existing value`() {
            val ctx1 = context.bindVariable("x", 42)
            val ctx2 = ctx1.bindVariable("x", 100)
            
            assertEquals(100, ctx2.lookupVariable("x"))
        }

        @Test
        fun `bindVariables binds multiple variables at once`() {
            val newContext = context.bindVariables(mapOf("x" to 1, "y" to 2, "z" to 3))
            
            assertEquals(1, newContext.lookupVariable("x"))
            assertEquals(2, newContext.lookupVariable("y"))
            assertEquals(3, newContext.lookupVariable("z"))
        }

        @Test
        fun `getAllVariables returns all bindings`() {
            val newContext = context
                .bindVariable("a", 1)
                .bindVariable("b", "hello")
            
            val allVars = newContext.getAllVariables()
            assertEquals(2, allVars.size)
            assertEquals(1, allVars["a"])
            assertEquals("hello", allVars["b"])
        }
    }

    @Nested
    inner class InstanceMappingTests {

        @Test
        fun `bindInstance creates new context with instance mapping`() {
            val newContext = context.bindInstance("house", "vertex-123")
            
            assertTrue(newContext.hasInstance("house"))
            assertEquals("vertex-123", newContext.lookupInstance("house"))
        }

        @Test
        fun `original context is unchanged after binding instance`() {
            context.bindInstance("house", "vertex-123")
            
            assertFalse(context.hasInstance("house"))
        }

        @Test
        fun `bindInstances binds multiple instances at once`() {
            val newContext = context.bindInstances(
                mapOf("house" to "v1", "room" to "v2", "door" to "v3")
            )
            
            assertEquals("v1", newContext.lookupInstance("house"))
            assertEquals("v2", newContext.lookupInstance("room"))
            assertEquals("v3", newContext.lookupInstance("door"))
        }

        @Test
        fun `getAllInstances returns all mappings`() {
            val newContext = context
                .bindInstance("a", "v1")
                .bindInstance("b", "v2")
            
            val allInstances = newContext.getAllInstances()
            assertEquals(2, allInstances.size)
            assertEquals("v1", allInstances["a"])
            assertEquals("v2", allInstances["b"])
        }
    }

    @Nested
    inner class ScopeTests {

        @Test
        fun `enterScope increases depth by 1`() {
            val childContext = context.enterScope()
            
            assertEquals(1, childContext.getScopeDepth())
        }

        @Test
        fun `nested enterScope increases depth further`() {
            val child = context.enterScope()
            val grandchild = child.enterScope()
            
            assertEquals(2, grandchild.getScopeDepth())
        }

        @Test
        fun `exitScope returns to parent scope`() {
            val parent = context.bindVariable("x", 1)
            val child = parent.enterScope()
            val restored = child.exitScope()
            
            assertEquals(0, restored.getScopeDepth())
        }

        @Test
        fun `exitScope on root context returns same context`() {
            val result = context.exitScope()
            
            assertEquals(0, result.getScopeDepth())
        }

        @Test
        fun `child scope inherits parent variables`() {
            val parent = context.bindVariable("x", 42)
            val child = parent.enterScope()
            
            assertEquals(42, child.lookupVariable("x"))
        }

        @Test
        fun `child scope can shadow parent variable`() {
            val parent = context.bindVariable("x", 42)
            val child = parent.enterScope().bindVariable("x", 100)
            
            assertEquals(100, child.lookupVariable("x"))
            assertEquals(42, parent.lookupVariable("x"))
        }

        @Test
        fun `child scope variable does not affect parent`() {
            val parent = context.bindVariable("x", 42)
            val child = parent.enterScope().bindVariable("y", 100)
            
            assertTrue(child.hasVariable("y"))
            assertFalse(parent.hasVariable("y"))
        }

        @Test
        fun `child scope inherits parent instances`() {
            val parent = context.bindInstance("house", "v1")
            val child = parent.enterScope()
            
            assertEquals("v1", child.lookupInstance("house"))
        }

        @Test
        fun `getAllVariables includes parent scope variables`() {
            val parent = context.bindVariable("x", 1)
            val child = parent.enterScope().bindVariable("y", 2)
            
            val allVars = child.getAllVariables()
            assertEquals(2, allVars.size)
            assertEquals(1, allVars["x"])
            assertEquals(2, allVars["y"])
        }

        @Test
        fun `getAllVariables child shadows parent`() {
            val parent = context.bindVariable("x", 1)
            val child = parent.enterScope().bindVariable("x", 2)
            
            val allVars = child.getAllVariables()
            assertEquals(1, allVars.size)
            assertEquals(2, allVars["x"])
        }

        @Test
        fun `getAllInstances includes parent scope instances`() {
            val parent = context.bindInstance("a", "v1")
            val child = parent.enterScope().bindInstance("b", "v2")
            
            val allInstances = child.getAllInstances()
            assertEquals(2, allInstances.size)
            assertEquals("v1", allInstances["a"])
            assertEquals("v2", allInstances["b"])
        }
    }

    @Nested
    inner class CompanionObjectTests {

        @Test
        fun `empty creates empty context`() {
            val emptyContext = TransformationExecutionContext.empty()
            
            assertEquals(0, emptyContext.getScopeDepth())
            assertTrue(emptyContext.getAllVariables().isEmpty())
            assertTrue(emptyContext.getAllInstances().isEmpty())
        }
    }

    @Nested
    inner class MixedTypeTests {

        @Test
        fun `can store various types as variable values`() {
            val ctx = context
                .bindVariable("int", 42)
                .bindVariable("double", 3.14)
                .bindVariable("string", "hello")
                .bindVariable("boolean", true)
                .bindVariable("list", listOf(1, 2, 3))
                .bindVariable("null", null)
            
            assertEquals(42, ctx.lookupVariable("int"))
            assertEquals(3.14, ctx.lookupVariable("double"))
            assertEquals("hello", ctx.lookupVariable("string"))
            assertEquals(true, ctx.lookupVariable("boolean"))
            assertEquals(listOf(1, 2, 3), ctx.lookupVariable("list"))
            assertNull(ctx.lookupVariable("null"))
        }

        @Test
        fun `can store various types as instance IDs`() {
            val ctx = context
                .bindInstance("node1", "string-id")
                .bindInstance("node2", 12345L)
                .bindInstance("node3", java.util.UUID.randomUUID())
            
            assertEquals("string-id", ctx.lookupInstance("node1"))
            assertEquals(12345L, ctx.lookupInstance("node2"))
            assertTrue(ctx.lookupInstance("node3") is java.util.UUID)
        }
    }
}
