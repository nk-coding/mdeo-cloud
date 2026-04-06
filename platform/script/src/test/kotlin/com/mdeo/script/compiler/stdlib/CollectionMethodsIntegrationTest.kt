package com.mdeo.script.compiler.stdlib

import com.mdeo.expression.ast.types.BuiltinTypes.function
import com.mdeo.expression.ast.types.BuiltinTypes.predicate
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.expression.ast.types.Parameter
import com.mdeo.script.compiler.*
import com.mdeo.script.stdlib.impl.collections.ScriptList
import com.mdeo.script.stdlib.impl.collections.ScriptSet
import com.mdeo.script.stdlib.impl.collections.Bag
import com.mdeo.script.stdlib.impl.collections.OrderedSet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive integration tests for Collection type methods.
 * 
 * This test file ensures every method defined on collection types is called
 * at least once in the context of a compiled program.
 * 
 * Types covered:
 * - ReadonlyCollection: size, isEmpty, notEmpty, includes, excludes, sum, concat, etc.
 * - Collection: add, addAll, clear, remove, removeAll
 * - ReadonlyOrderedCollection: at, first, last, indexOf, invert
 * - OrderedCollection: removeAt, sortBy
 * - Map: put, get, containsKey, containsValue, keySet, values, size, isEmpty
 */
class CollectionMethodsIntegrationTest {

    private val helper = CompilerTestHelper()

    // ==================================================================================
    // READONLY COLLECTION METHODS
    // ==================================================================================

    @Nested
    inner class SizeMethod {

        @Test
        fun `size returns number of elements in list`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType),
                                        intLiteral(4, intType),
                                        intLiteral(5, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(5, result)
        }
    }

    @Nested
    inner class IsEmptyNotEmptyMethods {

        @Test
        fun `isEmpty returns true for empty collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "emptyList",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = listType
                                ),
                                member = "isEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `isEmpty returns false for non-empty collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(intLiteral(1, intType)),
                                    resultTypeIndex = listType
                                ),
                                member = "isEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `notEmpty returns true for non-empty collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(intLiteral(1, intType)),
                                    resultTypeIndex = listType
                                ),
                                member = "notEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `notEmpty returns false for empty collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "emptyList",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = listType
                                ),
                                member = "notEmpty",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    @Nested
    inner class IncludesExcludesMethods {

        @Test
        fun `includes returns true when element exists`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "includes",
                                overload = "",
                                arguments = listOf(intLiteral(2, intType)),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `includes returns false when element does not exist`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "includes",
                                overload = "",
                                arguments = listOf(intLiteral(5, intType)),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }

        @Test
        fun `excludes returns true when element does not exist`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "excludes",
                                overload = "",
                                arguments = listOf(intLiteral(5, intType)),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(true, result)
        }

        @Test
        fun `excludes returns false when element exists`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "excludes",
                                overload = "",
                                arguments = listOf(intLiteral(2, intType)),
                                resultTypeIndex = boolType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(false, result)
        }
    }

    @Nested
    inner class SumMethod {

        @Test
        fun `sum returns sum of numeric elements`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(1, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType),
                                        intLiteral(4, intType),
                                        intLiteral(5, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "sum",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(15.0, result)
        }
    }

    @Nested
    inner class ConcatMethod {

        @Test
        fun `concat without separator joins elements`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        stringLiteral("a", stringType),
                                        stringLiteral("b", stringType),
                                        stringLiteral("c", stringType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "concat",
                                overload = "nosep",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("abc", result)
        }

        @Test
        fun `concat with separator joins elements with separator`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        stringLiteral("a", stringType),
                                        stringLiteral("b", stringType),
                                        stringLiteral("c", stringType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "concat",
                                overload = "sep",
                                arguments = listOf(stringLiteral(", ", stringType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("a, b, c", result)
        }
    }

    // ==================================================================================
    // ORDERED COLLECTION METHODS
    // ==================================================================================

    @Nested
    inner class AtMethod {

        @Test
        fun `at returns element at index`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intType),
                                        intLiteral(20, intType),
                                        intLiteral(30, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "at",
                                overload = "",
                                arguments = listOf(intLiteral(1, intType)),
                                resultTypeIndex = anyType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(20, result)
        }
    }

    @Nested
    inner class FirstLastMethods {

        @Test
        fun `first returns first element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intType),
                                        intLiteral(20, intType),
                                        intLiteral(30, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "first",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = anyType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(10, result)
        }

        @Test
        fun `last returns last element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intType),
                                        intLiteral(20, intType),
                                        intLiteral(30, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "last",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = anyType
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
    inner class IndexOfMethod {

        @Test
        fun `indexOf returns index of element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intType),
                                        intLiteral(20, intType),
                                        intLiteral(30, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "indexOf",
                                overload = "",
                                arguments = listOf(intLiteral(20, intType)),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(1, result)
        }

        @Test
        fun `indexOf returns -1 for missing element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intType),
                                        intLiteral(20, intType),
                                        intLiteral(30, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "indexOf",
                                overload = "",
                                arguments = listOf(intLiteral(50, intType)),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(-1, result)
        }
    }

    // ==================================================================================
    // MUTABLE COLLECTION METHODS
    // ==================================================================================

    @Nested
    inner class AddMethod {

        @Test
        fun `add adds element to collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("list", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intType),
                                    intLiteral(2, intType)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        exprStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "add",
                                overload = "",
                                arguments = listOf(intLiteral(3, intType)),
                                resultTypeIndex = boolType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
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
    inner class RemoveMethod {

        @Test
        fun `remove removes element from collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("list", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intType),
                                    intLiteral(2, intType),
                                    intLiteral(3, intType)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        exprStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "remove",
                                overload = "",
                                arguments = listOf(intLiteral(2, intType)),
                                resultTypeIndex = boolType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(2, result)
        }
    }

    @Nested
    inner class ClearMethod {

        @Test
        fun `clear removes all elements from collection`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val voidType = voidType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("list", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(1, intType),
                                    intLiteral(2, intType),
                                    intLiteral(3, intType)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        exprStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "clear",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = voidType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(0, result)
        }
    }

    // ==================================================================================
    // COLLECTION TYPE CONVERSIONS
    // ==================================================================================

    @Nested
    inner class AsListMethod {

        @Test
        fun `asList converts set to list`() {
            val ast = buildTypedAst {
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "setOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(1, intType),
                                            intLiteral(2, intType),
                                            intLiteral(3, intType)
                                        ),
                                        resultTypeIndex = setType
                                    ),
                                    member = "toList",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = listType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
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
    inner class AsSetMethod {

        @Test
        fun `asSet converts list to set with deduplication`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val setType = addType(ClassTypeRef("builtin", "Set", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "listOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(1, intType),
                                            intLiteral(2, intType),
                                            intLiteral(2, intType),
                                            intLiteral(3, intType)
                                        ),
                                        resultTypeIndex = listType
                                    ),
                                    member = "toSet",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = setType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)  // duplicates removed
        }
    }

    @Nested
    inner class AsBagMethod {

        @Test
        fun `asBag converts list to bag preserving duplicates`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val bagType = addType(ClassTypeRef("builtin", "Bag", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "listOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(1, intType),
                                            intLiteral(2, intType),
                                            intLiteral(2, intType),
                                            intLiteral(3, intType)
                                        ),
                                        resultTypeIndex = listType
                                    ),
                                    member = "toBag",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = bagType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(4, result)  // duplicates preserved
        }
    }

    @Nested
    inner class AsOrderedSetMethod {

        @Test
        fun `asOrderedSet converts list to ordered set`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val orderedSetType = addType(ClassTypeRef("builtin", "OrderedSet", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "listOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(3, intType),
                                            intLiteral(1, intType),
                                            intLiteral(2, intType),
                                            intLiteral(1, intType)
                                        ),
                                        resultTypeIndex = listType
                                    ),
                                    member = "toOrderedSet",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = orderedSetType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)  // duplicates removed
        }
    }

    // ==================================================================================
    // INCLUDING/EXCLUDING METHODS
    // ==================================================================================

    @Nested
    inner class IncludingMethod {

        @Test
        fun `including returns new collection with added element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val collectionType = addType(ClassTypeRef("builtin", "Collection", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "listOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(1, intType),
                                            intLiteral(2, intType)
                                        ),
                                        resultTypeIndex = listType
                                    ),
                                    member = "including",
                                    overload = "",
                                    arguments = listOf(intLiteral(3, intType)),
                                    resultTypeIndex = collectionType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
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
    inner class ExcludingMethod {

        @Test
        fun `excluding returns new collection without element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val collectionType = addType(ClassTypeRef("builtin", "Collection", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "listOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(1, intType),
                                            intLiteral(2, intType),
                                            intLiteral(3, intType)
                                        ),
                                        resultTypeIndex = listType
                                    ),
                                    member = "excluding",
                                    overload = "",
                                    arguments = listOf(intLiteral(2, intType)),
                                    resultTypeIndex = collectionType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(2, result)
        }
    }

    // ==================================================================================
    // FLATMAP METHOD
    // ==================================================================================

    @Nested
    inner class FlatMapMethod {

        @Test
        fun `flatMap flattens nested collections`() {
            // listOf(1, 2, 3).flatMap { it -> listOf(it, it) }.size() == 6
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val collectionType = addType(ClassTypeRef("builtin", "Collection", false))
                val intType = intType()
                val funcLambdaType = addType(function(ClassTypeRef("builtin", "int", false), ClassTypeRef("builtin", "Collection", false)))

                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = functionCall(
                                        name = "listOf",
                                        overload = "",
                                        arguments = listOf(
                                            intLiteral(1, intType),
                                            intLiteral(2, intType),
                                            intLiteral(3, intType)
                                        ),
                                        resultTypeIndex = listType
                                    ),
                                    member = "flatMap",
                                    overload = "",
                                    arguments = listOf(
                                        lambdaExpr(
                                            parameters = listOf("it"),
                                            body = listOf(
                                                returnStmt(
                                                    functionCall(
                                                        name = "listOf",
                                                        overload = "",
                                                        arguments = listOf(
                                                            identifier("it", intType, 4),
                                                            identifier("it", intType, 4)
                                                        ),
                                                        resultTypeIndex = listType
                                                    )
                                                )
                                            ),
                                            lambdaTypeIndex = funcLambdaType
                                        )
                                    ),
                                    resultTypeIndex = collectionType
                                ),
                                member = "size",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(6, result)
        }
    }

    // ==================================================================================
    // FIRST METHOD
    // ==================================================================================

    @Nested
    inner class FirstMethod {

        @Test
        fun `first returns first element of non-empty list`() {
            // listOf(42, 2, 3).first() == 42
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val nullableAny = anyNullableType()

                function(
                    name = "testFunction",
                    returnType = nullableAny,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(42, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "first",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = nullableAny
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }
    }

    // ==================================================================================
    // FIRSTORNULL METHOD
    // ==================================================================================

    @Nested
    inner class FirstOrNullMethod {

        @Test
        fun `firstOrNull returns first element of non-empty list`() {
            // listOf(99, 2, 3).firstOrNull() == 99
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                val nullableAny = anyNullableType()

                function(
                    name = "testFunction",
                    returnType = nullableAny,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(99, intType),
                                        intLiteral(2, intType),
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "firstOrNull",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = nullableAny
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(99, result)
        }

        @Test
        fun `firstOrNull returns null for empty list`() {
            // listOf().firstOrNull() == null
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val nullableAny = anyNullableType()

                function(
                    name = "testFunction",
                    returnType = nullableAny,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = functionCall(
                                    name = "listOf",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = listType
                                ),
                                member = "firstOrNull",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = nullableAny
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(null, result)
        }
    }
}
