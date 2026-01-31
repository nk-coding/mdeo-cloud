package com.mdeo.modeltransformation.ast.patterns

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for TypedPattern and pattern composition.
 */
class TypedPatternTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }
    
    // ========== Empty Pattern Tests ==========
    
    @Test
    fun `deserialize TypedPattern with empty elements`() {
        val jsonString = """{"elements": []}"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(0, result.elements.size)
    }
    
    // ========== Single Element Patterns ==========
    
    @Test
    fun `deserialize TypedPattern with single variable element`() {
        val jsonString = """{
            "elements": [
                {"kind": "variable", "variable": {"name": "x", "type": 0}}
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(1, result.elements.size)
        assertIs<TypedPatternVariableElement>(result.elements[0])
    }
    
    @Test
    fun `deserialize TypedPattern with single object instance element`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "objectInstance",
                    "objectInstance": {
                        "name": "person",
                        "className": "Person",
                        "properties": []
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(1, result.elements.size)
        assertIs<TypedPatternObjectInstanceElement>(result.elements[0])
    }
    
    @Test
    fun `deserialize TypedPattern with single link element`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "link",
                    "link": {
                        "source": {"objectName": "a"},
                        "target": {"objectName": "b"}
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(1, result.elements.size)
        assertIs<TypedPatternLinkElement>(result.elements[0])
    }
    
    @Test
    fun `deserialize TypedPattern with single where clause element`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "whereClause",
                    "whereClause": {
                        "expression": {"kind": "booleanLiteral", "evalType": 0, "value": true}
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(1, result.elements.size)
        assertIs<TypedPatternWhereClauseElement>(result.elements[0])
    }
    
    // ========== Multiple Elements Patterns ==========
    
    @Test
    fun `deserialize TypedPattern with multiple variables`() {
        val jsonString = """{
            "elements": [
                {"kind": "variable", "variable": {"name": "x", "type": 0}},
                {"kind": "variable", "variable": {"name": "y", "type": 1}},
                {"kind": "variable", "variable": {"name": "z", "type": 2}}
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(3, result.elements.size)
        result.elements.forEach { assertIs<TypedPatternVariableElement>(it) }
        assertEquals("x", (result.elements[0] as TypedPatternVariableElement).variable.name)
        assertEquals("y", (result.elements[1] as TypedPatternVariableElement).variable.name)
        assertEquals("z", (result.elements[2] as TypedPatternVariableElement).variable.name)
    }
    
    @Test
    fun `deserialize TypedPattern with multiple object instances`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "objectInstance",
                    "objectInstance": {"name": "p1", "className": "Person", "properties": []}
                },
                {
                    "kind": "objectInstance",
                    "objectInstance": {"name": "p2", "className": "Person", "properties": []}
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(2, result.elements.size)
        assertEquals("p1", (result.elements[0] as TypedPatternObjectInstanceElement).objectInstance.name)
        assertEquals("p2", (result.elements[1] as TypedPatternObjectInstanceElement).objectInstance.name)
    }
    
    @Test
    fun `deserialize TypedPattern with multiple links`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "link",
                    "link": {"source": {"objectName": "a"}, "target": {"objectName": "b"}}
                },
                {
                    "kind": "link",
                    "link": {"source": {"objectName": "b"}, "target": {"objectName": "c"}}
                },
                {
                    "kind": "link",
                    "link": {"source": {"objectName": "c"}, "target": {"objectName": "a"}}
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(3, result.elements.size)
        result.elements.forEach { assertIs<TypedPatternLinkElement>(it) }
    }
    
    @Test
    fun `deserialize TypedPattern with multiple where clauses`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "whereClause",
                    "whereClause": {"expression": {"kind": "booleanLiteral", "evalType": 0, "value": true}}
                },
                {
                    "kind": "whereClause",
                    "whereClause": {"expression": {"kind": "booleanLiteral", "evalType": 0, "value": false}}
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(2, result.elements.size)
        result.elements.forEach { assertIs<TypedPatternWhereClauseElement>(it) }
    }
    
    // ========== Mixed Element Patterns ==========
    
    @Test
    fun `deserialize TypedPattern with mixed element types`() {
        val jsonString = """{
            "elements": [
                {"kind": "variable", "variable": {"name": "x", "type": 0}},
                {
                    "kind": "objectInstance",
                    "objectInstance": {"name": "obj", "className": "MyClass", "properties": []}
                },
                {
                    "kind": "link",
                    "link": {"source": {"objectName": "obj"}, "target": {"objectName": "other"}}
                },
                {
                    "kind": "whereClause",
                    "whereClause": {"expression": {"kind": "booleanLiteral", "evalType": 0, "value": true}}
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(4, result.elements.size)
        assertIs<TypedPatternVariableElement>(result.elements[0])
        assertIs<TypedPatternObjectInstanceElement>(result.elements[1])
        assertIs<TypedPatternLinkElement>(result.elements[2])
        assertIs<TypedPatternWhereClauseElement>(result.elements[3])
    }
    
    @Test
    fun `deserialize TypedPattern with objects and links`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "objectInstance",
                    "objectInstance": {"name": "parent", "className": "Person", "properties": []}
                },
                {
                    "kind": "objectInstance",
                    "objectInstance": {"name": "child", "className": "Person", "properties": []}
                },
                {
                    "kind": "link",
                    "link": {
                        "source": {"objectName": "parent", "propertyName": "children"},
                        "target": {"objectName": "child"}
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(3, result.elements.size)
        assertIs<TypedPatternObjectInstanceElement>(result.elements[0])
        assertIs<TypedPatternObjectInstanceElement>(result.elements[1])
        assertIs<TypedPatternLinkElement>(result.elements[2])
    }
    
    // ========== Complex Pattern Scenarios ==========
    
    @Test
    fun `deserialize TypedPattern with create and delete modifiers`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "objectInstance",
                    "objectInstance": {"modifier": "delete", "name": "old", "className": "Node", "properties": []}
                },
                {
                    "kind": "objectInstance",
                    "objectInstance": {"modifier": "create", "name": "new", "className": "Node", "properties": []}
                },
                {
                    "kind": "link",
                    "link": {
                        "modifier": "delete",
                        "source": {"objectName": "parent"},
                        "target": {"objectName": "old"}
                    }
                },
                {
                    "kind": "link",
                    "link": {
                        "modifier": "create",
                        "source": {"objectName": "parent"},
                        "target": {"objectName": "new"}
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(4, result.elements.size)
        assertEquals("delete", (result.elements[0] as TypedPatternObjectInstanceElement).objectInstance.modifier)
        assertEquals("create", (result.elements[1] as TypedPatternObjectInstanceElement).objectInstance.modifier)
        assertEquals("delete", (result.elements[2] as TypedPatternLinkElement).link.modifier)
        assertEquals("create", (result.elements[3] as TypedPatternLinkElement).link.modifier)
    }
    
    @Test
    fun `deserialize TypedPattern with objects having properties`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "objectInstance",
                    "objectInstance": {
                        "name": "person",
                        "className": "Person",
                        "properties": [
                            {"propertyName": "name", "operator": "=", "value": {"kind": "intLiteral", "evalType": 0, "value": "1"}},
                            {"propertyName": "age", "operator": "==", "value": {"kind": "intLiteral", "evalType": 0, "value": "25"}}
                        ]
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(1, result.elements.size)
        val objElement = result.elements[0] as TypedPatternObjectInstanceElement
        assertEquals(2, objElement.objectInstance.properties.size)
    }
    
    @Test
    fun `deserialize TypedPattern with forbid modifier`() {
        val jsonString = """{
            "elements": [
                {
                    "kind": "objectInstance",
                    "objectInstance": {"modifier": "forbid", "name": "forbidden", "className": "BadNode", "properties": []}
                },
                {
                    "kind": "link",
                    "link": {
                        "modifier": "forbid",
                        "source": {"objectName": "a"},
                        "target": {"objectName": "forbidden"}
                    }
                }
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(2, result.elements.size)
        assertEquals("forbid", (result.elements[0] as TypedPatternObjectInstanceElement).objectInstance.modifier)
        assertEquals("forbid", (result.elements[1] as TypedPatternLinkElement).link.modifier)
    }
    
    // ========== Large Pattern Tests ==========
    
    @Test
    fun `deserialize TypedPattern with many elements`() {
        val elements = (1..10).map { i ->
            """{"kind": "variable", "variable": {"name": "var$i", "type": $i}}"""
        }.joinToString(",")
        
        val jsonString = """{"elements": [$elements]}"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(10, result.elements.size)
        for (i in 1..10) {
            val element = result.elements[i - 1] as TypedPatternVariableElement
            assertEquals("var$i", element.variable.name)
            assertEquals(i, element.variable.type)
        }
    }
    
    @Test
    fun `deserialize TypedPattern with complex graph structure`() {
        val jsonString = """{
            "elements": [
                {"kind": "objectInstance", "objectInstance": {"name": "n1", "className": "Node", "properties": []}},
                {"kind": "objectInstance", "objectInstance": {"name": "n2", "className": "Node", "properties": []}},
                {"kind": "objectInstance", "objectInstance": {"name": "n3", "className": "Node", "properties": []}},
                {"kind": "objectInstance", "objectInstance": {"name": "n4", "className": "Node", "properties": []}},
                {"kind": "link", "link": {"source": {"objectName": "n1"}, "target": {"objectName": "n2"}}},
                {"kind": "link", "link": {"source": {"objectName": "n2"}, "target": {"objectName": "n3"}}},
                {"kind": "link", "link": {"source": {"objectName": "n3"}, "target": {"objectName": "n4"}}},
                {"kind": "link", "link": {"source": {"objectName": "n4"}, "target": {"objectName": "n1"}}},
                {"kind": "link", "link": {"source": {"objectName": "n1"}, "target": {"objectName": "n3"}}},
                {"kind": "link", "link": {"source": {"objectName": "n2"}, "target": {"objectName": "n4"}}},
                {"kind": "whereClause", "whereClause": {"expression": {"kind": "booleanLiteral", "evalType": 0, "value": true}}}
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(11, result.elements.size)
        
        val objects = result.elements.filterIsInstance<TypedPatternObjectInstanceElement>()
        val links = result.elements.filterIsInstance<TypedPatternLinkElement>()
        val whereClauses = result.elements.filterIsInstance<TypedPatternWhereClauseElement>()
        
        assertEquals(4, objects.size)
        assertEquals(6, links.size)
        assertEquals(1, whereClauses.size)
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `deserialize TypedPattern with variable having zero type index`() {
        val jsonString = """{
            "elements": [
                {"kind": "variable", "variable": {"name": "zero", "type": 0}}
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        val varElement = result.elements[0] as TypedPatternVariableElement
        assertEquals(0, varElement.variable.type)
    }
    
    @Test
    fun `deserialize TypedPattern with objects of different classes`() {
        val jsonString = """{
            "elements": [
                {"kind": "objectInstance", "objectInstance": {"name": "p", "className": "Person", "properties": []}},
                {"kind": "objectInstance", "objectInstance": {"name": "a", "className": "Address", "properties": []}},
                {"kind": "objectInstance", "objectInstance": {"name": "c", "className": "Company", "properties": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(3, result.elements.size)
        assertEquals("Person", (result.elements[0] as TypedPatternObjectInstanceElement).objectInstance.className)
        assertEquals("Address", (result.elements[1] as TypedPatternObjectInstanceElement).objectInstance.className)
        assertEquals("Company", (result.elements[2] as TypedPatternObjectInstanceElement).objectInstance.className)
    }
    
    @Test
    fun `deserialize TypedPattern preserves element order`() {
        val jsonString = """{
            "elements": [
                {"kind": "variable", "variable": {"name": "first", "type": 0}},
                {"kind": "objectInstance", "objectInstance": {"name": "second", "className": "C", "properties": []}},
                {"kind": "link", "link": {"source": {"objectName": "a"}, "target": {"objectName": "b"}}},
                {"kind": "whereClause", "whereClause": {"expression": {"kind": "booleanLiteral", "evalType": 0, "value": true}}}
            ]
        }"""
        val result = json.decodeFromString(TypedPattern.serializer(), jsonString)
        
        assertEquals(4, result.elements.size)
        assertIs<TypedPatternVariableElement>(result.elements[0])
        assertIs<TypedPatternObjectInstanceElement>(result.elements[1])
        assertIs<TypedPatternLinkElement>(result.elements[2])
        assertIs<TypedPatternWhereClauseElement>(result.elements[3])
    }
}
