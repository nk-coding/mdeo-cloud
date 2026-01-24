package com.mdeo.script.compiler.util

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedIntLiteralExpression
import com.mdeo.script.ast.expressions.TypedNullLiteralExpression
import com.mdeo.script.ast.types.ClassTypeRef
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CoercionUtil.
 * Tests the utility functions for type coercion, boxing, and unboxing.
 */
class CoercionUtilTest {
    
    // ==================== isPrimitiveType Tests ====================
    
    @Nested
    inner class IsPrimitiveType {
        
        @Test
        fun `int is primitive type`() {
            assertTrue(CoercionUtil.isPrimitiveType("builtin.int"))
        }
        
        @Test
        fun `long is primitive type`() {
            assertTrue(CoercionUtil.isPrimitiveType("builtin.long"))
        }
        
        @Test
        fun `float is primitive type`() {
            assertTrue(CoercionUtil.isPrimitiveType("builtin.float"))
        }
        
        @Test
        fun `double is primitive type`() {
            assertTrue(CoercionUtil.isPrimitiveType("builtin.double"))
        }
        
        @Test
        fun `boolean is primitive type`() {
            assertTrue(CoercionUtil.isPrimitiveType("builtin.boolean"))
        }
        
        @Test
        fun `string is not primitive type`() {
            assertFalse(CoercionUtil.isPrimitiveType("builtin.string"))
        }
        
        @Test
        fun `unknown type is not primitive type`() {
            assertFalse(CoercionUtil.isPrimitiveType("custom.type"))
        }
    }
    
    // ==================== getPrimitiveTypeName Tests ====================
    
    @Nested
    inner class GetPrimitiveTypeName {
        
        @Test
        fun `returns type name for int`() {
            val type = ClassTypeRef("builtin.int", false)
            assertEquals("builtin.int", CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns type name for long`() {
            val type = ClassTypeRef("builtin.long", false)
            assertEquals("builtin.long", CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns type name for float`() {
            val type = ClassTypeRef("builtin.float", false)
            assertEquals("builtin.float", CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns type name for double`() {
            val type = ClassTypeRef("builtin.double", false)
            assertEquals("builtin.double", CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns type name for boolean`() {
            val type = ClassTypeRef("builtin.boolean", false)
            assertEquals("builtin.boolean", CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns type name for nullable int`() {
            val type = ClassTypeRef("builtin.int", true)
            assertEquals("builtin.int", CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns null for string`() {
            val type = ClassTypeRef("builtin.string", false)
            assertNull(CoercionUtil.getPrimitiveTypeName(type))
        }
        
        @Test
        fun `returns null for unknown type`() {
            val type = ClassTypeRef("custom.type", false)
            assertNull(CoercionUtil.getPrimitiveTypeName(type))
        }
    }
    
    // ==================== producesStackPrimitive Tests ====================
    
    @Nested
    inner class ProducesStackPrimitive {
        
        @Test
        fun `non-nullable int produces primitive`() {
            val type = ClassTypeRef("builtin.int", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable long produces primitive`() {
            val type = ClassTypeRef("builtin.long", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable float produces primitive`() {
            val type = ClassTypeRef("builtin.float", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable double produces primitive`() {
            val type = ClassTypeRef("builtin.double", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable boolean produces primitive`() {
            val type = ClassTypeRef("builtin.boolean", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `nullable int without expression does not produce primitive`() {
            val type = ClassTypeRef("builtin.int", true)
            assertFalse(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `nullable int with literal expression produces primitive`() {
            val type = ClassTypeRef("builtin.int", true)
            val expr = TypedIntLiteralExpression(evalType = 0, value = "42")
            assertTrue(CoercionUtil.producesStackPrimitive(type, expr))
        }
        
        @Test
        fun `string does not produce primitive`() {
            val type = ClassTypeRef("builtin.string", false)
            assertFalse(CoercionUtil.producesStackPrimitive(type))
        }
    }
    
    // ==================== expectsStackPrimitive Tests ====================
    
    @Nested
    inner class ExpectsStackPrimitive {
        
        @Test
        fun `non-nullable int expects primitive`() {
            val type = ClassTypeRef("builtin.int", false)
            assertTrue(CoercionUtil.expectsStackPrimitive(type))
        }
        
        @Test
        fun `nullable int does not expect primitive`() {
            val type = ClassTypeRef("builtin.int", true)
            assertFalse(CoercionUtil.expectsStackPrimitive(type))
        }
        
        @Test
        fun `string does not expect primitive`() {
            val type = ClassTypeRef("builtin.string", false)
            assertFalse(CoercionUtil.expectsStackPrimitive(type))
        }
    }
    
    // ==================== isNullLiteral Tests ====================
    
    @Nested
    inner class IsNullLiteral {
        
        @Test
        fun `null literal is detected`() {
            val expr = TypedNullLiteralExpression(evalType = 0)
            assertTrue(CoercionUtil.isNullLiteral(expr))
        }
        
        @Test
        fun `int literal is not null literal`() {
            val expr = TypedIntLiteralExpression(evalType = 0, value = "42")
            assertFalse(CoercionUtil.isNullLiteral(expr))
        }
    }
    
    // ==================== getPrimitiveDescriptor Tests ====================
    
    @Nested
    inner class GetPrimitiveDescriptor {
        
        @Test
        fun `int descriptor is I`() {
            assertEquals("I", CoercionUtil.getPrimitiveDescriptor("builtin.int"))
        }
        
        @Test
        fun `long descriptor is J`() {
            assertEquals("J", CoercionUtil.getPrimitiveDescriptor("builtin.long"))
        }
        
        @Test
        fun `float descriptor is F`() {
            assertEquals("F", CoercionUtil.getPrimitiveDescriptor("builtin.float"))
        }
        
        @Test
        fun `double descriptor is D`() {
            assertEquals("D", CoercionUtil.getPrimitiveDescriptor("builtin.double"))
        }
        
        @Test
        fun `boolean descriptor is Z`() {
            assertEquals("Z", CoercionUtil.getPrimitiveDescriptor("builtin.boolean"))
        }
        
        @Test
        fun `unknown type returns null`() {
            assertNull(CoercionUtil.getPrimitiveDescriptor("builtin.string"))
        }
    }
    
    // ==================== getStackSize Tests ====================
    
    @Nested
    inner class GetStackSize {
        
        @Test
        fun `non-nullable int type has size 1`() {
            val type = ClassTypeRef("builtin.int", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable long type has size 2`() {
            val type = ClassTypeRef("builtin.long", false)
            assertEquals(2, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `nullable int type has size 1`() {
            val type = ClassTypeRef("builtin.int", true)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `nullable long type has size 1`() {
            val type = ClassTypeRef("builtin.long", true)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable double type has size 2`() {
            val type = ClassTypeRef("builtin.double", false)
            assertEquals(2, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `nullable double type has size 1`() {
            val type = ClassTypeRef("builtin.double", true)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable float type has size 1`() {
            val type = ClassTypeRef("builtin.float", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable boolean type has size 1`() {
            val type = ClassTypeRef("builtin.boolean", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `string type has size 1`() {
            val type = ClassTypeRef("builtin.string", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
    }
}
