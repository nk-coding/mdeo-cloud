package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.util.TypeConversionUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Helper for compiling equality binary operations.
 * 
 * Handles the following equality operators:
 * - Structural equality (==): Uses .equals() for objects (Kotlin semantics)
 * - Structural inequality (!=): Negation of ==
 * - Strict/reference equality (===): Reference comparison for objects
 * - Strict/reference inequality (!==): Negation of ===
 * 
 * Equality comparisons are context-sensitive based on operand types:
 * - Null literals: compile-time evaluation when both are null
 * - Null checks: IFNULL/IFNONNULL for one null operand
 * - Strings: uses String.equals() or Objects.equals() for nullable strings
 * - Booleans: XOR for primitives, Objects.equals() for nullable
 * - Numerics: type promotion and primitive comparison
 * - References (==, !=): uses Objects.equals() for null-safe .equals() comparison
 * - References (===, !==): reference equality using IF_ACMPEQ/IF_ACMPNE
 * 
 * Special handling for nullable numeric types:
 * - Objects.equals() doesn't work correctly for mixed numeric types
 *   (e.g., Integer.equals(Long) returns false even for equal values)
 * - This helper properly unboxes and promotes values for comparison
 * 
 * @see TypeConversionUtil for type promotion logic
 * @see CoercionUtil for boxing/unboxing operations
 */
object EqualityOperationHelper {

    /**
     * Compiles a structural equality (==) or inequality (!=) operation.
     * 
     * Uses .equals() for object comparison (Kotlin semantics).
     * Dispatches to specialized methods based on operand types.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param isEquals true for ==, false for !=
     */
    fun compileEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        isEquals: Boolean
    ) {
        val leftIsNull = CoercionUtil.isNullLiteral(expr.left)
        val rightIsNull = CoercionUtil.isNullLiteral(expr.right)
        
        if (leftIsNull && rightIsNull) {
            mv.visitInsn(if (isEquals) Opcodes.ICONST_1 else Opcodes.ICONST_0)
            return
        }
        
        if (leftIsNull || rightIsNull) {
            compileNullComparison(expr, context, mv, leftIsNull, isEquals)
            return
        }
        
        if (TypeConversionUtil.isStringType(leftType) || TypeConversionUtil.isStringType(rightType)) {
            compileStringEquality(expr, context, mv, leftType, rightType, isEquals)
            return
        }
        
        if (TypeConversionUtil.isBooleanType(leftType) || TypeConversionUtil.isBooleanType(rightType)) {
            compileBooleanEquality(expr, context, mv, leftType, rightType, isEquals)
            return
        }
        
        if (isNumericOrNullableNumeric(leftType) && isNumericOrNullableNumeric(rightType)) {
            compileNumericEquality(expr, context, mv, leftType, rightType, isEquals)
            return
        }
        
        compileObjectEquality(expr, context, mv, isEquals)
    }

    /**
     * Compiles a strict/reference equality (===) or inequality (!==) operation.
     * 
     * Uses reference comparison for objects (like Java's ==).
     * Dispatches to specialized methods based on operand types.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param isEquals true for ===, false for !==
     */
    fun compileStrictEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        isEquals: Boolean
    ) {
        val leftIsNull = CoercionUtil.isNullLiteral(expr.left)
        val rightIsNull = CoercionUtil.isNullLiteral(expr.right)
        
        if (leftIsNull && rightIsNull) {
            mv.visitInsn(if (isEquals) Opcodes.ICONST_1 else Opcodes.ICONST_0)
            return
        }
        
        if (leftIsNull || rightIsNull) {
            compileNullComparison(expr, context, mv, leftIsNull, isEquals)
            return
        }
        
        if (TypeConversionUtil.isBooleanType(leftType) && TypeConversionUtil.isBooleanType(rightType) &&
            !isNullableType(leftType) && !isNullableType(rightType)) {
            compileBooleanEquality(expr, context, mv, leftType, rightType, isEquals)
            return
        }
        
        if (isNumericOrNullableNumeric(leftType) && isNumericOrNullableNumeric(rightType) &&
            !isNullableType(leftType) && !isNullableType(rightType)) {
            compileNumericEquality(expr, context, mv, leftType, rightType, isEquals)
            return
        }
        
        compileReferenceEquality(expr, context, mv, isEquals)
    }

    /**
     * Checks if a type is nullable.
     *
     * @param type The type to check
     * @return true if the type is nullable
     */
    private fun isNullableType(type: ReturnType): Boolean {
        return type is ClassTypeRef && type.isNullable
    }

    /**
     * Checks if a type is numeric or a nullable numeric wrapper.
     *
     * @param type The type to check
     * @return true if the type is a numeric primitive or nullable numeric wrapper, false otherwise
     */
    private fun isNumericOrNullableNumeric(type: ReturnType): Boolean {
        if (type !is ClassTypeRef) return false
        return type.`package` == "builtin" && type.type in setOf("int", "long", "float", "double")
    }

    /**
     * Compiles comparison where one operand is null.
     * 
     * Evaluates the non-null expression and compares with IFNULL/IFNONNULL.
     * For primitive types, the comparison always results in false (for ==)
     * since primitives cannot be null.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftIsNull true if the left operand is the null literal
     * @param isEquals true for ==, false for !=
     */
    private fun compileNullComparison(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftIsNull: Boolean,
        isEquals: Boolean
    ) {
        val trueLabel = Label()
        val endLabel = Label()
        
        val nonNullExpr = if (leftIsNull) expr.right else expr.left
        val exprType = context.getType(nonNullExpr.evalType)
        context.compileExpression(nonNullExpr, mv, exprType)
        if (CoercionUtil.producesStackPrimitive(exprType, nonNullExpr)) {
            popValue(exprType, mv)
            mv.visitInsn(if (isEquals) Opcodes.ICONST_0 else Opcodes.ICONST_1)
            return
        }
        
        val jumpOpcode = if (isEquals) Opcodes.IFNULL else Opcodes.IFNONNULL
        mv.visitJumpInsn(jumpOpcode, trueLabel)
        
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        
        mv.visitLabel(endLabel)
    }

    /**
     * Pops a value off the stack based on its type.
     *
     * Uses POP2 for 2-slot types (long, double) and POP for 1-slot types.
     *
     * @param type The type of the value on the stack, used to determine stack slot size
     * @param mv The ASM method visitor to emit the pop instruction
     */
    private fun popValue(type: ReturnType, mv: MethodVisitor) {
        val size = ASMUtil.getStackSize(type)
        if (size == 2) {
            mv.visitInsn(Opcodes.POP2)
        } else {
            mv.visitInsn(Opcodes.POP)
        }
    }

    /**
     * Compiles string equality using .equals() method.
     * 
     * For nullable strings, uses Objects.equals() for null-safe comparison.
     * For non-null strings, uses String.equals() directly.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param isEquals true for ==, false for !=
     */
    private fun compileStringEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        isEquals: Boolean
    ) {
        val leftMayBeNull = leftType is ClassTypeRef && leftType.isNullable
        val rightMayBeNull = rightType is ClassTypeRef && rightType.isNullable
        
        if (leftMayBeNull || rightMayBeNull) {
            context.compileExpression(expr.left, mv, leftType)
            context.compileExpression(expr.right, mv, rightType)
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/Objects",
                "equals",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                false
            )
        } else {
            context.compileExpression(expr.left, mv, leftType)
            context.compileExpression(expr.right, mv, rightType)
            mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "equals",
                "(Ljava/lang/Object;)Z",
                false
            )
        }
        
        if (!isEquals) {
            invertBoolean(mv)
        }
    }

    /**
     * Compiles boolean equality.
     * 
     * For primitive booleans, uses XOR to check equality.
     * For nullable Boolean wrappers, uses Objects.equals().
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param isEquals true for ==, false for !=
     */
    private fun compileBooleanEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        isEquals: Boolean
    ) {
        val leftIsNullable = leftType is ClassTypeRef && leftType.isNullable
        val rightIsNullable = rightType is ClassTypeRef && rightType.isNullable
        
        if (leftIsNullable || rightIsNullable) {
            context.compileExpression(expr.left, mv, leftType)
            if (!leftIsNullable) {
                CoercionUtil.emitBoxing(ClassTypeRef("builtin", "boolean", false), mv)
            }
            context.compileExpression(expr.right, mv, rightType)
            if (!rightIsNullable) {
                CoercionUtil.emitBoxing(ClassTypeRef("builtin", "boolean", false), mv)
            }
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/Objects",
                "equals",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                false
            )
            
            if (!isEquals) {
                invertBoolean(mv)
            }
        } else {
            context.compileExpression(expr.left, mv, leftType)
            context.compileExpression(expr.right, mv, rightType)
            
            val trueLabel = Label()
            val endLabel = Label()
            
            mv.visitInsn(Opcodes.IXOR)
            val jumpOpcode = if (isEquals) Opcodes.IFEQ else Opcodes.IFNE
            mv.visitJumpInsn(jumpOpcode, trueLabel)
            
            mv.visitInsn(Opcodes.ICONST_0)
            mv.visitJumpInsn(Opcodes.GOTO, endLabel)
            
            mv.visitLabel(trueLabel)
            mv.visitInsn(Opcodes.ICONST_1)
            
            mv.visitLabel(endLabel)
        }
    }

    /**
     * Compiles numeric equality.
     * 
     * Handles primitives, nullable wrappers, and mixed types with proper coercion.
     * 
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param isEquals true for ==, false for !=
     */
    private fun compileNumericEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        isEquals: Boolean
    ) {
        val leftIsNullable = leftType is ClassTypeRef && leftType.isNullable
        val rightIsNullable = rightType is ClassTypeRef && rightType.isNullable
        
        if (leftIsNullable || rightIsNullable) {
            compileNullableNumericEquality(expr, context, mv, leftType, rightType, leftIsNullable, rightIsNullable, isEquals)
            return
        }
        
        compilePrimitiveNumericEquality(expr, context, mv, leftType, rightType, isEquals)
    }

    /**
     * Compiles primitive numeric equality with type promotion.
     *
     * Promotes both operands to a common type and compares using
     * the appropriate JVM instructions.
     *
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param isEquals true for ==, false for !=
     */
    private fun compilePrimitiveNumericEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        isEquals: Boolean
    ) {
        val leftTypeName = (leftType as ClassTypeRef).type
        val rightTypeName = (rightType as ClassTypeRef).type
        val promotedType = TypeConversionUtil.getPromotedType(leftTypeName, rightTypeName)
        
        context.compileExpression(expr.left, mv, leftType)
        TypeConversionUtil.emitConversion(leftTypeName, promotedType, mv)
        
        context.compileExpression(expr.right, mv, rightType)
        TypeConversionUtil.emitConversion(rightTypeName, promotedType, mv)
        
        val trueLabel = Label()
        val endLabel = Label()
        
        when (promotedType) {
            "int" -> {
                val jumpOpcode = if (isEquals) Opcodes.IF_ICMPEQ else Opcodes.IF_ICMPNE
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
            "long" -> {
                mv.visitInsn(Opcodes.LCMP)
                val jumpOpcode = if (isEquals) Opcodes.IFEQ else Opcodes.IFNE
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
            "float" -> {
                mv.visitInsn(Opcodes.FCMPL)
                val jumpOpcode = if (isEquals) Opcodes.IFEQ else Opcodes.IFNE
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
            "double" -> {
                mv.visitInsn(Opcodes.DCMPL)
                val jumpOpcode = if (isEquals) Opcodes.IFEQ else Opcodes.IFNE
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
        }
        
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        
        mv.visitLabel(endLabel)
    }

    /**
     * Compiles nullable numeric equality with proper null handling.
     *
     * Logic:
     * - If both are null, return true (for ==)
     * - If one is null and other is not, return false (for ==)
     * - If both are non-null, unbox and compare as primitives
     *
     * This handles mixed numeric types correctly by promoting values
     * before comparison, unlike Objects.equals() which would return false
     * for Integer(42) == Long(42L).
     *
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The resolved type of the left operand
     * @param rightType The resolved type of the right operand
     * @param leftIsNullable true if the left operand is a nullable type
     * @param rightIsNullable true if the right operand is a nullable type
     * @param isEquals true for ==, false for !=
     */
    private fun compileNullableNumericEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ReturnType,
        rightType: ReturnType,
        leftIsNullable: Boolean,
        rightIsNullable: Boolean,
        isEquals: Boolean
    ) {
        val leftTypeName = (leftType as ClassTypeRef).type
        val rightTypeName = (rightType as ClassTypeRef).type
        
        if (leftIsNullable && !rightIsNullable) {
            compileOneNullableNumericEquality(expr, context, mv, leftType, rightType,
                true, isEquals)
            return
        }
        
        if (!leftIsNullable && rightIsNullable) {
            compileOneNullableNumericEquality(expr, context, mv, leftType, rightType,
                false, isEquals)
            return
        }
        
        val promotedType = TypeConversionUtil.getPromotedType(leftTypeName, rightTypeName)
        
        val leftNullCheckRightLabel = Label()
        val leftNotNullRightNullLabel = Label()
        val equalLabel = Label()
        val notEqualLabel = Label()
        val endLabel = Label()
        
        context.compileExpression(expr.left, mv, leftType)
        context.compileExpression(expr.right, mv, rightType)
        
        mv.visitInsn(Opcodes.SWAP)
        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNULL, leftNullCheckRightLabel)
        
        mv.visitInsn(Opcodes.SWAP)
        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNULL, leftNotNullRightNullLabel)
        
        CoercionUtil.emitUnboxing(rightType, mv)
        TypeConversionUtil.emitConversion(rightTypeName, promotedType, mv)
        
        when (promotedType) {
            "long", "double" -> {
                mv.visitInsn(Opcodes.DUP2_X1)
                mv.visitInsn(Opcodes.POP2)
            }
            else -> {
                mv.visitInsn(Opcodes.SWAP)
            }
        }
        
        CoercionUtil.emitUnboxing(leftType, mv)
        TypeConversionUtil.emitConversion(leftTypeName, promotedType, mv)
        emitSwap(promotedType, mv)
        
        emitPrimitiveComparison(promotedType, Opcodes.IFEQ, equalLabel, mv)
        mv.visitJumpInsn(Opcodes.GOTO, notEqualLabel)
        
        mv.visitLabel(leftNullCheckRightLabel)
        mv.visitInsn(Opcodes.POP)
        mv.visitJumpInsn(Opcodes.IFNULL, equalLabel)
        mv.visitJumpInsn(Opcodes.GOTO, notEqualLabel)
        
        mv.visitLabel(leftNotNullRightNullLabel)
        mv.visitInsn(Opcodes.POP)
        mv.visitInsn(Opcodes.POP)
        mv.visitJumpInsn(Opcodes.GOTO, notEqualLabel)
        
        mv.visitLabel(equalLabel)
        mv.visitInsn(if (isEquals) Opcodes.ICONST_1 else Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(notEqualLabel)
        mv.visitInsn(if (isEquals) Opcodes.ICONST_0 else Opcodes.ICONST_1)
        
        mv.visitLabel(endLabel)
    }

    /**
     * Compiles equality where one operand is nullable and one is primitive.
     *
     * If the nullable operand is null, returns false for == and true for !=.
     * Otherwise unboxes the nullable operand and compares as primitives with
     * proper type promotion.
     *
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param leftType The ClassTypeRef of the left operand
     * @param rightType The ClassTypeRef of the right operand
     * @param leftIsNullable true if the left operand is the nullable one
     * @param isEquals true for ==, false for !=
     */
    private fun compileOneNullableNumericEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        leftType: ClassTypeRef,
        rightType: ClassTypeRef,
        leftIsNullable: Boolean,
        isEquals: Boolean
    ) {
        val leftTypeName = leftType.type
        val rightTypeName = rightType.type
        val promotedType = TypeConversionUtil.getPromotedType(leftTypeName, rightTypeName)
        
        if (leftIsNullable) {
            val falseLabel = Label()
            val trueLabel = Label()
            val endLabel = Label()
            
            val leftExprType = context.getType(expr.left.evalType)
            context.compileExpression(expr.left, mv, leftExprType)
            mv.visitInsn(Opcodes.DUP)
            mv.visitJumpInsn(Opcodes.IFNULL, falseLabel)
            
            CoercionUtil.emitUnboxing(leftType, mv)
            TypeConversionUtil.emitConversion(leftTypeName, promotedType, mv)
            
            val rightType2 = context.getType(expr.right.evalType)
            context.compileExpression(expr.right, mv, rightType2)
            TypeConversionUtil.emitConversion(rightTypeName, promotedType, mv)
            
            emitPrimitiveComparison(promotedType, if (isEquals) Opcodes.IFEQ else Opcodes.IFNE, trueLabel, mv)
            
            mv.visitInsn(Opcodes.ICONST_0)
            mv.visitJumpInsn(Opcodes.GOTO, endLabel)
            
            mv.visitLabel(falseLabel)
            mv.visitInsn(Opcodes.POP)
            mv.visitInsn(if (isEquals) Opcodes.ICONST_0 else Opcodes.ICONST_1)
            mv.visitJumpInsn(Opcodes.GOTO, endLabel)
            
            mv.visitLabel(trueLabel)
            mv.visitInsn(Opcodes.ICONST_1)
            
            mv.visitLabel(endLabel)
        } else {
            val falseLabel = Label()
            val trueLabel = Label()
            val endLabel = Label()
            
            val rightExprType = context.getType(expr.right.evalType)
            context.compileExpression(expr.right, mv, rightExprType)
            mv.visitInsn(Opcodes.DUP)
            mv.visitJumpInsn(Opcodes.IFNULL, falseLabel)
            
            CoercionUtil.emitUnboxing(rightType, mv)
            TypeConversionUtil.emitConversion(rightTypeName, promotedType, mv)
            
            val leftType2 = context.getType(expr.left.evalType)
            context.compileExpression(expr.left, mv, leftType2)
            TypeConversionUtil.emitConversion(leftTypeName, promotedType, mv)
            
            emitSwap(promotedType, mv)
            
            emitPrimitiveComparison(promotedType, if (isEquals) Opcodes.IFEQ else Opcodes.IFNE, trueLabel, mv)
            
            mv.visitInsn(Opcodes.ICONST_0)
            mv.visitJumpInsn(Opcodes.GOTO, endLabel)
            
            mv.visitLabel(falseLabel)
            mv.visitInsn(Opcodes.POP)
            mv.visitInsn(if (isEquals) Opcodes.ICONST_0 else Opcodes.ICONST_1)
            mv.visitJumpInsn(Opcodes.GOTO, endLabel)
            
            mv.visitLabel(trueLabel)
            mv.visitInsn(Opcodes.ICONST_1)
            
            mv.visitLabel(endLabel)
        }
    }

    /**
     * Emits primitive comparison and jumps to trueLabel if condition is met.
     *
     * For int types, uses ISUB followed by the jump opcode.
     * For long, float, and double types, uses the appropriate CMP instruction
     * followed by the jump opcode.
     *
     * @param typeName The primitive type name (builtin.int, builtin.long, builtin.float, builtin.double)
     * @param jumpOpcode The jump opcode to use (e.g., IFEQ, IFNE)
     * @param trueLabel The label to jump to if the condition is met
     * @param mv The ASM method visitor
     */
    private fun emitPrimitiveComparison(typeName: String, jumpOpcode: Int, trueLabel: Label, mv: MethodVisitor) {
        when (typeName) {
            "int" -> {
                mv.visitInsn(Opcodes.ISUB)
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
            "long" -> {
                mv.visitInsn(Opcodes.LCMP)
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
            "float" -> {
                mv.visitInsn(Opcodes.FCMPL)
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
            "double" -> {
                mv.visitInsn(Opcodes.DCMPL)
                mv.visitJumpInsn(jumpOpcode, trueLabel)
            }
        }
    }

    /**
     * Emits swap instruction appropriate for the type size.
     *
     * For 1-slot types (int, float), uses the SWAP instruction directly.
     * For 2-slot types (long, double), uses DUP2_X2 followed by POP2 to achieve
     * the swap since JVM doesn't have a native 2-slot swap instruction.
     *
     * @param typeName The primitive type name determining stack slot size
     * @param mv The ASM method visitor
     */
    private fun emitSwap(typeName: String, mv: MethodVisitor) {
        when (typeName) {
            "long", "double" -> {
                mv.visitInsn(Opcodes.DUP2_X2)
                mv.visitInsn(Opcodes.POP2)
            }
            else -> {
                mv.visitInsn(Opcodes.SWAP)
            }
        }
    }

    /**
     * Compiles object equality using .equals() method.
     *
     * Uses Objects.equals() for null-safe comparison that calls the object's
     * equals() method. This implements Kotlin's == semantics for objects.
     *
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param isEquals true for ==, false for !=
     */
    private fun compileObjectEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        isEquals: Boolean
    ) {
        val leftType = context.getType(expr.left.evalType)
        val rightType = context.getType(expr.right.evalType)
        context.compileExpression(expr.left, mv, leftType)
        context.compileExpression(expr.right, mv, rightType)
        
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/util/Objects",
            "equals",
            "(Ljava/lang/Object;Ljava/lang/Object;)Z",
            false
        )
        
        if (!isEquals) {
            invertBoolean(mv)
        }
    }

    /**
     * Compiles reference equality for non-primitive types.
     *
     * Uses IF_ACMPEQ/IF_ACMPNE for object reference comparison.
     *
     * @param expr The binary expression AST node
     * @param context The compilation context
     * @param mv The ASM method visitor
     * @param isEquals true for ==, false for !=
     */
    private fun compileReferenceEquality(
        expr: TypedBinaryExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        isEquals: Boolean
    ) {
        val leftType = context.getType(expr.left.evalType)
        val rightType = context.getType(expr.right.evalType)
        context.compileExpression(expr.left, mv, leftType)
        context.compileExpression(expr.right, mv, rightType)
        
        val trueLabel = Label()
        val endLabel = Label()
        
        val jumpOpcode = if (isEquals) Opcodes.IF_ACMPEQ else Opcodes.IF_ACMPNE
        mv.visitJumpInsn(jumpOpcode, trueLabel)
        
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(trueLabel)
        mv.visitInsn(Opcodes.ICONST_1)
        
        mv.visitLabel(endLabel)
    }

    /**
     * Inverts a boolean value on the stack.
     *
     * XOR with 1 to invert: 0 -> 1, 1 -> 0.
     *
     * @param mv The ASM method visitor
     */
    private fun invertBoolean(mv: MethodVisitor) {
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IXOR)
    }
}
