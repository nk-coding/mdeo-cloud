package com.mdeo.script.compiler.util

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Unified utility for type coercion, including:
 * - Widening primitive conversions (int→long, int→double, etc.)
 * - Boxing (primitive → wrapper)
 * - Unboxing (wrapper → primitive)
 * 
 * This utility should be used anywhere a type conversion is needed to ensure
 * consistent behavior across the compiler.
 * 
 * ## Architecture
 * 
 * CoercionUtil is the main entry point for all type conversions. It delegates to:
 * - TypeConversionUtil: For widening conversions (int→long, int→double) and opcode generation
 * - Internal boxing/unboxing logic: For primitive ↔ wrapper conversions
 */
object CoercionUtil {
    
    private val PRIMITIVE_TYPES = setOf(
        "builtin.int",
        "builtin.long",
        "builtin.float",
        "builtin.double",
        "builtin.boolean"
    )
    
    /**
     * Checks if a type name represents a primitive type.
     * 
     * @param typeName The type name to check.
     * @return true if it's a primitive type (int, long, float, double, boolean).
     */
    fun isPrimitiveType(typeName: String): Boolean {
        return typeName in PRIMITIVE_TYPES
    }
    
    /**
     * Gets the primitive type name from a ClassTypeRef, regardless of nullability.
     * 
     * @param type The return type to extract the primitive type name from.
     * @return The primitive type name (e.g., "builtin.int"), or null if the type is not a primitive type.
     */
    fun getPrimitiveTypeName(type: ReturnType): String? {
        if (type !is ClassTypeRef) {
            return null
        }
        return if (isPrimitiveType(type.type)) {
            type.type
        } else {
            null
        }
    }
    
    /**
     * Checks if the source type produces a primitive value on the stack.
     * Non-nullable primitive types and literal expressions produce primitives.
     * Nullable types produce objects on the stack, unless it's a literal expression
     * that always produces a primitive.
     * 
     * @param type The return type to check.
     * @param expression Optional expression to check if it's a literal that always produces a primitive.
     * @return true if the type produces a primitive value on the stack.
     */
    fun producesStackPrimitive(type: ReturnType, expression: TypedExpression? = null): Boolean {
        if (type !is ClassTypeRef) {
            return false
        }
        
        if (type.isNullable) {
            if (expression != null) {
                return isLiteralExpression(expression)
            }
            return false
        }
        
        return isPrimitiveType(type.type)
    }
    
    /**
     * Checks if the target type expects a primitive value on the stack.
     * 
     * @param type The return type to check.
     * @return true if the type expects a primitive value (non-nullable primitive type).
     */
    fun expectsStackPrimitive(type: ReturnType): Boolean {
        if (type !is ClassTypeRef) {
            return false
        }
        return !type.isNullable && isPrimitiveType(type.type)
    }
    
    /**
     * Checks if an expression is a literal that always produces a primitive.
     * 
     * @param expression The typed expression to check.
     * @return true if the expression is a numeric or boolean literal.
     */
    private fun isLiteralExpression(expression: TypedExpression): Boolean {
        return expression.kind in setOf(
            TypedExpressionKind.IntLiteral,
            TypedExpressionKind.LongLiteral,
            TypedExpressionKind.FloatLiteral,
            TypedExpressionKind.DoubleLiteral,
            TypedExpressionKind.BooleanLiteral
        )
    }
    
    /**
     * Checks if an expression is a null literal.
     * 
     * @param expression The typed expression to check.
     * @return true if the expression is a null literal.
     */
    fun isNullLiteral(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.NullLiteral
    }
    
    /**
     * Emits boxing bytecode for a primitive type.
     * Uses valueOf methods: Integer.valueOf(int), Long.valueOf(long), etc.
     * 
     * @param primitiveTypeName The primitive type name (e.g., "builtin.int")
     * @param mv The method visitor
     * @return true if boxing was emitted
     */
    fun emitBoxing(primitiveTypeName: String, mv: MethodVisitor): Boolean {
        return when (primitiveTypeName) {
            "builtin.int" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Integer",
                    "valueOf",
                    "(I)Ljava/lang/Integer;",
                    false
                )
                true
            }
            "builtin.long" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Long",
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    false
                )
                true
            }
            "builtin.float" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Float",
                    "valueOf",
                    "(F)Ljava/lang/Float;",
                    false
                )
                true
            }
            "builtin.double" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Double",
                    "valueOf",
                    "(D)Ljava/lang/Double;",
                    false
                )
                true
            }
            "builtin.boolean" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/Boolean",
                    "valueOf",
                    "(Z)Ljava/lang/Boolean;",
                    false
                )
                true
            }
            else -> false
        }
    }
    
    /**
     * Emits unboxing bytecode for a wrapper type.
     * Uses instance methods: intValue(), longValue(), etc.
     * 
     * @param primitiveTypeName The primitive type name to unbox to (e.g., "builtin.int")
     * @param mv The method visitor
     * @return true if unboxing was emitted
     */
    fun emitUnboxing(primitiveTypeName: String, mv: MethodVisitor): Boolean {
        return when (primitiveTypeName) {
            "builtin.int" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Integer",
                    "intValue",
                    "()I",
                    false
                )
                true
            }
            "builtin.long" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Long",
                    "longValue",
                    "()J",
                    false
                )
                true
            }
            "builtin.float" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Float",
                    "floatValue",
                    "()F",
                    false
                )
                true
            }
            "builtin.double" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Double",
                    "doubleValue",
                    "()D",
                    false
                )
                true
            }
            "builtin.boolean" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Boolean",
                    "booleanValue",
                    "()Z",
                    false
                )
                true
            }
            else -> false
        }
    }
    
    /**
     * Emits coercion bytecode to convert from source type to target type.
     * Handles:
     * - Widening primitive conversions (int→long, int→double, etc.)
     * - Boxing when target is nullable and source is primitive
     * - Unboxing when target is primitive and source is nullable wrapper
     * - No-op when types are compatible
     * 
     * @param sourceType The source type (what's on the stack)
     * @param targetType The target type (what's expected)
     * @param expression The source expression (for detecting literals)
     * @param mv The method visitor
     * @return true if any conversion was emitted
     */
    fun emitCoercion(
        sourceType: ReturnType,
        targetType: ReturnType,
        expression: TypedExpression?,
        mv: MethodVisitor
    ): Boolean {
        if (sourceType !is ClassTypeRef || targetType !is ClassTypeRef) {
            return false
        }
        
        val sourceTypeName = sourceType.type
        val targetTypeName = targetType.type
        
        val sourceIsNullable = sourceType.isNullable
        val targetIsNullable = targetType.isNullable
        
        val sourceIsPrimitive = isPrimitiveType(sourceTypeName)
        val targetIsPrimitive = isPrimitiveType(targetTypeName)
        
        if (producesStackPrimitive(sourceType, expression) && targetIsNullable && targetIsPrimitive) {
            return emitPrimitiveToBoxedConversion(sourceTypeName, targetTypeName, mv)
        }
        
        /** Box primitive when target is a reference type (like Any or Any?) */
        if (producesStackPrimitive(sourceType, expression) && !targetIsPrimitive) {
            return emitBoxing(sourceTypeName, mv)
        }
        
        if (sourceIsNullable && sourceIsPrimitive && !targetIsNullable && targetIsPrimitive) {
            return emitBoxedToPrimitiveConversion(sourceTypeName, targetTypeName, mv)
        }
        
        if (!sourceIsNullable && sourceIsPrimitive && !targetIsNullable && targetIsPrimitive) {
            return TypeConversionUtil.emitConversion(sourceTypeName, targetTypeName, mv)
        }
        
        if (sourceType == targetType) {
            return false
        }
        
        if (sourceIsNullable && sourceIsPrimitive && targetIsNullable && targetIsPrimitive && sourceTypeName != targetTypeName) {
            return emitBoxedToBoxedConversion(sourceTypeName, targetTypeName, mv)
        }
        
        return false
    }
    
    /**
     * Emits conversion from a primitive value to a boxed (nullable) value.
     * First performs any necessary primitive widening conversion, then boxes the result.
     * 
     * @param sourceTypeName The source primitive type name.
     * @param targetTypeName The target primitive type name (for the boxed wrapper).
     * @param mv The method visitor to emit bytecode to.
     * @return true if boxing was emitted.
     */
    private fun emitPrimitiveToBoxedConversion(
        sourceTypeName: String,
        targetTypeName: String,
        mv: MethodVisitor
    ): Boolean {
        if (sourceTypeName != targetTypeName && TypeConversionUtil.isNumericType(sourceTypeName) && TypeConversionUtil.isNumericType(targetTypeName)) {
            TypeConversionUtil.emitConversion(sourceTypeName, targetTypeName, mv)
        }
        return emitBoxing(targetTypeName, mv)
    }
    
    /**
     * Emits conversion from a boxed (nullable) value to a primitive value.
     * First unboxes the value, then performs any necessary primitive widening conversion.
     * 
     * @param sourceTypeName The source primitive type name (of the boxed wrapper).
     * @param targetTypeName The target primitive type name.
     * @param mv The method visitor to emit bytecode to.
     * @return true always, as unboxing is always emitted.
     */
    private fun emitBoxedToPrimitiveConversion(
        sourceTypeName: String,
        targetTypeName: String,
        mv: MethodVisitor
    ): Boolean {
        emitUnboxing(sourceTypeName, mv)
        if (sourceTypeName != targetTypeName && TypeConversionUtil.isNumericType(sourceTypeName) && TypeConversionUtil.isNumericType(targetTypeName)) {
            TypeConversionUtil.emitConversion(sourceTypeName, targetTypeName, mv)
        }
        return true
    }
    
    /**
     * Emits conversion between two different boxed (nullable) primitive types.
     * Unboxes the source, performs primitive conversion, then reboxes to target type.
     * 
     * @param sourceTypeName The source primitive type name (of the boxed wrapper).
     * @param targetTypeName The target primitive type name (for the boxed wrapper).
     * @param mv The method visitor to emit bytecode to.
     * @return true always, as conversion is always emitted.
     */
    private fun emitBoxedToBoxedConversion(
        sourceTypeName: String,
        targetTypeName: String,
        mv: MethodVisitor
    ): Boolean {
        emitUnboxing(sourceTypeName, mv)
        TypeConversionUtil.emitConversion(sourceTypeName, targetTypeName, mv)
        emitBoxing(targetTypeName, mv)
        return true
    }
    
    /**
     * Emits coercion without expression context.
     * Uses source type nullability to determine if value is boxed.
     * 
     * @param sourceType The source type (what's on the stack).
     * @param targetType The target type (what's expected).
     * @param mv The method visitor to emit bytecode to.
     * @return true if any conversion was emitted.
     */
    fun emitCoercion(
        sourceType: ReturnType,
        targetType: ReturnType,
        mv: MethodVisitor
    ): Boolean {
        return emitCoercion(sourceType, targetType, null, mv)
    }

    
    /**
     * Gets the primitive type descriptor for JVM.
     * 
     * Delegates to ASMUtil for consistency.
     * 
     * @param primitiveTypeName The primitive type name (e.g., "builtin.int").
     * @return The JVM type descriptor (e.g., "I" for int), or null if not a primitive type.
     */
    fun getPrimitiveDescriptor(primitiveTypeName: String): String? {
        return ASMUtil.getPrimitiveDescriptor(primitiveTypeName)
    }
    
    /**
     * Gets the type name component for functional interface naming.
     * 
     * Converts a ReturnType to a string suitable for use in functional interface
     * names. For nullable types, uses wrapper class names (Integer, Long, etc.).
     * For non-nullable primitives, uses short names (Int, Long, etc.).
     * 
     * @param type The type to get the name for.
     * @return The type name (e.g., "Int", "Long", "Void", "Object").
     */
    fun getTypeNameForInterface(type: ReturnType): String {
        return when (type) {
            is com.mdeo.script.ast.types.VoidType -> "Void"
            is ClassTypeRef -> {
                if (type.isNullable) {
                    when (type.type) {
                        "builtin.int" -> "Integer"
                        "builtin.long" -> "Long"
                        "builtin.float" -> "Float"
                        "builtin.double" -> "Double"
                        "builtin.boolean" -> "Boolean"
                        "builtin.string" -> "String"
                        else -> "Object"
                    }
                } else {
                    when (type.type) {
                        "builtin.int" -> "Int"
                        "builtin.long" -> "Long"
                        "builtin.float" -> "Float"
                        "builtin.double" -> "Double"
                        "builtin.boolean" -> "Boolean"
                        "builtin.string" -> "String"
                        else -> "Object"
                    }
                }
            }
            else -> "Object"
        }
    }
    
    /**
     * Builds the functional interface name for a lambda type.
     * 
     * Interface names follow the pattern: Lambda$ReturnType$ParamType1$ParamType2...
     * 
     * Examples:
     * - () => void: Lambda$Void$0
     * - () => int: Lambda$Int$0
     * - (int) => int: Lambda$Int$Int
     * - (int, double) => void: Lambda$Void$Int$Double
     * 
     * @param returnType The return type of the lambda.
     * @param parameterTypes The parameter types of the lambda.
     * @return The functional interface name (e.g., "Lambda$Int$Double").
     */
    fun getFunctionalInterfaceName(returnType: ReturnType, parameterTypes: List<ReturnType>): String {
        val returnPart = getTypeNameForInterface(returnType)
        
        val paramParts = if (parameterTypes.isEmpty()) {
            "0"
        } else {
            parameterTypes.joinToString("\$") { paramType ->
                getTypeNameForInterface(paramType)
            }
        }
        
        return "Lambda\$$returnPart\$$paramParts"
    }
}
