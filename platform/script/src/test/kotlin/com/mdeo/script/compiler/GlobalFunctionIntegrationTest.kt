package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.script.stdlib.impl.collections.ScriptList
import com.mdeo.script.stdlib.impl.collections.ScriptSet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for global function infrastructure.
 * 
 * Tests end-to-end scenarios including:
 * - Variable assignment with global functions
 * - Operations on assigned collection variables
 * - Chaining operations on global function results
 * - Combining global functions with other language features
 * 
 * Collection types (stdlib.List, stdlib.Set, stdlib.Bag, stdlib.OrderedSet) are 
 * now fully registered in the TypeRegistry with their method definitions.
 * See BagAndOrderedSetIntegrationTest for additional collection tests.
 */
class GlobalFunctionIntegrationTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class VariableAssignment {

        /**
         * Test: val myList = listOf(1, 2, 3)
         * Verifies that listOf result can be assigned to a variable.
         */
        @Test
        fun `can assign listOf result to variable`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        varDecl("myList", listType, 
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(3, intTypeIndex)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        returnStmt(identifier("myList", listType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(3, result.size())
            assertEquals(1, result.at(0))
            assertEquals(2, result.at(1))
            assertEquals(3, result.at(2))
        }

        /**
         * Test: val mySet = setOf("a", "b", "c")
         */
        @Test
        fun `can assign setOf result to variable`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val stringTypeIndex = stringType()

                function(
                    name = "testFunction",
                    returnType = setType,
                    body = listOf(
                        varDecl("mySet", setType,
                            functionCall(
                                name = "setOf",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("a", stringTypeIndex),
                                    stringLiteral("b", stringTypeIndex),
                                    stringLiteral("c", stringTypeIndex)
                                ),
                                resultTypeIndex = setType
                            )
                        ),
                        returnStmt(identifier("mySet", setType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptSet<*>)
            assertEquals(3, result.size())
        }

        /**
         * Test: val emptyCollection = emptyList<String>()
         */
        @Test
        fun `can assign emptyList result to variable`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        varDecl("emptyCollection", listType,
                            functionCall(
                                name = "emptyList",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = listType
                            )
                        ),
                        returnStmt(identifier("emptyCollection", listType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptList<*>)
            assertEquals(0, result.size())
        }

        /**
         * Test: val emptyS = emptySet<Int>()
         */
        @Test
        fun `can assign emptySet result to variable`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin", "Set", false))

                function(
                    name = "testFunction",
                    returnType = setType,
                    body = listOf(
                        varDecl("emptyS", setType,
                            functionCall(
                                name = "emptySet",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = setType
                            )
                        ),
                        returnStmt(identifier("emptyS", setType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptSet<*>)
            assertEquals(0, result.size())
        }
    }

    @Nested
    inner class OperationsOnAssignedVariables {

        /**
         * Test: val myList = listOf(1, 2, 3); return myList.size()
         */
        @Test
        fun `can call size on assigned list variable`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        varDecl("myList", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(3, intTypeIndex)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("myList", listType, 3),
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
         * Test: val mySet = setOf(1, 2, 2, 3); return mySet.size()
         * Set should deduplicate, so size is 3.
         */
        @Test
        fun `can call size on assigned set variable with deduplication`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        varDecl("mySet", setType,
                            functionCall(
                                name = "setOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(2, intTypeIndex),  // duplicate
                                    intLiteral(3, intTypeIndex)
                                ),
                                resultTypeIndex = setType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("mySet", setType, 3),
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
            assertEquals(3, result)  // 1, 2, 3 - duplicate 2 removed
        }
    }

    @Nested
    inner class ChainingOperations {

        /**
         * Test: listOf(1, 2, 3).size() - direct chaining without variable
         */
        @Test
        fun `can chain size call directly on listOf`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
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
         * Test: setOf(1, 2, 3).size()
         */
        @Test
        fun `can chain size call directly on setOf`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "setOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intTypeIndex),
                                        intLiteral(2, intTypeIndex),
                                        intLiteral(3, intTypeIndex)
                                    ),
                                    resultTypeIndex = setType
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
         * Test: emptyList().size() == 0
         */
        @Test
        fun `emptyList has size zero`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
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

        /**
         * Test: emptySet().size() == 0
         */
        @Test
        fun `emptySet has size zero`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "emptySet",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = setType
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
    inner class MixedTypesInCollections {

        /**
         * Test that listOf can hold different types.
         */
        @Test
        fun `listOf with mixed types works`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        varDecl("mixed", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(42, intTypeIndex),
                                    stringLiteral("hello", stringType()),
                                    booleanLiteral(true, booleanType())
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("mixed", listType, 3),
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
    }

    @Nested
    inner class MultipleCollectionsInFunction {

        /**
         * Test that we can have multiple collection variables in one function.
         */
        @Test
        fun `multiple collection variables work correctly`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val intTypeIndex = intType()

                function(
                    name = "testFunction",
                    returnType = intTypeIndex,
                    body = listOf(
                        // Create list with 3 elements
                        varDecl("myList", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intTypeIndex),
                                    intLiteral(2, intTypeIndex),
                                    intLiteral(3, intTypeIndex)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        // Create set with 2 elements (after deduplication)
                        varDecl("mySet", setType,
                            functionCall(
                                name = "setOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(10, intTypeIndex),
                                    intLiteral(10, intTypeIndex),  // duplicate
                                    intLiteral(20, intTypeIndex)
                                ),
                                resultTypeIndex = setType
                            )
                        ),
                        // Return list.size() + set.size() = 3 + 2 = 5
                        returnStmt(
                            binaryExpr(
                                memberCall(
                                    expression = identifier("myList", listType, 3),
                                    member = "size",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = intTypeIndex
                                ),
                                "+",
                                memberCall(
                                    expression = identifier("mySet", setType, 3),
                                    member = "size",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = intTypeIndex
                                ),
                                intTypeIndex
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(5, result)
        }
    }
}
