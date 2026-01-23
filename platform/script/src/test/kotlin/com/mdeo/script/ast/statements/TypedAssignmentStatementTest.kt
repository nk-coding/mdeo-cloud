package com.mdeo.script.ast.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedBinaryExpression
import com.mdeo.script.ast.expressions.TypedIdentifierExpression
import com.mdeo.script.ast.expressions.TypedIntLiteralExpression
import com.mdeo.script.ast.expressions.TypedMemberAccessExpression
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for deserializing TypedAssignmentStatement.
 */
class TypedAssignmentStatementTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @Test
    fun `deserialize simple identifier assignment`() {
        val jsonString = """{
            "kind": "assignment",
            "left": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 2},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "10"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedAssignmentStatement>(result)
        assertEquals(TypedStatementKind.Assignment, result.kind)
        
        val left = result.left
        assertIs<TypedIdentifierExpression>(left)
        assertEquals("x", left.name)
        assertEquals(2, left.scope)
        
        val right = result.right
        assertIs<TypedIntLiteralExpression>(right)
        assertEquals("10", right.value)
    }
    
    @Test
    fun `deserialize member access assignment`() {
        val jsonString = """{
            "kind": "assignment",
            "left": {
                "kind": "memberAccess",
                "evalType": 0,
                "expression": {"kind": "identifier", "evalType": 1, "name": "obj", "scope": 2},
                "member": "value",
                "isNullChaining": false
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedAssignmentStatement>(result)
        
        val left = result.left
        assertIs<TypedMemberAccessExpression>(left)
        assertEquals("value", left.member)
        assertEquals(false, left.isNullChaining)
        
        val targetObj = left.expression
        assertIs<TypedIdentifierExpression>(targetObj)
        assertEquals("obj", targetObj.name)
    }
    
    @Test
    fun `deserialize assignment with binary expression`() {
        val jsonString = """{
            "kind": "assignment",
            "left": {"kind": "identifier", "evalType": 0, "name": "sum", "scope": 3},
            "right": {
                "kind": "binary",
                "evalType": 0,
                "operator": "+",
                "left": {"kind": "identifier", "evalType": 0, "name": "a", "scope": 2},
                "right": {"kind": "identifier", "evalType": 0, "name": "b", "scope": 2}
            }
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedAssignmentStatement>(result)
        
        val right = result.right
        assertIs<TypedBinaryExpression>(right)
        assertEquals("+", right.operator)
    }
    
    @Test
    fun `deserialize nested member access assignment`() {
        val jsonString = """{
            "kind": "assignment",
            "left": {
                "kind": "memberAccess",
                "evalType": 0,
                "expression": {
                    "kind": "memberAccess",
                    "evalType": 1,
                    "expression": {"kind": "identifier", "evalType": 2, "name": "obj", "scope": 2},
                    "member": "inner",
                    "isNullChaining": false
                },
                "member": "value",
                "isNullChaining": false
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "100"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedAssignmentStatement>(result)
        
        val left = result.left
        assertIs<TypedMemberAccessExpression>(left)
        assertEquals("value", left.member)
        
        val innerAccess = left.expression
        assertIs<TypedMemberAccessExpression>(innerAccess)
        assertEquals("inner", innerAccess.member)
    }
}
