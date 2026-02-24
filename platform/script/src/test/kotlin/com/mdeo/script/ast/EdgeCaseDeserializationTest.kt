package com.mdeo.script.ast

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedExtensionCallExpression
import com.mdeo.script.ast.expressions.TypedExpressionSerializer
import com.mdeo.script.ast.expressions.TypedLambdaExpression
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedLongLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedUnaryExpression
import com.mdeo.expression.ast.statements.TypedAssignmentStatement
import com.mdeo.expression.ast.statements.TypedForStatement
import com.mdeo.expression.ast.statements.TypedIfStatement
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.script.ast.statements.TypedStatementSerializer
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.statements.TypedVariableDeclarationStatement
import com.mdeo.expression.ast.statements.TypedWhileStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.GenericTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.expression.ast.types.ReturnTypeSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Edge case tests for TypedAST deserialization.
 * These tests verify handling of edge cases and potential issues found during code review.
 */
class EdgeCaseDeserializationTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedStatement::class, TypedStatementSerializer)
        }
    }

    // ============================================================
    // Issue 1: Test if statement with elseBlock field completely absent (not null)
    // In TypeScript, elseBlock is optional (?:), so it might not be present at all
    // ============================================================
    
    @Test
    fun `deserialize if statement when elseBlock field is completely absent from JSON`() {
        // Note: This JSON does NOT have an elseBlock field at all
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [{"kind": "break"}],
            "elseIfs": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertNull(result.elseBlock)
    }

    // ============================================================
    // Issue 2: Test return statement with value field completely absent
    // In TypeScript, value is optional (?:), so it might not be present at all
    // ============================================================
    
    @Test
    fun `deserialize return statement when value field is completely absent from JSON`() {
        // Note: This JSON does NOT have a value field at all
        val jsonString = """{"kind": "return"}"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedReturnStatement>(result)
        assertNull(result.value)
    }

    // ============================================================
    // Issue 3: Test variable declaration with initialValue field completely absent
    // In TypeScript, initialValue is optional (?:)
    // ============================================================
    
    @Test
    fun `deserialize variable declaration when initialValue field is completely absent from JSON`() {
        val jsonString = """{
            "kind": "variableDeclaration",
            "name": "myVar",
            "type": 0
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedVariableDeclarationStatement>(result)
        assertNull(result.initialValue)
        assertEquals("myVar", result.name)
        assertEquals(0, result.type)
    }

    // ============================================================
    // Issue 4: Test ClassTypeRef without typeArgs (optional field)
    // ============================================================
    
    @Test
    fun `deserialize ClassTypeRef when typeArgs field is completely absent from JSON`() {
        val jsonString = """{"package": "builtin", "type": "string", "isNullable": false}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("string", result.type)
        assertEquals(false, result.isNullable)
        assertNull(result.typeArgs)
    }

    // ============================================================
    // Issue 5: Test GenericTypeRef without isNullable (optional field)
    // ============================================================
    
    @Test
    fun `deserialize GenericTypeRef when isNullable field is completely absent from JSON`() {
        val jsonString = """{"generic": "T"}"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<GenericTypeRef>(result)
        assertEquals("T", result.generic)
        assertNull(result.isNullable)
    }

    // ============================================================
    // Issue 6: Test empty collections work correctly
    // ============================================================
    
    @Test
    fun `deserialize if statement with empty thenBlock`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfs": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertEquals(0, result.thenBlock.size)
    }
    
    @Test
    fun `deserialize if statement with empty elseBlock`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfs": [],
            "elseBlock": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertEquals(0, result.elseBlock?.size)
    }
    
    @Test
    fun `deserialize function with empty parameters list`() {
        val jsonString = """{
            "name": "noParams",
            "parameters": [],
            "returnType": 0,
            "body": {"body": []}
        }"""
        val result = json.decodeFromString(TypedFunction.serializer(), jsonString)
        
        assertEquals(0, result.parameters.size)
    }

    // ============================================================
    // Issue 7: Test lambda with empty parameters
    // ============================================================
    
    @Test
    fun `deserialize lambda expression with empty parameters list`() {
        val jsonString = """{
            "kind": "lambda",
            "evalType": 0,
            "parameters": [],
            "body": {"body": []}
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLambdaExpression>(result)
        assertEquals(0, result.parameters.size)
    }

    // ============================================================
    // Issue 8: Test extension call with empty arguments
    // ============================================================
    
    @Test
    fun `deserialize extension call with empty arguments list`() {
        val jsonString = """{
            "kind": "extensionCall",
            "evalType": 0,
            "name": "noArgs",
            "arguments": [],
            "overload": ""
        }"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedExtensionCallExpression>(result)
        assertEquals(0, result.arguments.size)
    }

    // ============================================================
    // Issue 9: Test LambdaType in types array with empty parameters
    // ============================================================
    
    @Test
    fun `deserialize LambdaType with empty parameters list`() {
        val jsonString = """{
            "returnType": {"kind": "void"},
            "parameters": [],
            "isNullable": false
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<LambdaType>(result)
        assertEquals(0, result.parameters.size)
    }

    // ============================================================
    // Issue 10: Verify assignment left can be MemberAccessExpression
    // TypeScript allows: TypedIdentifierExpression | TypedMemberAccessExpression
    // ============================================================
    
    @Test
    fun `deserialize assignment with member access on left side`() {
        val jsonString = """{
            "kind": "assignment",
            "left": {
                "kind": "memberAccess",
                "evalType": 0,
                "expression": {"kind": "identifier", "evalType": 1, "name": "obj", "scope": 1},
                "member": "field",
                "isNullChaining": false
            },
            "right": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedAssignmentStatement>(result)
        val left = result.left
        assertIs<TypedMemberAccessExpression>(left)
        assertEquals("field", left.member)
    }
    
    @Test
    fun `deserialize assignment with identifier on left side`() {
        val jsonString = """{
            "kind": "assignment",
            "left": {"kind": "identifier", "evalType": 0, "name": "x", "scope": 1},
            "right": {"kind": "intLiteral", "evalType": 0, "value": "42"}
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedAssignmentStatement>(result)
        assertIs<TypedIdentifierExpression>(result.left)
    }

    // ============================================================
    // Issue 11: Test TypedAst with empty imports and functions
    // ============================================================
    
    @Test
    fun `deserialize TypedAst with empty imports and functions`() {
        val jsonString = """{
            "types": [{"package": "builtin", "type": "int", "isNullable": false}],
            "imports": [],
            "functions": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertEquals(0, result.imports.size)
        assertEquals(0, result.functions.size)
    }
    
    @Test
    fun `deserialize TypedAst with empty types array`() {
        val jsonString = """{
            "types": [],
            "imports": [],
            "functions": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(0, result.types.size)
    }

    // ============================================================
    // Issue 12: Test for loop with empty body
    // ============================================================
    
    @Test
    fun `deserialize for statement with empty body`() {
        val jsonString = """{
            "kind": "for",
            "variableName": "i",
            "variableType": 0,
            "iterable": {"kind": "identifier", "evalType": 1, "name": "items", "scope": 1},
            "body": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedForStatement>(result)
        assertEquals(0, result.body.size)
    }

    // ============================================================
    // Issue 13: Test while statement with empty body
    // ============================================================
    
    @Test
    fun `deserialize while statement with empty body`() {
        val jsonString = """{
            "kind": "while",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "body": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedWhileStatement>(result)
        assertEquals(0, result.body.size)
    }

    // ============================================================
    // Issue 14: Test nested type arguments
    // ============================================================
    
    @Test
    fun `deserialize deeply nested type arguments`() {
        val jsonString = """{
            "package": "builtin",
            "type": "map",
            "isNullable": false,
            "typeArgs": {
                "K": {"package": "builtin", "type": "string", "isNullable": false},
                "V": {
                    "package": "builtin",
                    "type": "List",
                    "isNullable": true,
                    "typeArgs": {
                        "T": {
                            "package": "builtin",
                            "type": "map",
                            "isNullable": false,
                            "typeArgs": {
                                "K": {"package": "builtin", "type": "int", "isNullable": false},
                                "V": {"package": "builtin", "type": "string", "isNullable": true}
                            }
                        }
                    }
                }
            }
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        assertEquals("map", result.type)
        
        val vType = result.typeArgs?.get("V")
        assertIs<ClassTypeRef>(vType)
        assertEquals("List", vType.type)
        assertEquals(true, vType.isNullable)
        
        val innerT = vType.typeArgs?.get("T")
        assertIs<ClassTypeRef>(innerT)
        assertEquals("map", innerT.type)
    }

    // ============================================================
    // Issue 15: Test LambdaType with generic type in parameters
    // ============================================================
    
    @Test
    fun `deserialize LambdaType with generic type parameter`() {
        val jsonString = """{
            "returnType": {"generic": "T"},
            "parameters": [
                {"name": "input", "type": {"generic": "T"}}
            ],
            "isNullable": false
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<LambdaType>(result)
        
        val returnType = result.returnType
        assertIs<GenericTypeRef>(returnType)
        assertEquals("T", returnType.generic)
        
        val paramType = result.parameters[0].type
        assertIs<GenericTypeRef>(paramType)
        assertEquals("T", paramType.generic)
    }

    // ============================================================
    // Issue 16: Test LambdaType as type argument
    // ============================================================
    
    @Test
    fun `deserialize ClassTypeRef with lambda type argument`() {
        val jsonString = """{
            "package": "builtin",
            "type": "List",
            "isNullable": false,
            "typeArgs": {
                "T": {
                    "returnType": {"package": "builtin", "type": "int", "isNullable": false},
                    "parameters": [
                        {"name": "x", "type": {"package": "builtin", "type": "int", "isNullable": false}}
                    ],
                    "isNullable": false
                }
            }
        }"""
        val result = json.decodeFromString(ReturnTypeSerializer, jsonString)
        
        assertIs<ClassTypeRef>(result)
        
        val typeArg = result.typeArgs?.get("T")
        assertIs<LambdaType>(typeArg)
        assertEquals(1, typeArg.parameters.size)
    }

    // ============================================================
    // Issue 17: Test unknown expression kind throws proper error
    // ============================================================
    
    @Test
    fun `throw error for unknown expression kind`() {
        val jsonString = """{"kind": "unknownKind", "evalType": 0}"""
        
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(TypedExpressionSerializer, jsonString)
        }
    }

    // ============================================================
    // Issue 18: Test unknown statement kind throws proper error
    // ============================================================
    
    @Test
    fun `throw error for unknown statement kind`() {
        val jsonString = """{"kind": "unknownStatement"}"""
        
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(TypedStatementSerializer, jsonString)
        }
    }

    // ============================================================
    // Issue 19: Test missing kind field throws proper error
    // ============================================================
    
    @Test
    fun `throw error when expression is missing kind field`() {
        val jsonString = """{"evalType": 0, "value": "test"}"""
        
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(TypedExpressionSerializer, jsonString)
        }
    }
    
    @Test
    fun `throw error when statement is missing kind field`() {
        val jsonString = """{"body": []}"""
        
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(TypedStatementSerializer, jsonString)
        }
    }

    // ============================================================
    // Issue 20: Test all binary operators from TypeScript
    // TypeScript: "+" | "-" | "*" | "/" | "%" | "&&" | "||" | "==" | "!=" | "<" | ">" | "<=" | ">="
    // ============================================================
    
    @Test
    fun `deserialize binary expression with all valid operators`() {
        val operators = listOf("+", "-", "*", "/", "%", "&&", "||", "==", "!=", "<", ">", "<=", ">=")
        
        for (op in operators) {
            val jsonString = """{
                "kind": "binary",
                "evalType": 0,
                "operator": "$op",
                "left": {"kind": "intLiteral", "evalType": 0, "value": "1"},
                "right": {"kind": "intLiteral", "evalType": 0, "value": "2"}
            }"""
            val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
            
            assertIs<TypedBinaryExpression>(result)
            assertEquals(op, result.operator, "Failed for operator: $op")
        }
    }

    // ============================================================
    // Issue 21: Test all unary operators from TypeScript
    // TypeScript: "-" | "!"
    // ============================================================
    
    @Test
    fun `deserialize unary expression with all valid operators`() {
        val operators = listOf("-", "!")
        
        for (op in operators) {
            val jsonString = """{
                "kind": "unary",
                "evalType": 0,
                "operator": "$op",
                "expression": {"kind": "intLiteral", "evalType": 0, "value": "1"}
            }"""
            val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
            
            assertIs<TypedUnaryExpression>(result)
            assertEquals(op, result.operator, "Failed for operator: $op")
        }
    }

    // ============================================================
    // Issue 22: Test scope values including edge cases
    // ============================================================
    
    @Test
    fun `deserialize identifier with scope 0 (global)`() {
        val jsonString = """{"kind": "identifier", "evalType": 0, "name": "globalVar", "scope": 0}"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals(0, result.scope)
    }
    
    @Test
    fun `deserialize identifier with high scope value`() {
        val jsonString = """{"kind": "identifier", "evalType": 0, "name": "deeplyNestedVar", "scope": 100}"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIdentifierExpression>(result)
        assertEquals(100, result.scope)
    }

    // ============================================================
    // Issue 23: Test large integer values as strings
    // ============================================================
    
    @Test
    fun `deserialize int literal with max int value`() {
        val jsonString = """{"kind": "intLiteral", "evalType": 0, "value": "2147483647"}"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedIntLiteralExpression>(result)
        assertEquals("2147483647", result.value)
    }
    
    @Test
    fun `deserialize long literal with large value`() {
        val jsonString = """{"kind": "longLiteral", "evalType": 0, "value": "9223372036854775807"}"""
        val result = json.decodeFromString(TypedExpressionSerializer, jsonString)
        
        assertIs<TypedLongLiteralExpression>(result)
        assertEquals("9223372036854775807", result.value)
    }

    // ============================================================
    // Issue 24: Test deeply nested else-if chains
    // ============================================================
    
    @Test
    fun `deserialize if statement with many else-if clauses`() {
        val jsonString = """{
            "kind": "if",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfs": [
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "thenBlock": []},
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "thenBlock": []},
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "thenBlock": []},
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "thenBlock": []},
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "thenBlock": []}
            ],
            "elseBlock": []
        }"""
        val result = json.decodeFromString(TypedStatementSerializer, jsonString)
        
        assertIs<TypedIfStatement>(result)
        assertEquals(5, result.elseIfs.size)
    }
}
