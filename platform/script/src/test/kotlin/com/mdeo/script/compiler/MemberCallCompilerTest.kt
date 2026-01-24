package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for MemberCallCompiler.
 * 
 * Tests verify that member calls (method invocations on objects) are correctly
 * compiled and executed, including string methods, null-safe chaining, and
 * type coercion for method arguments.
 */
class MemberCallCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== String Method Calls ====================
    
    @Test
    fun `string length method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("hello", stringType),
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
        assertEquals(5, result)
    }
    
    @Test
    fun `string length on empty string`() {
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
    
    @Test
    fun `string toUpperCase method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("hello", stringType),
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
        assertEquals("HELLO", result)
    }
    
    @Test
    fun `string toLowerCase method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("HELLO", stringType),
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
        assertEquals("hello", result)
    }
    
    @Test
    fun `string trim method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("  hello  ", stringType),
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
    
    // ==================== String Methods with Arguments ====================
    
    @Test
    fun `string concat method`() {
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
                            arguments = listOf(stringLiteral("World", stringType)),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("Hello, World", result)
    }
    
    @Test
    fun `string startsWith method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val boolType = booleanType()
            
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("Hello, World", stringType),
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
    fun `string startsWith returns false`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val boolType = booleanType()
            
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("Hello, World", stringType),
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
    fun `string endsWith method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val boolType = booleanType()
            
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("Hello, World", stringType),
                            member = "endsWith",
                            overload = "",
                            arguments = listOf(stringLiteral("World", stringType)),
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
    fun `string substring with one argument`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("Hello, World", stringType),
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
        assertEquals("World", result)
    }
    
    @Test
    fun `string substring with two arguments`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("Hello, World", stringType),
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
    
    @Test
    fun `string characterAt method`() {
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
                            arguments = listOf(intLiteral(1, intType)),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("e", result)
    }
    
    @Test
    fun `string isSubstringOf method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val boolType = booleanType()
            
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("llo", stringType),
                            member = "isSubstringOf",
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
    fun `string matches method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val boolType = booleanType()
            
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("hello123", stringType),
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
    fun `string replace method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("hello world", stringType),
                            member = "replace",
                            overload = "",
                            arguments = listOf(
                                stringLiteral("world", stringType),
                                stringLiteral("there", stringType)
                            ),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello there", result)
    }
    
    @Test
    fun `string isInteger method returns true`() {
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
    fun `string isInteger method returns false`() {
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
    fun `string isReal method returns true`() {
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
    fun `string asInteger method`() {
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
    fun `string firstToUpperCase method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("hello", stringType),
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
        assertEquals("Hello", result)
    }
    
    @Test
    fun `string firstToLowerCase method`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("HELLO", stringType),
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
        assertEquals("hELLO", result)
    }
    
    // ==================== Chained Method Calls ====================
    
    @Test
    fun `chained string method calls`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            // "  hello  ".trim().toUpperCase()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = memberCall(
                                expression = stringLiteral("  hello  ", stringType),
                                member = "trim",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            ),
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
        assertEquals("HELLO", result)
    }
    
    @Test
    fun `multiple chained method calls`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            
            // "Hello World".toLowerCase().concat("!").toUpperCase()
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = memberCall(
                                expression = memberCall(
                                    expression = stringLiteral("Hello World", stringType),
                                    member = "toLowerCase",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = stringType
                                ),
                                member = "concat",
                                overload = "",
                                arguments = listOf(stringLiteral("!", stringType)),
                                resultTypeIndex = stringType
                            ),
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
        assertEquals("HELLO WORLD!", result)
    }
    
    // ==================== Null-Safe Chaining ====================
    
    @Test
    fun `null-safe chaining with null value returns null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    // null?.length()
                    returnStmt(
                        memberCall(
                            expression = nullLiteral(stringNullableType),
                            member = "length",
                            overload = "",
                            arguments = emptyList(),
                            isNullChaining = true,
                            resultTypeIndex = intNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `null-safe chaining with non-null value returns result`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("hello", stringNullableType)),
                    // s?.length() where s = "hello"
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "length",
                            overload = "",
                            arguments = emptyList(),
                            isNullChaining = true,
                            resultTypeIndex = intNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(5, result)
    }
    
    @Test
    fun `null-safe chaining on toUpperCase with null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    // null?.toUpperCase()
                    returnStmt(
                        memberCall(
                            expression = nullLiteral(stringNullableType),
                            member = "toUpperCase",
                            overload = "",
                            arguments = emptyList(),
                            isNullChaining = true,
                            resultTypeIndex = stringNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    @Test
    fun `null-safe chaining on toUpperCase with non-null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("hello", stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "toUpperCase",
                            overload = "",
                            arguments = emptyList(),
                            isNullChaining = true,
                            resultTypeIndex = stringNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("HELLO", result)
    }
    
    // ==================== Method Calls on Variables ====================
    
    @Test
    fun `method call on string variable`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    varDecl("message", stringType, stringLiteral("hello world", stringType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("message", stringType, 3),
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
        assertEquals(11, result)
    }
    
    @Test
    fun `method call result used in expression`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            // "hello".length() + "world".length()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            memberCall(
                                expression = stringLiteral("hello", stringType),
                                member = "length",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            ),
                            "+",
                            memberCall(
                                expression = stringLiteral("world", stringType),
                                member = "length",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            ),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(10, result)
    }
    
    @Test
    fun `method call result used in comparison`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            val boolType = booleanType()
            
            // "hello".length() > 3
            function(
                name = "testFunction",
                returnType = boolType,
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            memberCall(
                                expression = stringLiteral("hello", stringType),
                                member = "length",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = intType
                            ),
                            ">",
                            intLiteral(3, intType),
                            boolType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // ==================== Method Call in Conditional ====================
    
    @Test
    fun `method call result in ternary condition`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            val boolType = booleanType()
            
            // "hello".startsWith("h") ? 1 : 0
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        ternaryExpr(
                            memberCall(
                                expression = stringLiteral("hello", stringType),
                                member = "startsWith",
                                overload = "",
                                arguments = listOf(stringLiteral("h", stringType)),
                                resultTypeIndex = boolType
                            ),
                            intLiteral(1, intType),
                            intLiteral(0, intType),
                            intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(1, result)
    }
}
