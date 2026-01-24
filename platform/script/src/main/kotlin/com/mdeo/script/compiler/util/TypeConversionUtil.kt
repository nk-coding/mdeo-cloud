package com.mdeo.script.compiler.util

import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Utility for numeric type conversions following Java's widening conversion rules.
 * 
 * This utility handles implicit type promotion for arithmetic operations and
 * type coercion when a value needs to be converted to a different numeric type.
 * 
 * Java's widening conversions (no data loss):
 * - byte → short → int → long → float → double
 * - char → int → long → float → double
 * 
 * For this language, we support: int, long, float, double
 * 
 * The conversion uses the following JVM instructions:
 * - I2L: int to long
 * - I2F: int to float
 * - I2D: int to double
 * - L2F: long to float
 * - L2D: long to double
 * - F2D: float to double
 */
object TypeConversionUtil {
    
    /**
     * Numeric type priority for promotion rules.
     * Higher priority wins when determining the result type.
     */
    private val numericPriority = mapOf(
        "builtin.int" to 1,
        "builtin.long" to 2,
        "builtin.float" to 3,
        "builtin.double" to 4
    )
    
    /**
     * Gets the type name from a ReturnType if it's a numeric type.
     * 
     * @param type The return type to check.
     * @return The type name if numeric, null otherwise.
     */
    fun getNumericTypeName(type: ReturnType): String? {
        if (type !is ClassTypeRef) {
            return null
        }
        return if (type.type in numericPriority.keys) type.type else null
    }
    
    /**
     * Checks if a type is a numeric type.
     * 
     * @param type The type to check.
     * @return true if the type is int, long, float, or double.
     */
    fun isNumericType(type: ReturnType): Boolean {
        return getNumericTypeName(type) != null
    }
    
    /**
     * Checks if a type name represents a numeric type.
     * 
     * @param typeName The type name to check.
     * @return true if the type is int, long, float, or double.
     */
    fun isNumericType(typeName: String): Boolean {
        return typeName in numericPriority.keys
    }
    
    /**
     * Checks if a type is an integer type (int or long).
     * 
     * @param type The type to check.
     * @return true if the type is int or long.
     */
    fun isIntegerType(type: ReturnType): Boolean {
        val typeName = getNumericTypeName(type) ?: return false
        return typeName == "builtin.int" || typeName == "builtin.long"
    }
    
    /**
     * Checks if a type is a floating point type (float or double).
     * 
     * @param type The type to check.
     * @return true if the type is float or double.
     */
    fun isFloatingPointType(type: ReturnType): Boolean {
        val typeName = getNumericTypeName(type) ?: return false
        return typeName == "builtin.float" || typeName == "builtin.double"
    }
    
    /**
     * Determines the result type for a binary operation between two numeric types.
     * Follows Java's binary numeric promotion rules:
     * 1. If either operand is double, the result is double
     * 2. Otherwise, if either operand is float, the result is float
     * 3. Otherwise, if either operand is long, the result is long
     * 4. Otherwise, the result is int
     * 
     * @param leftType The left operand type name.
     * @param rightType The right operand type name.
     * @return The result type name.
     */
    fun getPromotedType(leftType: String, rightType: String): String {
        val leftPriority = numericPriority[leftType] ?: return leftType
        val rightPriority = numericPriority[rightType] ?: return rightType
        return if (leftPriority >= rightPriority) leftType else rightType
    }
    
    /**
     * Emits bytecode to convert a value from one numeric type to another.
     * Only emits conversion instructions when necessary.
     * 
     * @param fromType The source type name (e.g., "builtin.int").
     * @param toType The target type name (e.g., "builtin.double").
     * @param mv The method visitor to emit bytecode to.
     * @return true if conversion was emitted, false if no conversion was needed.
     */
    fun emitConversion(fromType: String, toType: String, mv: MethodVisitor): Boolean {
        if (fromType == toType) {
            return false
        }
        
        when (fromType) {
            "builtin.int" -> when (toType) {
                "builtin.long" -> mv.visitInsn(Opcodes.I2L)
                "builtin.float" -> mv.visitInsn(Opcodes.I2F)
                "builtin.double" -> mv.visitInsn(Opcodes.I2D)
                else -> return false
            }
            "builtin.long" -> when (toType) {
                "builtin.float" -> mv.visitInsn(Opcodes.L2F)
                "builtin.double" -> mv.visitInsn(Opcodes.L2D)
                else -> return false
            }
            "builtin.float" -> when (toType) {
                "builtin.double" -> mv.visitInsn(Opcodes.F2D)
                else -> return false
            }
            else -> return false
        }
        return true
    }
    
    /**
     * Emits bytecode to convert a value to the target type if needed.
     * 
     * @param fromType The source ReturnType.
     * @param toType The target ReturnType.
     * @param mv The method visitor to emit bytecode to.
     * @return true if conversion was emitted, false if no conversion was needed.
     */
    fun emitConversionIfNeeded(fromType: ReturnType, toType: ReturnType, mv: MethodVisitor): Boolean {
        val fromTypeName = getNumericTypeName(fromType) ?: return false
        val toTypeName = getNumericTypeName(toType) ?: return false
        return emitConversion(fromTypeName, toTypeName, mv)
    }
    
    /**
     * Gets the JVM opcode for an arithmetic operation based on the type.
     * 
     * @param operation The base operation (e.g., Opcodes.IADD for int add).
     * @param typeName The type name determining which variant to use.
     * @return The correct opcode for the type.
     */
    fun getTypedOpcode(operation: Int, typeName: String): Int {
        val offset = when (typeName) {
            "builtin.int" -> 0
            "builtin.long" -> 1
            "builtin.float" -> 2
            "builtin.double" -> 3
            else -> 0
        }
        return operation + offset
    }
    
    /**
     * Gets the appropriate addition opcode for a numeric type.
     * 
     * @param typeName The numeric type name.
     * @return The ADD opcode (IADD, LADD, FADD, or DADD).
     */
    fun getAddOpcode(typeName: String): Int = getTypedOpcode(Opcodes.IADD, typeName)
    
    /**
     * Gets the appropriate subtraction opcode for a numeric type.
     * 
     * @param typeName The numeric type name.
     * @return The SUB opcode (ISUB, LSUB, FSUB, or DSUB).
     */
    fun getSubOpcode(typeName: String): Int = getTypedOpcode(Opcodes.ISUB, typeName)
    
    /**
     * Gets the appropriate multiplication opcode for a numeric type.
     * 
     * @param typeName The numeric type name.
     * @return The MUL opcode (IMUL, LMUL, FMUL, or DMUL).
     */
    fun getMulOpcode(typeName: String): Int = getTypedOpcode(Opcodes.IMUL, typeName)
    
    /**
     * Gets the appropriate division opcode for a numeric type.
     * 
     * @param typeName The numeric type name.
     * @return The DIV opcode (IDIV, LDIV, FDIV, or DDIV).
     */
    fun getDivOpcode(typeName: String): Int = getTypedOpcode(Opcodes.IDIV, typeName)
    
    /**
     * Gets the appropriate remainder (modulo) opcode for a numeric type.
     * 
     * @param typeName The numeric type name.
     * @return The REM opcode (IREM, LREM, FREM, or DREM).
     */
    fun getRemOpcode(typeName: String): Int = getTypedOpcode(Opcodes.IREM, typeName)
    
    /**
     * Gets the appropriate negation opcode for a numeric type.
     * 
     * @param typeName The numeric type name.
     * @return The NEG opcode (INEG, LNEG, FNEG, or DNEG).
     */
    fun getNegOpcode(typeName: String): Int = getTypedOpcode(Opcodes.INEG, typeName)
    
    /**
     * Checks if a type is the string type.
     * 
     * @param type The type to check.
     * @return true if the type is builtin.string.
     */
    fun isStringType(type: ReturnType): Boolean {
        return type is ClassTypeRef && type.type == "builtin.string"
    }
    
    /**
     * Checks if a type name represents the string type.
     * 
     * @param typeName The type name to check.
     * @return true if the type is builtin.string.
     */
    fun isStringType(typeName: String): Boolean {
        return typeName == "builtin.string"
    }
    
    /**
     * Checks if a type is the boolean type.
     * 
     * @param type The type to check.
     * @return true if the type is builtin.boolean.
     */
    fun isBooleanType(type: ReturnType): Boolean {
        return type is ClassTypeRef && type.type == "builtin.boolean"
    }
}
