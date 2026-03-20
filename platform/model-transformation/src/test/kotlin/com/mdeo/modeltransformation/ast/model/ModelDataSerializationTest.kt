package com.mdeo.modeltransformation.ast.model

import com.mdeo.metamodel.data.ModelData
import com.mdeo.metamodel.data.ModelDataInstance
import com.mdeo.metamodel.data.ModelDataLink
import com.mdeo.metamodel.data.ModelDataPropertyValue
import com.mdeo.metamodel.data.ModelDataPropertyValueSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for serialization and deserialization of ModelData structures.
 *
 * Verifies that all ModelData types can be correctly serialized to JSON
 * and deserialized back to their Kotlin representations.
 */
class ModelDataSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Nested
    inner class ModelDataPropertyValueTests {

        @Test
        fun `deserialize string value`() {
            val jsonString = """"hello world""""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.StringValue>(result)
            assertEquals("hello world", result.value)
        }

        @Test
        fun `deserialize integer number value`() {
            val jsonString = """42"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.NumberValue>(result)
            assertEquals(42.0, result.value)
        }

        @Test
        fun `deserialize floating point number value`() {
            val jsonString = """3.14159"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.NumberValue>(result)
            assertEquals(3.14159, result.value, 0.00001)
        }

        @Test
        fun `deserialize negative number value`() {
            val jsonString = """-99"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.NumberValue>(result)
            assertEquals(-99.0, result.value)
        }

        @Test
        fun `deserialize boolean true value`() {
            val jsonString = """true"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.BooleanValue>(result)
            assertEquals(true, result.value)
        }

        @Test
        fun `deserialize boolean false value`() {
            val jsonString = """false"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.BooleanValue>(result)
            assertEquals(false, result.value)
        }

        @Test
        fun `deserialize null value`() {
            val jsonString = """null"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.NullValue>(result)
        }

        @Test
        fun `deserialize empty list value`() {
            val jsonString = """[]"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.ListValue>(result)
            assertEquals(0, result.values.size)
        }

        @Test
        fun `deserialize list of strings`() {
            val jsonString = """["a", "b", "c"]"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.ListValue>(result)
            assertEquals(3, result.values.size)
            assertIs<ModelDataPropertyValue.StringValue>(result.values[0])
            assertEquals("a", (result.values[0] as ModelDataPropertyValue.StringValue).value)
        }

        @Test
        fun `deserialize list of mixed types`() {
            val jsonString = """["text", 123, true, null]"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.ListValue>(result)
            assertEquals(4, result.values.size)
            assertIs<ModelDataPropertyValue.StringValue>(result.values[0])
            assertIs<ModelDataPropertyValue.NumberValue>(result.values[1])
            assertIs<ModelDataPropertyValue.BooleanValue>(result.values[2])
            assertIs<ModelDataPropertyValue.NullValue>(result.values[3])
        }

        @Test
        fun `deserialize nested list`() {
            val jsonString = """[[1, 2], [3, 4]]"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.ListValue>(result)
            assertEquals(2, result.values.size)
            assertIs<ModelDataPropertyValue.ListValue>(result.values[0])
            assertIs<ModelDataPropertyValue.ListValue>(result.values[1])
        }

        @Test
        fun `serialize string value`() {
            val value = ModelDataPropertyValue.StringValue("hello")
            val result = json.encodeToString(ModelDataPropertyValueSerializer, value)

            assertEquals("\"hello\"", result)
        }

        @Test
        fun `serialize number value`() {
            val value = ModelDataPropertyValue.NumberValue(42.0)
            val result = json.encodeToString(ModelDataPropertyValueSerializer, value)

            assertEquals("42.0", result)
        }

        @Test
        fun `serialize boolean value`() {
            val value = ModelDataPropertyValue.BooleanValue(true)
            val result = json.encodeToString(ModelDataPropertyValueSerializer, value)

            assertEquals("true", result)
        }

        @Test
        fun `serialize null value`() {
            val value = ModelDataPropertyValue.NullValue
            val result = json.encodeToString(ModelDataPropertyValueSerializer, value)

            assertEquals("null", result)
        }

        @Test
        fun `serialize list value`() {
            val value = ModelDataPropertyValue.ListValue(
                listOf(
                    ModelDataPropertyValue.StringValue("a"),
                    ModelDataPropertyValue.NumberValue(1.0)
                )
            )
            val result = json.encodeToString(ModelDataPropertyValueSerializer, value)

            // Using contains to handle whitespace differences
            assert(result.contains("\"a\""))
            assert(result.contains("1.0"))
        }

        @Test
        fun `roundtrip string value`() {
            val original = ModelDataPropertyValue.StringValue("test")
            val serialized = json.encodeToString(ModelDataPropertyValueSerializer, original)
            val deserialized = json.decodeFromString(ModelDataPropertyValueSerializer, serialized)

            assertEquals(original, deserialized)
        }

        @Test
        fun `roundtrip complex list`() {
            val original = ModelDataPropertyValue.ListValue(
                listOf(
                    ModelDataPropertyValue.StringValue("name"),
                    ModelDataPropertyValue.NumberValue(99.5),
                    ModelDataPropertyValue.BooleanValue(false),
                    ModelDataPropertyValue.NullValue,
                    ModelDataPropertyValue.ListValue(
                        listOf(ModelDataPropertyValue.StringValue("nested"))
                    )
                )
            )
            val serialized = json.encodeToString(ModelDataPropertyValueSerializer, original)
            val deserialized = json.decodeFromString(ModelDataPropertyValueSerializer, serialized)

            assertEquals(original, deserialized)
        }

        @Test
        fun `deserialize enum value`() {
            val jsonString = """{"enum": "RED"}"""
            val result = json.decodeFromString(ModelDataPropertyValueSerializer, jsonString)

            assertIs<ModelDataPropertyValue.EnumValue>(result)
            assertEquals("RED", result.enumEntry)
        }

        @Test
        fun `serialize enum value`() {
            val value = ModelDataPropertyValue.EnumValue("BLUE")
            val result = json.encodeToString(ModelDataPropertyValueSerializer, value)

            assert(result.contains("enum"))
            assert(result.contains("BLUE"))
        }

        @Test
        fun `roundtrip enum value`() {
            val original = ModelDataPropertyValue.EnumValue("GREEN")
            val serialized = json.encodeToString(ModelDataPropertyValueSerializer, original)
            val deserialized = json.decodeFromString(ModelDataPropertyValueSerializer, serialized)

            assertEquals(original, deserialized)
        }
    }

    @Nested
    inner class ModelDataInstanceTests {

        @Test
        fun `deserialize instance with multiple properties`() {
            val jsonString = """{
                "name": "person1",
                "className": "mypackage.Person",
                "properties": {
                    "firstName": "John",
                    "lastName": "Doe",
                    "age": 30
                }
            }"""
            val result = json.decodeFromString<ModelDataInstance>(jsonString)

            assertEquals("person1", result.name)
            assertEquals("mypackage.Person", result.className)
            assertEquals(3, result.properties.size)
            val firstName = result.properties["firstName"]
            assertIs<ModelDataPropertyValue.StringValue>(firstName)
            assertEquals("John", firstName.value)
        }

        @Test
        fun `deserialize instance with empty properties`() {
            val jsonString = """{
                "name": "emptyObj",
                "className": "mypackage.Empty",
                "properties": {}
            }"""
            val result = json.decodeFromString<ModelDataInstance>(jsonString)

            assertEquals("emptyObj", result.name)
            assertEquals("mypackage.Empty", result.className)
            assertEquals(0, result.properties.size)
        }

        @Test
        fun `deserialize instance with list property`() {
            val jsonString = """{
                "name": "obj1",
                "className": "pkg.MyClass",
                "properties": {
                    "tags": ["tag1", "tag2", "tag3"]
                }
            }"""
            val result = json.decodeFromString<ModelDataInstance>(jsonString)

            assertEquals(1, result.properties.size)
            val tags = result.properties["tags"]
            assertIs<ModelDataPropertyValue.ListValue>(tags)
            assertEquals(3, tags.values.size)
        }

        @Test
        fun `deserialize instance with enum property`() {
            val jsonString = """{
                "name": "obj1",
                "className": "pkg.MyClass",
                "properties": {
                    "color": {"enum": "RED"}
                }
            }"""
            val result = json.decodeFromString<ModelDataInstance>(jsonString)

            assertEquals(1, result.properties.size)
            val color = result.properties["color"]
            assertIs<ModelDataPropertyValue.EnumValue>(color)
            assertEquals("RED", color.enumEntry)
        }

        @Test
        fun `serialize instance roundtrip`() {
            val original = ModelDataInstance(
                name = "testInstance",
                className = "pkg.TestClass",
                properties = mapOf(
                    "prop1" to ModelDataPropertyValue.StringValue("value1"),
                    "prop2" to ModelDataPropertyValue.NumberValue(42.0),
                    "prop3" to ModelDataPropertyValue.NullValue
                )
            )
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ModelDataInstance>(serialized)

            assertEquals(original, deserialized)
        }
    }

    @Nested
    inner class ModelDataLinkTests {

        @Test
        fun `deserialize link with all properties`() {
            val jsonString = """{
                "sourceName": "person1",
                "sourceProperty": "employer",
                "targetName": "company1",
                "targetProperty": "employees"
            }"""
            val result = json.decodeFromString<ModelDataLink>(jsonString)

            assertEquals("person1", result.sourceName)
            assertEquals("employer", result.sourceProperty)
            assertEquals("company1", result.targetName)
            assertEquals("employees", result.targetProperty)
        }

        @Test
        fun `deserialize link with null property names`() {
            val jsonString = """{
                "sourceName": "node1",
                "sourceProperty": null,
                "targetName": "node2",
                "targetProperty": null
            }"""
            val result = json.decodeFromString<ModelDataLink>(jsonString)

            assertEquals("node1", result.sourceName)
            assertEquals(null, result.sourceProperty)
            assertEquals("node2", result.targetName)
            assertEquals(null, result.targetProperty)
        }

        @Test
        fun `deserialize link with partial null properties`() {
            val jsonString = """{
                "sourceName": "parent",
                "sourceProperty": "children",
                "targetName": "child1",
                "targetProperty": null
            }"""
            val result = json.decodeFromString<ModelDataLink>(jsonString)

            assertEquals("parent", result.sourceName)
            assertEquals("children", result.sourceProperty)
            assertEquals("child1", result.targetName)
            assertEquals(null, result.targetProperty)
        }

        @Test
        fun `serialize link roundtrip`() {
            val original = ModelDataLink(
                sourceName = "src",
                sourceProperty = "outgoing",
                targetName = "tgt",
                targetProperty = "incoming"
            )
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ModelDataLink>(serialized)

            assertEquals(original, deserialized)
        }

        @Test
        fun `serialize link with nulls roundtrip`() {
            val original = ModelDataLink(
                sourceName = "a",
                sourceProperty = null,
                targetName = "b",
                targetProperty = null
            )
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ModelDataLink>(serialized)

            assertEquals(original, deserialized)
        }
    }

    @Nested
    inner class ModelDataTests {

        @Test
        fun `deserialize complete model`() {
            val jsonString = """{
                "metamodelPath": "file:///path/to/metamodel.mm",
                "instances": [
                    {
                        "name": "person1",
                        "className": "Person",
                        "properties": {
                            "name": "John"
                        }
                    },
                    {
                        "name": "company1",
                        "className": "Company",
                        "properties": {
                            "name": "Acme Inc."
                        }
                    }
                ],
                "links": [
                    {
                        "sourceName": "person1",
                        "sourceProperty": "employer",
                        "targetName": "company1",
                        "targetProperty": "employees"
                    }
                ]
            }"""
            val result = json.decodeFromString<ModelData>(jsonString)

            assertEquals("file:///path/to/metamodel.mm", result.metamodelPath)
            assertEquals(2, result.instances.size)
            assertEquals(1, result.links.size)
            assertEquals("person1", result.instances[0].name)
            assertEquals("company1", result.instances[1].name)
        }

        @Test
        fun `deserialize empty model`() {
            val jsonString = """{
                "metamodelPath": "file:///empty.mm",
                "instances": [],
                "links": []
            }"""
            val result = json.decodeFromString<ModelData>(jsonString)

            assertEquals("file:///empty.mm", result.metamodelPath)
            assertEquals(0, result.instances.size)
            assertEquals(0, result.links.size)
        }

        @Test
        fun `deserialize model with complex properties`() {
            val jsonString = """{
                "metamodelPath": "file:///complex.mm",
                "instances": [
                    {
                        "name": "obj1",
                        "className": "ComplexClass",
                        "properties": {
                            "stringProp": "text",
                            "numberProp": 123.45,
                            "boolProp": true,
                            "nullProp": null,
                            "listProp": [1, 2, 3],
                            "enumProp": {"enum": "ACTIVE"}
                        }
                    }
                ],
                "links": []
            }"""
            val result = json.decodeFromString<ModelData>(jsonString)

            assertEquals(1, result.instances.size)
            val instance = result.instances[0]
            assertEquals(6, instance.properties.size)

            val listProp = instance.properties["listProp"]
            assertIs<ModelDataPropertyValue.ListValue>(listProp)
            
            val enumProp = instance.properties["enumProp"]
            assertIs<ModelDataPropertyValue.EnumValue>(enumProp)
            assertEquals("ACTIVE", enumProp.enumEntry)
        }

        @Test
        fun `serialize complete model roundtrip`() {
            val original = ModelData(
                metamodelPath = "file:///test/metamodel.mm",
                instances = listOf(
                    ModelDataInstance(
                        name = "instance1",
                        className = "TestClass",
                        properties = mapOf(
                            "name" to ModelDataPropertyValue.StringValue("Test"),
                            "count" to ModelDataPropertyValue.NumberValue(5.0),
                            "enabled" to ModelDataPropertyValue.BooleanValue(true),
                            "optional" to ModelDataPropertyValue.NullValue,
                            "color" to ModelDataPropertyValue.EnumValue("RED"),
                            "items" to ModelDataPropertyValue.ListValue(
                                listOf(
                                    ModelDataPropertyValue.StringValue("item1"),
                                    ModelDataPropertyValue.StringValue("item2")
                                )
                            )
                        )
                    )
                ),
                links = listOf(
                    ModelDataLink("a", "prop", "b", null),
                    ModelDataLink("c", null, "d", "backRef")
                )
            )
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ModelData>(serialized)

            assertEquals(original, deserialized)
        }

        @Test
        fun `serialize model with special characters in strings`() {
            val original = ModelData(
                metamodelPath = "file:///path/with spaces/metamodel.mm",
                instances = listOf(
                    ModelDataInstance(
                        name = "instance_with-special.chars",
                        className = "pkg.sub.ClassName",
                        properties = mapOf(
                            "description" to ModelDataPropertyValue.StringValue("Contains \"quotes\" and \\ backslash")
                        )
                    )
                ),
                links = emptyList()
            )
            val serialized = json.encodeToString(original)
            val deserialized = json.decodeFromString<ModelData>(serialized)

            assertEquals(original, deserialized)
        }
    }
}
