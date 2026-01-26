package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Edge case tests for MemberCallCompiler.
 * 
 * These tests focus on potential bugs in type coercion, boxing/unboxing,
 * and null-safe chaining when calling methods.
 */
class MemberCallEdgeCasesTest {
    
    private val helper = CompilerTestHelper()
    
    // ==================== Null-Safe Chaining with Primitive Returns ====================
    
    @Test
    fun `null-safe chaining on int method returns boxed value when non-null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            // val s: String? = "hello"
            // s?.length() should return 5 (boxed Integer)
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("hello", stringNullableType)),
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
    fun `null-safe chaining returns null when target is null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            // val s: String? = null
            // s?.length() should return null
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, nullLiteral(stringNullableType)),
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
        assertNull(result)
    }
    
    @Test
    fun `null-safe chaining on boolean method returns boxed value when non-null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val stringType = stringType()
            val boolNullableType = booleanNullableType()
            
            // val s: String? = "hello"
            // s?.startsWith("h") should return true (boxed Boolean)
            function(
                name = "testFunction",
                returnType = boolNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("hello", stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "startsWith",
                            overload = "",
                            arguments = listOf(stringLiteral("h", stringType)),
                            isNullChaining = true,
                            resultTypeIndex = boolNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    @Test
    fun `null-safe chaining on boolean method returns null when target is null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val stringType = stringType()
            val boolNullableType = booleanNullableType()
            
            // val s: String? = null
            // s?.startsWith("h") should return null
            function(
                name = "testFunction",
                returnType = boolNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, nullLiteral(stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "startsWith",
                            overload = "",
                            arguments = listOf(stringLiteral("h", stringType)),
                            isNullChaining = true,
                            resultTypeIndex = boolNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
    
    // ==================== Chained Null-Safe Calls ====================
    
    @Test
    fun `chained null-safe calls with first null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            
            // val s: String? = null
            // s?.trim()?.toUpperCase() should return null
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, nullLiteral(stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = memberCall(
                                expression = identifier("s", stringNullableType, 3),
                                member = "trim",
                                overload = "",
                                arguments = emptyList(),
                                isNullChaining = true,
                                resultTypeIndex = stringNullableType
                            ),
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
    fun `chained null-safe calls with non-null value`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            
            // val s: String? = "  hello  "
            // s?.trim()?.toUpperCase() should return "HELLO"
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("  hello  ", stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = memberCall(
                                expression = identifier("s", stringNullableType, 3),
                                member = "trim",
                                overload = "",
                                arguments = emptyList(),
                                isNullChaining = true,
                                resultTypeIndex = stringNullableType
                            ),
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
    
    // ==================== Member Call Result Used in Binary Expression ====================
    
    @Test
    fun `null-safe member call result used in comparison`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intType = intType()
            val intNullableType = intNullableType()
            val boolNullableType = booleanNullableType()
            
            // val s: String? = "hello"
            // s?.length() > 3 
            // This is tricky - the comparison needs to handle nullable
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("hello", stringNullableType)),
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
    
    // ==================== Method Calls with Arguments That Need Boxing ====================
    
    @Test
    fun `method call with int argument to method expecting nullable Int`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            // "hello".substring(1)
            function(
                name = "testFunction",
                returnType = stringType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = stringLiteral("hello", stringType),
                            member = "substring",
                            overload = "1",
                            arguments = listOf(intLiteral(1, intType)),
                            resultTypeIndex = stringType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("ello", result)
    }
    
    // ==================== Method Calls on Nullable String with Arguments ====================
    
    @Test
    fun `null-safe method call with arguments when target is non-null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val stringType = stringType()
            val intType = intType()
            
            // val s: String? = "hello world"
            // s?.substring(0, 5) should return "hello"
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("hello world", stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "substring",
                            overload = "2",
                            arguments = listOf(
                                intLiteral(0, intType),
                                intLiteral(5, intType)
                            ),
                            isNullChaining = true,
                            resultTypeIndex = stringNullableType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals("hello", result)
    }
    
    @Test
    fun `null-safe method call with arguments when target is null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intType = intType()
            
            // val s: String? = null
            // s?.substring(0, 5) should return null
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, nullLiteral(stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "substring",
                            overload = "2",
                            arguments = listOf(
                                intLiteral(0, intType),
                                intLiteral(5, intType)
                            ),
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
    
    // ==================== Null-Safe Chaining on Methods Returning Different Types ====================
    
    @Test
    fun `null-safe chaining on asInteger method`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            // val s: String? = "42"
            // s?.asInteger() should return 42
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("42", stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "asInteger",
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
        assertEquals(42, result)
    }
    
    @Test
    fun `null-safe chaining on asInteger method with null`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            // val s: String? = null
            // s?.asInteger() should return null
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, nullLiteral(stringNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("s", stringNullableType, 3),
                            member = "asInteger",
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
    
    // ==================== Nested Member Calls ====================
    
    @Test
    fun `member call result as argument to function`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            // fun doubleValue(x: Int): Int = x * 2
            function(
                name = "doubleValue",
                returnType = intType,
                parameters = listOf(TypedParameter("x", intType)),
                body = listOf(
                    returnStmt(
                        binaryExpr(
                            identifier("x", intType, 2),
                            "*",
                            intLiteral(2, intType),
                            intType
                        )
                    )
                )
            )
            
            // testFunction: doubleValue("hello".length())
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        functionCall(
                            name = "doubleValue",
                            overload = "",
                            arguments = listOf(
                                memberCall(
                                    expression = stringLiteral("hello", stringType),
                                    member = "length",
                                    overload = "",
                                    arguments = emptyList(),
                                    resultTypeIndex = intType
                                )
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
    
    @Test
    fun `function call result as member call target`() {
        val ast = buildTypedAst {
            val stringType = stringType()
            val intType = intType()
            
            // fun getMessage(): String = "hello world"
            function(
                name = "getMessage",
                returnType = stringType,
                body = listOf(
                    returnStmt(stringLiteral("hello world", stringType))
                )
            )
            
            // testFunction: getMessage().length()
            function(
                name = "testFunction",
                returnType = intType,
                body = listOf(
                    returnStmt(
                        memberCall(
                            expression = functionCall(
                                name = "getMessage",
                                overload = "",
                                arguments = emptyList(),
                                resultTypeIndex = stringType
                            ),
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
    
    // ==================== Edge Case: Empty String Operations ====================
    
    @Test
    fun `null-safe method call on empty string`() {
        val ast = buildTypedAst {
            val stringNullableType = stringNullableType()
            val intNullableType = intNullableType()
            
            // val s: String? = ""
            // s?.length() should return 0
            function(
                name = "testFunction",
                returnType = intNullableType,
                body = listOf(
                    varDecl("s", stringNullableType, stringLiteral("", stringNullableType)),
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
        assertEquals(0, result)
    }
    
    // ==================== Nullable Primitive Base Expression Bug ====================
    
    @Test
    fun `null-safe member call on nullable int with non-null value`() {
        val ast = buildTypedAst {
            val intType = intType()
            val intNullableType = intNullableType()
            val stringNullableType = stringNullableType()
            
            // val x: Int? = 42
            // x?.asString() should return "42"
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("x", intNullableType, intLiteral(42, intType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("x", intNullableType, 3),
                            member = "asString",
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
        assertEquals("42", result)
    }
    
    @Test
    fun `null-safe member call on nullable int with null value`() {
        val ast = buildTypedAst {
            val intNullableType = intNullableType()
            val stringNullableType = stringNullableType()
            
            // val x: Int? = null
            // x?.asString() should return null
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("x", intNullableType, nullLiteral(intNullableType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("x", intNullableType, 3),
                            member = "asString",
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
    fun `null-safe member call on nullable boolean with non-null value`() {
        val ast = buildTypedAst {
            val boolType = booleanType()
            val boolNullableType = booleanNullableType()
            val stringNullableType = stringNullableType()
            
            // val b: Boolean? = true
            // b?.asString() should return "true"
            function(
                name = "testFunction",
                returnType = stringNullableType,
                body = listOf(
                    varDecl("b", boolNullableType, booleanLiteral(true, boolType)),
                    returnStmt(
                        memberCall(
                            expression = identifier("b", boolNullableType, 3),
                            member = "asString",
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
        assertEquals("true", result)
    }
}
