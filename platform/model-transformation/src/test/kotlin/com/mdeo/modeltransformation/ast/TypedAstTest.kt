package com.mdeo.modeltransformation.ast

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.expression.ast.types.TypedClass
import com.mdeo.expression.ast.types.TypedProperty
import com.mdeo.expression.ast.types.TypedRelation
import com.mdeo.modeltransformation.ast.expressions.TypedExpressionSerializer
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatementSerializer
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedIfMatchStatement
import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElementSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for deserializing the root TypedAst and related types.
 */
class TypedAstTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(TypedExpression::class, TypedExpressionSerializer)
            contextual(TypedTransformationStatement::class, TypedTransformationStatementSerializer)
            contextual(TypedPatternElement::class, TypedPatternElementSerializer)
        }
    }
    
    // ========== Basic TypedAst Deserialization ==========
    
    @Test
    fun `deserialize TypedAst with all fields`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.int", "isNullable": false},
                {"type": "builtin.string", "isNullable": false}
            ],
            "metamodelUri": "file:///path/to/metamodel.mm",
            "statements": [
                {
                    "kind": "match",
                    "pattern": {"elements": []}
                }
            ]
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(2, result.types.size)
        assertEquals("file:///path/to/metamodel.mm", result.metamodelUri)
        assertEquals(1, result.statements.size)
        assertIs<TypedMatchStatement>(result.statements[0])
    }
    
    @Test
    fun `deserialize TypedAst with empty types`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "file:///metamodel.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(0, result.types.size)
        assertEquals(0, result.statements.size)
    }
    
    @Test
    fun `deserialize TypedAst with empty statements`() {
        val jsonString = """{
            "types": [{"type": "builtin.int", "isNullable": false}],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertEquals(0, result.statements.size)
    }
    
    @Test
    fun `deserialize TypedAst with empty metamodelUri`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals("", result.metamodelUri)
    }
    
    // ========== TypedAst with Different Type Variants ==========
    
    @Test
    fun `deserialize TypedAst with class type`() {
        val jsonString = """{
            "types": [
                {"type": "com.example.Person", "isNullable": false}
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertIs<ClassTypeRef>(result.types[0])
        assertEquals("com.example.Person", (result.types[0] as ClassTypeRef).type)
        assertEquals(false, (result.types[0] as ClassTypeRef).isNullable)
    }
    
    @Test
    fun `deserialize TypedAst with nullable class type`() {
        val jsonString = """{
            "types": [
                {"type": "com.example.Person", "isNullable": true}
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertIs<ClassTypeRef>(result.types[0])
        assertEquals(true, (result.types[0] as ClassTypeRef).isNullable)
    }
    
    @Test
    fun `deserialize TypedAst with void type`() {
        val jsonString = """{
            "types": [
                {"kind": "void"}
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertIs<VoidType>(result.types[0])
    }
    
    @Test
    fun `deserialize TypedAst with lambda type`() {
        val jsonString = """{
            "types": [
                {
                    "returnType": {"type": "builtin.int", "isNullable": false},
                    "parameters": [
                        {"name": "x", "type": {"type": "builtin.int", "isNullable": false}}
                    ],
                    "isNullable": false
                }
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertIs<LambdaType>(result.types[0])
    }
    
    @Test
    fun `deserialize TypedAst with multiple types`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.int", "isNullable": false},
                {"type": "builtin.string", "isNullable": true},
                {"type": "builtin.boolean", "isNullable": false},
                {"type": "builtin.double", "isNullable": false},
                {"kind": "void"}
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(5, result.types.size)
    }
    
    // ========== TypedAst with Multiple Statements ==========
    
    @Test
    fun `deserialize TypedAst with multiple statements`() {
        val jsonString = """{
            "types": [{"type": "builtin.int", "isNullable": false}],
            "metamodelUri": "file:///test.mm",
            "statements": [
                {"kind": "match", "pattern": {"elements": []}},
                {"kind": "stop", "keyword": "stop"},
                {"kind": "match", "pattern": {"elements": []}}
            ]
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(3, result.statements.size)
        assertIs<TypedMatchStatement>(result.statements[0])
        assertIs<TypedStopStatement>(result.statements[1])
        assertIs<TypedMatchStatement>(result.statements[2])
    }
    
    @Test
    fun `deserialize TypedAst with nested statements`() {
        val jsonString = """{
            "types": [{"type": "builtin.boolean", "isNullable": false}],
            "metamodelUri": "file:///test.mm",
            "statements": [
                {
                    "kind": "ifMatch",
                    "pattern": {"elements": []},
                    "thenBlock": [
                        {"kind": "stop", "keyword": "stop"}
                    ],
                    "elseBlock": [
                        {"kind": "stop", "keyword": "kill"}
                    ]
                }
            ]
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.statements.size)
        val ifMatch = result.statements[0]
        assertIs<TypedIfMatchStatement>(ifMatch)
        assertEquals(1, ifMatch.thenBlock.size)
        assertEquals(1, ifMatch.elseBlock?.size)
    }
    
    // ========== Edge Cases ==========
    
    @Test
    fun `deserialize TypedAst with special characters in metamodelUri`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "file:///path/with spaces/and-dashes/test_underscore.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals("file:///path/with spaces/and-dashes/test_underscore.mm", result.metamodelUri)
    }
    
    @Test
    fun `deserialize TypedAst with unicode in metamodelUri`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "file:///путь/到/ファイル.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals("file:///путь/到/ファイル.mm", result.metamodelUri)
    }
    
    @Test
    fun `deserialize TypedAst with large type index`() {
        val jsonString = """{
            "types": [
                {"type": "type0", "isNullable": false},
                {"type": "type1", "isNullable": false},
                {"type": "type2", "isNullable": false},
                {"type": "type3", "isNullable": false},
                {"type": "type4", "isNullable": false},
                {"type": "type5", "isNullable": false},
                {"type": "type6", "isNullable": false},
                {"type": "type7", "isNullable": false},
                {"type": "type8", "isNullable": false},
                {"type": "type9", "isNullable": false}
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(10, result.types.size)
        for (i in 0..9) {
            assertEquals("type$i", (result.types[i] as ClassTypeRef).type)
        }
    }
    
    @Test
    fun `deserialize TypedAst with http metamodelUri`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "https://example.com/metamodels/test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals("https://example.com/metamodels/test.mm", result.metamodelUri)
    }
    
    @Test
    fun `deserialize TypedAst with very long type name`() {
        val longTypeName = "com.very.long.package.name.that.keeps.going.and.going.SomeVeryLongClassName"
        val jsonString = """{
            "types": [
                {"type": "$longTypeName", "isNullable": false}
            ],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.types.size)
        assertEquals(longTypeName, (result.types[0] as ClassTypeRef).type)
    }
    
    // ========== Combined Complex Scenarios ==========
    
    @Test
    fun `deserialize complete TypedAst with all statement types`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.boolean", "isNullable": false},
                {"type": "com.example.Person", "isNullable": false}
            ],
            "metamodelUri": "file:///complex/metamodel.mm",
            "statements": [
                {"kind": "match", "pattern": {"elements": []}},
                {
                    "kind": "ifMatch",
                    "pattern": {"elements": []},
                    "thenBlock": [],
                    "elseBlock": null
                },
                {
                    "kind": "whileMatch",
                    "pattern": {"elements": []},
                    "doBlock": []
                },
                {
                    "kind": "untilMatch",
                    "pattern": {"elements": []},
                    "doBlock": []
                },
                {
                    "kind": "forMatch",
                    "pattern": {"elements": []},
                    "doBlock": []
                },
                {
                    "kind": "ifExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": true},
                    "thenBlock": [],
                    "elseIfBranches": [],
                    "elseBlock": null
                },
                {
                    "kind": "whileExpression",
                    "condition": {"kind": "booleanLiteral", "evalType": 0, "value": false},
                    "block": []
                },
                {"kind": "stop", "keyword": "stop"}
            ]
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(2, result.types.size)
        assertEquals(8, result.statements.size)
    }
    
    // ========== TypedClass Deserialization ==========
    
    @Test
    fun `deserialize TypedAst with empty classes list`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "file:///test.mm",
            "classes": [],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(0, result.classes.size)
    }
    
    @Test
    fun `deserialize TypedAst with simple class`() {
        val jsonString = """{
            "types": [{"type": "builtin.string", "isNullable": false}],
            "metamodelUri": "file:///test.mm",
            "classes": [
                {
                    "name": "metamodel.Person",
                    "package": "",
                    "superClasses": [],
                    "properties": [
                        {"name": "firstName", "typeIndex": 0}
                    ],
                    "relations": []
                }
            ],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.classes.size)
        val personClass = result.classes[0]
        assertEquals("metamodel.Person", personClass.name)
        assertEquals(0, personClass.superClasses.size)
        assertEquals(1, personClass.properties.size)
        assertEquals("firstName", personClass.properties[0].name)
        assertEquals(0, personClass.properties[0].typeIndex)
        assertEquals(0, personClass.relations.size)
    }
    
    @Test
    fun `deserialize TypedAst with class having superclasses`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "file:///test.mm",
            "classes": [
                {
                    "name": "metamodel.Employee",
                    "package": "",
                    "superClasses": ["metamodel.Person", "metamodel.Named"],
                    "properties": [],
                    "relations": []
                }
            ],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.classes.size)
        val employeeClass = result.classes[0]
        assertEquals("metamodel.Employee", employeeClass.name)
        assertEquals(2, employeeClass.superClasses.size)
        assertTrue(employeeClass.superClasses.contains("metamodel.Person"))
        assertTrue(employeeClass.superClasses.contains("metamodel.Named"))
    }
    
    @Test
    fun `deserialize TypedAst with class having relations`() {
        val jsonString = """{
            "types": [
                {"type": "metamodel.Department", "isNullable": false},
                {"type": "builtin.List", "isNullable": false, "typeArgs": {"T": {"type": "metamodel.Employee", "isNullable": false}}}
            ],
            "metamodelUri": "file:///test.mm",
            "classes": [
                {
                    "name": "metamodel.Employee",
                    "package": "",
                    "superClasses": [],
                    "properties": [],
                    "relations": [
                        {
                            "property": "department",
                            "oppositeProperty": "employees",
                            "oppositeClassName": "metamodel.Department",
                            "isOutgoing": true,
                            "typeIndex": 0
                        },
                        {
                            "property": "subordinates",
                            "oppositeProperty": "manager",
                            "oppositeClassName": "metamodel.Employee",
                            "isOutgoing": true,
                            "typeIndex": 1
                        }
                    ]
                }
            ],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.classes.size)
        val employeeClass = result.classes[0]
        assertEquals(2, employeeClass.relations.size)
        
        val worksInRelation = employeeClass.relations[0]
        assertEquals("department", worksInRelation.property)
        assertEquals("employees", worksInRelation.oppositeProperty)
        assertEquals("metamodel.Department", worksInRelation.oppositeClassName)
        assertTrue(worksInRelation.isOutgoing)
        assertEquals(0, worksInRelation.typeIndex)
        
        val managesRelation = employeeClass.relations[1]
        assertEquals("subordinates", managesRelation.property)
        assertEquals("manager", managesRelation.oppositeProperty)
        assertEquals("metamodel.Employee", managesRelation.oppositeClassName)
        assertTrue(managesRelation.isOutgoing)
        assertEquals(1, managesRelation.typeIndex)
    }
    
    @Test
    fun `deserialize TypedAst with incoming relation`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.List", "isNullable": false, "typeArgs": {"T": {"type": "metamodel.Employee", "isNullable": false}}}
            ],
            "metamodelUri": "file:///test.mm",
            "classes": [
                {
                    "name": "metamodel.Department",
                    "package": "",
                    "superClasses": [],
                    "properties": [],
                    "relations": [
                        {
                            "property": "employees",
                            "oppositeProperty": "department",
                            "oppositeClassName": "metamodel.Employee",
                            "isOutgoing": false,
                            "typeIndex": 0
                        }
                    ]
                }
            ],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.classes.size)
        val deptClass = result.classes[0]
        assertEquals(1, deptClass.relations.size)
        
        val employsRelation = deptClass.relations[0]
        assertFalse(employsRelation.isOutgoing)
    }
    
    @Test
    fun `deserialize TypedAst with multiple classes`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.string", "isNullable": false},
                {"type": "builtin.int", "isNullable": false}
            ],
            "metamodelUri": "file:///test.mm",
            "classes": [
                {
                    "name": "metamodel.Person",
                    "package": "",
                    "superClasses": [],
                    "properties": [{"name": "name", "typeIndex": 0}],
                    "relations": []
                },
                {
                    "name": "metamodel.Employee",
                    "package": "",
                    "superClasses": ["metamodel.Person"],
                    "properties": [{"name": "salary", "typeIndex": 1}],
                    "relations": []
                }
            ],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(2, result.classes.size)
        assertEquals("metamodel.Person", result.classes[0].name)
        assertEquals("metamodel.Employee", result.classes[1].name)
        assertTrue(result.classes[1].superClasses.contains("metamodel.Person"))
    }
    
    @Test
    fun `deserialize TypedAst with class having multiple properties`() {
        val jsonString = """{
            "types": [
                {"type": "builtin.string", "isNullable": false},
                {"type": "builtin.int", "isNullable": false},
                {"type": "builtin.boolean", "isNullable": false}
            ],
            "metamodelUri": "file:///test.mm",
            "classes": [
                {
                    "name": "metamodel.Person",
                    "package": "",
                    "superClasses": [],
                    "properties": [
                        {"name": "firstName", "typeIndex": 0},
                        {"name": "lastName", "typeIndex": 0},
                        {"name": "age", "typeIndex": 1},
                        {"name": "isActive", "typeIndex": 2}
                    ],
                    "relations": []
                }
            ],
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(1, result.classes.size)
        val personClass = result.classes[0]
        assertEquals(4, personClass.properties.size)
        assertEquals("firstName", personClass.properties[0].name)
        assertEquals("lastName", personClass.properties[1].name)
        assertEquals("age", personClass.properties[2].name)
        assertEquals("isActive", personClass.properties[3].name)
    }
    
    @Test
    fun `deserialize TypedAst without classes field uses default empty list`() {
        val jsonString = """{
            "types": [],
            "metamodelUri": "file:///test.mm",
            "statements": []
        }"""
        val result = json.decodeFromString(TypedAst.serializer(), jsonString)
        
        assertEquals(0, result.classes.size)
    }
}
