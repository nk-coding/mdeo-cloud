package com.mdeo.modeltransformation.runtime.match

import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.testBindVariable
import com.mdeo.modeltransformation.runtime.testBindInstance
import com.mdeo.modeltransformation.runtime.testGetAllVariables
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

class MatchResultTest {

    @Nested
    inner class MatchedTests {

        @Test
        fun `Matched with empty bindings`() {
            val result = MatchResult.Matched()
            
            assertTrue(result.isMatched())
            assertFalse(result.isNoMatch())
            assertTrue(result.bindings.isEmpty())
            assertTrue(result.instanceMappings.isEmpty())
        }

        @Test
        fun `Matched with variable bindings`() {
            val result = MatchResult.Matched(
                bindings = mapOf("x" to 42, "name" to "test")
            )
            
            assertEquals(42, result.bindings["x"])
            assertEquals("test", result.bindings["name"])
        }

        @Test
        fun `Matched with instance mappings`() {
            val result = MatchResult.Matched(
                instanceMappings = mapOf("house" to "v-123", "room" to "v-456")
            )
            
            assertEquals("v-123", result.instanceMappings["house"])
            assertEquals("v-456", result.instanceMappings["room"])
        }

        @Test
        fun `Matched with matched node IDs`() {
            val result = MatchResult.Matched(
                matchedNodeIds = setOf("v1", "v2", "v3")
            )
            
            assertEquals(3, result.matchedNodeIds.size)
            assertTrue(result.matchedNodeIds.contains("v2"))
        }

        @Test
        fun `matchedOrNull returns Matched`() {
            val result: MatchResult = MatchResult.Matched()
            
            val matched = result.matchedOrNull()
            assertIs<MatchResult.Matched>(matched)
        }
    }

    @Nested
    inner class ApplyToContextTests {

        @Test
        fun `applyTo empty Matched does not change context`() {
            val context = TransformationExecutionContext.empty()
            val matched = MatchResult.Matched()
            
            val result = matched.applyTo(context)
            
            assertTrue(result.testGetAllVariables().isEmpty())
            assertTrue(result.getAllInstances().isEmpty())
        }

        @Test
        fun `applyTo adds variable bindings to context`() {
            val context = TransformationExecutionContext.empty()
            val matched = MatchResult.Matched(
                bindings = mapOf("x" to 1, "y" to 2)
            )
            
            val result = matched.applyTo(context)
            
            assertEquals(1, (result.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
            assertEquals(2, (result.variableScope.getVariable("y") as? VariableBinding.ValueBinding)?.value)
        }

        @Test
        fun `applyTo adds instance mappings to context`() {
            val context = TransformationExecutionContext.empty()
            val matched = MatchResult.Matched(
                instanceMappings = mapOf("house" to "v-123")
            )
            
            val result = matched.applyTo(context)
            
            assertEquals("v-123", (result.variableScope.getVariable("house") as? VariableBinding.InstanceBinding)?.vertexId)
        }

        @Test
        fun `applyTo preserves existing context bindings`() {
            val context = TransformationExecutionContext.empty()
                .testBindVariable("existing", 100)
                .testBindInstance("existingNode", "v-000")
            
            val matched = MatchResult.Matched(
                bindings = mapOf("new" to 200),
                instanceMappings = mapOf("newNode" to "v-111")
            )
            
            val result = matched.applyTo(context)
            
            assertEquals(100, (result.variableScope.getVariable("existing") as? VariableBinding.ValueBinding)?.value)
            assertEquals(200, (result.variableScope.getVariable("new") as? VariableBinding.ValueBinding)?.value)
            assertEquals("v-000", (result.variableScope.getVariable("existingNode") as? VariableBinding.InstanceBinding)?.vertexId)
            assertEquals("v-111", (result.variableScope.getVariable("newNode") as? VariableBinding.InstanceBinding)?.vertexId)
        }

        @Test
        fun `applyTo can override existing bindings`() {
            val context = TransformationExecutionContext.empty()
                .testBindVariable("x", 100)
            
            val matched = MatchResult.Matched(
                bindings = mapOf("x" to 200)
            )
            
            val result = matched.applyTo(context)
            
            assertEquals(200, (result.variableScope.getVariable("x") as? VariableBinding.ValueBinding)?.value)
        }
    }

    @Nested
    inner class NoMatchTests {

        @Test
        fun `NoMatch with no reason`() {
            val result = MatchResult.NoMatch()
            
            assertFalse(result.isMatched())
            assertTrue(result.isNoMatch())
            assertNull(result.reason)
        }

        @Test
        fun `NoMatch with reason`() {
            val result = MatchResult.NoMatch(reason = "No vertex of type House found")
            
            assertEquals("No vertex of type House found", result.reason)
        }

        @Test
        fun `matchedOrNull returns null for NoMatch`() {
            val result: MatchResult = MatchResult.NoMatch()
            
            assertNull(result.matchedOrNull())
        }
    }

    @Nested
    inner class ExtensionFunctionTests {

        @Test
        fun `isMatched returns true only for Matched`() {
            val matched: MatchResult = MatchResult.Matched()
            val noMatch: MatchResult = MatchResult.NoMatch()
            
            assertTrue(matched.isMatched())
            assertFalse(noMatch.isMatched())
        }

        @Test
        fun `isNoMatch returns true only for NoMatch`() {
            val matched: MatchResult = MatchResult.Matched()
            val noMatch: MatchResult = MatchResult.NoMatch()
            
            assertFalse(matched.isNoMatch())
            assertTrue(noMatch.isNoMatch())
        }
    }

    @Nested
    inner class ComplexBindingTests {

        @Test
        fun `Matched can store null values in bindings`() {
            val result = MatchResult.Matched(
                bindings = mapOf("nullable" to null)
            )
            
            assertTrue(result.bindings.containsKey("nullable"))
            assertNull(result.bindings["nullable"])
        }

        @Test
        fun `Matched can store various types in bindings`() {
            val result = MatchResult.Matched(
                bindings = mapOf(
                    "int" to 42,
                    "double" to 3.14,
                    "string" to "hello",
                    "boolean" to true,
                    "list" to listOf(1, 2, 3)
                )
            )
            
            assertEquals(42, result.bindings["int"])
            assertEquals(3.14, result.bindings["double"])
            assertEquals("hello", result.bindings["string"])
            assertEquals(true, result.bindings["boolean"])
            assertEquals(listOf(1, 2, 3), result.bindings["list"])
        }

        @Test
        fun `applyTo with null values in bindings`() {
            val context = TransformationExecutionContext.empty()
            val matched = MatchResult.Matched(
                bindings = mapOf("nullable" to null)
            )
            
            val result = matched.applyTo(context)
            
            assertTrue(result.testGetAllVariables().containsKey("nullable"))
            assertNull((result.variableScope.getVariable("nullable") as? VariableBinding.ValueBinding)?.value)
        }
    }
}
