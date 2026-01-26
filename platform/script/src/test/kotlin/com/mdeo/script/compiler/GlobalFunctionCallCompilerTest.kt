package com.mdeo.script.compiler

import com.mdeo.script.stdlib.impl.collections.Bag
import com.mdeo.script.stdlib.impl.collections.OrderedSet
import com.mdeo.script.stdlib.impl.collections.ScriptList
import com.mdeo.script.stdlib.impl.collections.ScriptSet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for global stdlib function compilation and execution.
 *
 * Tests cover:
 * - println function
 * - Collection factory functions (listOf, setOf, bagOf, orderedSetOf)
 * - Empty collection functions (emptyList, emptySet, emptyBag, emptyOrderedSet)
 */
class GlobalFunctionCallCompilerTest {

    private val helper = CompilerTestHelper()

    @Nested
    inner class CollectionFactoryFunctions {

        @Test
        fun `listOf creates list from varargs`() {
            val ast = buildTypedAst {
                val listType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.List", false)
                )

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intType()),
                                    intLiteral(2, intType()),
                                    intLiteral(3, intType())
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
            assertEquals(3, (result as ScriptList<*>).size())
        }

        @Test
        fun `setOf creates set from varargs`() {
            val ast = buildTypedAst {
                val setType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.Set", false)
                )

                function(
                    name = "testFunction",
                    returnType = setType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "setOf",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("a", stringType()),
                                    stringLiteral("b", stringType()),
                                    stringLiteral("a", stringType())
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
            assertEquals(2, (result as ScriptSet<*>).size())
        }

        @Test
        fun `bagOf creates bag from varargs`() {
            val ast = buildTypedAst {
                val bagType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.Bag", false)
                )

                function(
                    name = "testFunction",
                    returnType = bagType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "bagOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(5, intType()),
                                    intLiteral(5, intType()),
                                    intLiteral(10, intType())
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

        @Test
        fun `orderedSetOf creates ordered set from varargs`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.OrderedSet", false)
                )

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
            assertEquals(3, (result as OrderedSet<*>).size())
        }

        @Test
        fun `listOf with no arguments creates empty list`() {
            val ast = buildTypedAst {
                val listType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.List", false)
                )

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
            assertEquals(0, (result as ScriptList<*>).size())
        }
    }

    @Nested
    inner class EmptyCollectionFunctions {

        @Test
        fun `emptyList creates empty list`() {
            val ast = buildTypedAst {
                val listType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.List", false)
                )

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "emptyList",
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
            assertEquals(0, (result as ScriptList<*>).size())
        }

        @Test
        fun `emptySet creates empty set`() {
            val ast = buildTypedAst {
                val setType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.Set", false)
                )

                function(
                    name = "testFunction",
                    returnType = setType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "emptySet",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = setType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is ScriptSet<*>)
            assertEquals(0, (result as ScriptSet<*>).size())
        }

        @Test
        fun `emptyBag creates empty bag`() {
            val ast = buildTypedAst {
                val bagType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.Bag", false)
                )

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

        @Test
        fun `emptyOrderedSet creates empty ordered set`() {
            val ast = buildTypedAst {
                val orderedSetType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.OrderedSet", false)
                )

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
    }

    @Nested
    inner class MixedTypesInCollections {

        @Test
        fun `listOf with mixed types`() {
            val ast = buildTypedAst {
                val listType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.List", false)
                )

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
                                    stringLiteral("hello", stringType()),
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
            val list = result as ScriptList<*>
            assertEquals(3, list.size())
        }

        @Test
        fun `listOf with null values`() {
            val ast = buildTypedAst {
                val listType = addType(
                    com.mdeo.expression.ast.types.ClassTypeRef("builtin.List", false)
                )
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
                                    intLiteral(1, intType()),
                                    nullLiteral(nullableInt),
                                    intLiteral(3, intType())
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
            val list = result as ScriptList<*>
            assertEquals(3, list.size())
        }
    }

    @Nested
    inner class LocalFunctionCalls {

        @Test
        fun `local function call still works`() {
            val ast = buildTypedAst {
                val intType = intType()

                function(
                    name = "helper",
                    returnType = intType,
                    body = listOf(returnStmt(intLiteral(100, intType)))
                )

                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "helper",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertEquals(100, result)
        }

        @Test
        fun `local function with parameters still works`() {
            val ast = buildTypedAst {
                val intType = intType()

                function(
                    name = "add",
                    returnType = intType,
                    parameters = listOf(
                        com.mdeo.script.ast.TypedParameter("a", intType),
                        com.mdeo.script.ast.TypedParameter("b", intType)
                    ),
                    body = listOf(
                        returnStmt(
                            binaryExpr(
                                identifier("a", intType, 2),
                                "+",
                                identifier("b", intType, 2),
                                intType
                            )
                        )
                    )
                )

                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            functionCall(
                                name = "add",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(3, intType),
                                    intLiteral(7, intType)
                                ),
                                resultTypeIndex = intType
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
