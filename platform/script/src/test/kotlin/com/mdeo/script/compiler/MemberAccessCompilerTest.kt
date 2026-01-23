package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedParameter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for member access expression compilation.
 * 
 * Member access expressions access properties/fields on objects.
 * For example: "hello".length or person.name
 */
class MemberAccessCompilerTest {
    
    private val helper = CompilerTestHelper()
    
    @Test
    fun `access string length property`() {
        // return "hello".length
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = emptyList(),
                body = listOf(
                    returnStmt(
                        memberAccess(
                            expression = stringLiteral("hello", stringType),
                            member = "length",
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction")
        assertEquals(5, result)
    }
    
    @Test
    fun `access string length property on parameter`() {
        // return str.length
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = listOf(TypedParameter("str", stringType)),
                body = listOf(
                    returnStmt(
                        memberAccess(
                            expression = identifier("str", stringType, 2),
                            member = "length",
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction", "hello world")
        assertEquals(11, result)
    }
    
    @Test
    fun `access string length with empty string`() {
        // return "".length
        val ast = buildTypedAst {
            val intType = intType()
            val stringType = stringType()
            function(
                name = "testFunction",
                returnType = intType,
                parameters = emptyList(),
                body = listOf(
                    returnStmt(
                        memberAccess(
                            expression = stringLiteral("", stringType),
                            member = "length",
                            resultTypeIndex = intType
                        )
                    )
                )
            )
        }
        
        val result = helper.compileAndInvoke(ast, "testFunction")
        assertEquals(0, result)
    }
}
