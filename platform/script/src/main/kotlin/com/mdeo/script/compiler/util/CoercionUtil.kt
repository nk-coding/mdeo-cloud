package com.mdeo.script.compiler.util

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.VoidType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.LambdaInterfaceRegistry
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

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
    fun isPrimitiveType(type: ReturnType): Boolean {
        return type is ClassTypeRef && PRIMITIVE_TYPES.contains(type.type)
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
        
        return isPrimitiveType(type)
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
     * - Lambda type coercion (when context is provided)
     * - No-op when types are compatible
     * 
     * @param sourceType The source type (what's on the stack)
     * @param targetType The target type (what's expected)
     * @param mv The method visitor
     * @param context Optional compilation context (required for lambda coercion)
     * @return true if any conversion was emitted
     */
    fun emitCoercion(
        sourceType: ReturnType,
        targetType: ReturnType,
        mv: MethodVisitor,
        context: CompilationContext? = null
    ): Boolean {
        if (sourceType is LambdaType && targetType is LambdaType) {
            if (context != null) {
                return emitLambdaCoercion(sourceType, targetType, context, mv)
            }
            return false
        }
        
        if (sourceType is ClassTypeRef && targetType is ClassTypeRef) {
            return emitClassTypeCoercion(sourceType, targetType, mv)
        }
        
        return false
    }
    
    /**
     * Emits coercion bytecode for class type conversions (primitives and wrappers).
     */
    private fun emitClassTypeCoercion(
        sourceType: ClassTypeRef,
        targetType: ClassTypeRef,
        mv: MethodVisitor
    ): Boolean {
        val sourceTypeName = sourceType.type
        val targetTypeName = targetType.type
        
        val sourceIsNullable = sourceType.isNullable
        val targetIsNullable = targetType.isNullable
        
        val sourceIsPrimitive = isPrimitiveType(sourceType)
        val targetIsPrimitive = isPrimitiveType(targetType)
        val sourceIsAny = sourceTypeName == "builtin.any"
        
        if (sourceIsPrimitive && !sourceIsNullable && targetIsPrimitive && targetIsNullable) {
            return emitPrimitiveToBoxedConversion(sourceTypeName, targetTypeName, mv)
        }
        
        if (sourceIsPrimitive && !sourceIsNullable && !targetIsPrimitive) {
            return emitBoxing(sourceTypeName, mv)
        }
        
        if (sourceIsAny && !targetIsNullable && targetIsPrimitive) {
            return emitAnyToPrimitiveConversion(targetTypeName, mv)
        }
        
        if (sourceIsAny && targetIsNullable && targetIsPrimitive) {
            return emitAnyToBoxedConversion(targetTypeName, mv)
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

        if (!sourceIsPrimitive && !targetIsPrimitive && sourceTypeName != targetTypeName) {
            return emitReferenceTypeConversion(sourceType, targetType, mv)
        }
        
        return false
    }
    
    /**
     * Emits conversion from Any/Object to a primitive type.
     * CHECKCASTs to the wrapper class, then unboxes.
     * 
     * @param targetTypeName The target primitive type name.
     * @param mv The method visitor.
     * @return true if coercion was emitted.
     */
    private fun emitAnyToPrimitiveConversion(targetTypeName: String, mv: MethodVisitor): Boolean {
        val wrapperClass = getWrapperClassName(targetTypeName) ?: return false
        mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperClass)
        return emitUnboxing(targetTypeName, mv)
    }
    
    /**
     * Emits conversion from Any/Object to a nullable primitive (boxed) type.
     * CHECKCASTs to the wrapper class.
     * 
     * @param targetTypeName The target primitive type name.
     * @param mv The method visitor.
     * @return true if coercion was emitted.
     */
    private fun emitAnyToBoxedConversion(targetTypeName: String, mv: MethodVisitor): Boolean {
        val wrapperClass = getWrapperClassName(targetTypeName) ?: return false
        mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperClass)
        return true
    }
    
    /**
     * Gets the JVM internal class name for a primitive type's wrapper.
     * 
     * @param primitiveTypeName The primitive type name (e.g., "builtin.int").
     * @return The wrapper class name (e.g., "java/lang/Integer"), or null if not a primitive.
     */
    private fun getWrapperClassName(primitiveTypeName: String): String? {
        return when (primitiveTypeName) {
            "builtin.int" -> "java/lang/Integer"
            "builtin.long" -> "java/lang/Long"
            "builtin.float" -> "java/lang/Float"
            "builtin.double" -> "java/lang/Double"
            "builtin.boolean" -> "java/lang/Boolean"
            else -> null
        }
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
     * Emits conversion between reference types using CHECKCAST.
     * 
     * This handles conversions between non-primitive types, such as:
     * - Any? to string (needs CHECKCAST to String)
     * - Any? to Integer/Long/etc (needs CHECKCAST to wrapper type)
     * - string to Any? (no CHECKCAST needed - upcast is automatic)
     * - Integer to Any? (no CHECKCAST needed - upcast is automatic)
     * - Custom type to Custom type (needs CHECKCAST if different)
     * - Object to wrapper types (needs CHECKCAST)
     * 
     * @param sourceType The source class type reference.
     * @param targetType The target class type reference.
     * @param mv The method visitor to emit bytecode to.
     * @return true if a CHECKCAST was emitted or the conversion is valid.
     */
    private fun emitReferenceTypeConversion(
        sourceType: ClassTypeRef,
        targetType: ClassTypeRef,
        mv: MethodVisitor
    ): Boolean {
        val sourceTypeName = sourceType.type
        val targetTypeName = targetType.type
        
        // Upcasts to Any/Any? don't need CHECKCAST (automatic in JVM)
        if (targetTypeName == "builtin.any") {
            return false
        }
        
        // If types are identical, no conversion needed
        if (sourceTypeName == targetTypeName && sourceType.isNullable == targetType.isNullable) {
            return false
        }
        
        // For any downcast or cross-cast, emit CHECKCAST
        // This includes:
        // - Any? -> string, Any? -> CustomType
        // - Object -> Integer (wrapper), Object -> String
        // - string -> CustomType (invalid at runtime, but let JVM handle it)
        // - Nullable wrapper to non-nullable wrapper of same type
        if (sourceTypeName != targetTypeName || sourceType.isNullable != targetType.isNullable) {
            val targetDescriptor = ASMUtil.getTypeDescriptor(targetType)
            if (targetDescriptor.startsWith("L") && targetDescriptor.endsWith(";")) {
                // Extract internal name from descriptor (e.g., "Ljava/lang/String;" -> "java/lang/String")
                val internalName = targetDescriptor.substring(1, targetDescriptor.length - 1)
                mv.visitTypeInsn(Opcodes.CHECKCAST, internalName)
                return true
            }
        }
        
        return false
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
    
    /**
     * Emits lambda type coercion bytecode.
     * 
     * When the source is a lambda type and the target is a different lambda type,
     * this creates a wrapper lambda that:
     * 1. Takes the target signature's parameters
     * 2. Coerces them to the source lambda's parameter types
     * 3. Calls the source lambda
     * 4. Coerces the return value to the target's return type
     * 
     * @param sourceType The source lambda type (what's on the stack).
     * @param targetType The target lambda type (what's expected).
     * @param context The compilation context.
     * @param mv The method visitor.
     * @return true if coercion was emitted, false if types are compatible.
     */
    fun emitLambdaCoercion(
        sourceType: LambdaType,
        targetType: LambdaType,
        context: CompilationContext,
        mv: MethodVisitor
    ): Boolean {
        if (areLambdaTypesCompatible(sourceType, targetType)) {
            return false
        }
        
        val wrapperMethodName = context.generateLambdaMethodName()
        generateWrapperLambdaMethod(wrapperMethodName, sourceType, targetType, context)
        emitWrapperInvokeDynamic(wrapperMethodName, sourceType, targetType, context, mv)
        
        return true
    }
    
    /**
     * Checks if two lambda types are compatible without needing coercion.
     */
    private fun areLambdaTypesCompatible(source: LambdaType, target: LambdaType): Boolean {
        if (source.parameters.size != target.parameters.size) return false
        if (source.returnType != target.returnType) return false
        
        for (i in source.parameters.indices) {
            if (source.parameters[i].type != target.parameters[i].type) return false
        }
        
        return true
    }
    
    /**
     * Generates a wrapper lambda method that adapts the source lambda to the target signature.
     */
    private fun generateWrapperLambdaMethod(
        methodName: String,
        sourceType: LambdaType,
        targetType: LambdaType,
        context: CompilationContext
    ) {
        val cw = context.classWriter
            ?: throw IllegalStateException("ClassWriter required for lambda coercion")
        
        val sourceInterfaceName = getInterfaceForLambdaType(sourceType, context)
        val wrapperDescriptor = buildWrapperMethodDescriptor(sourceInterfaceName, targetType, context)
        
        val wrapperMv = cw.visitMethod(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            methodName,
            wrapperDescriptor,
            null,
            null
        )
        
        wrapperMv.visitCode()
        emitWrapperMethodBody(wrapperMv, sourceType, targetType, context)
        wrapperMv.visitMaxs(0, 0)
        wrapperMv.visitEnd()
    }
    
    /**
     * Emits the body of the wrapper method that calls the source lambda with coerced params.
     */
    private fun emitWrapperMethodBody(
        mv: MethodVisitor,
        sourceType: LambdaType,
        targetType: LambdaType,
        context: CompilationContext
    ) {
        val sourceInterfaceName = getInterfaceForLambdaType(sourceType, context)
        
        // Load source lambda (first parameter, slot 0)
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        
        // Load and coerce each parameter from target types to source types
        var paramSlot = 1
        for (i in sourceType.parameters.indices) {
            val sourceParamType = sourceType.parameters[i].type
            val targetParamType = targetType.parameters[i].type
            
            mv.visitVarInsn(ASMUtil.getLoadOpcode(targetParamType), paramSlot)
            emitCoercion(targetParamType, sourceParamType, mv)
            
            paramSlot += ASMUtil.getSlotsForType(targetParamType)
        }
        
        // Invoke the source lambda's call method
        val sourceMethodDescriptor = buildLambdaCallDescriptor(sourceType, context)
        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            sourceInterfaceName,
            "call",
            sourceMethodDescriptor,
            true
        )
        
        // Coerce return value from source to target type
        emitCoercion(sourceType.returnType, targetType.returnType, mv)
        
        // Return with appropriate instruction
        emitReturnInstruction(targetType.returnType, mv)
    }
    
    /**
     * Builds the method descriptor for the wrapper synthetic method.
     * Takes the source lambda interface as first param, then target param types.
     */
    private fun buildWrapperMethodDescriptor(
        sourceInterfaceName: String,
        targetType: LambdaType,
        context: CompilationContext
    ): String {
        val params = StringBuilder()
        params.append("L").append(sourceInterfaceName).append(";")
        
        for (i in 0 until targetType.parameters.size.coerceAtMost(
            // Only include params that the source lambda actually uses
            targetType.parameters.size
        )) {
            val paramType = targetType.parameters[i].type
            params.append(context.getTypeDescriptor(paramType))
        }
        
        val returnDesc = context.getTypeDescriptor(targetType.returnType)
        return "(${params})$returnDesc"
    }
    
    /**
     * Builds the method descriptor for calling the source lambda's call method.
     */
    private fun buildLambdaCallDescriptor(lambdaType: LambdaType, context: CompilationContext): String {
        val params = lambdaType.parameters.joinToString("") { param ->
            context.getTypeDescriptor(param.type)
        }
        val returnDesc = context.getTypeDescriptor(lambdaType.returnType)
        return "(${params})$returnDesc"
    }
    
    /**
     * Emits the invokedynamic instruction to create the wrapper lambda.
     */
    private fun emitWrapperInvokeDynamic(
        methodName: String,
        sourceType: LambdaType,
        targetType: LambdaType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val sourceInterfaceName = getInterfaceForLambdaType(sourceType, context)
        val targetInterfaceName = getInterfaceForLambdaType(targetType, context)
        
        // The source lambda is already on the stack, it will be captured
        val capturedDesc = "L${sourceInterfaceName};"
        val invokeDynamicDesc = "(${capturedDesc})L${targetInterfaceName};"
        
        val samMethodType = buildErasedOrActualMethodType(targetType, context)
        val wrapperDescriptor = buildWrapperMethodDescriptor(sourceInterfaceName, targetType, context)
        
        val implMethodHandle = Handle(
            Opcodes.H_INVOKESTATIC,
            context.currentClassName,
            methodName,
            wrapperDescriptor,
            false
        )
        
        val instantiatedMethodType = buildInstantiatedMethodType(targetType, context)
        
        mv.visitInvokeDynamicInsn(
            "call",
            invokeDynamicDesc,
            Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
            ),
            samMethodType,
            implMethodHandle,
            instantiatedMethodType
        )
    }
    
    /**
     * Gets the functional interface for a lambda type.
     */
    private fun getInterfaceForLambdaType(lambdaType: LambdaType, context: CompilationContext): String {
        val registry = context.getLambdaInterfaceRegistry()
        val lookupResult = registry.getInterfaceForLambdaType(lambdaType)
        
        if (lookupResult.isNewlyGenerated) {
            val bytecode = generateFunctionalInterfaceBytecode(lookupResult.interfaceName, lambdaType, context)
            context.registerInterface(lookupResult.interfaceName, bytecode)
        }
        
        return lookupResult.interfaceName
    }
    
    /**
     * Generates bytecode for a functional interface.
     */
    private fun generateFunctionalInterfaceBytecode(
        interfaceName: String,
        lambdaType: LambdaType,
        context: CompilationContext
    ): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        
        cw.visit(
            Opcodes.V11,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
            interfaceName,
            null,
            "java/lang/Object",
            null
        )
        
        val methodDescriptor = buildLambdaCallDescriptor(lambdaType, context)
        
        cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
            "call",
            methodDescriptor,
            null,
            null
        )?.visitEnd()
        
        cw.visitEnd()
        
        return cw.toByteArray()
    }
    
    /**
     * Builds the SAM method type for interfaces.
     * 
     * For predefined interfaces (Func0-3, Action0-3, Predicate1), uses erased Object types.
     * For generated interfaces (Lambda$N), uses the actual types from the lambda type.
     */
    private fun buildErasedOrActualMethodType(lambdaType: LambdaType, context: CompilationContext): Type {
        val registry = context.getLambdaInterfaceRegistry()
        val lookupResult = registry.getInterfaceForLambdaType(lambdaType)
        val isPredefined = lookupResult.interfaceName.startsWith("com/mdeo/script/runtime")
        
        val descriptor = if (isPredefined) {
            // Predefined interfaces use erased Object types for generics
            buildErasedMethodDescriptor(lambdaType)
        } else {
            // Generated interfaces use actual types
            buildLambdaCallDescriptor(lambdaType, context)
        }
        return Type.getMethodType(descriptor)
    }
    
    /**
     * Builds the erased method descriptor for generic interfaces.
     */
    private fun buildErasedMethodDescriptor(lambdaType: LambdaType): String {
        val params = lambdaType.parameters.joinToString("") { "Ljava/lang/Object;" }
        val returnDesc = if (lambdaType.returnType is VoidType) {
            "V"
        } else {
            "Ljava/lang/Object;"
        }
        return "($params)$returnDesc"
    }
    
    /**
     * Builds the instantiated method type for the target lambda.
     * 
     * For predefined generic interfaces (Func0-3), this uses boxed types (Integer, etc.)
     * because the implementation needs to bridge from Object to boxed types.
     * 
     * For generated interfaces, this matches the interface method descriptor exactly
     * because the interface already uses the correct types.
     */
    private fun buildInstantiatedMethodType(lambdaType: LambdaType, context: CompilationContext): Type {
        val registry = context.getLambdaInterfaceRegistry()
        val lookupResult = registry.getInterfaceForLambdaType(lambdaType)
        val isPredefined = lookupResult.interfaceName.startsWith("com/mdeo/script/runtime")
        
        val descriptor = if (isPredefined) {
            // For predefined interfaces, use boxed types (Integer for int, etc.)
            buildBoxedMethodDescriptor(lambdaType)
        } else {
            // For generated interfaces, use actual types
            buildLambdaCallDescriptor(lambdaType, context)
        }
        return Type.getMethodType(descriptor)
    }
    
    /**
     * Builds the boxed method descriptor for instantiated generic interfaces.
     */
    private fun buildBoxedMethodDescriptor(lambdaType: LambdaType): String {
        val params = lambdaType.parameters.joinToString("") { param ->
            getBoxedTypeDescriptor(param.type)
        }
        val returnDesc = if (lambdaType.returnType is VoidType) {
            "V"
        } else {
            getBoxedTypeDescriptor(lambdaType.returnType)
        }
        return "($params)$returnDesc"
    }
    
    /**
     * Gets the boxed type descriptor for a type.
     */
    private fun getBoxedTypeDescriptor(type: ReturnType): String {
        if (type !is ClassTypeRef) return "Ljava/lang/Object;"
        
        return when (type.type) {
            "builtin.int" -> "Ljava/lang/Integer;"
            "builtin.long" -> "Ljava/lang/Long;"
            "builtin.float" -> "Ljava/lang/Float;"
            "builtin.double" -> "Ljava/lang/Double;"
            "builtin.boolean" -> "Ljava/lang/Boolean;"
            "builtin.string" -> "Ljava/lang/String;"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Emits the appropriate return instruction for a type.
     */
    private fun emitReturnInstruction(returnType: ReturnType, mv: MethodVisitor) {
        when {
            returnType is VoidType -> mv.visitInsn(Opcodes.RETURN)
            returnType is ClassTypeRef && !returnType.isNullable -> {
                when (returnType.type) {
                    "builtin.int", "builtin.boolean" -> mv.visitInsn(Opcodes.IRETURN)
                    "builtin.long" -> mv.visitInsn(Opcodes.LRETURN)
                    "builtin.float" -> mv.visitInsn(Opcodes.FRETURN)
                    "builtin.double" -> mv.visitInsn(Opcodes.DRETURN)
                    else -> mv.visitInsn(Opcodes.ARETURN)
                }
            }
            else -> mv.visitInsn(Opcodes.ARETURN)
        }
    }
}
