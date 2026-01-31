package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.script.ast.TypedParameter
import com.mdeo.script.compiler.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for [ListLiteralCompiler].
 *
 * These tests verify that list literal expressions are correctly compiled to bytecode
 * that creates ArrayList instances populated with the specified elements.
 *
 * Test categories:
 * 1. Empty lists - `[]`
 * 2. Single element lists - `[element]`
 * 3. Multiple elements of same type - `[1, 2, 3]`
 * 4. Nested lists - `[[1, 2], [3, 4]]`
 * 5. Lists with string elements - `["a", "b", "c"]`
 * 6. Lists with null elements - `[null, 1, null]`
 * 7. List element access after creation
 * 8. Lists passed to functions
 * 9. Lists used in for loops
 *
 * Scope reference for tests:
 * - With function parameter: param at scope 2, local vars at scope 3, for-loop var at scope 4
 * - Without function parameter: local vars at scope 3, for-loop var at scope 4
 */
class ListLiteralCompilerTest {

    private val helper = CompilerTestHelper()

    // ==================== Empty List Tests ====================

    @Nested
    inner class EmptyListTests {

        /**
         * Test: `[]` creates an empty ArrayList.
         * Verifies that an empty list literal compiles to an empty ArrayList.
         */
        @Test
        fun `empty list literal creates empty ArrayList`() {
            val ast = buildTypedAst {
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(emptyList(), listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(0, result.size)
        }

        /**
         * Test: Empty list can be stored in a variable and returned.
         */
        @Test
        fun `empty list assigned to variable and returned`() {
            val ast = buildTypedAst {
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        varDecl("myList", listType, listLiteral(emptyList(), listType)),
                        returnStmt(identifier("myList", listType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertTrue(result.isEmpty())
        }
    }

    // ==================== Single Element List Tests ====================

    @Nested
    inner class SingleElementListTests {

        /**
         * Test: `[1]` creates a list with one integer element.
         */
        @Test
        fun `single integer element list`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(listOf(intLiteral(42, intType)), listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(1, result.size)
            assertEquals(42, result[0])
        }

        /**
         * Test: `["hello"]` creates a list with one string element.
         */
        @Test
        fun `single string element list`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(listOf(stringLiteral("hello", stringType)), listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(1, result.size)
            assertEquals("hello", result[0])
        }

        /**
         * Test: `[true]` creates a list with one boolean element.
         */
        @Test
        fun `single boolean element list`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(listOf(booleanLiteral(true, boolType)), listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(1, result.size)
            assertEquals(true, result[0])
        }

        /**
         * Test: `[3.14]` creates a list with one double element.
         */
        @Test
        fun `single double element list`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(listOf(doubleLiteral(3.14, doubleType)), listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(1, result.size)
            assertEquals(3.14, result[0])
        }
    }

    // ==================== Multiple Elements Same Type Tests ====================

    @Nested
    inner class MultipleElementsSameTypeTests {

        /**
         * Test: `[1, 2, 3]` creates a list with three integer elements.
         */
        @Test
        fun `multiple integer elements`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    intLiteral(1, intType),
                                    intLiteral(2, intType),
                                    intLiteral(3, intType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(1, result[0])
            assertEquals(2, result[1])
            assertEquals(3, result[2])
        }

        /**
         * Test: `["a", "b", "c", "d", "e"]` creates a list with five string elements.
         */
        @Test
        fun `multiple string elements`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    stringLiteral("a", stringType),
                                    stringLiteral("b", stringType),
                                    stringLiteral("c", stringType),
                                    stringLiteral("d", stringType),
                                    stringLiteral("e", stringType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(5, result.size)
            assertEquals("a", result[0])
            assertEquals("b", result[1])
            assertEquals("c", result[2])
            assertEquals("d", result[3])
            assertEquals("e", result[4])
        }

        /**
         * Test: `[true, false, true]` creates a list with three boolean elements.
         */
        @Test
        fun `multiple boolean elements`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    booleanLiteral(true, boolType),
                                    booleanLiteral(false, boolType),
                                    booleanLiteral(true, boolType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(true, result[0])
            assertEquals(false, result[1])
            assertEquals(true, result[2])
        }

        /**
         * Test: `[1.1, 2.2, 3.3, 4.4]` creates a list with four double elements.
         */
        @Test
        fun `multiple double elements`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    doubleLiteral(1.1, doubleType),
                                    doubleLiteral(2.2, doubleType),
                                    doubleLiteral(3.3, doubleType),
                                    doubleLiteral(4.4, doubleType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(4, result.size)
            assertEquals(1.1, result[0])
            assertEquals(2.2, result[1])
            assertEquals(3.3, result[2])
            assertEquals(4.4, result[3])
        }

        /**
         * Test: List with many elements (10+) to test large initial capacity (BIPUSH bytecode).
         */
        @Test
        fun `list with many elements tests BIPUSH capacity`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                val elements = (1..20).map { intLiteral(it, intType) }

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(elements, listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(20, result.size)
            for (i in 1..20) {
                assertEquals(i, result[i - 1])
            }
        }
    }

    // ==================== Nested List Tests ====================

    @Nested
    inner class NestedListTests {

        /**
         * Test: `[[1, 2], [3, 4]]` creates a list of lists.
         */
        @Test
        fun `nested list of integers`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    listLiteral(
                                        listOf(intLiteral(1, intType), intLiteral(2, intType)),
                                        listType
                                    ),
                                    listLiteral(
                                        listOf(intLiteral(3, intType), intLiteral(4, intType)),
                                        listType
                                    )
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(2, result.size)

            @Suppress("UNCHECKED_CAST")
            val innerList1 = result[0] as List<*>
            assertEquals(2, innerList1.size)
            assertEquals(1, innerList1[0])
            assertEquals(2, innerList1[1])

            @Suppress("UNCHECKED_CAST")
            val innerList2 = result[1] as List<*>
            assertEquals(2, innerList2.size)
            assertEquals(3, innerList2[0])
            assertEquals(4, innerList2[1])
        }

        /**
         * Test: `[["a", "b"], ["c"]]` creates a list of string lists with different sizes.
         */
        @Test
        fun `nested list of strings with different sizes`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    listLiteral(
                                        listOf(
                                            stringLiteral("a", stringType),
                                            stringLiteral("b", stringType)
                                        ),
                                        listType
                                    ),
                                    listLiteral(
                                        listOf(stringLiteral("c", stringType)),
                                        listType
                                    )
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(2, result.size)

            @Suppress("UNCHECKED_CAST")
            val innerList1 = result[0] as List<*>
            assertEquals(2, innerList1.size)
            assertEquals("a", innerList1[0])
            assertEquals("b", innerList1[1])

            @Suppress("UNCHECKED_CAST")
            val innerList2 = result[1] as List<*>
            assertEquals(1, innerList2.size)
            assertEquals("c", innerList2[0])
        }

        /**
         * Test: Deeply nested list `[[[1]]]` creates a list of list of list.
         */
        @Test
        fun `deeply nested list three levels`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    listLiteral(
                                        listOf(
                                            listLiteral(
                                                listOf(intLiteral(1, intType)),
                                                listType
                                            )
                                        ),
                                        listType
                                    )
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(1, result.size)

            @Suppress("UNCHECKED_CAST")
            val level2 = result[0] as List<*>
            assertEquals(1, level2.size)

            @Suppress("UNCHECKED_CAST")
            val level3 = level2[0] as List<*>
            assertEquals(1, level3.size)
            assertEquals(1, level3[0])
        }
    }

    // ==================== List with Null Elements Tests ====================

    @Nested
    inner class NullElementTests {

        /**
         * Test: `[null]` creates a list with one null element.
         */
        @Test
        fun `list with single null element`() {
            val ast = buildTypedAst {
                val nullableIntType = intNullableType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(listLiteral(listOf(nullLiteral(nullableIntType)), listType))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(1, result.size)
            assertNull(result[0])
        }

        /**
         * Test: `[null, "hello", null]` creates a list with mixed null and string elements.
         */
        @Test
        fun `list with mixed null and non-null string elements`() {
            val ast = buildTypedAst {
                val nullableStringType = stringNullableType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    nullLiteral(nullableStringType),
                                    stringLiteral("hello", nullableStringType),
                                    nullLiteral(nullableStringType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertNull(result[0])
            assertEquals("hello", result[1])
            assertNull(result[2])
        }
    }

    // ==================== List with Long Values Tests ====================

    @Nested
    inner class LongValueTests {

        /**
         * Test: `[1L, 2L, 3L]` creates a list with long elements.
         */
        @Test
        fun `list with long elements`() {
            val ast = buildTypedAst {
                val longType = longType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    longLiteral(1L, longType),
                                    longLiteral(2L, longType),
                                    longLiteral(3L, longType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(1L, result[0])
            assertEquals(2L, result[1])
            assertEquals(3L, result[2])
        }

        /**
         * Test: List with large long values beyond int range.
         */
        @Test
        fun `list with large long values`() {
            val ast = buildTypedAst {
                val longType = longType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    longLiteral(Long.MAX_VALUE, longType),
                                    longLiteral(Long.MIN_VALUE, longType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(2, result.size)
            assertEquals(Long.MAX_VALUE, result[0])
            assertEquals(Long.MIN_VALUE, result[1])
        }
    }

    // ==================== List with Float Values Tests ====================

    @Nested
    inner class FloatValueTests {

        /**
         * Test: `[1.0f, 2.5f, 3.75f]` creates a list with float elements.
         */
        @Test
        fun `list with float elements`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    floatLiteral(1.0f, floatType),
                                    floatLiteral(2.5f, floatType),
                                    floatLiteral(3.75f, floatType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(1.0f, result[0])
            assertEquals(2.5f, result[1])
            assertEquals(3.75f, result[2])
        }
    }

    // ==================== List Assignment and Variable Usage Tests ====================

    @Nested
    inner class VariableUsageTests {

        /**
         * Test: Create list, assign to variable, return it.
         */
        @Test
        fun `list assigned to variable and returned`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        varDecl(
                            "numbers",
                            listType,
                            listLiteral(
                                listOf(
                                    intLiteral(10, intType),
                                    intLiteral(20, intType),
                                    intLiteral(30, intType)
                                ),
                                listType
                            )
                        ),
                        returnStmt(identifier("numbers", listType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(10, result[0])
            assertEquals(20, result[1])
            assertEquals(30, result[2])
        }

        /**
         * Test: Verify list size using Java reflection (not through script method call).
         * This tests that the compiled list is actually a Java List.
         */
        @Test
        fun `list is a valid Java List instance`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    intLiteral(1, intType),
                                    intLiteral(2, intType),
                                    intLiteral(3, intType),
                                    intLiteral(4, intType),
                                    intLiteral(5, intType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(5, result.size)
        }
    }

    // ==================== List in For Loop Tests ====================

    @Nested
    inner class ForLoopTests {

        /**
         * Test: Create list, iterate with for loop, sum elements.
         * Uses function with list parameter to match existing patterns.
         * Scopes: param at 2, local var at 3, for-loop var at 4
         */
        @Test
        fun `for loop iterates over list and sums elements`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = intType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        varDecl("sum", intType, intLiteral(0, intType)),
                        forStmt(
                            variableName = "item",
                            variableType = intType,
                            iterable = identifier("list", listType, 2),  // param at scope 2
                            body = listOf(
                                assignment(
                                    identifier("sum", intType, 3),  // local var at scope 3
                                    binaryExpr(
                                        identifier("sum", intType, 3),
                                        "+",
                                        identifier("item", intType, 4),  // for-loop var at scope 4
                                        intType
                                    )
                                )
                            )
                        ),
                        returnStmt(identifier("sum", intType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4, 5))
            assertEquals(15, result) // 1 + 2 + 3 + 4 + 5 = 15
        }

        /**
         * Test: For loop with break over list.
         */
        @Test
        fun `for loop with break exits early`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = intType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        varDecl("sum", intType, intLiteral(0, intType)),
                        forStmt(
                            variableName = "item",
                            variableType = intType,
                            iterable = identifier("list", listType, 2),
                            body = listOf(
                                ifStmt(
                                    condition = binaryExpr(
                                        identifier("item", intType, 4),
                                        ">",
                                        intLiteral(3, intType),
                                        boolType
                                    ),
                                    thenBlock = listOf(breakStmt())
                                ),
                                assignment(
                                    identifier("sum", intType, 3),
                                    binaryExpr(
                                        identifier("sum", intType, 3),
                                        "+",
                                        identifier("item", intType, 4),
                                        intType
                                    )
                                )
                            )
                        ),
                        returnStmt(identifier("sum", intType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4, 5))
            assertEquals(6, result) // 1 + 2 + 3 = 6 (break when item > 3)
        }

        /**
         * Test: For loop with continue skips elements.
         */
        @Test
        fun `for loop with continue skips elements`() {
            val ast = buildTypedAst {
                val intType = intType()
                val boolType = booleanType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = intType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        varDecl("sum", intType, intLiteral(0, intType)),
                        forStmt(
                            variableName = "item",
                            variableType = intType,
                            iterable = identifier("list", listType, 2),
                            body = listOf(
                                ifStmt(
                                    condition = binaryExpr(
                                        identifier("item", intType, 4),
                                        "==",
                                        intLiteral(2, intType),
                                        boolType
                                    ),
                                    thenBlock = listOf(continueStmt())
                                ),
                                assignment(
                                    identifier("sum", intType, 3),
                                    binaryExpr(
                                        identifier("sum", intType, 3),
                                        "+",
                                        identifier("item", intType, 4),
                                        intType
                                    )
                                )
                            )
                        ),
                        returnStmt(identifier("sum", intType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast, "testFunction", listOf(1, 2, 3, 4))
            assertEquals(8, result) // 1 + 3 + 4 = 8 (skip when item == 2)
        }

        /**
         * Test: For loop over empty list does nothing.
         */
        @Test
        fun `for loop over empty list does not execute body`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = intType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        varDecl("count", intType, intLiteral(0, intType)),
                        forStmt(
                            variableName = "item",
                            variableType = intType,
                            iterable = identifier("list", listType, 2),
                            body = listOf(
                                assignment(
                                    identifier("count", intType, 3),
                                    binaryExpr(
                                        identifier("count", intType, 3),
                                        "+",
                                        intLiteral(1, intType),
                                        intType
                                    )
                                )
                            )
                        ),
                        returnStmt(identifier("count", intType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast, "testFunction", emptyList<Int>())
            assertEquals(0, result) // No iterations
        }

        /**
         * Test: For loop over string list.
         */
        @Test
        fun `for loop over string list concatenates elements`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = stringType,
                    parameters = listOf(TypedParameter("list", listType)),
                    body = listOf(
                        varDecl("result", stringType, stringLiteral("", stringType)),
                        forStmt(
                            variableName = "item",
                            variableType = stringType,
                            iterable = identifier("list", listType, 2),
                            body = listOf(
                                assignment(
                                    identifier("result", stringType, 3),
                                    binaryExpr(
                                        identifier("result", stringType, 3),
                                        "+",
                                        identifier("item", stringType, 4),
                                        stringType
                                    )
                                )
                            )
                        ),
                        returnStmt(identifier("result", stringType, 3))
                    )
                )
            }

            val result = helper.compileAndInvoke(ast, "testFunction", listOf("Hello", " ", "World"))
            assertEquals("Hello World", result)
        }
    }

    // ==================== Edge Cases Tests ====================

    @Nested
    inner class EdgeCaseTests {

        /**
         * Test: List with negative integers.
         */
        @Test
        fun `list with negative integers`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    intLiteral(-1, intType),
                                    intLiteral(-100, intType),
                                    intLiteral(Int.MIN_VALUE, intType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(-1, result[0])
            assertEquals(-100, result[1])
            assertEquals(Int.MIN_VALUE, result[2])
        }

        /**
         * Test: List with zero value.
         */
        @Test
        fun `list with zero values`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    intLiteral(0, intType),
                                    intLiteral(0, intType),
                                    intLiteral(0, intType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(0, result[0])
            assertEquals(0, result[1])
            assertEquals(0, result[2])
        }

        /**
         * Test: List with duplicate values.
         */
        @Test
        fun `list with duplicate values`() {
            val ast = buildTypedAst {
                val intType = intType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    intLiteral(5, intType),
                                    intLiteral(5, intType),
                                    intLiteral(5, intType),
                                    intLiteral(5, intType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(4, result.size)
            result.forEach { assertEquals(5, it) }
        }

        /**
         * Test: List with empty strings.
         */
        @Test
        fun `list with empty strings`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    stringLiteral("", stringType),
                                    stringLiteral("", stringType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(2, result.size)
            assertEquals("", result[0])
            assertEquals("", result[1])
        }

        /**
         * Test: List with special float values (NaN, Infinity).
         */
        @Test
        fun `list with special double values`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    doubleLiteral(Double.POSITIVE_INFINITY, doubleType),
                                    doubleLiteral(Double.NEGATIVE_INFINITY, doubleType),
                                    doubleLiteral(Double.NaN, doubleType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals(Double.POSITIVE_INFINITY, result[0])
            assertEquals(Double.NEGATIVE_INFINITY, result[1])
            assertTrue((result[2] as Double).isNaN())
        }

        /**
         * Test: List with unicode strings.
         */
        @Test
        fun `list with unicode strings`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = listType()

                function(
                    name = "testFunction",
                    returnType = listType,
                    body = listOf(
                        returnStmt(
                            listLiteral(
                                listOf(
                                    stringLiteral("Hello 世界", stringType),
                                    stringLiteral("🌍🌎🌏", stringType),
                                    stringLiteral("αβγδ", stringType)
                                ),
                                listType
                            )
                        )
                    )
                )
            }

            val result = helper.compileAndInvoke(ast)
            assertNotNull(result)
            assertTrue(result is List<*>)
            assertEquals(3, result.size)
            assertEquals("Hello 世界", result[0])
            assertEquals("🌍🌎🌏", result[1])
            assertEquals("αβγδ", result[2])
        }
    }
}
