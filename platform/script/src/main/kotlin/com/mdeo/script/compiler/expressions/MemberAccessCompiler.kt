package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedMemberAccessExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.compiler.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles member access expressions to bytecode.
 * 
 * Member access expressions access properties/fields on objects.
 * Supports null-safe chaining (?.) where if the expression is null,
 * the result is null instead of throwing a NullPointerException.
 * 
 * The compiler generates either:
 * - GETFIELD for direct field access
 * - INVOKEVIRTUAL/INVOKEINTERFACE for getter method calls
 * - INVOKESTATIC for helper classes (primitives/stdlib)
 */
class MemberAccessCompiler : ExpressionCompiler {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is a member access expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.MemberAccess
    }
    
    /**
     * Compiles a member access expression to bytecode.
     *
     * @param expression The typed member access expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val memberAccess = expression as TypedMemberAccessExpression
        val targetType = context.getType(memberAccess.expression.evalType)
        val resultType = context.getType(memberAccess.evalType)
        
        if (memberAccess.isNullChaining) {
            compileNullSafeMemberAccess(memberAccess, context, mv, targetType, resultType)
        } else {
            compileMemberAccess(memberAccess, context, mv, targetType, resultType)
        }
    }
    
    /**
     * Compiles a null-safe member access (?.).
     * If the expression is null, the result is null instead of throwing a NullPointerException.
     *
     * @param memberAccess The typed member access expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor used to emit bytecode instructions.
     * @param targetType The type of the object being accessed.
     * @param resultType The type of the member being accessed.
     */
    private fun compileNullSafeMemberAccess(
        memberAccess: TypedMemberAccessExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: com.mdeo.script.ast.types.ReturnType,
        resultType: com.mdeo.script.ast.types.ReturnType
    ) {
        val nullLabel = Label()
        val endLabel = Label()
        
        context.compileExpression(memberAccess.expression, mv)
        
        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNULL, nullLabel)
        
        emitMemberAccess(memberAccess.member, targetType, resultType, mv)
        
        if (resultType is ClassTypeRef && !resultType.isNullable && CoercionUtil.getPrimitiveTypeName(resultType) != null) {
            CoercionUtil.emitBoxing(resultType.type, mv)
        }
        
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
        
        mv.visitLabel(nullLabel)
        mv.visitInsn(Opcodes.POP)
        mv.visitInsn(Opcodes.ACONST_NULL)
        
        mv.visitLabel(endLabel)
    }
    
    /**
     * Compiles a regular (non-null-safe) member access.
     *
     * @param memberAccess The typed member access expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor used to emit bytecode instructions.
     * @param targetType The type of the object being accessed.
     * @param resultType The type of the member being accessed.
     */
    private fun compileMemberAccess(
        memberAccess: TypedMemberAccessExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: com.mdeo.script.ast.types.ReturnType,
        resultType: com.mdeo.script.ast.types.ReturnType
    ) {
        context.compileExpression(memberAccess.expression, mv)
        
        emitMemberAccess(memberAccess.member, targetType, resultType, mv)
    }
    
    /**
     * Emits the bytecode for accessing a member on the object on the stack.
     *
     * @param memberName The name of the member to access.
     * @param targetType The type of the object being accessed.
     * @param resultType The type of the member being accessed.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    private fun emitMemberAccess(
        memberName: String,
        targetType: com.mdeo.script.ast.types.ReturnType,
        resultType: com.mdeo.script.ast.types.ReturnType,
        mv: MethodVisitor
    ) {
        if (targetType !is ClassTypeRef) {
            throw UnsupportedOperationException("Cannot access member on non-class type")
        }
        
        when {
            CoercionUtil.isPrimitiveType(targetType.type) -> {
                emitPrimitiveMemberAccess(memberName, targetType, resultType, mv)
            }
            targetType.type == "builtin.string" -> {
                emitStringMemberAccess(memberName, resultType, mv)
            }
            isCollectionType(targetType.type) -> {
                emitCollectionMemberAccess(memberName, targetType, resultType, mv)
            }
            else -> {
                emitObjectMemberAccess(memberName, targetType, resultType, mv)
            }
        }
    }
    
    /**
     * Checks if a type is a collection type.
     *
     * @param typeName The fully qualified type name to check.
     * @return True if the type is a stdlib collection type (List, Set, Bag, OrderedSet, Map, Sequence), false otherwise.
     */
    private fun isCollectionType(typeName: String): Boolean {
        return typeName.startsWith("stdlib.") && typeName.substringAfter("stdlib.") in listOf(
            "List", "Set", "Bag", "OrderedSet", "Map", "Sequence"
        )
    }
    
    /**
     * Emits member access for primitive types using helper classes.
     *
     * @param memberName The name of the member to access.
     * @param targetType The primitive type reference of the object being accessed.
     * @param resultType The type of the member being accessed.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    private fun emitPrimitiveMemberAccess(
        memberName: String,
        targetType: ClassTypeRef,
        resultType: com.mdeo.script.ast.types.ReturnType,
        mv: MethodVisitor
    ) {
        val helperClass = getHelperClass(targetType.type)
        val descriptor = getPrimitiveMemberDescriptor(memberName, targetType, resultType)
        
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            helperClass,
            memberName,
            descriptor,
            false
        )
    }
    
    /**
     * Gets the helper class for a primitive type.
     *
     * @param typeName The fully qualified primitive type name.
     * @return The JVM internal name of the helper class for the primitive type.
     * @throws IllegalArgumentException If the type name is not a known primitive type.
     */
    private fun getHelperClass(typeName: String): String {
        return when (typeName) {
            "builtin.int" -> "com/mdeo/script/stdlib/primitives/IntHelper"
            "builtin.long" -> "com/mdeo/script/stdlib/primitives/LongHelper"
            "builtin.float" -> "com/mdeo/script/stdlib/primitives/FloatHelper"
            "builtin.double" -> "com/mdeo/script/stdlib/primitives/DoubleHelper"
            "builtin.boolean" -> "com/mdeo/script/stdlib/primitives/BooleanHelper"
            "builtin.string" -> "com/mdeo/script/stdlib/primitives/StringHelper"
            else -> throw IllegalArgumentException("Unknown primitive type: $typeName")
        }
    }
    
    /**
     * Gets the method descriptor for a primitive member access.
     *
     * @param memberName The name of the member being accessed.
     * @param targetType The primitive type reference of the object being accessed.
     * @param resultType The type of the member being accessed.
     * @return The JVM method descriptor for the helper method call.
     */
    private fun getPrimitiveMemberDescriptor(
        memberName: String,
        targetType: ClassTypeRef,
        resultType: com.mdeo.script.ast.types.ReturnType
    ): String {
        val paramDesc = getPrimitiveDescriptor(targetType.type)
        val returnDesc = getTypeDescriptor(resultType)
        return "($paramDesc)$returnDesc"
    }
    
    /**
     * Emits member access for string type.
     * Special cases like 'length' are handled directly, others delegate to StringHelper.
     *
     * @param memberName The name of the member to access.
     * @param resultType The type of the member being accessed.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    private fun emitStringMemberAccess(
        memberName: String,
        resultType: com.mdeo.script.ast.types.ReturnType,
        mv: MethodVisitor
    ) {
        when (memberName) {
            "length" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "length",
                    "()I",
                    false
                )
            }
            else -> {
                val returnDesc = getTypeDescriptor(resultType)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/mdeo/script/stdlib/primitives/StringHelper",
                    memberName,
                    "(Ljava/lang/String;)$returnDesc",
                    false
                )
            }
        }
    }
    
    /**
     * Emits member access for collection types.
     *
     * @param memberName The name of the member to access.
     * @param targetType The collection type reference of the object being accessed.
     * @param resultType The type of the member being accessed.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    private fun emitCollectionMemberAccess(
        memberName: String,
        targetType: ClassTypeRef,
        resultType: com.mdeo.script.ast.types.ReturnType,
        mv: MethodVisitor
    ) {
        val collectionInterface = getCollectionInterface(targetType.type)
        val returnDesc = getTypeDescriptor(resultType)
        
        when (memberName) {
            "size" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    collectionInterface,
                    "size",
                    "()I",
                    true
                )
            }
            "isEmpty" -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    collectionInterface,
                    "isEmpty",
                    "()Z",
                    true
                )
            }
            else -> {
                mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    collectionInterface,
                    memberName,
                    "()$returnDesc",
                    true
                )
            }
        }
    }
    
    /**
     * Gets the collection interface name for a stdlib collection type.
     *
     * @param typeName The fully qualified stdlib collection type name.
     * @return The JVM internal name of the collection interface.
     */
    private fun getCollectionInterface(typeName: String): String {
        val collectionName = typeName.substringAfter("stdlib.")
        return "com/mdeo/script/stdlib/collections/Script$collectionName"
    }
    
    /**
     * Emits member access for general object types.
     * Uses getter methods by convention (getXxx for property xxx).
     *
     * @param memberName The name of the member to access.
     * @param targetType The class type reference of the object being accessed.
     * @param resultType The type of the member being accessed.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    private fun emitObjectMemberAccess(
        memberName: String,
        targetType: ClassTypeRef,
        resultType: com.mdeo.script.ast.types.ReturnType,
        mv: MethodVisitor
    ) {
        val getterName = "get${memberName.replaceFirstChar { it.uppercaseChar() }}"
        val returnDesc = getTypeDescriptor(resultType)
        val ownerClass = typeToInternalName(targetType.type)
        
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            ownerClass,
            getterName,
            "()$returnDesc",
            false
        )
    }
    
    /**
     * Gets the primitive descriptor for a primitive type.
     *
     * @param typeName The fully qualified primitive type name.
     * @return The JVM type descriptor for the primitive type.
     */
    private fun getPrimitiveDescriptor(typeName: String): String {
        return when (typeName) {
            "builtin.int" -> "I"
            "builtin.long" -> "J"
            "builtin.float" -> "F"
            "builtin.double" -> "D"
            "builtin.boolean" -> "Z"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Gets the type descriptor for a return type.
     * Handles nullable types by returning boxed type descriptors.
     *
     * @param type The return type to get a descriptor for.
     * @return The JVM type descriptor for the type.
     */
    private fun getTypeDescriptor(type: com.mdeo.script.ast.types.ReturnType): String {
        if (type !is ClassTypeRef) return "Ljava/lang/Object;"
        
        return when (type.type) {
            "builtin.int" -> if (type.isNullable) "Ljava/lang/Integer;" else "I"
            "builtin.long" -> if (type.isNullable) "Ljava/lang/Long;" else "J"
            "builtin.float" -> if (type.isNullable) "Ljava/lang/Float;" else "F"
            "builtin.double" -> if (type.isNullable) "Ljava/lang/Double;" else "D"
            "builtin.boolean" -> if (type.isNullable) "Ljava/lang/Boolean;" else "Z"
            "builtin.string" -> "Ljava/lang/String;"
            else -> "L${typeToInternalName(type.type)};"
        }
    }
    
    /**
     * Converts a type name to a JVM internal class name.
     * Handles stdlib types specially by mapping them to the appropriate package.
     *
     * @param typeName The fully qualified type name to convert.
     * @return The JVM internal class name (with slashes instead of dots).
     */
    private fun typeToInternalName(typeName: String): String {
        return when {
            typeName.startsWith("stdlib.") -> {
                val name = typeName.substringAfter("stdlib.")
                "com/mdeo/script/stdlib/collections/Script$name"
            }
            else -> typeName.replace(".", "/")
        }
    }
}
