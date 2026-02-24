package com.mdeo.script.compiler.util

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.VoidType
import org.junit.jupiter.api.Test
import org.objectweb.asm.Opcodes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for TypeConversionUtil.
 */
class TypeConversionUtilTest {
    
    // ==================== getNumericTypeName Tests ====================
    
    @Test
    fun `getNumericTypeName returns type name for int`() {
        val type = ClassTypeRef("builtin", "int", false)
        assertEquals("int", TypeConversionUtil.getNumericTypeName(type))
    }
    
    @Test
    fun `getNumericTypeName returns type name for long`() {
        val type = ClassTypeRef("builtin", "long", false)
        assertEquals("long", TypeConversionUtil.getNumericTypeName(type))
    }
    
    @Test
    fun `getNumericTypeName returns type name for float`() {
        val type = ClassTypeRef("builtin", "float", false)
        assertEquals("float", TypeConversionUtil.getNumericTypeName(type))
    }
    
    @Test
    fun `getNumericTypeName returns type name for double`() {
        val type = ClassTypeRef("builtin", "double", false)
        assertEquals("double", TypeConversionUtil.getNumericTypeName(type))
    }
    
    @Test
    fun `getNumericTypeName returns null for string`() {
        val type = ClassTypeRef("builtin", "string", false)
        assertNull(TypeConversionUtil.getNumericTypeName(type))
    }
    
    @Test
    fun `getNumericTypeName returns null for boolean`() {
        val type = ClassTypeRef("builtin", "boolean", false)
        assertNull(TypeConversionUtil.getNumericTypeName(type))
    }
    
    @Test
    fun `getNumericTypeName returns null for void type`() {
        val type = VoidType()
        assertNull(TypeConversionUtil.getNumericTypeName(type))
    }
    
    // ==================== isNumericType Tests ====================
    
    @Test
    fun `isNumericType returns true for int`() {
        val type = ClassTypeRef("builtin", "int", false)
        assertTrue(TypeConversionUtil.isNumericType(type))
    }
    
    @Test
    fun `isNumericType returns true for long`() {
        val type = ClassTypeRef("builtin", "long", false)
        assertTrue(TypeConversionUtil.isNumericType(type))
    }
    
    @Test
    fun `isNumericType returns true for float`() {
        val type = ClassTypeRef("builtin", "float", false)
        assertTrue(TypeConversionUtil.isNumericType(type))
    }
    
    @Test
    fun `isNumericType returns true for double`() {
        val type = ClassTypeRef("builtin", "double", false)
        assertTrue(TypeConversionUtil.isNumericType(type))
    }
    
    @Test
    fun `isNumericType returns false for string`() {
        val type = ClassTypeRef("builtin", "string", false)
        assertFalse(TypeConversionUtil.isNumericType(type))
    }
    
    @Test
    fun `isNumericType returns false for boolean`() {
        val type = ClassTypeRef("builtin", "boolean", false)
        assertFalse(TypeConversionUtil.isNumericType(type))
    }
    
    @Test
    fun `isNumericType string returns true for int`() {
        assertTrue(TypeConversionUtil.isNumericType("int"))
    }
    
    @Test
    fun `isNumericType string returns false for string`() {
        assertFalse(TypeConversionUtil.isNumericType("string"))
    }
    
    // ==================== isIntegerType Tests ====================
    
    @Test
    fun `isIntegerType returns true for int`() {
        val type = ClassTypeRef("builtin", "int", false)
        assertTrue(TypeConversionUtil.isIntegerType(type))
    }
    
    @Test
    fun `isIntegerType returns true for long`() {
        val type = ClassTypeRef("builtin", "long", false)
        assertTrue(TypeConversionUtil.isIntegerType(type))
    }
    
    @Test
    fun `isIntegerType returns false for float`() {
        val type = ClassTypeRef("builtin", "float", false)
        assertFalse(TypeConversionUtil.isIntegerType(type))
    }
    
    @Test
    fun `isIntegerType returns false for double`() {
        val type = ClassTypeRef("builtin", "double", false)
        assertFalse(TypeConversionUtil.isIntegerType(type))
    }
    
    // ==================== isFloatingPointType Tests ====================
    
    @Test
    fun `isFloatingPointType returns true for float`() {
        val type = ClassTypeRef("builtin", "float", false)
        assertTrue(TypeConversionUtil.isFloatingPointType(type))
    }
    
    @Test
    fun `isFloatingPointType returns true for double`() {
        val type = ClassTypeRef("builtin", "double", false)
        assertTrue(TypeConversionUtil.isFloatingPointType(type))
    }
    
    @Test
    fun `isFloatingPointType returns false for int`() {
        val type = ClassTypeRef("builtin", "int", false)
        assertFalse(TypeConversionUtil.isFloatingPointType(type))
    }
    
    @Test
    fun `isFloatingPointType returns false for long`() {
        val type = ClassTypeRef("builtin", "long", false)
        assertFalse(TypeConversionUtil.isFloatingPointType(type))
    }
    
    // ==================== getPromotedType Tests ====================
    
    @Test
    fun `getPromotedType int and int returns int`() {
        val result = TypeConversionUtil.getPromotedType("int", "int")
        assertEquals("int", result)
    }
    
    @Test
    fun `getPromotedType int and long returns long`() {
        val result = TypeConversionUtil.getPromotedType("int", "long")
        assertEquals("long", result)
    }
    
    @Test
    fun `getPromotedType long and int returns long`() {
        val result = TypeConversionUtil.getPromotedType("long", "int")
        assertEquals("long", result)
    }
    
    @Test
    fun `getPromotedType int and float returns float`() {
        val result = TypeConversionUtil.getPromotedType("int", "float")
        assertEquals("float", result)
    }
    
    @Test
    fun `getPromotedType float and int returns float`() {
        val result = TypeConversionUtil.getPromotedType("float", "int")
        assertEquals("float", result)
    }
    
    @Test
    fun `getPromotedType int and double returns double`() {
        val result = TypeConversionUtil.getPromotedType("int", "double")
        assertEquals("double", result)
    }
    
    @Test
    fun `getPromotedType double and int returns double`() {
        val result = TypeConversionUtil.getPromotedType("double", "int")
        assertEquals("double", result)
    }
    
    @Test
    fun `getPromotedType long and float returns float`() {
        val result = TypeConversionUtil.getPromotedType("long", "float")
        assertEquals("float", result)
    }
    
    @Test
    fun `getPromotedType float and long returns float`() {
        val result = TypeConversionUtil.getPromotedType("float", "long")
        assertEquals("float", result)
    }
    
    @Test
    fun `getPromotedType long and double returns double`() {
        val result = TypeConversionUtil.getPromotedType("long", "double")
        assertEquals("double", result)
    }
    
    @Test
    fun `getPromotedType double and long returns double`() {
        val result = TypeConversionUtil.getPromotedType("double", "long")
        assertEquals("double", result)
    }
    
    @Test
    fun `getPromotedType float and double returns double`() {
        val result = TypeConversionUtil.getPromotedType("float", "double")
        assertEquals("double", result)
    }
    
    @Test
    fun `getPromotedType double and float returns double`() {
        val result = TypeConversionUtil.getPromotedType("double", "float")
        assertEquals("double", result)
    }
    
    @Test
    fun `getPromotedType double and double returns double`() {
        val result = TypeConversionUtil.getPromotedType("double", "double")
        assertEquals("double", result)
    }
    
    // ==================== isStringType Tests ====================
    
    @Test
    fun `isStringType returns true for string`() {
        val type = ClassTypeRef("builtin", "string", false)
        assertTrue(TypeConversionUtil.isStringType(type))
    }
    
    @Test
    fun `isStringType returns false for int`() {
        val type = ClassTypeRef("builtin", "int", false)
        assertFalse(TypeConversionUtil.isStringType(type))
    }
    
    @Test
    fun `isStringType string returns true for string`() {
        assertTrue(TypeConversionUtil.isStringType("string"))
    }
    
    @Test
    fun `isStringType string returns false for int`() {
        assertFalse(TypeConversionUtil.isStringType("int"))
    }
    
    // ==================== isBooleanType Tests ====================
    
    @Test
    fun `isBooleanType returns true for boolean`() {
        val type = ClassTypeRef("builtin", "boolean", false)
        assertTrue(TypeConversionUtil.isBooleanType(type))
    }
    
    @Test
    fun `isBooleanType returns false for int`() {
        val type = ClassTypeRef("builtin", "int", false)
        assertFalse(TypeConversionUtil.isBooleanType(type))
    }
    
    // ==================== Opcode Tests ====================
    
    @Test
    fun `getAddOpcode returns IADD for int`() {
        assertEquals(Opcodes.IADD, TypeConversionUtil.getAddOpcode("int"))
    }
    
    @Test
    fun `getAddOpcode returns LADD for long`() {
        assertEquals(Opcodes.LADD, TypeConversionUtil.getAddOpcode("long"))
    }
    
    @Test
    fun `getAddOpcode returns FADD for float`() {
        assertEquals(Opcodes.FADD, TypeConversionUtil.getAddOpcode("float"))
    }
    
    @Test
    fun `getAddOpcode returns DADD for double`() {
        assertEquals(Opcodes.DADD, TypeConversionUtil.getAddOpcode("double"))
    }
    
    @Test
    fun `getSubOpcode returns ISUB for int`() {
        assertEquals(Opcodes.ISUB, TypeConversionUtil.getSubOpcode("int"))
    }
    
    @Test
    fun `getSubOpcode returns LSUB for long`() {
        assertEquals(Opcodes.LSUB, TypeConversionUtil.getSubOpcode("long"))
    }
    
    @Test
    fun `getSubOpcode returns FSUB for float`() {
        assertEquals(Opcodes.FSUB, TypeConversionUtil.getSubOpcode("float"))
    }
    
    @Test
    fun `getSubOpcode returns DSUB for double`() {
        assertEquals(Opcodes.DSUB, TypeConversionUtil.getSubOpcode("double"))
    }
    
    @Test
    fun `getMulOpcode returns IMUL for int`() {
        assertEquals(Opcodes.IMUL, TypeConversionUtil.getMulOpcode("int"))
    }
    
    @Test
    fun `getMulOpcode returns LMUL for long`() {
        assertEquals(Opcodes.LMUL, TypeConversionUtil.getMulOpcode("long"))
    }
    
    @Test
    fun `getMulOpcode returns FMUL for float`() {
        assertEquals(Opcodes.FMUL, TypeConversionUtil.getMulOpcode("float"))
    }
    
    @Test
    fun `getMulOpcode returns DMUL for double`() {
        assertEquals(Opcodes.DMUL, TypeConversionUtil.getMulOpcode("double"))
    }
    
    @Test
    fun `getDivOpcode returns IDIV for int`() {
        assertEquals(Opcodes.IDIV, TypeConversionUtil.getDivOpcode("int"))
    }
    
    @Test
    fun `getDivOpcode returns LDIV for long`() {
        assertEquals(Opcodes.LDIV, TypeConversionUtil.getDivOpcode("long"))
    }
    
    @Test
    fun `getDivOpcode returns FDIV for float`() {
        assertEquals(Opcodes.FDIV, TypeConversionUtil.getDivOpcode("float"))
    }
    
    @Test
    fun `getDivOpcode returns DDIV for double`() {
        assertEquals(Opcodes.DDIV, TypeConversionUtil.getDivOpcode("double"))
    }
    
    @Test
    fun `getRemOpcode returns IREM for int`() {
        assertEquals(Opcodes.IREM, TypeConversionUtil.getRemOpcode("int"))
    }
    
    @Test
    fun `getRemOpcode returns LREM for long`() {
        assertEquals(Opcodes.LREM, TypeConversionUtil.getRemOpcode("long"))
    }
    
    @Test
    fun `getRemOpcode returns FREM for float`() {
        assertEquals(Opcodes.FREM, TypeConversionUtil.getRemOpcode("float"))
    }
    
    @Test
    fun `getRemOpcode returns DREM for double`() {
        assertEquals(Opcodes.DREM, TypeConversionUtil.getRemOpcode("double"))
    }
    
    @Test
    fun `getNegOpcode returns INEG for int`() {
        assertEquals(Opcodes.INEG, TypeConversionUtil.getNegOpcode("int"))
    }
    
    @Test
    fun `getNegOpcode returns LNEG for long`() {
        assertEquals(Opcodes.LNEG, TypeConversionUtil.getNegOpcode("long"))
    }
    
    @Test
    fun `getNegOpcode returns FNEG for float`() {
        assertEquals(Opcodes.FNEG, TypeConversionUtil.getNegOpcode("float"))
    }
    
    @Test
    fun `getNegOpcode returns DNEG for double`() {
        assertEquals(Opcodes.DNEG, TypeConversionUtil.getNegOpcode("double"))
    }
}
