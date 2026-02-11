package com.mdeo.modeltransformation.runtime

import com.mdeo.modeltransformation.compiler.VariableBinding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
            assertNull((context.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `empty context has no instances`() {
            assertFalse(context.testHasInstance("node1"))
            assertNull((context.variableScope.getVariable("node1") as? VariableBinding.InstanceBinding)?.vertexId)
        }

        @Test
        fun `empty context starts at scope index 1`() {
            assertEquals(1, context.scopeIndex)
        }

        @Test
        fun `getAllVariables returns empty map`() {
            assertTrue(context.testGetAllVariables().isEmpty())
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
            val newContext = context.testBindVariable("x", 42)
            
            assertNotNull((newContext.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
            assertEquals(42, (newContext.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `original context is unchanged after binding`() {
            context.testBindVariable("x", 42)
            
            assertNull((context.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `bindVariable with null value marks variable as bound`() {
            val newContext = context.testBindVariable("x", null)
            
            // Variable is bound but has null value
            assertNull((newContext.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
            // Check that variable exists in getAllVariables
            assertTrue(newContext.testGetAllVariables().containsKey("x"))
        }

        @Test
        fun `bindVariable overwrites existing value`() {
            val ctx1 = context.testBindVariable("x", 42)
            val ctx2 = ctx1.testBindVariable("x", 100)
            
            assertEquals(100, (ctx2.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `bindVariables binds multiple variables at once`() {
            val newContext = context.testBindVariables(mapOf("x" to 1, "y" to 2, "z" to 3))
            
            assertEquals(1, (newContext.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
            assertEquals(2, (newContext.variableScope.getVariable("y") as? VariableBinding.ValueBinding)?.value)
            assertEquals(3, (newContext.variableScope.getVariable("z") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `getAllVariables returns all bindings`() {
            val newContext = context
                .testBindVariable("a", 1)
                .testBindVariable("b", "hello")
            
            val allVars = newContext.testGetAllVariables()
            assertEquals(2, allVars.size)
            assertEquals(1, allVars["a"])
            assertEquals("hello", allVars["b"])
        }
    }

    @Nested
    inner class InstanceMappingTests {

        @Test
        fun `bindInstance creates new context with instance mapping`() {
            val newContext = context.testBindInstance("house", "vertex-123")
            
            assertTrue(newContext.testHasInstance("house"))
            assertEquals("vertex-123", (newContext.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId)
        }

        @Test
        fun `original context is unchanged after binding instance`() {
            context.testBindInstance("house", "vertex-123")
            
            assertFalse(context.testHasInstance("house"))
        }

        @Test
        fun `bindInstances binds multiple instances at once`() {
            val newContext = context.testBindInstances(
                mapOf("house" to "v1", "room" to "v2", "door" to "v3")
            )
            
            assertEquals("v1", (newContext.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId)
            assertEquals("v2", (newContext.variableScope.getVariable("room") as? VariableBinding.InstanceBinding)?.vertexId)
            assertEquals("v3", (newContext.variableScope.getVariable("door") as? VariableBinding.InstanceBinding)?.vertexId)
        }

        @Test
        fun `getAllInstances returns all mappings`() {
            val newContext = context
                .testBindInstance("a", "v1")
                .testBindInstance("b", "v2")
            
            val allInstances = newContext.getAllInstances()
            assertEquals(2, allInstances.size)
            assertEquals("v1", allInstances["a"])
            assertEquals("v2", allInstances["b"])
        }
    }

    @Nested
    inner class ScopeTests {

        @Test
        fun `enterScope increases scope index by 1`() {
            val childContext = context.enterScope()
            
            assertEquals(context.scopeIndex + 1, childContext.scopeIndex)
        }

        @Test
        fun `nested enterScope increases scope index further`() {
            val child = context.enterScope()
            val grandchild = child.enterScope()
            
            assertEquals(context.scopeIndex + 2, grandchild.scopeIndex)
        }

        @Test
        fun `exitScope returns to parent scope`() {
            val parent = context.testBindVariable("x", 1)
            val child = parent.enterScope()
            val restored = child.exitScope()
            
            assertEquals(parent.scopeIndex, restored.scopeIndex)
        }

        @Test
        fun `exitScope on root context returns same context`() {
            val result = context.exitScope()
            
            assertEquals(context.scopeIndex, result.scopeIndex)
        }

        @Test
        fun `child scope inherits parent variables`() {
            val parent = context.testBindVariable("x", 42)
            val child = parent.enterScope()
            
            assertEquals(42, (child.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `child scope can shadow parent variable`() {
            val parent = context.testBindVariable("x", 42)
            val child = parent.enterScope().testBindVariable("x", 100)
            
            assertEquals(100, (child.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
            assertEquals(42, (parent.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `child scope variable does not affect parent`() {
            val parent = context.testBindVariable("x", 42)
            val child = parent.enterScope().testBindVariable("y", 100)
            
            assertNotNull((child.variableScope.getVariable("y") as? VariableBinding.ValueBinding)?.value)
            assertNull((parent.variableScope.getVariable("y") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `child scope inherits parent instances`() {
            val parent = context.testBindInstance("house", "v1")
            val child = parent.enterScope()
            
            assertEquals("v1", (child.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId)
        }

        @Test
        fun `getAllVariables includes parent scope variables`() {
            val parent = context.testBindVariable("x", 1)
            val child = parent.enterScope().testBindVariable("y", 2)
            
            val allVars = child.testGetAllVariables()
            assertEquals(2, allVars.size)
            assertEquals(1, allVars["x"])
            assertEquals(2, allVars["y"])
        }

        @Test
        fun `getAllVariables child shadows parent`() {
            val parent = context.testBindVariable("x", 1)
            val child = parent.enterScope().testBindVariable("x", 2)
            
            val allVars = child.testGetAllVariables()
            assertEquals(1, allVars.size)
            assertEquals(2, allVars["x"])
        }

        @Test
        fun `getAllInstances includes parent scope instances`() {
            val parent = context.testBindInstance("a", "v1")
            val child = parent.enterScope().testBindInstance("b", "v2")
            
            val allInstances = child.getAllInstances()
            assertEquals(2, allInstances.size)
            assertEquals("v1", allInstances["a"])
            assertEquals("v2", allInstances["b"])
        }
    }

    @Nested
    inner class CompanionObjectTests {

        @Test
        fun `empty creates empty context at scope index 1`() {
            val emptyContext = TransformationExecutionContext.empty()
            
            assertEquals(1, emptyContext.scopeIndex)
            assertTrue(emptyContext.testGetAllVariables().isEmpty())
            assertTrue(emptyContext.getAllInstances().isEmpty())
        }
    }

    @Nested
    inner class MixedTypeTests {

        @Test
        fun `can store various types as variable values`() {
            val ctx = context
                .testBindVariable("int", 42)
                .testBindVariable("double", 3.14)
                .testBindVariable("string", "hello")
                .testBindVariable("boolean", true)
                .testBindVariable("list", listOf(1, 2, 3))
                .testBindVariable("null", null)
            
            assertEquals(42, (ctx.variableScope.getVariable("int") as? VariableBinding.ValueBinding)?.value)
            assertEquals(3.14, (ctx.variableScope.getVariable("double") as? VariableBinding.ValueBinding)?.value)
            assertEquals("hello", (ctx.variableScope.getVariable("string") as? VariableBinding.ValueBinding)?.value)
            assertEquals(true, (ctx.variableScope.getVariable("boolean") as? VariableBinding.ValueBinding)?.value)
            assertEquals(listOf(1, 2, 3), (ctx.variableScope.getVariable("list") as? VariableBinding.ValueBinding)?.value)
            assertNull((ctx.variableScope.getVariable("null") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `can store various types as instance IDs`() {
            val ctx = context
                .testBindInstance("node1", "string-id")
                .testBindInstance("node2", 12345L)
                .testBindInstance("node3", java.util.UUID.randomUUID())
            
            assertEquals("string-id", (ctx.variableScope.getVariable("node1") as? VariableBinding.InstanceBinding)?.vertexId)
            assertEquals(12345L, (ctx.variableScope.getVariable("node2") as? VariableBinding.InstanceBinding)?.vertexId)
            assertTrue((ctx.variableScope.getVariable("node3") as? VariableBinding.InstanceBinding)?.vertexId is java.util.UUID)
        }
    }
}
