package com.mdeo.script.compiler.util

import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.expression.ast.types.ClassTypeRef
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

    
    // ==================== producesStackPrimitive Tests ====================
    
    @Nested
    inner class ProducesStackPrimitive {
        
        @Test
        fun `non-nullable int produces primitive`() {
            val type = ClassTypeRef("builtin", "int", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable long produces primitive`() {
            val type = ClassTypeRef("builtin", "long", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable float produces primitive`() {
            val type = ClassTypeRef("builtin", "float", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable double produces primitive`() {
            val type = ClassTypeRef("builtin", "double", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `non-nullable boolean produces primitive`() {
            val type = ClassTypeRef("builtin", "boolean", false)
            assertTrue(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `nullable int without expression does not produce primitive`() {
            val type = ClassTypeRef("builtin", "int", true)
            assertFalse(CoercionUtil.producesStackPrimitive(type))
        }
        
        @Test
        fun `nullable int with literal expression produces primitive`() {
            val type = ClassTypeRef("builtin", "int", true)
            val expr = TypedIntLiteralExpression(evalType = 0, value = "42")
            assertTrue(CoercionUtil.producesStackPrimitive(type, expr))
        }
        
        @Test
        fun `string does not produce primitive`() {
            val type = ClassTypeRef("builtin", "string", false)
            assertFalse(CoercionUtil.producesStackPrimitive(type))
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
            assertEquals("I", CoercionUtil.getPrimitiveDescriptor(ClassTypeRef("builtin", "int", false)))
        }
        
        @Test
        fun `long descriptor is J`() {
            assertEquals("J", CoercionUtil.getPrimitiveDescriptor(ClassTypeRef("builtin", "long", false)))
        }
        
        @Test
        fun `float descriptor is F`() {
            assertEquals("F", CoercionUtil.getPrimitiveDescriptor(ClassTypeRef("builtin", "float", false)))
        }
        
        @Test
        fun `double descriptor is D`() {
            assertEquals("D", CoercionUtil.getPrimitiveDescriptor(ClassTypeRef("builtin", "double", false)))
        }
        
        @Test
        fun `boolean descriptor is Z`() {
            assertEquals("Z", CoercionUtil.getPrimitiveDescriptor(ClassTypeRef("builtin", "boolean", false)))
        }
        
        @Test
        fun `unknown type returns null`() {
            assertNull(CoercionUtil.getPrimitiveDescriptor(ClassTypeRef("builtin", "string", false)))
        }
    }
    
    // ==================== getStackSize Tests ====================
    
    @Nested
    inner class GetStackSize {
        
        @Test
        fun `non-nullable int type has size 1`() {
            val type = ClassTypeRef("builtin", "int", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable long type has size 2`() {
            val type = ClassTypeRef("builtin", "long", false)
            assertEquals(2, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `nullable int type has size 1`() {
            val type = ClassTypeRef("builtin", "int", true)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `nullable long type has size 1`() {
            val type = ClassTypeRef("builtin", "long", true)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable double type has size 2`() {
            val type = ClassTypeRef("builtin", "double", false)
            assertEquals(2, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `nullable double type has size 1`() {
            val type = ClassTypeRef("builtin", "double", true)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable float type has size 1`() {
            val type = ClassTypeRef("builtin", "float", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `non-nullable boolean type has size 1`() {
            val type = ClassTypeRef("builtin", "boolean", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
        
        @Test
        fun `string type has size 1`() {
            val type = ClassTypeRef("builtin", "string", false)
            assertEquals(1, ASMUtil.getStackSize(type))
        }
    }
}
