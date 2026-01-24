package com.mdeo.script.compiler

import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.stdlib.impl.collections.Bag
import com.mdeo.script.stdlib.impl.collections.OrderedSet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Bag and OrderedSet collection types.
 * 
 * These tests verify that:
 * - bagOf() and orderedSetOf() global functions work
 * - emptyBag() and emptyOrderedSet() global functions work
 * - Methods on Bag and OrderedSet collections can be called
 * - Method inheritance works correctly for these collection types
 */
class BagAndOrderedSetIntegrationTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class BagCreation {

        /**
         * Test: bagOf(1, 2, 3) creates a bag with 3 elements
         */
        @Test
        fun `can create bag with bagOf`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = bagType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "bagOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(3, intTypeIndex)
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
            assertEquals(3, (result as Bag<*>).size())
        }

        /**
         * Test: emptyBag() creates an empty bag
         */
        @Test
        fun `can create empty bag with emptyBag`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))

                function(
                    name = "testFunction",
                    returnType = bagType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "emptyBag",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = bagType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is Bag<*>)
            assertEquals(0, (result as Bag<*>).size())
        }

        /**
         * Test: bagOf(1, 1, 2) allows duplicates - size is 3
         */
        @Test
        fun `bag allows duplicate elements`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "bagOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(1, intTypeIndex),  // duplicate
                                        intLiteral(2, intTypeIndex)
                                    ),
                                    resultTypeIndex = bagType
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
            assertEquals(3, result)  // Duplicates are preserved in Bag
        }
    }

    @Nested
    inner class OrderedSetCreation {

        /**
         * Test: orderedSetOf(1, 2, 3) creates an ordered set with 3 elements
         */
        @Test
        fun `can create ordered set with orderedSetOf`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = orderedSetType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "orderedSetOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(3, intTypeIndex)
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
            assertEquals(3, (result as OrderedSet<*>).size())
        }

        /**
         * Test: emptyOrderedSet() creates an empty ordered set
         */
        @Test
        fun `can create empty ordered set with emptyOrderedSet`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))

                function(
                    name = "testFunction",
                    returnType = orderedSetType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "emptyOrderedSet",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = orderedSetType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is OrderedSet<*>)
            assertEquals(0, (result as OrderedSet<*>).size())
        }

        /**
         * Test: orderedSetOf(1, 1, 2) deduplicates - size is 2
         */
        @Test
        fun `ordered set removes duplicate elements`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "orderedSetOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(1, intTypeIndex),  // duplicate
                                        intLiteral(2, intTypeIndex)
                                    ),
                                    resultTypeIndex = orderedSetType
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
            assertEquals(2, result)  // Duplicates are removed in OrderedSet
        }
    }

    @Nested
    inner class BagMethodCalls {

        /**
         * Test: bagOf(1, 2, 3).size()
         */
        @Test
        fun `can call size on bag`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "bagOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(2, intTypeIndex),
                                        intLiteral(3, intTypeIndex)
                                    ),
                                    resultTypeIndex = bagType
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
         * Test: emptyBag().isEmpty()
         */
        @Test
        fun `emptyBag is empty`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))
                val boolTypeIndex = booleanType()

                function(
                    name = "testFunction",
                    returnType = boolTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "emptyBag",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = bagType
                                ),
                                member = "isEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        /**
         * Test: bagOf(1, 2, 3).notEmpty()
         */
        @Test
        fun `non-empty bag returns true for notEmpty`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))
                val intTypeIndex = intType()
                val boolTypeIndex = booleanType()

                function(
                    name = "testFunction",
                    returnType = boolTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "bagOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(2, intTypeIndex)
                                    ),
                                    resultTypeIndex = bagType
                                ),
                                member = "notEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }
    }

    @Nested
    inner class OrderedSetMethodCalls {

        /**
         * Test: orderedSetOf(1, 2, 3).size()
         */
        @Test
        fun `can call size on ordered set`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "orderedSetOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(2, intTypeIndex),
                                        intLiteral(3, intTypeIndex)
                                    ),
                                    resultTypeIndex = orderedSetType
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
         * Test: emptyOrderedSet().isEmpty()
         */
        @Test
        fun `emptyOrderedSet is empty`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val boolTypeIndex = booleanType()

                function(
                    name = "testFunction",
                    returnType = boolTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "emptyOrderedSet",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = orderedSetType
                                ),
                                member = "isEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        /**
         * Test: orderedSetOf(10, 20, 30).at(1) should return 20
         */
        @Test
        fun `can call at on ordered set`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()
                val anyTypeIndex = addType(ClassTypeRef("builtin.any", true))

                function(
                    name = "testFunction",
                    returnType = anyTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "orderedSetOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intTypeIndex),
                                        intLiteral(20, intTypeIndex),
                                        intLiteral(30, intTypeIndex)
                                    ),
                                    resultTypeIndex = orderedSetType
                                ),
                                member = "at",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex)
                                ),
                                resultTypeIndex = anyTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(20, result)
        }

        /**
         * Test: orderedSetOf(10, 20, 30).first() should return 10
         */
        @Test
        fun `can call first on ordered set`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()
                val anyTypeIndex = addType(ClassTypeRef("builtin.any", true))

                function(
                    name = "testFunction",
                    returnType = anyTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "orderedSetOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intTypeIndex),
                                        intLiteral(20, intTypeIndex),
                                        intLiteral(30, intTypeIndex)
                                    ),
                                    resultTypeIndex = orderedSetType
                                ),
                                member = "first",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = anyTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(10, result)
        }

        /**
         * Test: orderedSetOf(10, 20, 30).last() should return 30
         */
        @Test
        fun `can call last on ordered set`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()
                val anyTypeIndex = addType(ClassTypeRef("builtin.any", true))

                function(
                    name = "testFunction",
                    returnType = anyTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "orderedSetOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intTypeIndex),
                                        intLiteral(20, intTypeIndex),
                                        intLiteral(30, intTypeIndex)
                                    ),
                                    resultTypeIndex = orderedSetType
                                ),
                                member = "last",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = anyTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(30, result)
        }
    }

    @Nested
    inner class VariableAssignmentAndMethodCalls {

        /**
         * Test: val bag = bagOf(1, 2, 3); return bag.size()
         */
        @Test
        fun `can assign bag to variable and call methods`() {
            val ast = buildTypedAst {
                val bagType = addType(ClassTypeRef("builtin.Bag", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        varDecl("myBag", bagType,
                            functionCall(
                                name = "bagOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(3, intTypeIndex)
                                ),
                                resultTypeIndex = bagType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("myBag", bagType, 3),
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
         * Test: val orderedSet = orderedSetOf(10, 20, 30); return orderedSet.at(0)
         */
        @Test
        fun `can assign ordered set to variable and call at`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(ClassTypeRef("builtin.OrderedSet", false))
                val intTypeIndex = intType()
                val anyTypeIndex = addType(ClassTypeRef("builtin.any", true))

                function(
                    name = "testFunction",
                    returnType = anyTypeIndex,
                    body = listOf(
                        varDecl("myOrderedSet", orderedSetType,
                            functionCall(
                                name = "orderedSetOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(10, intTypeIndex),
                                    intLiteral(20, intTypeIndex),
                                    intLiteral(30, intTypeIndex)
                                ),
                                resultTypeIndex = orderedSetType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("myOrderedSet", orderedSetType, 3),
                                member = "at",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(0, intTypeIndex)
                                ),
                                resultTypeIndex = anyTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(10, result)
        }
    }
}
