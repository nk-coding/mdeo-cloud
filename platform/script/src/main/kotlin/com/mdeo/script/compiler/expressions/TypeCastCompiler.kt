package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedTypeCastExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.registry.type.TypeRegistry
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.util.TypeConversionUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for type cast expressions (as / as?).
 *
 * The `as` operator performs a regular Java cast. For primitives, it handles
 * numeric conversions (e.g., double to int). Throws ClassCastException if incompatible.
 *
 * The `as?` (safe cast) operator returns null instead of throwing an exception
 * when the cast would fail. It performs an `is` check first, then casts or returns null.
 *
 * Examples:
 * ```
 * val x: Any? = 42
 * val y = x as int     // throws if x is not an int/Integer
 * val z = x as? int    // returns null if x is not an int/Integer
 * ```
 *
 * Optimizations:
 * - No-op when source and target types are identical (e.g., int as int)
 * - Boxing is emitted when casting primitives to Any/Any? (e.g., int as Any)
 */
class TypeCastCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return true if the expression is a type cast expression.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "typeCast"
    }

    /**
     * Compiles the type cast expression to bytecode.
     *
     * @param expression The type cast expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor to emit bytecode to.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val castExpr = expression as TypedTypeCastExpression
        val sourceType = context.getType(castExpr.expression.evalType)
        val targetType = context.getType(castExpr.targetType)

        if (castExpr.isSafe) {
            compileSafeCast(castExpr, sourceType, targetType, context, mv)
        } else {
            val targetIsNullable = (targetType as? ClassTypeRef)?.isNullable == true
            compileRegularCast(castExpr, sourceType, targetType, targetIsNullable, context, mv)
        }
    }

    /**
     * Compiles a regular cast (as) expression.
     * Handles primitive conversions and reference type casts.
     */
    private fun compileRegularCast(
        castExpr: TypedTypeCastExpression,
        sourceType: ReturnType,
        targetType: ReturnType,
        targetIsNullable: Boolean,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        context.compileExpression(castExpr.expression, mv, sourceType)

        if (isNoOpCast(sourceType, targetType)) {
            return
        }

        emitCastConversion(sourceType, targetType, targetIsNullable, context, mv)
    }

    /**
     * Compiles a safe cast (as?) expression.
     * Returns null if the cast would fail instead of throwing ClassCastException.
     */
    private fun compileSafeCast(
        castExpr: TypedTypeCastExpression,
        sourceType: ReturnType,
        targetType: ReturnType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        if (canDetermineAlwaysTrue(sourceType, targetType, context)) {
            context.compileExpression(castExpr.expression, mv, sourceType)
            emitCastConversion(sourceType, targetType, true, context, mv)
            return
        }

        if (canDetermineAlwaysFalse(sourceType, targetType, context)) {
            context.compileExpression(castExpr.expression, mv, sourceType)
            popValue(sourceType, mv)
            mv.visitInsn(Opcodes.ACONST_NULL)
            return
        }

        compileSafeCastWithRuntimeCheck(castExpr, sourceType, targetType, context, mv)
    }

    /**
     * Compiles a safe cast that requires runtime type checking.
     */
    private fun compileSafeCastWithRuntimeCheck(
        castExpr: TypedTypeCastExpression,
        sourceType: ReturnType,
        targetType: ReturnType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        context.compileExpression(castExpr.expression, mv, sourceType)
        ensureObjectOnStack(sourceType, mv)

        val nullLabel = Label()
        val endLabel = Label()

        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNULL, nullLabel)

        mv.visitInsn(Opcodes.DUP)
        val checkClass = getInstanceOfClass(targetType, context)
        mv.visitTypeInsn(Opcodes.INSTANCEOF, checkClass)
        mv.visitJumpInsn(Opcodes.IFEQ, nullLabel)

        mv.visitTypeInsn(Opcodes.CHECKCAST, checkClass)

        mv.visitJumpInsn(Opcodes.GOTO, endLabel)

        mv.visitLabel(nullLabel)
        mv.visitInsn(Opcodes.POP)
        mv.visitInsn(Opcodes.ACONST_NULL)

        mv.visitLabel(endLabel)
    }

    /**
     * Emits the appropriate conversion bytecode for the cast.
     */
    private fun emitCastConversion(
        sourceType: ReturnType,
        targetType: ReturnType,
        resultIsNullable: Boolean,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val sourceIsPrimitive = CoercionUtil.producesStackPrimitive(sourceType)
        val targetIsPrimitive = CoercionUtil.isPrimitiveType(targetType)

        when {
            sourceIsPrimitive && targetIsPrimitive && !resultIsNullable -> {
                emitPrimitiveToPrimitiveConversion(sourceType, targetType, mv)
            }
            sourceIsPrimitive && targetIsPrimitive && resultIsNullable -> {
                emitPrimitiveToPrimitiveConversion(sourceType, targetType, mv)
                boxPrimitive(targetType, mv)
            }
            sourceIsPrimitive && !targetIsPrimitive -> {
                boxPrimitive(sourceType, mv)
                emitReferenceTypeCast(targetType, context, mv)
            }
            !sourceIsPrimitive && targetIsPrimitive && !resultIsNullable -> {
                emitObjectToPrimitiveConversion(sourceType, targetType, context, mv)
            }
            !sourceIsPrimitive && targetIsPrimitive && resultIsNullable -> {
                emitObjectToBoxedConversion(targetType, context, mv)
            }
            else -> {
                emitReferenceTypeCast(targetType, context, mv)
            }
        }
    }

    /**
     * Emits primitive-to-primitive numeric conversion.
     */
    private fun emitPrimitiveToPrimitiveConversion(
        sourceType: ReturnType,
        targetType: ReturnType,
        mv: MethodVisitor
    ) {
        val sourceName = (sourceType as ClassTypeRef).type
        val targetName = (targetType as ClassTypeRef).type

        if (sourceName == targetName) return

        emitNarrowingOrWideningConversion(sourceName, targetName, mv)
    }

    /**
     * Emits numeric conversions including narrowing (e.g., double to int).
     */
    private fun emitNarrowingOrWideningConversion(sourceName: String, targetName: String, mv: MethodVisitor) {
        if (TypeConversionUtil.emitConversion(sourceName, targetName, mv)) {
            return
        }

        when (sourceName) {
            "builtin.long" -> when (targetName) {
                "builtin.int" -> mv.visitInsn(Opcodes.L2I)
                "builtin.float" -> mv.visitInsn(Opcodes.L2F)
                "builtin.double" -> mv.visitInsn(Opcodes.L2D)
            }
            "builtin.float" -> when (targetName) {
                "builtin.int" -> mv.visitInsn(Opcodes.F2I)
                "builtin.long" -> mv.visitInsn(Opcodes.F2L)
                "builtin.double" -> mv.visitInsn(Opcodes.F2D)
            }
            "builtin.double" -> when (targetName) {
                "builtin.int" -> mv.visitInsn(Opcodes.D2I)
                "builtin.long" -> mv.visitInsn(Opcodes.D2L)
                "builtin.float" -> mv.visitInsn(Opcodes.D2F)
            }
        }
    }

    /**
     * Emits conversion from object/boxed type to primitive.
     */
    private fun emitObjectToPrimitiveConversion(
        sourceType: ReturnType,
        targetType: ReturnType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val targetName = (targetType as ClassTypeRef).type
        val sourceRef = sourceType as ClassTypeRef

        if (sourceRef.type == "builtin.any") {
            val wrapperClass = context.typeRegistry.getWrapperClassName(targetName) ?: "java/lang/Object"
            mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperClass)
            CoercionUtil.emitUnboxing(targetName, mv)
        } else if (CoercionUtil.isPrimitiveType(sourceType) && sourceRef.isNullable) {
            CoercionUtil.emitUnboxing(sourceRef.type, mv)
            if (sourceRef.type != targetName) {
                emitNarrowingOrWideningConversion(sourceRef.type, targetName, mv)
            }
        } else {
            val wrapperClass = context.typeRegistry.getWrapperClassName(targetName) ?: "java/lang/Object"
            mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperClass)
            CoercionUtil.emitUnboxing(targetName, mv)
        }
    }

    /**
     * Emits conversion from object to boxed primitive (nullable result).
     */
    private fun emitObjectToBoxedConversion(
        targetType: ReturnType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val targetName = (targetType as ClassTypeRef).type
        val wrapperClass = context.typeRegistry.getWrapperClassName(targetName) ?: "java/lang/Object"
        mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperClass)
    }

    /**
     * Emits CHECKCAST for reference type conversions using TypeRegistry.
     */
    private fun emitReferenceTypeCast(
        targetType: ReturnType,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val targetRef = targetType as ClassTypeRef
        if (targetRef.type == "builtin.any") {
            return
        }

        val targetClass = context.typeRegistry.getJvmClassName(targetRef.type, false) ?: "java/lang/Object"
        mv.visitTypeInsn(Opcodes.CHECKCAST, targetClass)
    }

    /**
     * Checks if the cast is a no-op (source and target are the same type).
     */
    private fun isNoOpCast(sourceType: ReturnType, targetType: ReturnType): Boolean {
        if (sourceType !is ClassTypeRef || targetType !is ClassTypeRef) {
            return false
        }

        if (sourceType.type == targetType.type && !sourceType.isNullable && !targetType.isNullable) {
            return true
        }

        if (sourceType.type == "builtin.any" && targetType.type == "builtin.any") {
            return true
        }

        return false
    }

    /**
     * Determines if a cast will always succeed at compile time.
     */
    private fun canDetermineAlwaysTrue(
        sourceType: ReturnType,
        targetType: ReturnType,
        context: CompilationContext
    ): Boolean {
        if (sourceType !is ClassTypeRef || targetType !is ClassTypeRef) {
            return false
        }

        if (sourceType.type == targetType.type && !sourceType.isNullable) {
            return true
        }

        if (!sourceType.isNullable && context.typeRegistry.isSubtype(sourceType.type, targetType.type)) {
            return true
        }

        return false
    }

    /**
     * Determines if a cast will always fail at compile time.
     */
    private fun canDetermineAlwaysFalse(
        sourceType: ReturnType,
        targetType: ReturnType,
        context: CompilationContext
    ): Boolean {
        if (sourceType !is ClassTypeRef || targetType !is ClassTypeRef) {
            return false
        }

        val sourceIsPrimitive = CoercionUtil.isPrimitiveType(sourceType)
        val targetIsPrimitive = CoercionUtil.isPrimitiveType(targetType)

        if (sourceIsPrimitive && !sourceType.isNullable && targetIsPrimitive && !targetType.isNullable) {
            if (sourceType.type != targetType.type && !areConvertiblePrimitives(sourceType.type, targetType.type)) {
                return false
            }
        }

        return false
    }

    /**
     * Checks if two primitive types can be converted (numerics are convertible).
     */
    private fun areConvertiblePrimitives(source: String, target: String): Boolean {
        val numericTypes = setOf("builtin.int", "builtin.long", "builtin.float", "builtin.double")
        return source in numericTypes && target in numericTypes
    }

    /**
     * Ensures there's an object (not primitive) on the stack for instanceof checks.
     */
    private fun ensureObjectOnStack(sourceType: ReturnType, mv: MethodVisitor) {
        if (CoercionUtil.producesStackPrimitive(sourceType)) {
            boxPrimitive(sourceType, mv)
        }
    }

    /**
     * Boxes a primitive value on the stack.
     */
    private fun boxPrimitive(type: ReturnType, mv: MethodVisitor) {
        val typeName = (type as ClassTypeRef).type
        CoercionUtil.emitBoxing(typeName, mv)
    }

    /**
     * Pops a value from the stack, accounting for long/double taking 2 slots.
     */
    private fun popValue(type: ReturnType, mv: MethodVisitor) {
        if (type is ClassTypeRef && !type.isNullable) {
            when (type.type) {
                "builtin.long", "builtin.double" -> mv.visitInsn(Opcodes.POP2)
                else -> mv.visitInsn(Opcodes.POP)
            }
        } else {
            mv.visitInsn(Opcodes.POP)
        }
    }

    /**
     * Gets the JVM class name for INSTANCEOF checks using TypeRegistry.
     */
    private fun getInstanceOfClass(targetType: ReturnType, context: CompilationContext): String {
        val ref = targetType as ClassTypeRef
        
        val jvmClass = context.typeRegistry.getJvmClassName(ref.type, true)
        
        return jvmClass ?: "java/lang/Object"
    }
}
