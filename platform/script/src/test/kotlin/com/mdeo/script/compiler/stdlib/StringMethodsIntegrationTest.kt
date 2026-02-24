package com.mdeo.script.compiler.stdlib

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.script.compiler.*
import com.mdeo.script.stdlib.impl.collections.ScriptList
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive integration tests for String type methods.
 * 
 * This test file ensures every method defined on the String type is called
 * at least once in the context of a compiled program.
 * 
 * Methods covered:
 * - characterAt, concat, endsWith, firstToLowerCase, firstToUpperCase
 * - isInteger, isReal, isSubstringOf, length, matches, pad, replace
 * - split, startsWith, substring (2 overloads), toCharSequence
 * - toLowerCase, toUpperCase, trim
 */
class StringMethodsIntegrationTest {

    private val helper = CompilerTestHelper()

    // ==================================================================================
    // BASIC STRING METHODS
    // ==================================================================================

    @Nested
    inner class LengthMethod {

        @Test
        fun `length returns string length`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "length",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals(13, result)
        }

        @Test
        fun `length of empty string is zero`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("", stringType),
                                member = "length",
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

    @Nested
    inner class TrimMethod {

        @Test
        fun `trim removes leading and trailing whitespace`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("  hello world  ", stringType),
                                member = "trim",
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

        @Test
        fun `trim on string without whitespace returns same string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("hello", stringType),
                                member = "trim",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("hello", result)
        }
    }

    @Nested
    inner class CaseConversionMethods {

        @Test
        fun `toUpperCase converts to uppercase`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello World", stringType),
                                member = "toUpperCase",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("HELLO WORLD", result)
        }

        @Test
        fun `toLowerCase converts to lowercase`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello World", stringType),
                                member = "toLowerCase",
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

        @Test
        fun `firstToUpperCase capitalizes first character`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("hello world", stringType),
                                member = "firstToUpperCase",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Hello world", result)
        }

        @Test
        fun `firstToLowerCase lowercases first character`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("HELLO WORLD", stringType),
                                member = "firstToLowerCase",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("hELLO WORLD", result)
        }
    }

    @Nested
    inner class CharacterAtMethod {

        @Test
        fun `characterAt returns character at index`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello", stringType),
                                member = "characterAt",
                                overload = "",
                                arguments = listOf(intLiteral(0, intType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("H", result)
        }

        @Test
        fun `characterAt returns last character`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello", stringType),
                                member = "characterAt",
                                overload = "",
                                arguments = listOf(intLiteral(4, intType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("o", result)
        }
    }

    @Nested
    inner class ConcatMethod {

        @Test
        fun `concat joins two strings`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, ", stringType),
                                member = "concat",
                                overload = "",
                                arguments = listOf(stringLiteral("World!", stringType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Hello, World!", result)
        }
    }

    @Nested
    inner class SubstringMethods {

        @Test
        fun `substring with one argument returns from index to end`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "substring",
                                overload = "1",
                                arguments = listOf(intLiteral(7, intType)),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("World!", result)
        }

        @Test
        fun `substring with two arguments returns range`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "substring",
                                overload = "2",
                                arguments = listOf(
                                    intLiteral(0, intType),
                                    intLiteral(5, intType)
                                ),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Hello", result)
        }
    }

    @Nested
    inner class StartsWithEndsWithMethods {

        @Test
        fun `startsWith returns true when string starts with prefix`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "startsWith",
                                overload = "",
                                arguments = listOf(stringLiteral("Hello", stringType)),
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
        fun `startsWith returns false when string does not start with prefix`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "startsWith",
                                overload = "",
                                arguments = listOf(stringLiteral("World", stringType)),
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
        fun `endsWith returns true when string ends with suffix`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "endsWith",
                                overload = "",
                                arguments = listOf(stringLiteral("World!", stringType)),
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
        fun `endsWith returns false when string does not end with suffix`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "endsWith",
                                overload = "",
                                arguments = listOf(stringLiteral("Hello", stringType)),
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
    inner class IsSubstringOfMethod {

        @Test
        fun `isSubstringOf returns true when substring exists`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("World", stringType),
                                member = "isSubstringOf",
                                overload = "",
                                arguments = listOf(stringLiteral("Hello, World!", stringType)),
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
        fun `isSubstringOf returns false when substring does not exist`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Goodbye", stringType),
                                member = "isSubstringOf",
                                overload = "",
                                arguments = listOf(stringLiteral("Hello, World!", stringType)),
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
    inner class MatchesMethod {

        @Test
        fun `matches returns true when regex matches`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("test123", stringType),
                                member = "matches",
                                overload = "",
                                arguments = listOf(stringLiteral("[a-z]+[0-9]+", stringType)),
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
        fun `matches returns false when regex does not match`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("123test", stringType),
                                member = "matches",
                                overload = "",
                                arguments = listOf(stringLiteral("[a-z]+[0-9]+", stringType)),
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
    inner class ReplaceMethod {

        @Test
        fun `replace substitutes matching pattern`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello, World!", stringType),
                                member = "replace",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("World", stringType),
                                    stringLiteral("Universe", stringType)
                                ),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Hello, Universe!", result)
        }

        @Test
        fun `replace with regex pattern`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hello 123 World 456", stringType),
                                member = "replace",
                                overload = "",
                                arguments = listOf(
                                    stringLiteral("[0-9]+", stringType),
                                    stringLiteral("###", stringType)
                                ),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Hello ### World ###", result)
        }
    }

    @Nested
    inner class SplitMethod {

        @Test
        fun `split divides string by delimiter`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = stringLiteral("a,b,c,d", stringType),
                                    member = "split",
                                    overload = "",
                                    arguments = listOf(stringLiteral(",", stringType)),
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
            assertEquals(4, result)
        }
    }

    @Nested
    inner class IsIntegerIsRealMethods {

        @Test
        fun `isInteger returns true for integer string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("12345", stringType),
                                member = "isInteger",
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
        fun `isInteger returns false for non-integer string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("12.34", stringType),
                                member = "isInteger",
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
        fun `isReal returns true for real number string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("3.14159", stringType),
                                member = "isReal",
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
        fun `isReal returns false for non-numeric string`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val boolType = booleanType()
                function(
                    name = "testFunction",
                    returnType = boolType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("hello", stringType),
                                member = "isReal",
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
    inner class PadMethod {

        @Test
        fun `pad adds padding to reach length`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("42", stringType),
                                member = "pad",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(5, intType),
                                    stringLiteral("0", stringType),
                                    booleanLiteral(false, booleanType())
                                ),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("00042", result)
        }

        @Test
        fun `pad with right padding`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = stringType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = stringLiteral("Hi", stringType),
                                member = "pad",
                                overload = "",
                                arguments = listOf(
                                    intLiteral(5, intType),
                                    stringLiteral("-", stringType),
                                    booleanLiteral(true, booleanType())
                                ),
                                resultTypeIndex = stringType
                            )
                        )
                    )
                )
            }
            val result = helper.compileAndInvoke(ast)
            assertEquals("Hi---", result)
        }
    }

    @Nested
    inner class ToCharSequenceMethod {

        @Test
        fun `toCharSequence returns list of characters`() {
            val ast = buildTypedAst {
                val stringType = stringType()
                val listType = addType(ClassTypeRef("builtin", "List", false))
                val intType = intType()
                function(
                    name = "testFunction",
                    returnType = intType,
                    body = listOf(
                        returnStmt(
                            memberCall(
                                expression = memberCall(
                                    expression = stringLiteral("Hello", stringType),
                                    member = "toCharSequence",
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
            assertEquals(5, result)
        }
    }
}
