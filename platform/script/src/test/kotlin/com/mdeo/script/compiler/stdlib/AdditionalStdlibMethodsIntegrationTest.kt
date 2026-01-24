package com.mdeo.script.compiler.stdlib

import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.compiler.*
import com.mdeo.script.stdlib.impl.collections.ScriptList
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Additional integration tests for stdlib methods that were not covered
 * in the existing test files.
 *
 * This test file covers:
 * - Int/Long: iota, to, asString, asDouble, asFloat
 * - Float: asString, asDouble
 * - Double: asString
 * - Boolean: asString
 * - Any: asBoolean, asInteger, asReal, asDouble, asFloat, asString, format, hasProperty
 * - String: asInteger, asDouble
 * - Collection: count, removeAt
 */
class AdditionalStdlibMethodsIntegrationTest {

    private val helper = CompilerTestHelper()

    // ==================================================================================
    // INT ADDITIONAL METHODS
    // ==================================================================================

    @Nested
    inner class IntIotaMethod {

        @Test
        fun `int iota generates sequence from start to end with step`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = intLiteral(0, intType),
                                    member = "iota",
                                    overload = "",
                                    arguments = listOf(
                                        intLiteral(10, intType),
                                        intLiteral(2, intType)
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
            assertEquals(5, result) // 0, 2, 4, 6, 8
        }
    }

    @Nested
    inner class IntToMethod {

        @Test
        fun `int to generates sequence from this to end`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = intLiteral(1, intType),
                                    member = "to",
                                    overload = "",
                                    arguments = listOf(intLiteral(5, intType)),
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
            assertEquals(5, result) // 1, 2, 3, 4, 5
        }
    }

    @Nested
    inner class IntAsStringMethod {

        @Test
        fun `int asString converts int to string`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(42, intType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("42", result)
        }

        @Test
        fun `int asString converts negative int to string`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(-123, intType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("-123", result)
        }
    }

    @Nested
    inner class IntAsDoubleMethod {

        @Test
        fun `int asDouble converts int to double`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(42, intType),
                                member = "asDouble",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }
    }

    @Nested
    inner class IntAsFloatMethod {

        @Test
        fun `int asFloat converts int to float`() {
            val ast = buildTypedAst {
                val intType = intType()
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(42, intType),
                                member = "asFloat",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = floatType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0f, result)
        }
    }

    // ==================================================================================
    // LONG ADDITIONAL METHODS
    // ==================================================================================

    @Nested
    inner class LongIotaMethod {

        @Test
        fun `long iota generates sequence from start to end with step`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val longType = longType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = longLiteral(0L, longType),
                                    member = "iota",
                                    overload = "",
                                    arguments = listOf(
                                        longLiteral(10L, longType),
                                        longLiteral(2L, longType)
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
            assertEquals(5, result) // 0, 2, 4, 6, 8
        }
    }

    @Nested
    inner class LongToMethod {

        @Test
        fun `long to generates sequence from this to end`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val longType = longType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = longLiteral(1L, longType),
                                    member = "to",
                                    overload = "",
                                    arguments = listOf(longLiteral(5L, longType)),
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
            assertEquals(5, result) // 1, 2, 3, 4, 5
        }
    }

    @Nested
    inner class LongAsStringMethod {

        @Test
        fun `long asString converts long to string`() {
            val ast = buildTypedAst {
                val longType = longType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = longLiteral(9876543210L, longType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("9876543210", result)
        }
    }

    @Nested
    inner class LongAsDoubleMethod {

        @Test
        fun `long asDouble converts long to double`() {
            val ast = buildTypedAst {
                val longType = longType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = longLiteral(1000L, longType),
                                member = "asDouble",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(1000.0, result)
        }
    }

    // ==================================================================================
    // FLOAT ADDITIONAL METHODS
    // ==================================================================================

    @Nested
    inner class FloatAsStringMethod {

        @Test
        fun `float asString converts float to string`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = floatLiteral(3.14f, floatType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("3.14", result)
        }
    }

    @Nested
    inner class FloatAsDoubleMethod {

        @Test
        fun `float asDouble converts float to double`() {
            val ast = buildTypedAst {
                val floatType = floatType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = floatLiteral(3.14f, floatType),
                                member = "asDouble",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast) as Double
            assertTrue(result > 3.13 && result < 3.15, "Expected approximately 3.14")
        }
    }

    // ==================================================================================
    // DOUBLE ADDITIONAL METHODS
    // ==================================================================================

    @Nested
    inner class DoubleAsStringMethod {

        @Test
        fun `double asString converts double to string`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = doubleLiteral(3.14159, doubleType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("3.14159", result)
        }
    }

    // ==================================================================================
    // BOOLEAN METHODS
    // ==================================================================================

    @Nested
    inner class BooleanAsStringMethod {

        @Test
        fun `boolean asString converts true to string`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = booleanLiteral(true, boolType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("true", result)
        }

        @Test
        fun `boolean asString converts false to string`() {
            val ast = buildTypedAst {
                val boolType = booleanType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = booleanLiteral(false, boolType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("false", result)
        }
    }

    // ==================================================================================
    // ANY METHODS
    // ==================================================================================

    @Nested
    inner class AnyAsBooleanMethod {

        @Test
        fun `any asBoolean converts true string to boolean`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val anyType = addType(ClassTypeRef("builtin.any", false))
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("true", stringType),
                                member = "asBoolean",
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
        fun `any asBoolean converts false string to boolean`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("false", stringType),
                                member = "asBoolean",
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
    inner class AnyAsIntegerMethod {

        @Test
        fun `any asInteger converts string to int`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("42", stringType),
                                member = "asInteger",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }

        @Test
        fun `any asInteger converts double to int`() {
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = doubleLiteral(42.9, doubleType),
                                member = "asInteger",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(42, result)
        }
    }

    @Nested
    inner class AnyAsRealMethod {

        @Test
        fun `any asReal converts string to double`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("3.14", stringType),
                                member = "asReal",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14, result)
        }

        @Test
        fun `any asReal converts int to double`() {
            val ast = buildTypedAst {
                val intType = intType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(42, intType),
                                member = "asReal",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(42.0, result)
        }
    }

    @Nested
    inner class AnyAsDoubleMethod {

        @Test
        fun `any asDouble converts string to double`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("2.718", stringType),
                                member = "asDouble",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(2.718, result)
        }
    }

    @Nested
    inner class AnyAsFloatMethod {

        @Test
        fun `any asFloat converts string to float`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val floatType = floatType()
                function(
                    name = "testFunction",
                    returnType = floatType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("1.5", stringType),
                                member = "asFloat",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = floatType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(1.5f, result)
        }
    }

    @Nested
    inner class AnyAsStringMethod {

        @Test
        fun `any asString converts object to string`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(123, intType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("123", result)
        }
    }

    @Nested
    inner class AnyFormatMethod {

        @Test
        fun `any format formats integer with pattern`() {
            // Note: AnyHelper.format uses MessageFormat.format which uses {0} style patterns
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(42, intType),
                                member = "format",
                                overload = "",
                                arguments = listOf(stringLiteral("Value: {0}", stringType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Value: 42", result)
        }

        @Test
        fun `any format formats double with pattern`() {
            // Note: AnyHelper.format uses MessageFormat.format which uses {0} style patterns
            // MessageFormat uses its own number formatting which rounds to 3 decimal places by default
            val ast = buildTypedAst {
                val doubleType = doubleType()
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = doubleLiteral(3.14159, doubleType),
                                member = "format",
                                overload = "",
                                arguments = listOf(stringLiteral("Pi is approximately {0}", stringType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Pi is approximately 3.142", result)
        }
    }

    @Nested
    inner class AnyHasPropertyMethod {

        @Test
        fun `any hasProperty returns false for primitive without property`() {
            val ast = buildTypedAst {
                val intType = intType()
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = intLiteral(42, intType),
                                member = "hasProperty",
                                overload = "",
                                arguments = listOf(stringLiteral("nonexistent", stringType)),
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

    // ==================================================================================
    // STRING ADDITIONAL METHODS
    // ==================================================================================

    @Nested
    inner class StringAsIntegerMethod {

        @Test
        fun `string asInteger converts numeric string to int`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("12345", stringType),
                                member = "asInteger",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(12345, result)
        }

        @Test
        fun `string asInteger converts negative numeric string to int`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("-999", stringType),
                                member = "asInteger",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(-999, result)
        }
    }

    @Nested
    inner class StringAsDoubleMethod {

        @Test
        fun `string asDouble converts numeric string to double`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val doubleType = doubleType()
                function(
                    name = "testFunction",
                    returnType = doubleType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("3.14159", stringType),
                                member = "asDouble",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = doubleType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(3.14159, result)
        }
    }

    @Nested
    inner class StringAsStringMethod {

        @Test
        fun `string asString returns same string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("hello world", stringType),
                                member = "asString",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello world", result)
        }
    }

    // ==================================================================================
    // COLLECTION ADDITIONAL METHODS
    // ==================================================================================

    @Nested
    inner class CollectionCountMethod {

        @Test
        fun `collection count returns occurrences of element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
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
                                        intLiteral(2, intType),
                                        intLiteral(3, intType),
                                        intLiteral(2, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "count",
                                overload = "",
                                arguments = listOf(intLiteral(2, intType)),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(3, result)
        }

        @Test
        fun `collection count returns zero for missing element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
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
                                        intLiteral(3, intType)
                                    ),
                                    resultTypeIndex = listType
                                ),
                                member = "count",
                                overload = "",
                                arguments = listOf(intLiteral(5, intType)),
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

    @Nested
    inner class OrderedCollectionRemoveAtMethod {

        @Test
        fun `ordered collection removeAt removes element at index`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        varDecl("list", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(10, intType),
                                    intLiteral(20, intType),
                                    intLiteral(30, intType),
                                    intLiteral(40, intType)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        exprStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "removeAt",
                                overload = "",
                                arguments = listOf(intLiteral(1, intType)),
                                resultTypeIndex = anyType
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

        @Test
        fun `ordered collection removeAt returns removed element`() {
            val ast = buildTypedAst {
                val listType = addType(ClassTypeRef("builtin.List", false))
                val intType = intType()
                val anyType = anyNullableType()
                function(
                    name = "testFunction",
                    returnType = anyType,
                    body = listOf(
                        varDecl("list", listType,
                            functionCall(
                                name = "listOf",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(10, intType),
                                    intLiteral(20, intType),
                                    intLiteral(30, intType)
                                ),
                                resultTypeIndex = listType
                            )
                        ),
                        returnStmt(
                            memberCall(
                                expression = identifier("list", listType, 3),
                                member = "removeAt",
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
}
