package com.mdeo.script.compiler.registry

import com.mdeo.expression.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.type.InstanceMethodDefinition
import com.mdeo.script.compiler.registry.type.InstancePropertyDefinition
import com.mdeo.script.compiler.registry.type.StaticMethodDefinition
import com.mdeo.script.compiler.registry.type.StaticPropertyDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition
import com.mdeo.script.compiler.registry.type.TypeRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.mdeo.expression.ast.types.ClassTypeRef

/**
 * Comprehensive tests for the TypeRegistry infrastructure.
 *
 * Tests cover:
 * - Type registration and lookup
 * - Method lookup with inheritance
 * - Property lookup
 * - Type hierarchy traversal
 */
class TypeRegistryTest {

    private lateinit var registry: TypeRegistry

    @BeforeEach
    fun setUp() {
        registry = TypeRegistry()
    }

    @Nested
    inner class TypeRegistration {

        @Test
        fun `can register and retrieve a type`() {
            val typeDef = typeDefinition("test", "MyType") {}
            registry.register(typeDef)

            val retrieved = registry.getType(ClassTypeRef("test", "MyType", false))
            assertNotNull(retrieved)
            assertEquals("test", retrieved.typePackage)
            assertEquals("MyType", retrieved.typeName)
        }

        @Test
        fun `returns null for unregistered type`() {
            val retrieved = registry.getType(ClassTypeRef("nonexistent", "Type", false))
            assertNull(retrieved)
        }

        @Test
        fun `can register multiple types`() {
            registry.register(typeDefinition("type", "A") {})
            registry.register(typeDefinition("type", "B") {})
            registry.register(typeDefinition("type", "C") {})

            assertNotNull(registry.getType(ClassTypeRef("type", "A", false)))
            assertNotNull(registry.getType(ClassTypeRef("type", "B", false)))
            assertNotNull(registry.getType(ClassTypeRef("type", "C", false)))
        }
    }

    @Nested
    inner class MethodLookup {

        @Test
        fun `can lookup method by overload key`() {
            val typeDef = typeDefinition("test", "Type") {
                staticMethod("doSomething") {
                    overload("", "(I)I", "test/Helper", parameterTypes = listOf(BuiltinTypes.INT), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(typeDef)

            val method = registry.lookupMethod(ClassTypeRef("test", "Type", false), "doSomething", "")
            assertNotNull(method)
            assertEquals("", method.overloadKey)
        }

        @Test
        fun `returns null for nonexistent method`() {
            val typeDef = typeDefinition("test", "Type") {}
            registry.register(typeDef)

            val method = registry.lookupMethod(ClassTypeRef("test", "Type", false), "nonexistent", "")
            assertNull(method)
        }

        @Test
        fun `can lookup method with parameters`() {
            val typeDef = typeDefinition("test", "Type") {
                staticMethod("add") {
                    overload("", "(II)I", "test/Helper", parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.INT), returnType = BuiltinTypes.INT)
                    overload("", "(ID)D", "test/Helper", parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.DOUBLE), returnType = BuiltinTypes.DOUBLE)
                }
            }
            registry.register(typeDef)

            val intMethod = registry.lookupMethod(ClassTypeRef("test", "Type", false), "add", "")
            val doubleMethod = registry.lookupMethod(ClassTypeRef("test", "Type", false), "add", "")

            assertNotNull(intMethod)
            assertNotNull(doubleMethod)
            assertEquals("", intMethod.overloadKey)
            assertEquals("", doubleMethod.overloadKey)
        }

        @Test
        fun `can lookup instance method`() {
            val typeDef = typeDefinition("test", "Type") {
                instanceMethod("getValue") {
                    overload("", "()I", "test/Type", parameterTypes = emptyList(), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(typeDef)

            val method = registry.lookupMethod(ClassTypeRef("test", "Type", false), "getValue", "")
            assertNotNull(method)
            assertTrue(method is InstanceMethodDefinition)
        }

        @Test
        fun `can lookup all overloads of a method`() {
            val typeDef = typeDefinition("test", "Type") {
                staticMethod("add") {
                    overload("", "(II)I", "test/Helper", parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.INT), returnType = BuiltinTypes.INT)
                    overload("", "(ID)D", "test/Helper", parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.DOUBLE), returnType = BuiltinTypes.DOUBLE)
                    overload("", "(ILjava/lang/String;)Ljava/lang/String;", "test/Helper", parameterTypes = listOf(BuiltinTypes.INT, BuiltinTypes.STRING), returnType = BuiltinTypes.STRING)
                }
            }
            registry.register(typeDef)

            val methods = registry.lookupMethods(ClassTypeRef("test", "Type", false), "add")
            assertEquals(3, methods.size)
        }
    }

    @Nested
    inner class InheritanceLookup {

        @Test
        fun `can lookup method from parent type`() {
            // Parent type
            val parentDef = typeDefinition("base", "Parent") {
                staticMethod("parentMethod") {
                    overload("", "(Ljava/lang/Object;)I", "base/ParentHelper", parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(parentDef)

            // Child type extends parent
            val childDef = typeDefinition("child", "Child") {
                extends("base", "Parent")
                staticMethod("childMethod") {
                    overload("", "(Ljava/lang/Object;)I", "child/ChildHelper", parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(childDef)

            // Should find method from parent through hierarchy
            val method = registry.lookupMethod(ClassTypeRef("child", "Child", false), "parentMethod", "")
            assertNotNull(method)
            assertEquals("", method.overloadKey)
        }

        @Test
        fun `child method overrides parent method`() {
            val parentDef = typeDefinition("base", "Parent") {
                staticMethod("getValue") {
                    overload("", "(Ljava/lang/Object;)I", "base/ParentHelper", parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(parentDef)

            val childDef = typeDefinition("child", "Child") {
                extends("base", "Parent")
                staticMethod("getValue") {
                    overload("", "(Ljava/lang/Object;)I", "child/ChildHelper", parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(childDef)

            val method = registry.lookupMethod(ClassTypeRef("child", "Child", false), "getValue", "")
            assertNotNull(method)
            // Should find child's version (first in hierarchy)
            assertTrue((method as StaticMethodDefinition).ownerClass == "child/ChildHelper")
        }

        @Test
        fun `multi-level inheritance works`() {
            val grandparentDef = typeDefinition("base", "Grandparent") {
                staticMethod("grandparentMethod") {
                    overload("", "(Ljava/lang/Object;)I", "base/GrandparentHelper", parameterTypes = listOf(BuiltinTypes.NULLABLE_ANY), returnType = BuiltinTypes.INT)
                }
            }
            registry.register(grandparentDef)

            val parentDef = typeDefinition("base", "Parent") {
                extends("base", "Grandparent")
            }
            registry.register(parentDef)

            val childDef = typeDefinition("child", "Child") {
                extends("base", "Parent")
            }
            registry.register(childDef)

            val method = registry.lookupMethod(ClassTypeRef("child", "Child", false), "grandparentMethod", "")
            assertNotNull(method)
        }
    }

    @Nested
    inner class PropertyLookup {

        @Test
        fun `can lookup static property`() {
            val typeDef = typeDefinition("test", "Type") {
                staticProperty("PI") {
                    returns("D")
                    owner("test/MathHelper")
                    getter("PI")
                }
            }
            registry.register(typeDef)

            val prop = registry.lookupProperty(ClassTypeRef("test", "Type", false), "PI")
            assertNotNull(prop)
            assertTrue(prop is StaticPropertyDefinition)
        }

        @Test
        fun `can lookup instance property`() {
            val typeDef = typeDefinition("test", "Type") {
                instanceProperty("value") {
                    returns("I")
                    owner("test/Type")
                    getter("getValue")
                }
            }
            registry.register(typeDef)

            val prop = registry.lookupProperty(ClassTypeRef("test", "Type", false), "value")
            assertNotNull(prop)
            assertTrue(prop is InstancePropertyDefinition)
        }

        @Test
        fun `returns null for nonexistent property`() {
            val typeDef = typeDefinition("test", "Type") {}
            registry.register(typeDef)

            val prop = registry.lookupProperty(ClassTypeRef("test", "Type", false), "nonexistent")
            assertNull(prop)
        }
    }

    @Nested
    inner class GlobalRegistry {

        @Test
        fun `global registry contains any type`() {
            val anyType = TypeRegistry.GLOBAL.getType(ClassTypeRef("builtin", "Any", false))
            assertNotNull(anyType)
        }

        @Test
        fun `global registry contains int type`() {
            val intType = TypeRegistry.GLOBAL.getType(ClassTypeRef("builtin", "int", false))
            assertNotNull(intType)
        }

        @Test
        fun `global registry contains string type`() {
            val stringType = TypeRegistry.GLOBAL.getType(ClassTypeRef("builtin", "string", false))
            assertNotNull(stringType)
        }

        @Test
        fun `int type extends any type`() {
            val intType = TypeRegistry.GLOBAL.getType(ClassTypeRef("builtin", "int", false))
            assertNotNull(intType)
            assertTrue(intType.extends.contains(ClassTypeRef("builtin", "Any", false)))
        }

        @Test
        fun `can lookup inherited method on int`() {
            // Int should inherit toString from Any
            val method = TypeRegistry.GLOBAL.lookupMethod(ClassTypeRef("builtin", "int", false), "toString", "")
            assertNotNull(method)
        }

        @Test
        fun `can lookup abs method on int`() {
            val method = TypeRegistry.GLOBAL.lookupMethod(ClassTypeRef("builtin", "int", false), "abs", "")
            assertNotNull(method)
        }
    }
}
