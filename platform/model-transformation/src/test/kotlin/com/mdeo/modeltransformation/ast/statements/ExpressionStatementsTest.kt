package com.mdeo.modeltransformation.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for expression-based transformation statements.
 */
class ExpressionStatementsTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }
    
    // ========== TypedElseIfBranch Tests ==========
    
    @Test
    fun `deserialize TypedElseIfBranch with empty block`() {
        val jsonString = """{
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": []
        }"""
        val result = json.decodeFromString(TypedElseIfBranch.serializer(), jsonString)
        
        assertIs<TypedBooleanLiteralExpression>(result.condition)
        assertEquals(0, result.block.size)
    }
    
    @Test
    fun `deserialize TypedElseIfBranch with true condition`() {
        val jsonString = """{
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": []
        }"""
        val result = json.decodeFromString(TypedElseIfBranch.serializer(), jsonString)
        
        val cond = result.condition as TypedBooleanLiteralExpression
        assertEquals(true, cond.value)
    }
    
    @Test
    fun `deserialize TypedElseIfBranch with false condition`() {
        val jsonString = """{
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
            "block": []
        }"""
        val result = json.decodeFromString(TypedElseIfBranch.serializer(), jsonString)
        
        val cond = result.condition as TypedBooleanLiteralExpression
        assertEquals(false, cond.value)
    }
    
    @Test
    fun `deserialize TypedElseIfBranch with statements in block`() {
        val jsonString = """{
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": [
                {"kind": "stop", "keyword": "stop"},
                {"kind": "match", "pattern": {"elements": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedElseIfBranch.serializer(), jsonString)
        
        assertEquals(2, result.block.size)
        assertIs<TypedStopStatement>(result.block[0])
        assertIs<TypedMatchStatement>(result.block[1])
    }
    
    @Test
    fun `deserialize TypedElseIfBranch with integer condition`() {
        val jsonString = """{
            "condition": {"kind": "intLiteral", "evalType": 1, "value": "42"},
            "block": []
        }"""
        val result = json.decodeFromString(TypedElseIfBranch.serializer(), jsonString)
        
        assertIs<TypedIntLiteralExpression>(result.condition)
        assertEquals("42", result.condition.value)
    }
    
    // ========== TypedIfExpressionStatement Tests ==========
    
    @Test
    fun `deserialize TypedIfExpressionStatement with empty blocks`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals("ifExpression", result.kind)
        assertIs<TypedBooleanLiteralExpression>(result.condition)
        assertEquals(0, result.thenBlock.size)
        assertEquals(0, result.elseIfBranches.size)
        assertNull(result.elseBlock)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with null elseBlock`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": [],
            "elseBlock": null
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertNull(result.elseBlock)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with elseBlock`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": [],
            "elseBlock": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(0, result.elseBlock?.size)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with statements in thenBlock`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {"kind": "stop", "keyword": "stop"}
            ],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(1, result.thenBlock.size)
        assertIs<TypedStopStatement>(result.thenBlock[0])
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with statements in elseBlock`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": [],
            "elseBlock": [
                {"kind": "stop", "keyword": "kill"},
                {"kind": "match", "pattern": {"elements": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(2, result.elseBlock?.size)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with one elseIfBranch`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": [
                {
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "block": []
                }
            ]
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(1, result.elseIfBranches.size)
        assertIs<TypedBooleanLiteralExpression>(result.elseIfBranches[0].condition)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with multiple elseIfBranches`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": [
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "block": []},
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": true}, "block": []},
                {"condition": {"kind": "booleanLiteral", "evalType": 0, "value": false}, "block": []}
            ]
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(3, result.elseIfBranches.size)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with elseIfBranches having statements`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [{"kind": "stop", "keyword": "stop"}],
            "elseIfBranches": [
                {
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "block": [
                        {"kind": "match", "pattern": {"elements": []}},
                        {"kind": "stop", "keyword": "stop"}
                    ]
                },
                {
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                    "block": [{"kind": "stop", "keyword": "kill"}]
                }
            ],
            "elseBlock": [{"kind": "stop", "keyword": "abort"}]
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(1, result.thenBlock.size)
        assertEquals(2, result.elseIfBranches.size)
        assertEquals(2, result.elseIfBranches[0].block.size)
        assertEquals(1, result.elseIfBranches[1].block.size)
        assertEquals(1, result.elseBlock?.size)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with nested ifExpression`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {
                    "kind": "ifExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "thenBlock": [{"kind": "stop", "keyword": "stop"}],
                    "elseIfBranches": [],
                    "elseBlock": null
                }
            ],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(1, result.thenBlock.size)
        val nested = result.thenBlock[0]
        assertIs<TypedIfExpressionStatement>(nested)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedIfExpressionStatement>(result)
    }
    
    @Test
    fun `deserialize TypedIfExpressionStatement with integer condition`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "intLiteral", "evalType": 1, "value": "1"},
            "thenBlock": [],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertIs<TypedIntLiteralExpression>(result.condition)
    }
    
    // ========== TypedWhileExpressionStatement Tests ==========
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with empty block`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": []
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        assertEquals("whileExpression", result.kind)
        assertIs<TypedBooleanLiteralExpression>(result.condition)
        assertEquals(0, result.block.size)
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with true condition`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": []
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        val cond = result.condition as TypedBooleanLiteralExpression
        assertEquals(true, cond.value)
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with false condition`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
            "block": []
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        val cond = result.condition as TypedBooleanLiteralExpression
        assertEquals(false, cond.value)
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with statements in block`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "stop", "keyword": "stop"}
            ]
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        assertEquals(2, result.block.size)
        assertIs<TypedMatchStatement>(result.block[0])
        assertIs<TypedStopStatement>(result.block[1])
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with many statements`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        assertEquals(5, result.block.size)
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with nested while`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": [
                {
                    "kind": "whileExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "block": [{"kind": "stop", "keyword": "stop"}]
                }
            ]
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        assertEquals(1, result.block.size)
        val nested = result.block[0]
        assertIs<TypedWhileExpressionStatement>(nested)
        assertEquals(1, nested.block.size)
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement with integer condition`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "intLiteral", "evalType": 1, "value": "100"},
            "block": []
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        assertIs<TypedIntLiteralExpression>(result.condition)
        assertEquals("100", result.condition.value)
    }
    
    @Test
    fun `deserialize TypedWhileExpressionStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": []
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedWhileExpressionStatement>(result)
    }
    
    // ========== Mixed Expression and Pattern Statements Tests ==========
    
    @Test
    fun `deserialize ifExpression with pattern statements inside`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "ifMatch", "pattern": {"elements": []}, "thenBlock": []},
                {"kind": "whileMatch", "pattern": {"elements": []}, "doBlock": []},
                {"kind": "forMatch", "pattern": {"elements": []}, "doBlock": []}
            ],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(4, result.thenBlock.size)
        assertIs<TypedMatchStatement>(result.thenBlock[0])
        assertIs<TypedIfMatchStatement>(result.thenBlock[1])
        assertIs<TypedWhileMatchStatement>(result.thenBlock[2])
        assertIs<TypedForMatchStatement>(result.thenBlock[3])
    }
    
    @Test
    fun `deserialize whileExpression with pattern statements inside`() {
        val jsonString = """{
            "kind": "whileExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "block": [
                {"kind": "forMatch", "pattern": {"elements": []}, "doBlock": []},
                {"kind": "untilMatch", "pattern": {"elements": []}, "doBlock": []}
            ]
        }"""
        val result = json.decodeFromString(TypedWhileExpressionStatement.serializer(), jsonString)
        
        assertEquals(2, result.block.size)
        assertIs<TypedForMatchStatement>(result.block[0])
        assertIs<TypedUntilMatchStatement>(result.block[1])
    }
    
    @Test
    fun `deserialize ifMatch with expression statements inside`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [
                {
                    "kind": "ifExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                    "thenBlock": [],
                    "elseIfBranches": []
                },
                {
                    "kind": "whileExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "block": []
                }
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(2, result.thenBlock.size)
        assertIs<TypedIfExpressionStatement>(result.thenBlock[0])
        assertIs<TypedWhileExpressionStatement>(result.thenBlock[1])
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `deserialize ifExpression with empty elseIfBranches list`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertTrue(result.elseIfBranches.isEmpty())
    }
    
    @Test
    fun `deserialize deeply nested expression statements`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {
                    "kind": "whileExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                    "block": [
                        {
                            "kind": "ifExpression",
                            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                            "thenBlock": [
                                {
                                    "kind": "whileExpression",
                                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                                    "block": [{"kind": "stop", "keyword": "stop"}]
                                }
                            ],
                            "elseIfBranches": []
                        }
                    ]
                }
            ],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        val while1 = result.thenBlock[0] as TypedWhileExpressionStatement
        val if2 = while1.block[0] as TypedIfExpressionStatement
        val while2 = if2.thenBlock[0] as TypedWhileExpressionStatement
        val stop = while2.block[0] as TypedStopStatement
        
        assertEquals("stop", stop.keyword)
    }
    
    @Test
    fun `deserialize ifExpression with many elseIfBranches`() {
        val branches = (1..10).map { i ->
            """{"condition": {"kind": "intLiteral", "evalType": 0, "value": "$i"}, "block": []}"""
        }.joinToString(",")
        
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [],
            "elseIfBranches": [$branches]
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(10, result.elseIfBranches.size)
        for (i in 1..10) {
            val branch = result.elseIfBranches[i - 1]
            assertIs<TypedIntLiteralExpression>(branch.condition)
            assertEquals("$i", branch.condition.value)
        }
    }
    
    @Test
    fun `deserialize all expression statement types in single block`() {
        val jsonString = """{
            "kind": "ifExpression",
            "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
            "thenBlock": [
                {
                    "kind": "ifExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                    "thenBlock": [],
                    "elseIfBranches": []
                },
                {
                    "kind": "whileExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "block": []
                },
                {"kind": "stop", "keyword": "stop"}
            ],
            "elseIfBranches": []
        }"""
        val result = json.decodeFromString(TypedIfExpressionStatement.serializer(), jsonString)
        
        assertEquals(3, result.thenBlock.size)
        assertIs<TypedIfExpressionStatement>(result.thenBlock[0])
        assertIs<TypedWhileExpressionStatement>(result.thenBlock[1])
        assertIs<TypedStopStatement>(result.thenBlock[2])
    }
}
