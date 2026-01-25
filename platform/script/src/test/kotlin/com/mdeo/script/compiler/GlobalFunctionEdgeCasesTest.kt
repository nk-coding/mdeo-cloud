package com.mdeo.script.compiler

import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.compiler.registry.function.GlobalFunctionRegistry
import com.mdeo.script.stdlib.impl.collections.Bag
import com.mdeo.script.stdlib.impl.collections.OrderedSet
import com.mdeo.script.stdlib.impl.collections.ScriptList
import com.mdeo.script.stdlib.impl.collections.ScriptSet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Edge case tests for global function infrastructure.
 *
 * These tests focus on corner cases and potential bugs:
 * - Empty overload key handling
 * - Null value handling in collections
 * - Large varargs arrays
 * - Chaining operations on returned collections
 * - Type inference for empty collections
 * - Missing overload error handling
 */
class GlobalFunctionEdgeCasesTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class NullValueHandling {

        /**
         * Test that collections can handle all null values.
         */
        @Test
        fun `listOf with all null values`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val nullableInt = intNullableType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    nullLiteral(nullableInt),
                                    nullLiteral(nullableInt),
                                    nullLiteral(nullableInt)
                                ),
                                resultTypeIndex = listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(3, result.size())
            // Verify all elements are null
            for (i in 0 until result.size()) {
                assertNull(result.at(i))
            }
        }

        /**
         * Test setOf handling of null - set should not deduplicate nulls in a special way.
         */
        @Test
        fun `setOf with multiple null values creates set with one null`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin.Set", false))
                val nullableString = stringNullableType()

                function(
                    name = "testFunction",
                    returnType = setType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "setOf",
                                overload = "",
                                arguments = listOf(
                                    nullLiteral(nullableString),
                                    stringLiteral("a", stringType()),
                                    nullLiteral(nullableString)
                                ),
                                resultTypeIndex = setType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptSet<*>)
            // Set should deduplicate nulls - should have null and "a" = 2 elements
            assertEquals(2, result.size())
        }
    }

    @Nested
    inner class LargeVarargsArrays {

        /**
         * Test that large varargs arrays are handled correctly.
         */
        @Test
        fun `listOf with many elements`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intTypeIndex = intType()

                // Create 100 integer arguments
                val arguments = (0 until 100).map { i ->
                    intLiteral(i, intTypeIndex)
                }

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = arguments,
                                resultTypeIndex = listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(100, result.size())
            
            // Verify values
            for (i in 0 until 100) {
                assertEquals(i, result.at(i))
            }
        }

        /**
         * Test that exactly 0 arguments works for varargs.
         */
        @Test
        fun `listOf with zero arguments creates empty list`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(0, result.size())
        }

        /**
         * Test exactly 1 argument for varargs.
         */
        @Test
        fun `listOf with single argument creates single element list`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(intLiteral(42, intTypeIndex)),
                                resultTypeIndex = listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(1, result.size())
            assertEquals(42, result.at(0))
        }
    }

    @Nested
    inner class CollectionOperationsChaining {

        /**
         * Test that we can call methods on returned collections.
         */
        @Test
        fun `listOf result can have size called on it`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(2, intTypeIndex),
                                        intLiteral(3, intTypeIndex)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)
        }

        /**
         * Test emptyList with method chaining.
         */
        @Test
        fun `emptyList result has size zero when queried`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "emptyList",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = listType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }
    }

    @Nested
    inner class PrimitiveBoxingEdgeCases {

        /**
         * Test that different primitive types are boxed correctly in mixed collection.
         */
        @Test
        fun `listOf boxes all primitive types correctly`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(42, intType()),
                                    longLiteral(123L, longType()),
                                    floatLiteral(1.5f, floatType()),
                                    doubleLiteral(3.14, doubleType()),
                                    booleanLiteral(true, booleanType())
                                ),
                                resultTypeIndex = listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(5, result.size())
            
            // Verify all values were correctly boxed
            assertEquals(42, result.at(0))
            assertEquals(123L, result.at(1))
            assertEquals(1.5f, result.at(2))
            assertEquals(3.14, result.at(3))
            assertEquals(true, result.at(4))
        }
    }

    @Nested
    inner class OrderedSetBehavior {

        /**
         * Test that OrderedSet maintains insertion order.
         */
        @Test
        fun `orderedSetOf maintains insertion order`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))

                function(
                    name = "testFunction",
                    returnType = orderedSetType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "orderedSetOf",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("z", stringType()),
                                    stringLiteral("a", stringType()),
                                    stringLiteral("m", stringType())
                                ),
                                resultTypeIndex = orderedSetType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is OrderedSet<*>)
            assertEquals(3, result.size())
            
            // Verify insertion order is maintained
            val iterator = result.iterator()
            assertEquals("z", iterator.next())
            assertEquals("a", iterator.next())
            assertEquals("m", iterator.next())
        }

        /**
         * Test that OrderedSet deduplicates but maintains first occurrence order.
         */
        @Test
        fun `orderedSetOf deduplicates while maintaining order`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))

                function(
                    name = "testFunction",
                    returnType = orderedSetType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "orderedSetOf",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("a", stringType()),
                                    stringLiteral("b", stringType()),
                                    stringLiteral("a", stringType()),  // duplicate
                                    stringLiteral("c", stringType())
                                ),
                                resultTypeIndex = orderedSetType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is OrderedSet<*>)
            val orderedSet = result as OrderedSet<*>
            assertEquals(3, orderedSet.size())
            
            // Verify order: a, b, c (first occurrence of 'a')
            val iterator = orderedSet.iterator()
            assertEquals("a", iterator.next())
            assertEquals("b", iterator.next())
            assertEquals("c", iterator.next())
        }
    }

    @Nested
    inner class BagBehavior {

        /**
         * Test that Bag allows duplicates.
         */
        @Test
        fun `bagOf allows duplicates`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))

                function(
                    name = "testFunction",
                    returnType = bagType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "bagOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intType()),
                                    intLiteral(1, intType()),
                                    intLiteral(2, intType())
                                ),
                                resultTypeIndex = bagType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is Bag<*>)
            val bag = result as Bag<*>
            assertEquals(3, bag.size())  // Bag should have 3 elements including duplicates
        }
    }

    @Nested
    inner class FallbackToLegacyPath {

        /**
         * When an unknown function is called (not in registry), the compiler
         * should fall back to the legacy function call path which looks for
         * local or imported functions.
         */
        @Test
        fun `calling local function does not conflict with global registry`() {
            val ast = buildTypedAst {
                val intTypeIndex = intType()

                // Define a local function named something different from stdlib
                function(
                    name = "myHelper",
                    returnType = intTypeIndex,
                    body = listOf(returnStmt(intLiteral(42, intTypeIndex)))
                )

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "myHelper",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }
    }
}
