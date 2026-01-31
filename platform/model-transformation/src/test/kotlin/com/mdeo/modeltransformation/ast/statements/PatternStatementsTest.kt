package com.mdeo.modeltransformation.ast.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import com.mdeo.modeltransformation.ast.patterns.TypedPatternVariableElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for pattern-based transformation statements.
 */
class PatternStatementsTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }
    
    // ========== TypedMatchStatement Tests ==========
    
    @Test
    fun `deserialize TypedMatchStatement with empty pattern`() {
        val jsonString = """{
            "kind": "match",
            "pattern": {"elements": []}
        }"""
        val result = json.decodeFromString(TypedMatchStatement.serializer(), jsonString)
        
        assertEquals("match", result.kind)
        assertEquals(0, result.pattern.elements.size)
    }
    
    @Test
    fun `deserialize TypedMatchStatement with pattern elements`() {
        val jsonString = """{
            "kind": "match",
            "pattern": {
                "elements": [
                    {"kind": "variable", "variable": {"name": "x", "type": 0}},
                    {"kind": "objectInstance", "objectInstance": {"name": "obj", "className": "MyClass", "properties": []}}
                ]
            }
        }"""
        val result = json.decodeFromString(TypedMatchStatement.serializer(), jsonString)
        
        assertEquals(2, result.pattern.elements.size)
        assertIs<TypedPatternVariableElement>(result.pattern.elements[0])
        assertIs<TypedPatternObjectInstanceElement>(result.pattern.elements[1])
    }
    
    @Test
    fun `deserialize TypedMatchStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "match",
            "pattern": {"elements": []}
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedMatchStatement>(result)
    }
    
    // ========== TypedIfMatchStatement Tests ==========
    
    @Test
    fun `deserialize TypedIfMatchStatement with empty blocks`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": []
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals("ifMatch", result.kind)
        assertEquals(0, result.pattern.elements.size)
        assertEquals(0, result.thenBlock.size)
        assertNull(result.elseBlock)
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement with null elseBlock`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [],
            "elseBlock": null
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertNull(result.elseBlock)
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement with elseBlock`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [],
            "elseBlock": []
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(0, result.elseBlock?.size)
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement with statements in thenBlock`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [
                {"kind": "stop", "keyword": "stop"}
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.thenBlock.size)
        assertIs<TypedStopStatement>(result.thenBlock[0])
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement with statements in elseBlock`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [],
            "elseBlock": [
                {"kind": "stop", "keyword": "kill"}
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.elseBlock?.size)
        assertIs<TypedStopStatement>(result.elseBlock?.get(0))
        assertEquals("kill", (result.elseBlock?.get(0) as TypedStopStatement).keyword)
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement with multiple statements in blocks`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "stop", "keyword": "stop"}
            ],
            "elseBlock": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "stop", "keyword": "kill"}
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(2, result.thenBlock.size)
        assertEquals(3, result.elseBlock?.size)
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement with nested ifMatch`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [
                {
                    "kind": "ifMatch",
                    "pattern": {"elements": []},
                    "thenBlock": [{"kind": "stop", "keyword": "stop"}],
                    "elseBlock": null
                }
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.thenBlock.size)
        val nested = result.thenBlock[0]
        assertIs<TypedIfMatchStatement>(nested)
        assertEquals(1, nested.thenBlock.size)
    }
    
    @Test
    fun `deserialize TypedIfMatchStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": []
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedIfMatchStatement>(result)
    }
    
    // ========== TypedWhileMatchStatement Tests ==========
    
    @Test
    fun `deserialize TypedWhileMatchStatement with empty blocks`() {
        val jsonString = """{
            "kind": "whileMatch",
            "pattern": {"elements": []},
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedWhileMatchStatement.serializer(), jsonString)
        
        assertEquals("whileMatch", result.kind)
        assertEquals(0, result.pattern.elements.size)
        assertEquals(0, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedWhileMatchStatement with pattern`() {
        val jsonString = """{
            "kind": "whileMatch",
            "pattern": {
                "elements": [
                    {"kind": "objectInstance", "objectInstance": {"name": "node", "className": "Node", "properties": []}}
                ]
            },
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedWhileMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.pattern.elements.size)
    }
    
    @Test
    fun `deserialize TypedWhileMatchStatement with doBlock statements`() {
        val jsonString = """{
            "kind": "whileMatch",
            "pattern": {"elements": []},
            "doBlock": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "stop", "keyword": "stop"}
            ]
        }"""
        val result = json.decodeFromString(TypedWhileMatchStatement.serializer(), jsonString)
        
        assertEquals(2, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedWhileMatchStatement with nested while`() {
        val jsonString = """{
            "kind": "whileMatch",
            "pattern": {"elements": []},
            "doBlock": [
                {
                    "kind": "whileMatch",
                    "pattern": {"elements": []},
                    "doBlock": []
                }
            ]
        }"""
        val result = json.decodeFromString(TypedWhileMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.doBlock.size)
        assertIs<TypedWhileMatchStatement>(result.doBlock[0])
    }
    
    @Test
    fun `deserialize TypedWhileMatchStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "whileMatch",
            "pattern": {"elements": []},
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedWhileMatchStatement>(result)
    }
    
    // ========== TypedUntilMatchStatement Tests ==========
    
    @Test
    fun `deserialize TypedUntilMatchStatement with empty blocks`() {
        val jsonString = """{
            "kind": "untilMatch",
            "pattern": {"elements": []},
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedUntilMatchStatement.serializer(), jsonString)
        
        assertEquals("untilMatch", result.kind)
        assertEquals(0, result.pattern.elements.size)
        assertEquals(0, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedUntilMatchStatement with pattern`() {
        val jsonString = """{
            "kind": "untilMatch",
            "pattern": {
                "elements": [
                    {"kind": "variable", "variable": {"name": "done", "type": 0}}
                ]
            },
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedUntilMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.pattern.elements.size)
    }
    
    @Test
    fun `deserialize TypedUntilMatchStatement with doBlock statements`() {
        val jsonString = """{
            "kind": "untilMatch",
            "pattern": {"elements": []},
            "doBlock": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "match", "pattern": {"elements": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedUntilMatchStatement.serializer(), jsonString)
        
        assertEquals(3, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedUntilMatchStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "untilMatch",
            "pattern": {"elements": []},
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedUntilMatchStatement>(result)
    }
    
    // ========== TypedForMatchStatement Tests ==========
    
    @Test
    fun `deserialize TypedForMatchStatement with empty blocks`() {
        val jsonString = """{
            "kind": "forMatch",
            "pattern": {"elements": []},
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedForMatchStatement.serializer(), jsonString)
        
        assertEquals("forMatch", result.kind)
        assertEquals(0, result.pattern.elements.size)
        assertEquals(0, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedForMatchStatement with pattern`() {
        val jsonString = """{
            "kind": "forMatch",
            "pattern": {
                "elements": [
                    {"kind": "objectInstance", "objectInstance": {"name": "item", "className": "Item", "properties": []}}
                ]
            },
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedForMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.pattern.elements.size)
    }
    
    @Test
    fun `deserialize TypedForMatchStatement with doBlock statements`() {
        val jsonString = """{
            "kind": "forMatch",
            "pattern": {"elements": []},
            "doBlock": [
                {"kind": "stop", "keyword": "stop"}
            ]
        }"""
        val result = json.decodeFromString(TypedForMatchStatement.serializer(), jsonString)
        
        assertEquals(1, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedForMatchStatement with complex pattern`() {
        val jsonString = """{
            "kind": "forMatch",
            "pattern": {
                "elements": [
                    {"kind": "objectInstance", "objectInstance": {"name": "a", "className": "A", "properties": []}},
                    {"kind": "objectInstance", "objectInstance": {"name": "b", "className": "B", "properties": []}},
                    {"kind": "link", "link": {"source": {"objectName": "a"}, "target": {"objectName": "b"}}},
                    {"kind": "whereClause", "whereClause": {"expression": {"kind": "booleanLiteral", "evalType": 0, "value": true}}}
                ]
            },
            "doBlock": [
                {"kind": "match", "pattern": {"elements": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedForMatchStatement.serializer(), jsonString)
        
        assertEquals(4, result.pattern.elements.size)
        assertEquals(1, result.doBlock.size)
    }
    
    @Test
    fun `deserialize TypedForMatchStatement as polymorphic TransformationStatement`() {
        val jsonString = """{
            "kind": "forMatch",
            "pattern": {"elements": []},
            "doBlock": []
        }"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedForMatchStatement>(result)
    }
    
    // ========== TypedStopStatement Tests ==========
    
    @Test
    fun `deserialize TypedStopStatement with stop keyword`() {
        val jsonString = """{"kind": "stop", "keyword": "stop"}"""
        val result = json.decodeFromString(TypedStopStatement.serializer(), jsonString)
        
        assertEquals("stop", result.kind)
        assertEquals("stop", result.keyword)
    }
    
    @Test
    fun `deserialize TypedStopStatement with kill keyword`() {
        val jsonString = """{"kind": "stop", "keyword": "kill"}"""
        val result = json.decodeFromString(TypedStopStatement.serializer(), jsonString)
        
        assertEquals("stop", result.kind)
        assertEquals("kill", result.keyword)
    }
    
    @Test
    fun `deserialize TypedStopStatement with custom keyword`() {
        val jsonString = """{"kind": "stop", "keyword": "abort"}"""
        val result = json.decodeFromString(TypedStopStatement.serializer(), jsonString)
        
        assertEquals("abort", result.keyword)
    }
    
    @Test
    fun `deserialize TypedStopStatement with empty keyword`() {
        val jsonString = """{"kind": "stop", "keyword": ""}"""
        val result = json.decodeFromString(TypedStopStatement.serializer(), jsonString)
        
        assertEquals("", result.keyword)
    }
    
    @Test
    fun `deserialize TypedStopStatement as polymorphic TransformationStatement`() {
        val jsonString = """{"kind": "stop", "keyword": "stop"}"""
        val result = json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        
        assertIs<TypedStopStatement>(result)
    }
    
    // ========== Polymorphic Serializer Tests ==========
    
    @Test
    fun `deserialize unknown statement kind throws exception`() {
        val jsonString = """{"kind": "unknownStatement", "data": {}}"""
        
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        }
    }
    
    @Test
    fun `deserialize statement without kind throws exception`() {
        val jsonString = """{"pattern": {"elements": []}}"""
        
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(TypedTransformationStatementSerializer, jsonString)
        }
    }
    
    // ========== Complex Nesting Tests ==========
    
    @Test
    fun `deserialize deeply nested statements`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [
                {
                    "kind": "whileMatch",
                    "pattern": {"elements": []},
                    "doBlock": [
                        {
                            "kind": "forMatch",
                            "pattern": {"elements": []},
                            "doBlock": [
                                {
                                    "kind": "untilMatch",
                                    "pattern": {"elements": []},
                                    "doBlock": [
                                        {"kind": "stop", "keyword": "stop"}
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        val whileMatch = result.thenBlock[0] as TypedWhileMatchStatement
        val forMatch = whileMatch.doBlock[0] as TypedForMatchStatement
        val untilMatch = forMatch.doBlock[0] as TypedUntilMatchStatement
        val stop = untilMatch.doBlock[0] as TypedStopStatement
        
        assertEquals("stop", stop.keyword)
    }
    
    @Test
    fun `deserialize multiple statement types in same block`() {
        val jsonString = """{
            "kind": "ifMatch",
            "pattern": {"elements": []},
            "thenBlock": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "whileMatch", "pattern": {"elements": []}, "doBlock": []},
                {"kind": "untilMatch", "pattern": {"elements": []}, "doBlock": []},
                {"kind": "forMatch", "pattern": {"elements": []}, "doBlock": []},
                {"kind": "ifMatch", "pattern": {"elements": []}, "thenBlock": []},
                {"kind": "stop", "keyword": "stop"}
            ]
        }"""
        val result = json.decodeFromString(TypedIfMatchStatement.serializer(), jsonString)
        
        assertEquals(6, result.thenBlock.size)
        assertIs<TypedMatchStatement>(result.thenBlock[0])
        assertIs<TypedWhileMatchStatement>(result.thenBlock[1])
        assertIs<TypedUntilMatchStatement>(result.thenBlock[2])
        assertIs<TypedForMatchStatement>(result.thenBlock[3])
        assertIs<TypedIfMatchStatement>(result.thenBlock[4])
        assertIs<TypedStopStatement>(result.thenBlock[5])
    }
}
